#![allow(non_camel_case_types)]

use std::cmp::Eq;
use std::hash::Hash;
use std::ops::*;

use core_simd::simd::*;
use std_float::StdFloat;

pub const X: usize = 0;
pub const Y: usize = 1;
pub const Z: usize = 2;
pub const W: usize = 3;

// the most common non-po2 length we use is 3, so we create shorthands for it
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

// additional useful shorthands
pub type f32x6 = Simd<f32, 6>;

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
ays)]
    fn x(&self) -
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
        self[X]
    }

    #[inline(always)]
    fn y(&self) -> T {
        self[Y]
    }

    #[inline(always)]
    fn z(&self) -> T {
        self[Z]
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
        self[X]
    }

    #[inline(always)]
    fn y(&self) -> T {
        self[Y]
    }

    #[inline(always)]
    fn z(&self) -> T {
        self[Z]
    }

    #[inline(always)]
    fn w(&self) -> T {
        self[W]
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

macro_rules! bitmask_macro {
    ($t:ty, [$($len:expr),+]) => {
        $(
            impl<T> ToBitMaskExtended for Mask<T, $len>
            where
                T: MaskElement,
            {
                type BitMask = $t;

                fn to_bitmask(self) -> Self::BitMask {
                    const NEXT_PO2_LANES: usize = 1_usize << (u8::BITS - ($len - 1_u8).leading_zeros());

                    // This is safe because the alignment should match the next PO2 type, and we are masking off
                    // the last value once converted.
                    let larger_mask =
                        unsafe { (&self as *const _ as *const Mask<T, { NEXT_PO2_LANES }>).read() };

                    larger_mask.to_bitmask() & !(<$t>::MAX << $len)
                }

                fn from_bitmask(bitmask: Self::BitMask) -> Self {
                    const NEXT_PO2_LANES: usize = 1_usize << (u8::BITS - ($len - 1_u8).leading_zeros());

                    let larger_mask = Mask::<T, { NEXT_PO2_LANES }>::from_bitmask(bitmask);

                    // This is safe because the alignment should be correct for the next PO2 type, and we are
                    // ignoring the last value.
                    unsafe { (&larger_mask as *const _ as *const Mask<T, $len>).read() }
                }
            }
        )+
    };
}

bitmask_macro!(u8, [3, 5, 6, 7]);
bitmask_macro!(u16, [9, 10, 11, 12, 13, 14, 15]);
bitmask_macro!(
    u32,
    [17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31]
);
bitmask_macro!(
    u64,
    [
        33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
        56, 57, 58, 59, 60, 61, 62, 63
    ]
);
