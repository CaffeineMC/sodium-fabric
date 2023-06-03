use fxhash::FxHashMap as HashMap;

use std::collections::VecDeque;
use std::fmt::Debug;
use std::mem::MaybeUninit;
use std::ops::ControlFlow;
use std::vec::Vec;

use std::ops;

use crate::ffi::CInlineVec;
use crate::ffi::CVec;
use crate::{
    frustum::Frustum,
    math::{IVec3, Vec3},
};

const SECTIONS_IN_REGION: usize = 8 * 4 * 8;

#[derive(Clone, Copy)]
pub struct LocalSectionIndex(u8);

impl LocalSectionIndex {
    fn from_global(global_pos: IVec3) -> LocalSectionIndex {
        let local_pos = global_pos & IVec3::new(7, 3, 7);
        let packed_pos = (local_pos.x << 5) | (local_pos.y << 0) | (local_pos.z << 2);

        LocalSectionIndex(packed_pos as u8)
    }
}

#[derive(Clone, Copy)]
pub struct PackedChunkCoord(u64);

impl PackedChunkCoord {
    const TOTAL_SIZE: usize = u64::BITS as usize;

    const X_MASK: u64 = 0x3FFFFF;
    const Y_MASK: u64 = 0x0FFFFF;
    const Z_MASK: u64 = 0x3FFFFF;

    const X_SIZE: usize = 22;
    const Y_SIZE: usize = 20;
    const Z_SIZE: usize = 22;

    const X_OFFSET: usize = 42;
    const Y_OFFSET: usize = 0;
    const Z_OFFSET: usize = 20;

    pub fn from(coord: IVec3) -> Self {
        let mut packed: u64 = 0;
        packed |= (coord.x as u64 & Self::X_MASK) << Self::X_OFFSET;
        packed |= (coord.y as u64 & Self::Y_MASK) << Self::Y_OFFSET;
        packed |= (coord.z as u64 & Self::Z_MASK) << Self::Z_OFFSET;

        PackedChunkCoord(packed)
    }

    pub fn x(&self) -> i32 {
        ((self.0 << (Self::TOTAL_SIZE - Self::X_SIZE - Self::X_OFFSET))
            >> (Self::TOTAL_SIZE - Self::X_SIZE)) as i32
    }

    pub fn y(&self) -> i32 {
        ((self.0 << (Self::TOTAL_SIZE - Self::Y_SIZE - Self::Y_OFFSET))
            >> (Self::TOTAL_SIZE - Self::Y_SIZE)) as i32
    }

    pub fn z(&self) -> i32 {
        ((self.0 << (Self::TOTAL_SIZE - Self::Z_SIZE - Self::Z_OFFSET))
            >> (Self::TOTAL_SIZE - Self::Z_SIZE)) as i32
    }
}

#[derive(Clone, Copy)]
pub enum GraphDirection {
    Down = 0,  // -y
    Up = 1,    // +y
    North = 2, // -z
    South = 3, // +z
    West = 4,  // -x
    East = 5,  // +x
}

impl GraphDirection {
    pub fn offset(&self) -> IVec3 {
        match *self {
            GraphDirection::Down => IVec3::new(0, -1, 0),
            GraphDirection::Up => IVec3::new(0, 1, 0),
            GraphDirection::North => IVec3::new(0, 0, -1),
            GraphDirection::South => IVec3::new(0, 0, 1),
            GraphDirection::West => IVec3::new(-1, 0, 0),
            GraphDirection::East => IVec3::new(1, 0, 0),
        }
    }

    pub fn ordered() -> &'static [GraphDirection; 6] {
        const ORDERED: [GraphDirection; 6] = [
            GraphDirection::Down,
            GraphDirection::Up,
            GraphDirection::North,
            GraphDirection::South,
            GraphDirection::West,
            GraphDirection::East,
        ];
        &ORDERED
    }

    pub fn opposite(&self) -> GraphDirection {
        match *self {
            GraphDirection::Up => GraphDirection::Down,
            GraphDirection::Down => GraphDirection::Up,
            GraphDirection::North => GraphDirection::South,
            GraphDirection::South => GraphDirection::North,
            GraphDirection::West => GraphDirection::East,
            GraphDirection::East => GraphDirection::West,
        }
    }
}

#[derive(Default, Clone, Copy, Debug)]
pub struct VisibilityData(u64);

impl VisibilityData {
    pub fn from_u64(packed: u64) -> Self {
        VisibilityData(packed)
    }

    // fn get_outgoing_directions(&self, incoming: GraphDirectionSet) -> GraphDirectionSet {
    //     let mut outgoing = GraphDirectionSet::default();

    //     for direction in GraphDirection::ordered() {
    //         if incoming.contains(*direction) {
    //             outgoing.add_all(self.data[*direction as usize]);
    //         }
    //     }

    //     outgoing
    // }

    fn get_outgoing_directions(&self, incoming: GraphDirectionSet) -> GraphDirectionSet {
        let mut c = (((0x810204081_u64 * incoming.0 as u64) & 0x010101010101_u64) * 0xFF) // turn bitmask into lane wise mask
        & self.0; // apply visibility to incoming
        
        c |= c >> 32; // fold top 32 bits onto bottom 32 bits
        c |= c >> 16; // fold top 16 bits onto bottom 16 bits
        c |= c >> 8; // fold top 8 bits onto bottom 8 bits

        GraphDirectionSet(c as u8)
    }
}

#[derive(Default, Clone, Copy, Debug)]
pub struct GraphDirectionSet(u8);

impl GraphDirectionSet {
    pub fn from(packed: u8) -> Self {
        GraphDirectionSet(packed)
    }

    pub fn add(&mut self, dir: GraphDirection) {
        self.0 |= 1 << dir as usize;
    }

    pub fn add_all(&mut self, set: GraphDirectionSet) {
        self.0 |= set.0;
    }

    pub fn contains(&self, dir: GraphDirection) -> bool {
        (self.0 & (1 << dir as usize)) != 0
    }

    pub fn all() -> GraphDirectionSet {
        GraphDirectionSet(!0)
    }
}

impl ops::BitAnd for GraphDirectionSet {
    type Output = GraphDirectionSet;

    fn bitand(self, rhs: Self) -> Self::Output {
        GraphDirectionSet(self.0 & rhs.0)
    }
}

#[derive(Default)]
pub struct GraphNode {
    pub connections: VisibilityData,
    pub flags: u32,

    pub incoming: GraphDirectionSet,
    pub visited: bool,
}

impl GraphNode {
    pub fn new(connections: VisibilityData, flags: u32) -> Self {
        GraphNode {
            connections,
            flags,
            incoming: GraphDirectionSet::default(),
            visited: false,
        }
    }
}

#[repr(C)]
pub struct RegionDrawBatch {
    region_coord: (i32, i32, i32),
    sections: CInlineVec<LocalSectionIndex, SECTIONS_IN_REGION>,
}

impl RegionDrawBatch {
    pub fn new(region_coord: IVec3) -> Self {
        RegionDrawBatch {
            region_coord: region_coord.into(),
            sections: CInlineVec::new(),
        }
    }
}

struct ArrayDeque<T> {
    head: usize,
    tail: usize,
    capacity: usize,
    buffer: Box<[MaybeUninit<T>]>
}

impl<T: Copy> ArrayDeque<T> {
    pub fn new(capacity: usize) -> ArrayDeque<T> {
        Self {
            head: 0,
            tail: 0,
            capacity,
            buffer: vec![MaybeUninit::uninit(); capacity]
                .into_boxed_slice()
        }
    }

    pub fn push(&mut self, value: T) {
        assert!(self.head < self.capacity, "deque is out of capacity");

        self.buffer[self.head] = MaybeUninit::new(value);
        self.head += 1;
    }

    pub fn pop(&mut self) -> Option<T> {
        if self.tail >= self.head {
            return None;
        }

        let value = self.buffer[self.tail];
        self.tail += 1;
        
        Some(unsafe { value.assume_init() })
    }
}

pub struct Graph {
    nodes: HashMap<IVec3, GraphNode>,
}

impl Graph {
    pub fn new() -> Self {
        Graph {
            nodes: HashMap::default(),
        }
    }

    pub fn search(&mut self, frustum: &Frustum, view_distance: i32) -> CVec<RegionDrawBatch> {
        for node in self.nodes.values_mut() {
            node.visited = false;
            node.incoming = GraphDirectionSet::default();
        }

        let mut queue: VecDeque<IVec3> = VecDeque::new();

        let camera_position = frustum.position();
        let center_position = IVec3 {
            x: (camera_position.x.floor() as i32) >> 4,
            y: (camera_position.y.floor() as i32) >> 4,
            z: (camera_position.z.floor() as i32) >> 4,
        };

        if let Some(node) = self.nodes.get_mut(&center_position) {
            queue.push_back(center_position);

            node.visited = true;
            node.incoming = node
                .connections
                .get_outgoing_directions(GraphDirectionSet::all());
        }

        let mut sorted_region_table: Vec<RegionDrawBatch> = Vec::new();
        let mut sorted_region_indices: HashMap<IVec3, usize> = HashMap::default();

        while let Some(node_position) = queue.pop_front() {
            if !chunk_inside_view_distance(node_position, center_position, view_distance)
                || !chunk_inside_frustum(node_position, frustum)
            {
                continue;
            }

            let region_position = node_position >> IVec3::new(3, 2, 3);
            let region_index = sorted_region_indices
                .entry(region_position)
                .or_insert_with_key(|key| {
                    let index = sorted_region_table.len();
                    sorted_region_table.push(RegionDrawBatch::new(*key));

                    index
                })
                .clone();

            let node = &self.nodes[&node_position];

            let region: &mut RegionDrawBatch = sorted_region_table
                .get_mut(region_index)
                .expect("The associated region from sorted lookup table does not exist");

            if node.flags != 0 {
                region
                    .sections
                    .push(LocalSectionIndex::from_global(node_position));
            }

            let valid_directions = get_valid_directions(center_position, node_position);
            let allowed_directions =
                node.connections.get_outgoing_directions(node.incoming) & valid_directions;

            self.try_enqueue_neighbor(
                allowed_directions,
                GraphDirection::Down,
                node_position,
                &mut queue,
            );
            self.try_enqueue_neighbor(
                allowed_directions,
                GraphDirection::Up,
                node_position,
                &mut queue,
            );
            self.try_enqueue_neighbor(
                allowed_directions,
                GraphDirection::North,
                node_position,
                &mut queue,
            );
            self.try_enqueue_neighbor(
                allowed_directions,
                GraphDirection::South,
                node_position,
                &mut queue,
            );
            self.try_enqueue_neighbor(
                allowed_directions,
                GraphDirection::West,
                node_position,
                &mut queue,
            );
            self.try_enqueue_neighbor(
                allowed_directions,
                GraphDirection::East,
                node_position,
                &mut queue,
            );
        }

        unsafe { CVec::from_boxed_slice(sorted_region_table.into_boxed_slice()) }
    }

    #[inline(always)]
    fn try_enqueue_neighbor(
        &mut self,
        outgoing_directions: GraphDirectionSet,
        direction: GraphDirection,
        node_position: IVec3,
        queue: &mut VecDeque<IVec3>,
    ) {
        if !outgoing_directions.contains(direction) {
            return;
        }

        let neighbor_position = node_position + direction.offset();
        let neighbor = match self.nodes.get_mut(&neighbor_position) {
            Some(node) => node,
            None => return,
        };

        neighbor.incoming.add(direction.opposite());

        if !neighbor.visited {
            unsafe {
                queue.push_back(neighbor_position);
            }

            neighbor.visited = true;
        }
    }

    pub fn add_chunk(&mut self, coord: IVec3) {
        self.nodes.insert(coord, GraphNode::default());
    }

    pub fn update_chunk(&mut self, coord: IVec3, node: GraphNode) {
        self.nodes.insert(coord, node);
    }

    pub fn remove_chunk(&mut self, coord: IVec3) {
        self.nodes.remove(&coord);
    }
}

fn chunk_inside_view_distance(position: IVec3, center: IVec3, view_distance: i32) -> bool {
    let distance = (position - center).abs();
    distance.max() < view_distance
}

pub fn get_valid_directions(center: IVec3, position: IVec3) -> GraphDirectionSet {
    let mut directions = 0;
    directions |= if position.x <= center.x { 1 } else { 0 } << GraphDirection::West as usize;
    directions |= if position.x >= center.x { 1 } else { 0 } << GraphDirection::East as usize;

    directions |= if position.y <= center.y { 1 } else { 0 } << GraphDirection::Down as usize;
    directions |= if position.y >= center.y { 1 } else { 0 } << GraphDirection::Up as usize;

    directions |= if position.z <= center.z { 1 } else { 0 } << GraphDirection::North as usize;
    directions |= if position.z >= center.z { 1 } else { 0 } << GraphDirection::South as usize;

    GraphDirectionSet::from(directions)
}

fn chunk_inside_frustum(position: IVec3, frustum: &Frustum) -> bool {
    let x = (position.x << 4) as f32;
    let y = (position.y << 4) as f32;
    let z = (position.z << 4) as f32;

    let min = Vec3::new(x, y, z);
    let max = Vec3::new(x + 16.0, y + 16.0, z + 16.0);

    frustum.test_bounding_box(min, max)
}
