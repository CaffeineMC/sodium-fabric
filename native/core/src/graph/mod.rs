use std::collections::VecDeque;
use std::fmt::Debug;
use std::intrinsics::{prefetch_read_data, prefetch_write_data};
use std::marker::PhantomData;
use std::mem::transmute;
use std::ops::*;
use std::vec::Vec;

use core_simd::simd::Which::*;
use core_simd::simd::*;
use local::LocalCoordinateContext;
use std_float::StdFloat;

use crate::collections::ArrayDeque;
use crate::ffi::{CInlineVec, CVec};
use crate::graph::local::*;
use crate::graph::octree::LinearBitOctree;
use crate::math::*;

pub mod local;
mod octree;

pub const REGION_COORD_MASK: u8x3 = u8x3::from_array([0b11111000, 0b11111100, 0b11111000]);
pub const SECTIONS_IN_REGION: usize = 8 * 4 * 8;
pub const SECTIONS_IN_GRAPH: usize = 256 * 256 * 256;

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct RegionSectionIndex(u8);

impl RegionSectionIndex {
    const X_MASK_SINGLE: u8 = 0b00000111;
    const Y_MASK_SINGLE: u8 = 0b00000011;
    const Z_MASK_SINGLE: u8 = 0b00000111;

    const X_MASK_SHIFT: u8 = 5;
    const Y_MASK_SHIFT: u8 = 3;
    const Z_MASK_SHIFT: u8 = 0;

    #[inline(always)]
    pub fn from_local(local_section_coord: u8x3) -> Self {
        Self(
            (local_section_coord
                & u8x3::from_array([
                    Self::X_MASK_SINGLE,
                    Self::Y_MASK_SINGLE,
                    Self::Z_MASK_SINGLE,
                ]) << u8x3::from_array([
                    Self::X_MASK_SHIFT,
                    Self::Y_MASK_SHIFT,
                    Self::Z_MASK_SHIFT,
                ]))
            .reduce_or(),
        )
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
#[repr(u8)]
pub enum GraphDirection {
    NegX = 0,
    NegY = 1,
    NegZ = 2,
    PosX = 3,
    PosY = 4,
    PosZ = 5,
}

impl GraphDirection {
    pub const ORDERED: [GraphDirection; 6] = [
        GraphDirection::NegX,
        GraphDirection::NegY,
        GraphDirection::NegZ,
        GraphDirection::PosX,
        GraphDirection::PosY,
        GraphDirection::PosZ,
    ];

    #[inline(always)]
    pub const fn opposite(&self) -> GraphDirection {
        match self {
            GraphDirection::NegX => GraphDirection::PosX,
            GraphDirection::NegY => GraphDirection::PosY,
            GraphDirection::NegZ => GraphDirection::PosZ,
            GraphDirection::PosX => GraphDirection::NegX,
            GraphDirection::PosY => GraphDirection::NegY,
            GraphDirection::PosZ => GraphDirection::NegZ,
        }
    }

    /// SAFETY: if out of bounds, this will fail to assert in debug mode
    #[inline(always)]
    pub unsafe fn from_int_unchecked(val: u8) -> Self {
        debug_assert!(val <= 5);
        transmute(val)
    }
}

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct GraphDirectionSet(u8);

impl GraphDirectionSet {
    #[inline(always)]
    pub fn from(packed: u8) -> Self {
        GraphDirectionSet(packed)
    }

    #[inline(always)]
    pub fn none() -> GraphDirectionSet {
        GraphDirectionSet(0)
    }

    #[inline(always)]
    pub fn all() -> GraphDirectionSet {
        let mut set = GraphDirectionSet::none();

        for dir in GraphDirection::ORDERED {
            set.add(dir);
        }

        set
    }

    #[inline(always)]
    pub fn single(direction: GraphDirection) -> GraphDirectionSet {
        let mut set = GraphDirectionSet::none();
        set.add(direction);
        set
    }

    #[inline(always)]
    pub fn add(&mut self, dir: GraphDirection) {
        self.0 |= 1 << dir as u8;
    }

    #[inline(always)]
    pub fn add_all(&mut self, set: GraphDirectionSet) {
        self.0 |= set.0;
    }

    #[inline(always)]
    pub fn contains(&self, dir: GraphDirection) -> bool {
        (self.0 & (1 << dir as u8)) != 0
    }

    #[inline(always)]
    pub fn is_empty(&self) -> bool {
        self.0 == 0
    }
}

impl Default for GraphDirectionSet {
    fn default() -> Self {
        GraphDirectionSet::none()
    }
}

impl BitAnd for GraphDirectionSet {
    type Output = GraphDirectionSet;

    fn bitand(self, rhs: Self) -> Self::Output {
        GraphDirectionSet(self.0 & rhs.0)
    }
}

impl IntoIterator for GraphDirectionSet {
    type Item = GraphDirection;
    type IntoIter = GraphDirectionSetIter;

    fn into_iter(self) -> Self::IntoIter {
        GraphDirectionSetIter(self.0)
    }
}

#[repr(transparent)]
pub struct GraphDirectionSetIter(u8);

impl Iterator for GraphDirectionSetIter {
    type Item = GraphDirection;

    #[inline(always)]
    fn next(&mut self) -> Option<Self::Item> {
        // Description of the iteration approach on daniel lemire's blog
        // https://lemire.me/blog/2018/02/21/iterating-over-set-bits-quickly/
        if self.0 != 0 {
            // SAFETY: the result from a valid GraphDirectionSet value should never be out of bounds
            let direction =
                unsafe { GraphDirection::from_int_unchecked(self.0.trailing_zeros() as u8) };
            self.0 &= (self.0 - 1);
            Some(direction)
        } else {
            None
        }
    }
}

// todo: should the top bit signify if it's populated or not?
#[derive(Default, Clone, Copy)]
#[repr(transparent)]
pub struct VisibilityData(u16);

impl VisibilityData {
    #[inline(always)]
    pub fn pack(mut raw: u64) -> Self {
        raw >>= 6;
        let mut packed = (raw & 0b1) as u16;
        raw >>= 5;
        packed |= (raw & 0b110) as u16;
        raw >>= 4;
        packed |= (raw & 0b111000) as u16;
        raw >>= 3;
        packed |= (raw & 0b1111000000) as u16;
        raw >>= 2;
        packed |= (raw & 0b111110000000000) as u16;

        VisibilityData(packed)
    }

    #[inline(always)]
    pub fn get_outgoing_directions(&self, incoming: GraphDirectionSet) -> GraphDirectionSet {
        // extend everything to u32s because we can shift them faster on x86 without avx512
        let vis_bits = Simd::<u32, 5>::splat(self.0 as u32);
        let in_bits = Simd::<u32, 5>::splat(incoming.0 as u32);

        let rows_cols = (vis_bits >> Simd::from_array([0_u32, 1_u32, 3_u32, 6_u32, 10_u32])).cast()
            & Simd::from_array([0b1_u32, 0b11_u32, 0b111_u32, 0b1111_u32, 0b11111_u32]);

        let rows = (rows_cols & in_bits)
            .cast::<i32>()
            .simd_ne(Simd::splat(0))
            .select(
                Simd::from_array([0b10_u32, 0b100_u32, 0b1000_u32, 0b10000_u32, 0b100000_u32]),
                Simd::splat(0_u32),
            );

        let cols = ((in_bits
            << Simd::from_array([
                u32::BITS - 2,
                u32::BITS - 3,
                u32::BITS - 4,
                u32::BITS - 5,
                u32::BITS - 6,
            ]))
        .cast::<i32>()
            >> Simd::splat((u32::BITS - 1) as i32))
        .cast::<u32>()
            & rows_cols;

        // extend to po2 vectors to make the reduction happy
        let outgoing_bits = simd_swizzle!(
            rows,
            cols,
            [
                First(0),
                First(1),
                First(2),
                First(3),
                First(4),
                First(4),
                First(4),
                First(4),
                Second(0),
                Second(1),
                Second(2),
                Second(3),
                Second(4),
                Second(4),
                Second(4),
                Second(4),
            ]
        )
        .reduce_or() as u8; // & !incoming

        GraphDirectionSet(outgoing_bits)
    }
}

pub struct GraphSearchState {
    incoming: [GraphDirectionSet; SECTIONS_IN_GRAPH],
    // TODO: figure out a way to calculate a smaller value
    queue: ArrayDeque<LocalNodeIndex<1>, SECTIONS_IN_GRAPH>,

    enqueued: bool,
}

impl GraphSearchState {
    pub fn enqueue(&mut self, index: LocalNodeIndex<1>, incoming_direction: GraphDirection) {
        // SAFETY: LocalNodeIndex should never have the top 8 bits set, and the array is exactly
        // 2^24 elements long.
        let node_incoming_directions =
            unsafe { self.incoming.get_unchecked_mut(index.as_array_offset()) };
        let should_enqueue = node_incoming_directions.is_empty();

        node_incoming_directions.add(incoming_direction);

        unsafe {
            self.queue
                .push_conditionally_unchecked(index, should_enqueue);
        }
    }

    fn reset(&mut self) {
        self.queue.reset();
        self.incoming.fill(GraphDirectionSet::none());

        self.enqueued = false;
    }
}

impl Default for GraphSearchState {
    fn default() -> Self {
        Self {
            queue: Default::default(),
            incoming: [GraphDirectionSet::default(); SECTIONS_IN_GRAPH],
            enqueued: false,
        }
    }
}

pub struct Graph {
    section_is_populated_bits: LinearBitOctree,
    section_is_visible_bits: LinearBitOctree,

    section_visibility_bit_sets: [VisibilityData; SECTIONS_IN_GRAPH],
}

impl Graph {
    pub fn new() -> Self {
        Graph {
            section_is_populated_bits: Default::default(),
            section_is_visible_bits: Default::default(),
            section_visibility_bit_sets: [Default::default(); SECTIONS_IN_GRAPH],
        }
    }

    pub fn cull(&mut self, context: &LocalCoordinateContext, no_occlusion_cull: bool) {
        self.section_is_visible_bits.clear();

        self.frustum_and_fog_cull(context);
    }

    // #[no_mangle]
    fn frustum_and_fog_cull(&mut self, context: &LocalCoordinateContext) {
        let mut level_3_index = context.iter_node_origin_index;

        // this could go more linear in memory prolly, but eh
        for _x in 0..context.level_3_node_iters.x() {
            for _y in 0..context.level_3_node_iters.y() {
                for _z in 0..context.level_3_node_iters.z() {
                    self.check_node(level_3_index, context);

                    level_3_index = level_3_index.inc_z();
                }
                level_3_index = level_3_index.inc_y();
            }
            level_3_index = level_3_index.inc_x();
        }
    }

    #[inline(always)]
    fn check_node<const LEVEL: u8>(
        &mut self,
        index: LocalNodeIndex<LEVEL>,
        context: &LocalCoordinateContext,
    ) {
        match context.test_node(index) {
            BoundsCheckResult::Outside => {}
            BoundsCheckResult::Inside => {
                self.section_is_visible_bits
                    .copy_from(&self.section_is_populated_bits, index);
            }
            BoundsCheckResult::Partial => match LEVEL {
                3 => {
                    for lower_node_index in index.iter_lower_nodes::<2>() {
                        self.check_node(lower_node_index, context);
                    }
                }
                2 => {
                    for lower_node_index in index.iter_lower_nodes::<1>() {
                        self.check_node(lower_node_index, context);
                    }
                }
                1 => {
                    for lower_node_index in index.iter_lower_nodes::<0>() {
                        self.check_node(lower_node_index, context);
                    }
                }
                0 => {
                    self.section_is_visible_bits
                        .copy_from(&self.section_is_populated_bits, index);
                }
                _ => panic!("Invalid node level: {}", LEVEL),
            },
        }
    }

    // fn bfs_and_occlusion_cull(
    //     &mut self,
    //     context: &LocalCoordinateContext,
    //     no_occlusion_cull: bool,
    // ) -> CVec<RegionDrawBatch> {
    //     let mut region_iteration_queue: VecDeque<LocalSectionCoord> = VecDeque::new();
    //
    //     let origin_node_coord = position_to_chunk_coord(*frustum.position());
    //
    //     if let Some(node) = self.get_node(origin_node_coord) {
    //         let region_coord = chunk_coord_to_region_coord(origin_node_coord);
    //
    //         let mut region = self.regions.get_mut(&region_coord).unwrap();
    //         region.search_state.enqueue(
    //             LocalNodeIndex::from_global(origin_node_coord),
    //             GraphDirectionSet::all(),
    //         );
    //         region.search_state.enqueued = true;
    //
    //         region_iteration_queue.push_back(region_coord);
    //     }
    //
    //     let mut sorted_batches: Vec<RegionDrawBatch> = Vec::new();
    //
    //     while let Some(region_coord) = region_iteration_queue.pop_front() {
    //         let mut search_ctx = SearchContext::create(&mut self.regions, region_coord);
    //         let mut batch: RegionDrawBatch = RegionDrawBatch::new(region_coord);
    //
    //         while let Some(node_index) = search_ctx.origin().search_state.queue.pop() {
    //             let node_coord = node_index.as_global_coord(region_coord);
    //
    //             let node = search_ctx.origin().nodes[node_index.as_array_offset()];
    //             let node_incoming =
    //                 search_ctx.origin().search_state.incoming[node_index.as_array_offset()];
    //
    //             if !chunk_inside_fog(node_coord, origin_node_coord, view_distance)
    //                 || !chunk_inside_frustum(node_coord, frustum)
    //             {
    //                 continue;
    //             }
    //
    //             if (node.flags & (1 << 1)) != 0 {
    //                 batch.sections.push(node_index);
    //             }
    //
    //             let valid_directions = get_valid_directions(origin_node_coord, node_coord);
    //             let allowed_directions =
    //                 node.connections.get_outgoing_directions(node_incoming) & valid_directions;
    //
    //             Self::enqueue_all_neighbors(&mut search_ctx, allowed_directions, node_index);
    //         }
    //
    //         if !batch.is_empty() {
    //             sorted_batches.push(batch);
    //         }
    //
    //         for direction in GraphDirection::ORDERED {
    //             let adjacent_region_coord: LocalSectionCoord = region_coord + direction.as_vector();
    //
    //             if let Some(region) = &mut search_ctx.adjacent(*direction, true) {
    //                 if region.search_state.queue.is_empty() || region.search_state.enqueued {
    //                     continue;
    //                 }
    //
    //                 region.search_state.enqueued = true;
    //                 region_iteration_queue.push_back(adjacent_region_coord);
    //             }
    //         }
    //
    //         search_ctx.origin().search_state.reset();
    //     }
    //
    //     CVec::from_boxed_slice(sorted_batches.into_boxed_slice())
    // }

    fn divide_graph_into_regions(&self) -> CVec<RegionDrawBatch> {
        todo!()
    }

    // fn enqueue_all_neighbors(
    //     state: &mut GraphSearchState,
    //     directions: GraphDirectionSet,
    //     index: LocalNodeIndex<1>,
    // ) {
    //     for direction in GraphDirection::ORDERED {
    //         if directions.contains(direction) {
    //             let (neighbor, wrapped) = match direction {
    //                 GraphDirection::NegX => origin.dec_x(),
    //                 GraphDirection::NegY => origin.dec_y(),
    //                 GraphDirection::NegZ => origin.dec_z(),
    //                 GraphDirection::PosX => origin.inc_x(),
    //                 GraphDirection::PosY => origin.inc_y(),
    //                 GraphDirection::PosZ => origin.inc_z(),
    //             };
    //
    //             if let Some(neighbor_region) = context.adjacent(direction, wrapped) {
    //                 neighbor_region
    //                     .search_state
    //                     .enqueue(neighbor, GraphDirectionSet::single(direction.opposite()));
    //             }
    //         }
    //     }
    // }

    // pub fn add_section(&mut self, chunk_coord: LocalSectionCoord) {
    //     let mut region = self
    //         .regions
    //         .entry(chunk_coord_to_region_coord(chunk_coord))
    //         .or_insert_with(Region::new);
    //
    //     region.set_chunk(chunk_coord, Node::default());
    // }
    //
    // pub fn update_section(&mut self, chunk_coord: LocalSectionCoord, node: Node) {
    //     if let Some(region) = self
    //         .regions
    //         .get_mut(&chunk_coord_to_region_coord(chunk_coord))
    //     {
    //         region.set_chunk(chunk_coord, node);
    //     }
    // }
    //
    // pub fn remove_section(&mut self, chunk_coord: LocalSectionCoord) {
    //     if let Some(region) = self
    //         .regions
    //         .get_mut(&chunk_coord_to_region_coord(chunk_coord))
    //     {
    //         region.remove_chunk(chunk_coord);
    //     }
    // }
    //
    // fn get_node(&self, chunk_coord: LocalSectionCoord) -> Option<Node> {
    //     self.regions
    //         .get(&chunk_coord_to_region_coord(chunk_coord))
    //         .map(|region| *region.get_chunk(chunk_coord))
    // }
}

#[inline(always)]
pub fn get_neighbors(
    outgoing: GraphDirectionSet,
    index: LocalNodeIndex<1>,
    search_state: &mut GraphSearchState,
) {
    for direction in outgoing {
        let neighbor = match direction {
            GraphDirection::NegX => index.dec_x(),
            GraphDirection::NegY => index.dec_y(),
            GraphDirection::NegZ => index.dec_z(),
            GraphDirection::PosX => index.inc_x(),
            GraphDirection::PosY => index.inc_y(),
            GraphDirection::PosZ => index.inc_z(),
        };

        // the outgoing direction for the current node is the incoming direction for the neighbor
        search_state.enqueue(neighbor, direction.opposite());
    }
}

// #[no_mangle]
pub fn get_all_neighbors(index: LocalNodeIndex<1>) -> [LocalNodeIndex<1>; 6] {
    [
        index.dec_x(),
        index.dec_y(),
        index.dec_z(),
        index.inc_x(),
        index.inc_y(),
        index.inc_z(),
    ]
}

#[repr(C)]
pub struct RegionDrawBatch {
    region_coord: (i32, i32, i32),
    sections: CInlineVec<RegionSectionIndex, SECTIONS_IN_REGION>,
}

impl RegionDrawBatch {
    pub fn new(region_coord: i32x3) -> Self {
        RegionDrawBatch {
            region_coord: region_coord.into_tuple(),
            sections: CInlineVec::new(),
        }
    }

    fn is_empty(&self) -> bool {
        self.sections.is_empty()
    }
}
