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
            // These unsafe mask shenanigans just check if the sign bit is set for each lane.
            // This is faster than doing a manual comparison with something like simd_gt.
            let is_neg_x =
                Mask::from_int_unchecked(self.plane_xs.to_bits().cast::<i32>() >> Simd::splat(31));
            let is_neg_y =
                Mask::from_int_unchecked(self.plane_ys.to_bits().cast::<i32>() >> Simd::splat(31));
            let is_neg_z =
                Mask::from_int_unchecked(self.plane_zs.to_bits().cast::<i32>() >> Simd::splat(31));

            let bb_min_x = Simd::splat(bb.min.x());
            let bb_max_x = Simd::splat(bb.max.x());
            let outside_bounds_x = is_neg_x.select(bb_min_x, bb_max_x);
            let inside_bounds_x = is_neg_x.select(bb_max_x, bb_min_x);

            let bb_min_y = Simd::splat(bb.min.y());
            let bb_max_y = Simd::splat(bb.max.y());
            let outside_bounds_y = is_neg_y.select(bb_min_y, bb_max_y);
            let inside_bounds_y = is_neg_y.select(bb_max_y, bb_min_y);

            let bb_min_z = Simd::splat(bb.min.z());
            let bb_max_z = Simd::splat(bb.max.z());
            let outside_bounds_z = is_neg_z.select(bb_min_z, bb_max_z);
            let inside_bounds_z = is_neg_z.select(bb_max_z, bb_min_z);

            let outside_length_sq = self.plane_xs.fast_fma(
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
