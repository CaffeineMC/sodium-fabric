use crate::math::*;
use std::simd::*;

pub struct Frustum {
    plane_xs: f32x8,
    plane_ys: f32x8,
    plane_zs: f32x8,
    plane_ws: f32x8,
    position: Vec3,
}

impl Frustum {
    pub fn new(planes: [Vec4; 6], position: Vec3) -> Self {
        let mut plane_xs = Simd::splat(0.0_f32);
        let mut plane_ys = Simd::splat(0.0_f32);
        let mut plane_zs = Simd::splat(0.0_f32);
        let mut plane_ws = Simd::splat(0.0_f32);

        for (i, p) in planes.iter().enumerate() {
            plane_xs[i] = p.x();
            plane_ys[i] = p.y();
            plane_zs[i] = p.z();
            plane_ws[i] = p.w();
        }

        Frustum {
            plane_xs,
            plane_ys,
            plane_zs,
            plane_ws,
            position,
        }
    }

    pub fn test_bounding_box(self: &Frustum, bb: &BoundingBox) -> bool {
        let min = bb.min - self.position;
        let max = bb.max - self.position;

        unsafe {
            let points_x =
                Mask::from_int_unchecked(self.plane_xs.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(max.x()), Simd::splat(min.x()));
            let points_y =
                Mask::from_int_unchecked(self.plane_ys.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(max.y()), Simd::splat(min.y()));
            let points_z =
                Mask::from_int_unchecked(self.plane_zs.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(max.z()), Simd::splat(min.z()));

            let points_dot = self.plane_xs.fast_fma(
                points_x,
                self.plane_ys.fast_fma(points_y, self.plane_zs * points_z),
            );

            points_dot.simd_ge(self.plane_ws).to_bitmask() == 0b00111111
        }
    }

    pub fn position(&self) -> &Vec3 {
        &self.position
    }
}

pub struct BoundingBox {
    min: Vec3,
    max: Vec3,
}

impl BoundingBox {
    pub fn new(min: Vec3, max: Vec3) -> Self {
        BoundingBox { min, max }
    }
}
