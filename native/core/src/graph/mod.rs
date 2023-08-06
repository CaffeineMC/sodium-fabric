use std::collections::VecDeque;
use std::fmt::Debug;
use std::ops::*;
use std::vec::Vec;

use core_simd::simd::Which::*;
use core_simd::simd::*;
use local::LocalCoordinateContext;
use std_float::StdFloat;

use crate::collections::ArrayDeque;
use crate::ffi::{CInlineVec, CVec};
use crate::graph::octree::LinearBitOctree;
use crate::math::*;

pub mod local;
mod octree;

pub const REGION_COORD_MASK: u8x3 = u8x3::from_array([0b11111000, 0b11111100, 0b11111000]);
pub const SECTIONS_IN_REGION: usize = 8 * 4 * 8;
pub const SECTIONS_IN_GRAPH: usize = 256 * 256 * 256;

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct LocalNodeIndex(u32);

impl LocalNodeIndex {
    // XYZXYZXYZXYZXYZXYZXYZXYZ
    const X_MASK: u32 = 0b10010010_01001001_00100100;
    const Y_MASK: u32 = 0b01001001_00100100_10010010;
    const Z_MASK: u32 = 0b00100100_10010010_01001001;

    #[inline(always)]
    pub fn pack(unpacked: u8x3) -> Self {
        // allocate one byte per bit for each element.
        // each element is still has its individual bits in linear ordering, but the bytes in the
        // vector are in morton ordering.
        #[rustfmt::skip]
        let expanded_linear_bits = simd_swizzle!(
            unpacked,
            [
            //  X, Y, Z
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0, // LSB
            ]
        );

        // shift each bit into the sign bit for morton ordering
        #[rustfmt::skip]
        let expanded_morton_bits = expanded_linear_bits << Simd::<u8, 24>::from_array(
            [
                7, 7, 7,
                6, 6, 6,
                5, 5, 5,
                4, 4, 4,
                3, 3, 3,
                2, 2, 2,
                1, 1, 1,
                0, 0, 0, // LSB
            ],
        );

        // arithmetic shift to set each whole lane to its sign bit, then shrinking all lanes to bitmask
        let morton_packed = unsafe {
            Mask::<i8, 24>::from_int_unchecked(expanded_morton_bits.cast::<i8>() >> Simd::splat(7))
        }
        .to_bitmask();

        Self(morton_packed)
    }

    #[inline(always)]
    pub fn inc_x<const LEVEL: u8>(self) -> Self {
        self.inc::<{ Self::X_MASK }>()
    }

    #[inline(always)]
    pub fn inc_y(self) -> Self {
        self.inc::<{ Self::Y_MASK }>()
    }

    #[inline(always)]
    pub fn inc_z(self) -> Self {
        self.inc::<{ Self::Z_MASK }>()
    }

    #[inline(always)]
    pub fn dec_x(self) -> Self {
        self.dec::<{ Self::X_MASK }>()
    }

    #[inline(always)]
    pub fn dec_y(self) -> Self {
        self.dec::<{ Self::Y_MASK }>()
    }

    #[inline(always)]
    pub fn dec_z(self) -> Self {
        self.dec::<{ Self::Z_MASK }>()
    }

    #[inline(always)]
    pub fn inc<const MASK: u32>(self) -> Self {
        // make the other bits in the number 1
        let mut masked = self.0 | !MASK;

        // increment
        masked = masked.wrapping_add(1);

        // modify only the masked bits in the original number
        Self((self.0 & !MASK) | (masked & MASK))
    }

    #[inline(always)]
    pub fn dec<const MASK: u32>(self) -> Self {
        // make the other bits in the number 0
        let mut masked = self.0 & MASK;

        // decrement
        masked = masked.wrapping_sub(1);

        // modify only the masked bits in the original number
        Self((self.0 & !MASK) | (masked & MASK))
    }

    #[inline(always)]
    pub fn as_array_offset(&self) -> usize {
        self.0 as usize
    }

    #[inline(always)]
    pub fn unpack(&self) -> u8x3 {
        // allocate one byte per bit for each element.
        // each element is still has its individual bits in morton ordering, but the bytes in the
        // vector are in linear ordering.
        #[rustfmt::skip]
        let expanded_linear_bits = simd_swizzle!(
            u8x4::from_array(self.0.to_le_bytes()),
            [
                // X
                2, 2, 2, 1, 1, 1, 0, 0,
                // Y
                2, 2, 2, 1, 1, 0, 0, 0,
                // Z
                2, 2, 1, 1, 1, 0, 0, 0, // LSB
            ]
        );

        // shift each bit into the sign bit for morton ordering
        #[rustfmt::skip]
        let expanded_morton_bits = expanded_linear_bits << Simd::<u8, 24>::from_array(
            [
                // X
                0, 3, 6,
                1, 4, 7,
                2, 5,
                // Y
                1, 4, 7,
                2, 5, 0,
                3, 6,
                // Z
                2, 5, 0,
                3, 6, 1,
                4, 7, // LSB
            ],
        );

        // arithmetic shift to set each whole lane to its sign bit, then shrinking all lanes to bitmask
        let linear_packed = unsafe {
            Mask::<i8, 24>::from_int_unchecked(expanded_morton_bits.cast::<i8>() >> Simd::splat(7))
        }
        .to_bitmask();

        u8x3::from_slice(&linear_packed.to_le_bytes()[0..=2])
    }
}

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
pub enum GraphDirection {
    NegX,
    NegY,
    NegZ,
    PosX,
    PosY,
    PosZ,
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
}

#[derive(Clone, Copy)]
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
        self.0 |= 1 << dir as usize;
    }

    #[inline(always)]
    pub fn add_all(&mut self, set: GraphDirectionSet) {
        self.0 |= set.0;
    }

    #[inline(always)]
    pub fn contains(&self, dir: GraphDirection) -> bool {
        (self.0 & (1 << dir as usize)) != 0
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

#[derive(Default, Clone, Copy)]
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

struct GraphSearchState {
    incoming: [GraphDirectionSet; SECTIONS_IN_REGION],
    queue: ArrayDeque<RegionSectionIndex, { SECTIONS_IN_REGION + 1 }>,

    enqueued: bool,
}

impl GraphSearchState {
    // pub fn enqueue(&mut self, index: LocalNodeIndex, directions: GraphDirectionSet) {
    //     let incoming = &mut self.incoming[index.as_array_offset()];
    //     let should_enqueue = incoming.is_empty();
    //
    //     incoming.add_all(directions);
    //
    //     unsafe {
    //         self.queue
    //             .push_conditionally_unchecked(index, should_enqueue);
    //     }
    // }

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
            incoming: [GraphDirectionSet::default(); SECTIONS_IN_REGION],
            enqueued: false,
        }
    }
}

pub struct Graph {
    section_populated_bits: LinearBitOctree,
    section_visibility_bits: LinearBitOctree,
}

impl Graph {
    pub fn new() -> Self {
        Graph {
            section_populated_bits: Default::default(),
            section_visibility_bits: Default::default(),
        }
    }

    pub fn cull(&mut self, context: LocalCoordinateContext, no_occlusion_cull: bool) {}

    fn frustum_and_fog(&mut self, context: LocalCoordinateContext) {}

    // fn bfs_and_occlusion_cull(
    //     &mut self,
    //     context: LocalCoordinateContext,
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
    //         while let Some(node_idx) = search_ctx.origin().search_state.queue.pop() {
    //             let node_coord = node_idx.as_global_coord(region_coord);
    //
    //             let node = search_ctx.origin().nodes[node_idx.as_array_offset()];
    //             let node_incoming =
    //                 search_ctx.origin().search_state.incoming[node_idx.as_array_offset()];
    //
    //             if !chunk_inside_fog(node_coord, origin_node_coord, view_distance)
    //                 || !chunk_inside_frustum(node_coord, frustum)
    //             {
    //                 continue;
    //             }
    //
    //             if (node.flags & (1 << 1)) != 0 {
    //                 batch.sections.push(node_idx);
    //             }
    //
    //             let valid_directions = get_valid_directions(origin_node_coord, node_coord);
    //             let allowed_directions =
    //                 node.connections.get_outgoing_directions(node_incoming) & valid_directions;
    //
    //             Self::enqueue_all_neighbors(&mut search_ctx, allowed_directions, node_idx);
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
    //
    // fn enqueue_all_neighbors(
    //     context: &mut SearchContext,
    //     directions: GraphDirectionSet,
    //     origin: LocalNodeIndex,
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
