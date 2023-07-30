use std::mem::size_of;

use core_simd::simd::*;

use crate::graph::*;

pub type Level3Node = Simd<u8, 64>;
pub type Level2Node = u64;
pub type Level1Node = u8;
pub type Level0Node = bool;

pub const LEVEL_3_IDX_SHIFT: usize = 9;
pub const LEVEL_2_IDX_SHIFT: usize = 6;
pub const LEVEL_1_IDX_SHIFT: usize = 3;
pub const LEVEL_0_IDX_SHIFT: usize = 0;

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

impl LinearBitOctree {
    pub fn get<const LEVEL: usize>(&self, section: LocalSectionIndex) -> bool {
        let array_offset = section.as_array_offset();
        
        match LEVEL {
            0 => {
                let level_1_idx = array_offset >> LEVEL_1_IDX_SHIFT;
                let bit_idx = array_offset & 0b111;

                // SAFETY: The value returned by as_array_offset will never have the top 8 bits set,
                //  and our arrays are exactly 2^24 bytes long.
                let level_1_node = unsafe { *self.level_1.get_unchecked(level_1_idx) };

                let bit = (level_1_node >> bit_idx) & 0b1;

                bit == 0b1
            }
            1 => {
                let level_1_idx = array_offset >> LEVEL_1_IDX_SHIFT;

                // SAFETY: The value returned by as_array_offset will never have the top 8 bits set,
                //  and our arrays are exactly 2^24 bytes long.
                let level_1_node = unsafe { *self.level_1.get_unchecked(level_1_idx) };

                level_1_node == u8::MAX
            }
            2 => {
                let level_2_idx = array_offset >> LEVEL_2_IDX_SHIFT;

                let level_2_node = unsafe { *self.level_2.get_unchecked(level_2_idx) };

                level_2_node == u64::MAX
            }
            3 => {
                let level_3_idx = array_offset >> LEVEL_3_IDX_SHIFT;

                // SAFETY: The value returned by as_array_offset will never have the top 8 bits set,
                //  and our arrays are exactly 2^24 bytes long.
                let level_3_node = unsafe { *self.level_3.get_unchecked(level_3_idx) };

                level_3_node == u8x64::splat(u8::MAX)
            }
            _ => unreachable!(),
        }
    }

    pub fn set<const LEVEL: usize>(&self, section: LocalSectionIndex) {
        let array_offset = section.as_array_offset();
        
        match LEVEL {
            0 => {
                let level_1_idx = array_offset >> LEVEL_1_IDX_SHIFT;
                let bit_idx = array_offset & 0b111;

                // SAFETY: The value returned by as_array_offset will never have the top 8 bits set,
                //  and our arrays are exactly 2^24 bytes long.
                let level_1_node = unsafe { self.level_1.get_unchecked_mut(level_1_idx) };

                let bit = 0b1 << bit_idx;

                *level_1_node |= bit;
            }
            1 => {
                let level_1_idx = array_offset >> LEVEL_1_IDX_SHIFT;

                // SAFETY: The value returned by as_array_offset will never have the top 8 bits set,
                //  and our arrays are exactly 2^24 bytes long.
                let level_1_node = unsafe { self.level_1.get_unchecked_mut(level_1_idx) };

                *level_1_node = u8::MAX;
            }
            2 => {
                let level_2_idx = array_offset >> LEVEL_2_IDX_SHIFT;

                let level_2_node = unsafe { self.level_2.get_unchecked_mut(level_2_idx) };

                *level_2_node = u64::MAX;
            }
            3 => {
                let level_3_idx = array_offset >> LEVEL_3_IDX_SHIFT;

                // SAFETY: The value returned by as_array_offset will never have the top 8 bits set,
                //  and our arrays are exactly 2^24 bytes long.
                let level_3_node = unsafe { self.level_3.get_unchecked_mut(level_3_idx) };

                *level_3_node = u8x64::splat(u8::MAX);
            }
            _ => unreachable!(),
        }
    }
}
