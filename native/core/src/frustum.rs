use crate::math::*;

pub struct Frustum {
    planes: [Vec4; 6],
    position: Vec3,
}

impl Frustum {
    pub fn new(planes: [Vec4; 6], position: Vec3) -> Self {
        Frustum { planes, position }
    }

    pub fn test_bounding_box(self: &Frustum, bb: &BoundingBox) -> bool {
        let center = bb.center - self.position;
        let size = bb.size;

        let mut result = true;

        for p in &self.planes {
            let x = center.x() + size.x().copysign(p.x());
            let y = center.y() + size.y().copysign(p.y());
            let z = center.z() + size.z().copysign(p.z());

            result &= p.x() * x + p.y() * y + p.z() * z >= -p.w();
        }

        result
    }

    pub fn position(&self) -> &Vec3 {
        &self.position
    }
}

pub struct BoundingBox {
    center: Vec3,
    size: Vec3
}

impl BoundingBox {
    pub fn new(center: Vec3, size: Vec3) -> Self {
        BoundingBox { center, size }
    }
}