use std::ops;

#[repr(align(16))]
#[derive(Clone, Copy, Debug)]
pub struct Vec3 {
    pub x: f32,
    pub y: f32,
    pub z: f32,
}

impl Vec3 {
    pub fn new(x: f32, y: f32, z: f32) -> Self {
        Vec3 { x, y, z }
    }
}

impl From<[f32; 3]> for Vec3 {
    fn from(array: [f32; 3]) -> Self {
        Vec3 {
            x: array[0],
            y: array[1],
            z: array[2],
        }
    }
}

impl ops::Sub for Vec3 {
    type Output = Vec3;

    fn sub(self, rhs: Self) -> Self::Output {
        Vec3::new(self.x - rhs.x, self.y - rhs.y, self.z - rhs.z)
    }
}

impl ops::Add for Vec3 {
    type Output = Vec3;

    fn add(self, rhs: Self) -> Self::Output {
        Vec3::new(self.x + rhs.x, self.y + rhs.y, self.z + rhs.z)
    }
}

#[repr(align(16))]
#[derive(Clone, Copy, Debug)]
pub struct Vec4 {
    pub x: f32,
    pub y: f32,
    pub z: f32,
    pub w: f32,
}

impl Vec4 {
    pub fn new(x: f32, y: f32, z: f32, w: f32) -> Self {
        Vec4 { x, y, z, w }
    }
}

impl From<[f32; 4]> for Vec4 {
    fn from(array: [f32; 4]) -> Self {
        Vec4 {
            x: array[0],
            y: array[1],
            z: array[2],
            w: array[3],
        }
    }
}

#[repr(align(16))]
// we should really just use ultraviolet
#[derive(Eq, Hash, PartialEq, Clone, Copy)]
pub struct IVec3 {
    pub x: i32,
    pub y: i32,
    pub z: i32,
}

impl IVec3 {
    pub fn new(x: i32, y: i32, z: i32) -> Self {
        IVec3 { x, y, z }
    }

    pub fn abs(&self) -> Self {
        IVec3 {
            x: self.x.abs(),
            y: self.y.abs(),
            z: self.z.abs(),
        }
    }

    pub fn max(&self) -> i32 {
        i32::max(self.x, i32::max(self.y, self.z))
    }
}

impl Into<(i32, i32, i32)> for IVec3 {
    fn into(self) -> (i32, i32, i32) {
        (self.x, self.y, self.z)
    }
}

impl ops::Add for IVec3 {
    type Output = IVec3;

    fn add(self, rhs: Self) -> Self::Output {
        IVec3::new(self.x + rhs.x, self.y + rhs.y, self.z + rhs.z)
    }
}

impl ops::Sub for IVec3 {
    type Output = IVec3;

    fn sub(self, rhs: Self) -> Self::Output {
        IVec3::new(self.x - rhs.x, self.y - rhs.y, self.z - rhs.z)
    }
}

impl ops::Shr for IVec3 {
    type Output = IVec3;

    fn shr(self, rhs: Self) -> Self::Output {
        IVec3::new(self.x >> rhs.x, self.y >> rhs.y, self.z >> rhs.z)
    }
}

impl ops::BitAnd for IVec3 {
    type Output = IVec3;

    fn bitand(self, rhs: Self) -> Self::Output {
        IVec3::new(self.x & rhs.x, self.y & rhs.y, self.z & rhs.z)
    }
}
