use std::mem::transmute;
use std::ops::Shr;

use core_simd::simd::*;
use std_float::StdFloat;

use crate::graph::frustum::*;
use crate::graph::*;
use crate::math::*;

pub struct LocalCoordinateContext {
    frustum: LocalFrustum,

    // the camera coords relative to the local origin, which is the (0, 0, 0) point of the
    // 256x256x256 cube we hold the section data in.
    camera_coords: f32x3,
    camera_section_coords: u8x3,

    // this is the section that encompasses the corner of the view distance bounding box where the
    // coordinate for each axis is closest to negative infinity, and truncated to the origin of the
    // level 3 node it's contained in.
    iter_start_section_idx: LocalSectionIndex,
    iter_start_section_coords: u8x3,

    // similar to the previous, but rounded to the closest region coord and relative to the world
    // origin, rather than the data structure origin.
    iter_start_region_coord: i32x3,

    fog_distance_squared: f32,

    world_bottom_section_y: i32,
    world_top_section_y: i32,
}

impl LocalCoordinateContext {
    pub fn new(
        world_pos: f64x3,
        planes: [f32x6; 4],
        fog_distance: f32,
        section_view_distance: i32,
        world_bottom_section_y: i32,
        world_top_section_y: i32,
    ) -> Self {
        let frustum = LocalFrustum::new(planes);

        let camera_section_world_coords = world_pos.floor().cast::<i64>() >> i64x3::splat(4);

        // the cast to u8 puts it in the local coordinate space
        let camera_section_coords = camera_section_world_coords.cast::<u8>();

        let camera_coords = (camera_section_coords.cast::<i32>() << i32x3::splat(4)).cast::<f32>();

        let fog_distance_capped = fog_distance.min(((section_view_distance + 1) << 4) as f32);
        let fog_distance_squared = fog_distance_capped * fog_distance_capped;

        LocalCoordinateContext {
            frustum,
            camera_coords,
            camera_section_coords,
            iter_start_section_idx: LocalSectionIndex(),
            iter_start_region_coord: Default::default(),
            fog_distance_squared,
            world_bottom_section_y,
            world_top_section_y,
        }
    }

    #[inline(always)]
    pub fn bounds_inside_world(&self, local_bounds: LocalBoundingBox) -> BoundsCheckResult {
        // on the x and z axis, check if the bounding box is inside the fog.
        let camera_coords_xz = simd_swizzle!(self.camera_coords, [X, Z, X, Z]);

        // the first pair of lanes is the closest xz to the player in the chunk,
        // the second pair of lanes is the furthest xz from the player in the chunk
        let extrema_in_chunk = camera_coords_xz
            .simd_min(simd_swizzle!(
                local_bounds.min,
                local_bounds.max,
                [Second(X), Second(Z), First(X), First(Z)]
            ))
            .simd_max(simd_swizzle!(
                local_bounds.min,
                local_bounds.max,
                [First(X), First(Z), Second(X), Second(Z)]
            ));

        let differences = camera_coords_xz - extrema_in_chunk;
        let differences_squared = differences * differences;

        // lane 1 is closest dist, lane 2 is furthest dist
        let distances_squared =
            simd_swizzle!(differences_squared, [0, 2]) + simd_swizzle!(differences_squared, [1, 3]);

        let fog_result = unsafe {
            BoundsCheckResult::from_int_unchecked(
                distances_squared
                    .simd_lt(f32x2::splat(self.fog_distance_squared))
                    .select(u32x2::splat(1), u32x2::splat(0))
                    .reduce_sum() as u8,
            )
        };

        // on the y axis, check if the bounding box is inside the world height.
        let height_result = BoundsCheckResult::Outside;

        BoundsCheckResult::combine(fog_result, height_result)
    }

    #[inline(always)]
    pub fn get_valid_directions(&self, position: u8x3) -> GraphDirectionSet {
        let negative = position.simd_le(self.camera_section_coords);
        let positive = position.simd_ge(self.camera_section_coords);

        GraphDirectionSet::from(negative.to_bitmask() | (positive.to_bitmask() << 3))
    }

    #[inline(always)]
    pub fn node_get_local_bounds<const LEVEL: usize>(
        &self,
        local_origin: u8x3,
    ) -> LocalBoundingBox {
        let min_pos = local_origin.cast::<f32>()
            + local_origin
                .simd_lt(self.iter_start_section_coords)
                .cast()
                .select(Simd::splat(256.0_f32), Simd::splat(0.0_f32));

        let max_pos = min_pos + Simd::splat((16 << LEVEL) as f32);

        LocalBoundingBox {
            min: min_pos,
            max: max_pos,
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

pub struct LocalBoundingBox {
    pub min: f32x3,
    pub max: f32x3,
}
