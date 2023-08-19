use std::mem::size_of;

use core_simd::simd::*;

use crate::graph::*;

// operations on u8x64 are faster in some cases compared to u64x8
pub type Level3Node = Simd<u8, 64>;
pub type Level2Node = u64;
pub type Level1Node = u8;
pub type Level0Node = bool;

pub const LEVEL_3_INDEX_SHIFT: usize = 9;
pub const LEVEL_2_INDEX_SHIFT: usize = 6;
pub const LEVEL_1_INDEX_SHIFT: usize = 3;

pub const LEVEL_3_COORD_SHIFT: u8 = 3;
pub const LEVEL_2_COORD_SHIFT: u8 = 2;
pub const LEVEL_1_COORD_SHIFT: u8 = 1;

pub const LEVEL_3_COORD_LENGTH: u8 = 8;
pub const LEVEL_2_COORD_LENGTH: u8 = 4;
pub const LEVEL_1_COORD_LENGTH: u8 = 2;

pub const LEVEL_3_COORD_MASK: u8 = 0b11111000;
pub const LEVEL_2_COORD_MASK: u8 = 0b11111100;
pub const LEVEL_1_COORD_MASK: u8 = 0b11111110;

pub union LinearBitOctree {
    level_3: [Level3Node; SECTIONS_IN_GRAPH / size_of::<Level3Node>() / 8],
    level_2: [Level2Node; SECTIONS_IN_GRAPH / size_of::<Level2Node>() / 8],
    level_1: [Level1Node; SECTIONS_IN_GRAPH / size_of::<Level1Node>() / 8],
}

impl Default for LinearBitOctree {
    fn default() -> Self {
        LinearBitOctree {
            level_3: [Level3Node::splat(0); SECTIONS_IN_GRAPH / size_of::<Level3Node>() / 8],
        }
    }
}

// All of the unsafe gets should be safe, because LocalNodeIndex should never have the top 8 bits
// set, and our arrays are exactly 2^24 bytes long.
impl LinearBitOctree {
    /// Returns true if all of the bits in the node are true
    #[inline(always)]
    pub fn get<const LEVEL: u8>(&self, index: LocalNodeIndex<LEVEL>) -> bool {
        let array_offset = index.as_array_offset();

        match LEVEL {
            0 => {
                let level_1_index = array_offset >> LEVEL_1_INDEX_SHIFT;
                let bit_index = array_offset & 0b111;

                let level_1_node = unsafe { *self.level_1.get_unchecked(level_1_index) };

                let bit = (level_1_node >> bit_index) & 0b1;

                bit == 0b1
            }
            1 => {
                let level_1_index = array_offset >> LEVEL_1_INDEX_SHIFT;

                let level_1_node = unsafe { *self.level_1.get_unchecked(level_1_index) };

                level_1_node == u8::MAX
            }
            2 => {
                let level_2_index = array_offset >> LEVEL_2_INDEX_SHIFT;

                let level_2_node = unsafe { *self.level_2.get_unchecked(level_2_index) };

                level_2_node == u64::MAX
            }
            3 => {
                let level_3_index = array_offset >> LEVEL_3_INDEX_SHIFT;

                let level_3_node = unsafe { *self.level_3.get_unchecked(level_3_index) };

                level_3_node == u8x64::splat(u8::MAX)
            }
            _ => unreachable!(),
        }
    }

    /// Sets all of the bits in the node to the given value
    pub fn set<const LEVEL: u8>(&mut self, section: LocalNodeIndex<LEVEL>, value: bool) {
        let array_offset = section.as_array_offset();

        match LEVEL {
            0 => {
                let level_1_index = array_offset >> LEVEL_1_INDEX_SHIFT;
                let bit_index = array_offset & 0b111;

                let level_1_node = unsafe { self.level_1.get_unchecked_mut(level_1_index) };

                let bit = 0b1 << bit_index;

                if value {
                    *level_1_node |= bit;
                } else {
                    *level_1_node &= !bit;
                }
            }
            1 => {
                let level_1_index = array_offset >> LEVEL_1_INDEX_SHIFT;

                let level_1_node = unsafe { self.level_1.get_unchecked_mut(level_1_index) };

                *level_1_node = if value { u8::MAX } else { 0_u8 };
            }
            2 => {
                let level_2_index = array_offset >> LEVEL_2_INDEX_SHIFT;

                let level_2_node = unsafe { self.level_2.get_unchecked_mut(level_2_index) };

                *level_2_node = if value { u64::MAX } else { 0_u64 };
            }
            3 => {
                let level_3_index = array_offset >> LEVEL_3_INDEX_SHIFT;

                let level_3_node = unsafe { self.level_3.get_unchecked_mut(level_3_index) };

                *level_3_node = u8x64::splat(if value { u8::MAX } else { 0_u8 });
            }
            _ => unreachable!(),
        }
    }

    #[inline(always)]
    pub fn copy_from<const LEVEL: u8>(&mut self, src: &Self, index: LocalNodeIndex<LEVEL>) {
        let array_offset = index.as_array_offset();

        match LEVEL {
            0 => {
                let level_1_index = array_offset >> LEVEL_1_INDEX_SHIFT;
                let bit_index = array_offset & 0b111;

                let level_1_node_src = unsafe { *src.level_1.get_unchecked(level_1_index) };
                let level_1_node_dst = unsafe { self.level_1.get_unchecked_mut(level_1_index) };

                let bit_mask = 0b1 << bit_index;
                let src_bit = level_1_node_src & bit_mask;
                // clear the bit in the destination so the bitwise OR can always act as a copy
                *level_1_node_dst &= !bit_mask;
                *level_1_node_dst |= src_bit;
            }
            1 => {
                let level_1_index = array_offset >> LEVEL_1_INDEX_SHIFT;

                let level_1_node_src = unsafe { *src.level_1.get_unchecked(level_1_index) };
                let level_1_node_dst = unsafe { self.level_1.get_unchecked_mut(level_1_index) };

                *level_1_node_dst = level_1_node_src;
            }
            2 => {
                let level_2_index = array_offset >> LEVEL_2_INDEX_SHIFT;

                let level_2_node_src = unsafe { *src.level_2.get_unchecked(level_2_index) };
                let level_2_node_dst = unsafe { self.level_2.get_unchecked_mut(level_2_index) };

                *level_2_node_dst = level_2_node_src;
            }
            3 => {
                let level_3_index = array_offset >> LEVEL_3_INDEX_SHIFT;

                let level_3_node_src = unsafe { *src.level_3.get_unchecked(level_3_index) };
                let level_3_node_dst = unsafe { self.level_3.get_unchecked_mut(level_3_index) };

                *level_3_node_dst = level_3_node_src;
            }
            _ => unreachable!(),
        }
    }

    #[inline(always)]
    pub fn clear(&mut self) {
        unsafe { self.level_3 }.fill(Level3Node::splat(0));
    }

    // inside of individual level 3 nodes, the cache locality is *extremely* good.
    // const INTRINSIC_LOCALITY_LEVEL: i32 = 3;
    //
    // pub fn prefetch_top_node_read(&self, index: LocalNodeIndex<3>) {
    //     unsafe {
    //         let pointer = unsafe {
    //             self.level_1
    //                 .get_unchecked(index.as_array_offset() >> LEVEL_1_INDEX_SHIFT)
    //         };
    //
    //         prefetch_read_data(pointer, Self::INTRINSIC_LOCALITY_LEVEL);
    //     }
    // }
    //
    // pub fn prefetch_top_node_write(&self, index: LocalNodeIndex<3>) {
    //     unsafe {
    //         let pointer = unsafe {
    //             self.level_1
    //                 .get_unchecked(index.as_array_offset() >> LEVEL_1_INDEX_SHIFT)
    //         };
    //
    //         prefetch_write_data(pointer, Self::INTRINSIC_LOCALITY_LEVEL);
    //     }
    // }
}
