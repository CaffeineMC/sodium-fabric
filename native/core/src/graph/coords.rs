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

    // similar to the previous, but rounded to the closest region coord and relative to the worldFd
    // origin, rather than the data structure origin.
    iter_start_region_coords: i32x3,

    fog_distance_squared: f32,

    world_bottom_section_y: i8,
    world_top_section_y: i8,
}

impl LocalCoordinateContext {
    pub const NODE_HEIGHT_OFFSET: u8 = 128;

    pub fn new(
        world_pos: f64x3,
        planes: [f32x6; 4],
        fog_distance: f32,
        section_view_distance: i32,
        world_bottom_section_y: i8,
        world_top_section_y: i8,
    ) -> Self {
        let frustum = LocalFrustum::new(planes);

        let camera_section_world_coords = world_pos.floor().cast::<i64>() >> i64x3::splat(4);

        // the cast to u8 puts it in the local coordinate space by effectively doing a mod 256
        let camera_section_coords = camera_section_world_coords.cast::<u8>();
        let camera_coords = (world_pos % f64x3::splat(256.0)).cast::<f32>();

        let iter_start_section_coords = simd_swizzle!(
            camera_section_coords - Simd::splat(section_view_distance),
            Simd::splat(world_bottom_section_y as u8),
            [First(X), Second(0), First(Z)]
        );
        let iter_start_section_idx = LocalSectionIndex::pack(iter_start_section_coords);

        // this lower bound may not be necessary
        let fog_distance_capped = fog_distance.min(((section_view_distance + 1) << 4) as f32);
        let fog_distance_squared = fog_distance_capped * fog_distance_capped;

        LocalCoordinateContext {
            frustum,
            camera_coords,
            camera_section_coords,
            iter_start_section_idx,
            iter_start_section_coords,
            iter_start_region_coords,
            fog_distance_squared,
            world_bottom_section_y,
            world_top_section_y,
        }
    }

    #[inline(always)]
    pub fn bounds_inside_world_height<const LEVEL: u8>(
        &self,
        local_node_height: i8,
    ) -> BoundsCheckResult {
        let node_min_y = local_node_height as i32;
        let node_max_y = node_min_y + (1 << LEVEL) - 1;
        let world_min_y = self.world_bottom_section_y as i32;
        let world_max_y = self.world_top_section_y as i32;

        let min_in_bounds = node_min_y >= world_min_y && node_min_y <= world_max_y;
        let max_in_bounds = node_max_y >= world_min_y && node_max_y <= world_max_y;

        unsafe { BoundsCheckResult::from_int_unchecked(min_in_bounds as u8 + max_in_bounds as u8) }
    }

    // this only cares about the x and z axis
    #[inline(always)]
    pub fn bounds_inside_fog<const LEVEL: u8>(
        &self,
        local_bounds: LocalBoundingBox,
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
    pub fn get_valid_directions(&self, position: u8x3) -> GraphDirectionSet {
        let negative = position.simd_le(self.camera_section_coords);
        let positive = position.simd_ge(self.camera_section_coords);

        GraphDirectionSet::from(negative.to_bitmask() | (positive.to_bitmask() << 3))
    }

    #[inline(always)]
    pub fn node_get_local_bounds<const LEVEL: u8>(&self, local_node_pos: u8x3) -> LocalBoundingBox {
        let min_pos = local_node_pos.cast::<f32>()
            + local_node_pos
                .simd_lt(self.iter_start_section_coords)
                .cast()
                .select(Simd::splat(256.0_f32), Simd::splat(0.0_f32))
            - self.camera_coords;

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

/// Relative to the camera position
pub struct LocalBoundingBox {
    pub min: f32x3,
    pub max: f32x3,
}
