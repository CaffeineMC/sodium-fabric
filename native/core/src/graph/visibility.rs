use std::mem::transmute;
use std::ops::BitAnd;

use core_simd::simd::Which::*;
use core_simd::simd::*;

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
