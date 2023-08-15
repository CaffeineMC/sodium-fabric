use core_simd::simd::*;

use crate::graph::GraphDirection;
use crate::math::{u8x3, ToBitMaskExtended};

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct LocalNodeIndex<const LEVEL: u8>(u32);

// XYZXYZXYZXYZXYZXYZXYZXYZ
const LOCAL_NODE_INDEX_X_MASK: u32 = 0b10010010_01001001_00100100;
const LOCAL_NODE_INDEX_Y_MASK: u32 = 0b01001001_00100100_10010010;
const LOCAL_NODE_INDEX_Z_MASK: u32 = 0b00100100_10010010_01001001;

impl<const LEVEL: u8> LocalNodeIndex<LEVEL> {
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
    pub fn inc_x(self) -> Self {
        self.inc::<{ LOCAL_NODE_INDEX_X_MASK }>()
    }

    #[inline(always)]
    pub fn inc_y(self) -> Self {
        self.inc::<{ LOCAL_NODE_INDEX_Y_MASK }>()
    }

    #[inline(always)]
    pub fn inc_z(self) -> Self {
        self.inc::<{ LOCAL_NODE_INDEX_Z_MASK }>()
    }

    #[inline(always)]
    pub fn dec_x(self) -> Self {
        self.dec::<{ LOCAL_NODE_INDEX_X_MASK }>()
    }

    #[inline(always)]
    pub fn dec_y(self) -> Self {
        self.dec::<{ LOCAL_NODE_INDEX_Y_MASK }>()
    }

    #[inline(always)]
    pub fn dec_z(self) -> Self {
        self.dec::<{ LOCAL_NODE_INDEX_Z_MASK }>()
    }

    #[inline(always)]
    pub fn inc<const MASK: u32>(self) -> Self {
        // make the other bits in the number 1
        let mut masked = self.0 | !MASK;

        // increment
        masked = masked.wrapping_add(1_u32 << LEVEL);

        // modify only the masked bits in the original number
        Self((self.0 & !MASK) | (masked & MASK))
    }

    #[inline(always)]
    pub fn dec<const MASK: u32>(self) -> Self {
        // make the other bits in the number 0
        let mut masked = self.0 & MASK;

        // decrement
        masked = masked.wrapping_sub(1_u32 << LEVEL);

        // modify only the masked bits in the original number
        Self((self.0 & !MASK) | (masked & MASK))
    }

    #[inline(always)]
    pub fn as_array_offset(&self) -> usize {
        self.0 as usize
    }

    #[inline(always)]
    pub fn iter_lower_nodes<const LOWER_LEVEL: u8>(&self) -> LowerNodeIter<LEVEL, LOWER_LEVEL> {
        LowerNodeIter::new(self)
    }

    #[inline(always)]
    pub fn get_all_neighbors(&self) -> NeighborNodes<LEVEL> {
        const DEC_MASK: Simd<u32, 6> = Simd::from_array([
            LOCAL_NODE_INDEX_X_MASK,
            LOCAL_NODE_INDEX_Y_MASK,
            LOCAL_NODE_INDEX_Z_MASK,
            u32::MAX,
            u32::MAX,
            u32::MAX,
        ]);

        const INC_MASK: Simd<u32, 6> = Simd::from_array([
            u32::MAX,
            u32::MAX,
            u32::MAX,
            LOCAL_NODE_INDEX_X_MASK,
            LOCAL_NODE_INDEX_Y_MASK,
            LOCAL_NODE_INDEX_Z_MASK,
        ]);

        const FINAL_MASK: Simd<u32, 6> = Simd::from_array([
            LOCAL_NODE_INDEX_X_MASK,
            LOCAL_NODE_INDEX_Y_MASK,
            LOCAL_NODE_INDEX_Z_MASK,
            LOCAL_NODE_INDEX_X_MASK,
            LOCAL_NODE_INDEX_Y_MASK,
            LOCAL_NODE_INDEX_Z_MASK,
        ]);

        let vec = Simd::<u32, 6>::splat(self.0);
        // make the other bits in the number 0 for dec, 1 for inc
        let mut masked = (vec & DEC_MASK) | !INC_MASK;

        // inc/dec
        masked = (masked.cast::<i32>() + Simd::from_array([-1, -1, -1, 1, 1, 1])).cast::<u32>();

        // modify only the masked bits in the original number
        NeighborNodes::new((vec & !FINAL_MASK) | (masked & FINAL_MASK))
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

pub struct LowerNodeIter<const LEVEL: u8, const LOWER_LEVEL: u8> {
    current: u32,
    end: u32,
}

impl<const LEVEL: u8, const LOWER_LEVEL: u8> LowerNodeIter<LEVEL, LOWER_LEVEL> {
    fn new(index: &LocalNodeIndex<LEVEL>) -> Self {
        assert!(LEVEL > LOWER_LEVEL);

        let node_size = 1 << (LEVEL * 3);

        Self {
            current: index.0,
            end: index.0 + node_size,
        }
    }
}

impl<const LEVEL: u8, const LOWER_LEVEL: u8> Iterator for LowerNodeIter<LEVEL, LOWER_LEVEL> {
    type Item = LocalNodeIndex<LOWER_LEVEL>;

    fn next(&mut self) -> Option<Self::Item> {
        if self.current >= self.end {
            None
        } else {
            let current = self.current;

            let lower_node_size = 1 << (LOWER_LEVEL * 3);
            self.current += lower_node_size;

            Some(LocalNodeIndex(current))
        }
    }
}

#[repr(transparent)]
pub struct NeighborNodes<const LEVEL: u8>(u32x8);

impl<const LEVEL: u8> NeighborNodes<LEVEL> {
    #[inline(always)]
    fn new(raw_indices: Simd<u32, 6>) -> NeighborNodes<LEVEL> {
        // this produces slightly better codegen because we're able to batch the read in a single mov.
        // if we don't do this, it gets split into 3 different movs.
        NeighborNodes(unsafe { *(&raw_indices as *const _ as *const u32x8) })
    }

    #[inline(always)]
    pub fn get(&self, direction: GraphDirection) -> LocalNodeIndex<LEVEL> {
        LocalNodeIndex(self.0[direction as usize])
    }
}
