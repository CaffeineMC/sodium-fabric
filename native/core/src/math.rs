#![allow(non_camel_case_types)]

use std::cmp::Eq;
use std::hash::Hash;
use std::ops::*;

use core_simd::simd::*;
use std_float::StdFloat;

pub type i8x3 = Simd<i8, 3>;
pub type i16x3 = Simd<i16, 3>;
pub type i32x3 = Simd<i32, 3>;
pub type i64x3 = Simd<i64, 3>;

pub type u8x3 = Simd<u8, 3>;
pub type u16x3 = Simd<u16, 3>;
pub type u32x3 = Simd<u32, 3>;
pub type u64x3 = Simd<u64, 3>;

pub type f32x3 = Simd<f32, 3>;
pub type f64x3 = Simd<f64, 3>;

// additional declarations outside of traits for const usage
pub const fn from_xyz<T: SimdElement>(x: T, y: T, z: T) -> Simd<T, 3> {
    Simd::from_array([x, y, z])
}

pub const fn from_xyzw<T: SimdElement>(x: T, y: T, z: T, w: T) -> Simd<T, 4> {
    Simd::from_array([x, y, z, w])
}

pub trait Coords3<T> {
    fn from_xyz(x: T, y: T, z: T) -> Self;
    fn into_tuple(self) -> (T, T, T);
    fn x(&self) -> T;
    fn y(&self) -> T;
    fn z(&self) -> T;
}

impl<T> Coords3<T> for Simd<T, 3>
where
    T: SimdElement,
{
    #[inline(always)]
    fn from_xyz(x: T, y: T, z: T) -> Self {
        Simd::from_array([x, y, z])
    }

    #[inline(always)]
    fn into_tuple(self) -> (T, T, T) {
        (self.x(), self.y(), self.z())
    }

    #[inline(always)]
    fn x(&self) -> T {
        self[0]
    }

    #[inline(always)]
    fn y(&self) -> T {
        self[1]
    }

    #[inline(always)]
    fn z(&self) -> T {
        self[2]
    }
}

pub trait Coords4<T> {
    fn from_xyzw(x: T, y: T, z: T, w: T) -> Self;
    fn into_tuple(self) -> (T, T, T, T);
    fn x(&self) -> T;
    fn y(&self) -> T;
    fn z(&self) -> T;
    fn w(&self) -> T;
}

impl<T> Coords4<T> for Simd<T, 4>
where
    T: SimdElement,
{
    #[inline(always)]
    fn from_xyzw(x: T, y: T, z: T, w: T) -> Self {
        Simd::from_array([x, y, z, w])
    }

    #[inline(always)]
    fn into_tuple(self) -> (T, T, T, T) {
        (self.x(), self.y(), self.z(), self.w())
    }

    #[inline(always)]
    fn x(&self) -> T {
        self[0]
    }

    #[inline(always)]
    fn y(&self) -> T {
        self[1]
    }

    #[inline(always)]
    fn z(&self) -> T {
        self[2]
    }

    #[inline(always)]
    fn w(&self) -> T {
        self[3]
    }
}

pub trait FastFma {
    fn fast_fma(self, a: Self, b: Self) -> Self;
}

impl<const LANES: usize> FastFma for Simd<f32, LANES>
where
    LaneCount<LANES>: SupportedLaneCount,
{
    #[inline(always)]
    fn fast_fma(self, a: Self, b: Self) -> Self {
        if cfg!(target_feature = "fma") {
            self.mul_add(a, b)
        } else {
            self * a + b
        }
    }
}

impl<const LANES: usize> FastFma for Simd<f64, LANES>
where
    LaneCount<LANES>: SupportedLaneCount,
{
    #[inline(always)]
    fn fast_fma(self, a: Self, b: Self) -> Self {
        if cfg!(target_feature = "fma") {
            self.mul_add(a, b)
        } else {
            self * a + b
        }
    }
}

pub trait ToBitMaskExtended {
    type BitMask;

    fn to_bitmask(self) -> Self::BitMask;
    fn from_bitmask(bitmask: Self::BitMask) -> Self;
}

// if we need more impls, we can create a macro to generate them
impl<T> ToBitMaskExtended for Mask<T, 3>
where
    T: MaskElement,
{
    type BitMask = u8;

    fn to_bitmask(self) -> Self::BitMask {
        // This is safe because the alignment should match the next PO2 type, and we are masking off
        // the last value once converted.
        let larger_mask = unsafe { (&self as *const _ as *const Mask<T, 4>).read() };

        larger_mask.to_bitmask() & 0b111
    }

    fn from_bitmask(bitmask: Self::BitMask) -> Self {
        let larger_mask = Mask::<T, 4>::from_bitmask(bitmask);

        // This is safe because the alignment should be correct for the next PO2 type, and we are
        // ignoring the last value.
        unsafe { (&larger_mask as *const _ as *const Mask<T, 3>).read() }
    }
}
