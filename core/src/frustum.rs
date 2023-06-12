use crate::math::*;

pub struct Frustum {
    planes: [Vec4; 6],
    position: Vec3,
}

impl Frustum {
    pub fn new(planes: [Vec4; 6], position: Vec3) -> Self {
        Frustum { planes, position }
    }

    pub fn test_bounding_box(self: &Frustum, min: Vec3, max: Vec3) -> bool {
        let min = min - self.position;
        let max = max - self.position;

        let mut result = true;

        for p in &self.planes {
            let x = if p.x() < 0.0 { min.x() } else { max.x() };
            let y = if p.y() < 0.0 { min.y() } else { max.y() };
            let z = if p.z() < 0.0 { min.z() } else { max.z() };

            result &= p.x() * x + p.y() * y + p.z() * z >= -p.w();
        }

        result
    }

    pub fn position(&self) -> &Vec3 {
        &self.position
    }
}
