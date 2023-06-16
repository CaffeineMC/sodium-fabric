use crate::math::*;
use std::simd::*;

pub struct Frustum {
    planes: [Vec4; 6],
    plane_xs: f32x8,
    plane_ys: f32x8,
    plane_zs: f32x8,
    plane_ws: f32x8,
    position: Vec3,
}

impl Frustum {
    pub fn new(planes: [Vec4; 6], position: Vec3) -> Self {
        let mut plane_xs = Simd::splat(f32::NAN);
        let mut plane_ys = Simd::splat(f32::NAN);
        let mut plane_zs = Simd::splat(f32::NAN);
        let mut plane_ws = Simd::splat(f32::NAN);

        for (i, p) in planes.iter().enumerate() {
            plane_xs[i] = p.x();
            plane_ys[i] = p.y();
            plane_zs[i] = p.z();
            plane_ws[i] = p.w();
        }

        Frustum {
            planes,
            plane_xs,
            plane_ys,
            plane_zs,
            plane_ws,
            position,
        }
    }

    pub fn test_bounding_box(&self, bb: &BoundingBox) -> bool {
        let min_neg = self.position - bb.min;
        let max_neg = self.position - bb.max;

        unsafe {
            let points_x = Self::mask_plane_elements(
                Mask::from_int_unchecked(self.plane_xs.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(min_neg.x()), Simd::splat(max_neg.x())),
            );
            let points_y = Self::mask_plane_elements(
                Mask::from_int_unchecked(self.plane_ys.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(min_neg.y()), Simd::splat(max_neg.y())),
            );
            let points_z = Self::mask_plane_elements(
                Mask::from_int_unchecked(self.plane_zs.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(min_neg.z()), Simd::splat(max_neg.z())),
            );

            let points_dot = self.plane_xs.fast_fma(
                points_x,
                self.plane_ys.fast_fma(points_y, self.plane_zs * points_z),
            );

            points_dot.simd_le(self.plane_ws).to_bitmask() == 0b00111111
        }
    }

    fn mask_plane_elements(value: f32x8) -> f32x8 {
        f32x8::from_bits(value.to_bits() & u32x8::from_array([!0, !0, !0, !0, !0, !0, 0, 0]))
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
