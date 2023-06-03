use crate::math::*;

#[derive(Debug)]
pub struct Frustum {
    nx: Vec4,
    px: Vec4,
    ny: Vec4,
    py: Vec4,
    nz: Vec4,
    pz: Vec4,

    position: Vec3,
}

impl Frustum {
    #[rustfmt::skip]
    pub fn test_bounding_box(self: &Frustum, min: Vec3, max: Vec3) -> bool {
        let min = min - self.position;
        let max = max - self.position;

        /*
         * This is an implementation of the "2.4 Basic intersection test" of the mentioned site.
         * It does not distinguish between partially inside and fully inside, though, so the test with the 'p' vertex is omitted.
         */
        return self.nx.x * if self.nx.x < 0.0 { min.x } else { max.x } + self.nx.y * if self.nx.y < 0.0 { min.y } else { max.y } + self.nx.z * if self.nx.z < 0.0 { min.z } else { max.z } >= -self.nx.w &&
               self.px.x * if self.px.x < 0.0 { min.x } else { max.x } + self.px.y * if self.px.y < 0.0 { min.y } else { max.y } + self.px.z * if self.px.z < 0.0 { min.z } else { max.z } >= -self.px.w &&
               self.ny.x * if self.ny.x < 0.0 { min.x } else { max.x } + self.ny.y * if self.ny.y < 0.0 { min.y } else { max.y } + self.ny.z * if self.ny.z < 0.0 { min.z } else { max.z } >= -self.ny.w &&
               self.py.x * if self.py.x < 0.0 { min.x } else { max.x } + self.py.y * if self.py.y < 0.0 { min.y } else { max.y } + self.py.z * if self.py.z < 0.0 { min.z } else { max.z } >= -self.py.w &&
               self.nz.x * if self.nz.x < 0.0 { min.x } else { max.x } + self.nz.y * if self.nz.y < 0.0 { min.y } else { max.y } + self.nz.z * if self.nz.z < 0.0 { min.z } else { max.z } >= -self.nz.w &&
               self.pz.x * if self.pz.x < 0.0 { min.x } else { max.x } + self.pz.y * if self.pz.y < 0.0 { min.y } else { max.y } + self.pz.z * if self.pz.z < 0.0 { min.z } else { max.z } >= -self.pz.w
    }

    pub fn new(planes: &[[f32; 4]; 6], offset: &[f32; 3]) -> Frustum {
        let frustum = Frustum {
            nx: Vec4::from(planes[0]),
            px: Vec4::from(planes[1]),
            ny: Vec4::from(planes[2]),
            py: Vec4::from(planes[3]),
            nz: Vec4::from(planes[4]),
            pz: Vec4::from(planes[5]),

            position: Vec3::from(*offset),
        };

        frustum
    }

    pub fn position(&self) -> &Vec3 {
        &self.position
    }
}
