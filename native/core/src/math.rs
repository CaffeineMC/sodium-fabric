use std::cmp::Eq;
use std::hash::Hash;
use std::ops::{self, *};
use std::simd::*;

#[derive(Clone, Copy)]
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

    pub fn as_int(&self) -> IVec3 {
        IVec3(self.0.cast())
    }
}

impl ops::Add for Vec3 {
    type Output = Vec3;

    #[inline(always)]
    fn add(self, rhs: Self) -> Self::Output {
        Vec3(self.0 + rhs.0)
    }
}

impl ops::Sub for Vec3 {
    type Output = Vec3;

    #[inline(always)]
    fn sub(self, rhs: Self) -> Self::Output {
        Vec3(self.0 - rhs.0)
    }
}

#[derive(Clone, Copy)]
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

impl ops::Add for Vec4 {
    type Output = Vec4;

    #[inline(always)]
    fn add(self, rhs: Self) -> Self::Output {
        Vec4(self.0 + rhs.0)
    }
}

impl ops::Sub for Vec4 {
    type Output = Vec4;

    #[inline(always)]
    fn sub(self, rhs: Self) -> Self::Output {
        Vec4(self.0 - rhs.0)
    }
}

#[derive(Copy, Clone, Debug)]
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

impl ops::Add for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn add(self, rhs: Self) -> Self::Output {
        IVec3(self.0 + rhs.0)
    }
}

impl ops::Sub for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn sub(self, rhs: Self) -> Self::Output {
        IVec3(self.0 - rhs.0)
    }
}

impl ops::Shr for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn shr(self, rhs: Self) -> Self::Output {
        IVec3(self.0 >> rhs.0)
    }
}

impl ops::Shl for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn shl(self, rhs: Self) -> Self::Output {
        IVec3(self.0 << rhs.0)
    }
}

impl ops::BitAnd for IVec3 {
    type Output = IVec3;

    #[inline(always)]
    fn bitand(self, rhs: Self) -> Self::Output {
        IVec3(self.0 & rhs.0)
    }
}

impl From<IVec3> for (i32, i32, i32) {
    #[inline(always)]
    fn from(value: IVec3) -> Self {
        (value.x(), value.y(), value.z())
    }
}
