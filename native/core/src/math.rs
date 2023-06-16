use std::cmp::Eq;
use std::hash::Hash;
use std::ops::*;
use std::simd::*;

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct Vec3(f32x4);

impl Vec3 {
    #[inline(always)]
    pub fn splat(value: f32) -> Self {
        Vec3(f32x4::from_array([value, value, value, 0.0]))
    }

    #[inline(always)]
    pub fn new(x: f32, y: f32, z: f32) -> Self {
        Vec3::from_array([x, y, z])
    }

    #[inline(always)]
    pub fn from_array(array: [f32; 3]) -> Self {
        Vec3(f32x4::from_array([array[0], array[1], array[2], 0.0]))
    }

    #[inline(always)]
    pub fn floor(&self) -> Self {
        Vec3(self.0.floor())
    }

    #[inline(always)]
    pub fn x(&self) -> f32 {
        self.0[0]
    }

    #[inline(always)]
    pub fn y(&self) -> f32 {
        self.0[1]
    }

    #[inline(always)]
    pub fn z(&self) -> f32 {
        self.0[2]
    }

    #[inline(always)]
    pub fn as_int(&self) -> IVec3 {
        IVec3(self.0.cast())
    }

    #[inline(always)]
    pub unsafe fn as_int_unchecked(&self) -> IVec3 {
        IVec3(self.0.to_int_unchecked())
    }
}

impl Add for Vec3 {
    type Output = Vec3;

    #[inline(always)]
    fn add(self, rhs: Self) -> Self::Output {
        Vec3(self.0 + rhs.0)
    }
}

impl Sub for Vec3 {
    type Output = Vec3;

    #[inline(always)]
    fn sub(self, rhs: Self) -> Self::Output {
        Vec3(self.0 - rhs.0)
    }
}

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct Vec4(f32x4);

impl Vec4 {
    #[inline(always)]
    pub fn splat(value: f32) -> Self {
        Vec4(f32x4::splat(value))
    }

    #[inline(always)]
    pub fn new(x: f32, y: f32, z: f32, w: f32) -> Self {
        Vec4::from_array([x, y, z, w])
    }

    #[inline(always)]
    pub fn from_array(array: [f32; 4]) -> Self {
        Vec4(f32x4::from_array(array))
    }

    #[inline(always)]
    pub fn floor(&self) -> Self {
        Vec4(self.0.floor())
    }

    #[inline(always)]
    pub fn x(&self) -> f32 {
        self.0[0]
    }

    #[inline(always)]
    pub fn y(&self) -> f32 {
        self.0[1]
    }

    #[inline(always)]
    pub fn z(&self) -> f32 {
        self.0[2]
    }

    #[inline(always)]
    pub fn w(&self) -> f32 {
        self.0[3]
    }
}

impl Add for Vec4 {
    type Output = Vec4;

    #[inline(always)]
    fn add(self, rhs: Self) -> Self::Output {
        Vec4(self.0 + rhs.0)
    }
}

impl Sub for Vec4 {
    type Output = Vec4;

    #[inline(always)]
    fn sub(self, rhs: Self) -> Self::Output {
        Vec4(self.0 - rhs.0)
    }
}

#[derive(Copy, Clone, Debug)]
#[repr(transparent)]
pub struct IVec3(i32x4);

impl IVec3 {
    const LANE_MASK: i32x4 = i32x4::from_array([!0, !0, !0, 0]);

    #[inline(always)]
    pub const fn splat(value: i32) -> Self {
        IVec3(i32x4::from_array([value, value, value, 0]))
    }

    #[inline(always)]
    pub const fn new(x: i32, y: i32, z: i32) -> Self {
        IVec3(i32x4::from_array([x, y, z, 0]))
    }

    #[inline(always)]
    pub fn x(&self) -> i32 {
        self.0[0]
    }

    #[inline(always)]
    pub fn y(&self) -> i32 {
        self.0[1]
    }

    #[inline(always)]
    pub fn z(&self) -> i32 {
        self.0[2]
    }

    #[inline(always)]
    pub fn abs(&self) -> Self {
        IVec3(self.0.abs())
    }

    #[inline(always)]
    pub fn max_element(&self) -> i32 {
        (self.0 & Self::LANE_MASK).reduce_max()
    }

    #[inline(always)]
    pub fn as_float(&self) -> Vec3 {
        Vec3(self.0.cast())
    }

    #[inline(always)]
    pub fn less_than(&self, other: Self) -> bool {
        Self::check_comparison_mask(self.0.simd_lt(other.0))
    }

    #[inline(always)]
    pub fn less_than_equal(&self, other: Self) -> bool {
        Self::check_comparison_mask(self.0.simd_le(other.0))
    }

    #[inline(always)]
    pub fn greater_than(&self, other: Self) -> bool {
        Self::check_comparison_mask(self.0.simd_gt(other.0))
    }

    #[inline(always)]
    pub fn greater_than_equal(&self, other: Self) -> bool {
        Self::check_comparison_mask(self.0.simd_ge(other.0))
    }

    #[inline(always)]
    fn check_comparison_mask(mask: mask32x4) -> bool {
        (mask.to_bitmask() & 0b111) == 0b111
    }
}

impl Eq for IVec3 {}

impl PartialEq for IVec3 {
    #[inline(always)]
    fn eq(&self, other: &Self) -> bool {
        self.x() == other.x() && self.y() == other.y() && self.z() == other.z()
    }
}

impl Hash for IVec3 {
    #[inline(always)]
    fn hash<H: std::hash::Hasher>(&self, state: &mut H) {
        self.x().hash(state);
        self.y().hash(state);
        self.z().hash(state);
    }
}

impl Default for IVec3 {
    #[inline(always)]
    fn default() -> Self {
        IVec3::splat(0)
    }
}

impl Add for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn add(self, rhs: Self) -> Self::Output {
        IVec3(self.0 + rhs.0)
    }
}

impl Sub for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn sub(self, rhs: Self) -> Self::Output {
        IVec3(self.0 - rhs.0)
    }
}

impl Shr for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn shr(self, rhs: Self) -> Self::Output {
        IVec3(self.0 >> rhs.0)
    }
}

impl ShrAssign<IVec3> for IVec3 {
    #[inline(always)]
    fn shr_assign(&mut self, other: IVec3) {
        *self = *self >> other;
    }
}

impl Shl for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn shl(self, rhs: Self) -> Self::Output {
        IVec3(self.0 << rhs.0)
    }
}

impl BitAnd for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn bitand(self, rhs: Self) -> Self::Output {
        IVec3(self.0 & rhs.0)
    }
}

impl BitAndAssign<IVec3> for IVec3 {
    #[inline(always)]
    fn bitand_assign(&mut self, other: IVec3) {
        *self = *self & other;
    }
}

impl From<IVec3> for (i32, i32, i32) {
    #[inline(always)]
    fn from(value: IVec3) -> Self {
        (value.x(), value.y(), value.z())
    }
}

impl From<IVec3> for i32x4 {
    #[inline(always)]
    fn from(value: IVec3) -> Self {
        value.0 & i32x4::from_array([!0, !0, !0, 0])
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
