use core_simd::simd::*;

use crate::graph::coords::*;
use crate::graph::LocalSectionIndex;
use crate::math::*;

/// This is called the "local" frustum because it doesn't have a position associated with it.
/// When using this, it is expected that coordinates are relative to the camera rather than the
/// world origin.
pub struct LocalFrustum {
    plane_xs: f32x6,
    plane_ys: f32x6,
    plane_zs: f32x6,
    plane_ws: f32x6,
}

impl LocalFrustum {
    pub fn new(planes: [f32x6; 4]) -> Self {
        LocalFrustum {
            plane_xs: planes[0],
            plane_ys: planes[1],
            plane_zs: planes[2],
            plane_ws: planes[3],
        }
    }

    #[inline(always)]
    pub fn test_local_bounding_box(&self, bb: &LocalBoundingBox) -> BoundsCheckResult {
        unsafe {
            // The unsafe mask shenanigans here just checks if the sign bit is set for each lane.
            // This is faster than doing a manual comparison with something like simd_gt, and compiles
            // down to 1 instruction each (vmaskmovps) on x86 machines with AVX.

            let points_x =
                Mask::from_int_unchecked(self.plane_xs.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(bb.min.x()), Simd::splat(bb.max.x()));

            let points_y =
                Mask::from_int_unchecked(self.plane_ys.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(bb.min.y()), Simd::splat(bb.max.y()));

            let points_z =
                Mask::from_int_unchecked(self.plane_zs.to_bits().cast::<i32>() >> Simd::splat(31))
                    .select(Simd::splat(bb.min.z()), Simd::splat(bb.max.z()));

            let points_dot = self.plane_xs.fast_fma(
                points_x,
                self.plane_ys.fast_fma(points_y, self.plane_zs * points_z),
            );

            let int_result = points_dot.simd_ge(-self.plane_ws).to_bitmask();

            // janky way to create the enum result branchlessly
            let enum_int: u8 = if int_result > 0b000000 { 1 } else { 0 }
                + if int_result == 0b111111 { 1 } else { 0 };

            BoundsCheckResult::from_int_unchecked(enum_int)
        }
    }
}
