use std::mem::transmute;
use std::ops::Shr;

use core_simd::simd::*;
use std_float::StdFloat;

use crate::graph::octree::{LEVEL_3_COORD_LENGTH, LEVEL_3_COORD_MASK, LEVEL_3_COORD_SHIFT};
use crate::graph::*;
use crate::math::*;

pub struct LocalCoordinateContext {
    frustum: LocalFrustum,

    // the camera coords relative to the local origin, which is the (0, 0, 0) point of the
    // 256x256x256 cube we hold the section data in.
    pub camera_coords: f32x3,
    pub camera_section_coords: u8x3,

    fog_distance_squared: f32,

    world_bottom_section_y: i8,
    world_top_section_y: i8,

    // this is the index that encompasses the corner of the view distance bounding box where the
    // coordinate for each axis is closest to negative infinity, and truncated to the origin of the
    // level 3 node it's contained in.
    pub iter_node_origin_idx: LocalNodeIndex,
    pub iter_node_origin_coords: u8x3,
    pub level_3_node_iters: u8x3,

    // similar to the previous, but truncated to the closest region coord and relative to the world
    // origin, rather than the data structure origin.
    pub iter_region_origin_coords: i32x3,
    pub region_iters: u8x3,
}

impl LocalCoordinateContext {
    pub const NODE_HEIGHT_OFFSET: u8 = 128;

    pub fn new(
        world_pos: f64x3,
        planes: [f32x6; 4],
        fog_distance: f32,
        section_view_distance: u8,
        world_bottom_section_y: i8,
        world_top_section_y: i8,
    ) -> Self {
        assert!(
            section_view_distance <= 127,
            "View distances above 127 are not supported"
        );

        let frustum = LocalFrustum::new(planes);

        let camera_section_global_coords = world_pos.floor().cast::<i64>() >> i64x3::splat(4);

        // the cast to u8 puts it in the local coordinate space by effectively doing a mod 256
        let camera_section_coords = camera_section_global_coords.cast::<u8>();
        let camera_coords = (world_pos % f64x3::splat(256.0)).cast::<f32>();

        let iter_section_origin_coords = simd_swizzle!(
            camera_section_coords - Simd::splat(section_view_distance),
            Simd::splat(world_bottom_section_y as u8),
            [First(X), Second(0), First(Z)]
        );

        let iter_node_origin_coords = iter_section_origin_coords & u8x3::splat(LEVEL_3_COORD_MASK);
        let iter_node_origin_idx = LocalNodeIndex::pack(iter_node_origin_coords);

        let view_cube_length = (section_view_distance * 2) + 1;
        // convert to i32 to avoid implicit wrapping, then explicitly wrap
        // todo: should the +1 be here?
        let world_height =
            ((world_top_section_y as i32) - (world_bottom_section_y as i32) + 1) as u8;
        // the add is done to make sure we round up during truncation
        let level_3_node_iters = (u8x3::from_xyz(view_cube_length, world_height, view_cube_length)
            + Simd::splat(LEVEL_3_COORD_LENGTH - 1))
            >> Simd::splat(LEVEL_3_COORD_SHIFT);

        // this lower bound may not be necessary
        let fog_distance_capped = fog_distance.min(((section_view_distance + 1) << 4) as f32);
        let fog_distance_squared = fog_distance_capped * fog_distance_capped;

        LocalCoordinateContext {
            frustum,
            camera_coords,
            camera_section_coords,
            fog_distance_squared,
            world_bottom_section_y,
            world_top_section_y,
            iter_node_origin_idx,
            iter_node_origin_coords,
            level_3_node_iters,
            iter_region_origin_coords: todo!(),
            region_iters: todo!(),
        }
    }

    // #[no_mangle]
    // pub fn test(&self, local_node_pos: u8x3) -> BoundsCheckResult {
    //     self.check_node::<3>(local_node_pos)
    // }

    #[inline(always)]
    pub fn check_node<const LEVEL: u8>(&self, local_node_pos: u8x3) -> BoundsCheckResult {
        let bounds = self.node_get_local_bounds::<LEVEL>(local_node_pos);

        let mut result = self.bounds_inside_fog::<LEVEL>(&bounds);
        // should continue doing tests if we're already known to be outside? or is that more
        // of a detriment?
        result = result.combine(self.frustum.test_local_bounding_box(&bounds));
        result = result.combine(self.bounds_inside_world_height::<LEVEL>(local_node_pos.y() as i8));

        result
    }

    #[inline(always)]
    fn bounds_inside_world_height<const LEVEL: u8>(
        &self,
        local_node_height: i8,
    ) -> BoundsCheckResult {
        let node_min_y = local_node_height as i32;
        let node_max_y = node_min_y + (1 << LEVEL) - 1;
        let world_min_y = self.world_bottom_section_y as i32;
        let world_max_y = self.world_top_section_y as i32;

        let min_in_bounds = node_min_y >= world_min_y && node_min_y <= world_max_y;
        let max_in_bounds = node_max_y >= world_min_y && node_max_y <= world_max_y;

        // in normal circumstances, this really shouldn't ever return OUTSIDE
        unsafe { BoundsCheckResult::from_int_unchecked(min_in_bounds as u8 + max_in_bounds as u8) }
    }

    // this only cares about the x and z axis
    #[inline(always)]
    fn bounds_inside_fog<const LEVEL: u8>(
        &self,
        local_bounds: &LocalBoundingBox,
    ) -> BoundsCheckResult {
        // find closest to (0,0) because the bounding box coordinates are relative to the camera
        let closest_in_chunk = local_bounds
            .min
            .abs()
            .simd_lt(local_bounds.max.abs())
            .select(local_bounds.min, local_bounds.max);

        let furthest_in_chunk = {
            let adjusted_int = unsafe {
                // minus 1 if the input is negative
                // SAFETY: values will never be out of range
                closest_in_chunk.to_int_unchecked::<i32>()
                    + (closest_in_chunk.to_bits().cast::<i32>() >> Simd::splat(31))
            };

            let add_bit = Simd::splat(0b1000 << LEVEL);
            // additive is nonzero if the bit is *not* set
            let additive = ((adjusted_int & add_bit) ^ add_bit) << Simd::splat(1);

            // set the bottom (4 + LEVEL) bits to 0
            let bitmask = Simd::splat(-1 << (4 + LEVEL));

            ((adjusted_int + additive) & bitmask).cast::<f32>()
        };

        // combine operations and single out the XZ lanes on both extrema from here.
        // also, we don't have to subtract from the camera pos because the bounds are already
        // relative to it
        let differences = simd_swizzle!(
            closest_in_chunk,
            furthest_in_chunk,
            [First(X), First(Z), Second(X), Second(Z)]
        );
        let differences_squared = differences * differences;

        // add Xs and Zs
        let distances_squared =
            simd_swizzle!(differences_squared, [0, 2]) + simd_swizzle!(differences_squared, [1, 3]);

        // janky way of calculating the result from the two points
        unsafe {
            BoundsCheckResult::from_int_unchecked(
                distances_squared
                    .simd_lt(f32x2::splat(self.fog_distance_squared))
                    .select(u32x2::splat(1), u32x2::splat(0))
                    .reduce_sum() as u8,
            )
        }
    }

    #[inline(always)]
    fn node_get_local_bounds<const LEVEL: u8>(&self, local_node_pos: u8x3) -> LocalBoundingBox {
        let min_pos = local_node_pos.cast::<f32>()
            + local_node_pos
                .simd_lt(self.iter_node_origin_coords)
                .cast()
                .select(Simd::splat(256.0_f32), Simd::splat(0.0_f32))
            - self.camera_coords;

        let max_pos = min_pos + Simd::splat((16 << LEVEL) as f32);

        LocalBoundingBox {
            min: min_pos,
            max: max_pos,
        }
    }

    #[inline(always)]
    pub fn get_valid_directions(&self, local_section_pos: u8x3) -> GraphDirectionSet {
        let negative = local_section_pos.simd_le(self.camera_section_coords);
        let positive = local_section_pos.simd_ge(self.camera_section_coords);

        GraphDirectionSet::from(negative.to_bitmask() | (positive.to_bitmask() << 3))
    }
}

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
                outside_bounds_x,
                self.plane_ys
                    .fast_fma(outside_bounds_y, self.plane_zs * outside_bounds_z),
            );

            let inside_length_sq = self.plane_xs.fast_fma(
                inside_bounds_x,
                self.plane_ys
                    .fast_fma(inside_bounds_y, self.plane_zs * inside_bounds_z),
            );

            if outside_length_sq.simd_ge(-self.plane_ws).to_bitmask() == 0b111111 {
                if inside_length_sq.simd_ge(-self.plane_ws).to_bitmask() == 0b111111 {
                    BoundsCheckResult::Inside
                } else {
                    BoundsCheckResult::Partial
                }
            } else {
                if inside_length_sq.simd_ge(-self.plane_ws).to_bitmask() == 0b111111 {
                    panic!("BAD!!!!!");
                }
                BoundsCheckResult::Outside
            }
        }
    }
}

#[repr(u8)]
pub enum BoundsCheckResult {
    Outside = 0,
    Partial = 1,
    Inside = 2,
}

impl BoundsCheckResult {
    /// SAFETY: if out of bounds, this will fail to assert in debug mode
    pub unsafe fn from_int_unchecked(val: u8) -> Self {
        debug_assert!(val <= 2);
        transmute(val)
    }

    pub fn combine(self, rhs: Self) -> Self {
        // SAFETY: given 2 valid inputs, the result will always be valid
        unsafe { Self::from_int_unchecked((self as u8).min(rhs as u8)) }
    }
}

/// Relative to the camera position
pub struct LocalBoundingBox {
    pub min: f32x3,
    pub max: f32x3,
}
