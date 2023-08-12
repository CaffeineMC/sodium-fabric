use std::mem::transmute;
use std::ops::Shr;
use std::thread::current;

use core_simd::simd::*;
use std_float::StdFloat;

use crate::graph::octree::{LEVEL_3_COORD_LENGTH, LEVEL_3_COORD_MASK, LEVEL_3_COORD_SHIFT};
use crate::graph::*;
use crate::math::*;

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct LocalNodeIndex<const LEVEL: u8>(u32);

// XYZXYZXYZXYZXYZXYZXYZXYZ
const LOCAL_NODE_INDEX_X_MASK: u32 = 0b10010010_01001001_00100100;
const LOCAL_NODE_INDEX_Y_MASK: u32 = 0b01001001_00100100_10010010;
const LOCAL_NODE_INDEX_Z_MASK: u32 = 0b00100100_10010010_01001001;

impl<const LEVEL: u8> LocalNodeIndex<LEVEL> {
    #[inline(always)]
    pub fn pack(unpacked: u8x3) -> Self {
        // allocate one byte per bit for each element.
        // each element is still has its individual bits in linear ordering, but the bytes in the
        // vector are in morton ordering.
        #[rustfmt::skip]
            let expanded_linear_bits = simd_swizzle!(
            unpacked,
            [
            //  X, Y, Z
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0,
                2, 1, 0, // LSB
            ]
        );

        // shift each bit into the sign bit for morton ordering
        #[rustfmt::skip]
            let expanded_morton_bits = expanded_linear_bits << Simd::<u8, 24>::from_array(
            [
                7, 7, 7,
                6, 6, 6,
                5, 5, 5,
                4, 4, 4,
                3, 3, 3,
                2, 2, 2,
                1, 1, 1,
                0, 0, 0, // LSB
            ],
        );

        // arithmetic shift to set each whole lane to its sign bit, then shrinking all lanes to bitmask
        let morton_packed = unsafe {
            Mask::<i8, 24>::from_int_unchecked(expanded_morton_bits.cast::<i8>() >> Simd::splat(7))
        }
        .to_bitmask();

        Self(morton_packed)
    }

    #[inline(always)]
    pub fn inc_x(self) -> Self {
        self.inc::<{ LOCAL_NODE_INDEX_X_MASK }>()
    }

    #[inline(always)]
    pub fn inc_y(self) -> Self {
        self.inc::<{ LOCAL_NODE_INDEX_Y_MASK }>()
    }

    #[inline(always)]
    pub fn inc_z(self) -> Self {
        self.inc::<{ LOCAL_NODE_INDEX_Z_MASK }>()
    }

    #[inline(always)]
    pub fn dec_x(self) -> Self {
        self.dec::<{ LOCAL_NODE_INDEX_X_MASK }>()
    }

    #[inline(always)]
    pub fn dec_y(self) -> Self {
        self.dec::<{ LOCAL_NODE_INDEX_Y_MASK }>()
    }

    #[inline(always)]
    pub fn dec_z(self) -> Self {
        self.dec::<{ LOCAL_NODE_INDEX_Z_MASK }>()
    }

    #[inline(always)]
    pub fn inc<const MASK: u32>(self) -> Self {
        // make the other bits in the number 1
        let mut masked = self.0 | !MASK;

        // increment
        masked = masked.wrapping_add(1_u32 << LEVEL);

        // modify only the masked bits in the original number
        Self((self.0 & !MASK) | (masked & MASK))
    }

    #[inline(always)]
    pub fn dec<const MASK: u32>(self) -> Self {
        // make the other bits in the number 0
        let mut masked = self.0 & MASK;

        // decrement
        masked = masked.wrapping_sub(1_u32 << LEVEL);

        // modify only the masked bits in the original number
        Self((self.0 & !MASK) | (masked & MASK))
    }

    #[inline(always)]
    pub fn as_array_offset(&self) -> usize {
        self.0 as usize
    }

    #[inline(always)]
    pub fn iter_lower_nodes<const LOWER_LEVEL: u8>(&self) -> LowerNodeIter<LEVEL, LOWER_LEVEL> {
        LowerNodeIter::new(self)
    }

    #[inline(always)]
    pub fn get_all_neighbors(&self) -> [Self; 6] {
        const DEC_MASKS: Simd<u32, 6> = Simd::from_array([
            LOCAL_NODE_INDEX_X_MASK,
            LOCAL_NODE_INDEX_Y_MASK,
            LOCAL_NODE_INDEX_Z_MASK,
            u32::MAX,
            u32::MAX,
            u32::MAX,
        ]);

        const INC_MASKS: Simd<u32, 6> = Simd::from_array([
            u32::MAX,
            u32::MAX,
            u32::MAX,
            LOCAL_NODE_INDEX_X_MASK,
            LOCAL_NODE_INDEX_Y_MASK,
            LOCAL_NODE_INDEX_Z_MASK,
        ]);

        todo!()
    }

    #[inline(always)]
    pub fn unpack(&self) -> u8x3 {
        // allocate one byte per bit for each element.
        // each element is still has its individual bits in morton ordering, but the bytes in the
        // vector are in linear ordering.
        #[rustfmt::skip]
            let expanded_linear_bits = simd_swizzle!(
            u8x4::from_array(self.0.to_le_bytes()),
            [
                // X
                2, 2, 2, 1, 1, 1, 0, 0,
                // Y
                2, 2, 2, 1, 1, 0, 0, 0,
                // Z
                2, 2, 1, 1, 1, 0, 0, 0, // LSB
            ]
        );

        // shift each bit into the sign bit for morton ordering
        #[rustfmt::skip]
            let expanded_morton_bits = expanded_linear_bits << Simd::<u8, 24>::from_array(
            [
                // X
                0, 3, 6,
                1, 4, 7,
                2, 5,
                // Y
                1, 4, 7,
                2, 5, 0,
                3, 6,
                // Z
                2, 5, 0,
                3, 6, 1,
                4, 7, // LSB
            ],
        );

        // arithmetic shift to set each whole lane to its sign bit, then shrinking all lanes to bitmask
        let linear_packed = unsafe {
            Mask::<i8, 24>::from_int_unchecked(expanded_morton_bits.cast::<i8>() >> Simd::splat(7))
        }
        .to_bitmask();

        u8x3::from_slice(&linear_packed.to_le_bytes()[0..=2])
    }
}

pub struct LowerNodeIter<const LEVEL: u8, const LOWER_LEVEL: u8> {
    current: u32,
    end: u32,
}

impl<const LEVEL: u8, const LOWER_LEVEL: u8> LowerNodeIter<LEVEL, LOWER_LEVEL> {
    fn new(index: &LocalNodeIndex<LEVEL>) -> Self {
        assert!(LEVEL > LOWER_LEVEL);

        let node_size = 1 << (LEVEL * 3);

        Self {
            current: index.0,
            end: index.0 + node_size,
        }
    }
}

impl<const LEVEL: u8, const LOWER_LEVEL: u8> Iterator for LowerNodeIter<LEVEL, LOWER_LEVEL> {
    type Item = LocalNodeIndex<LOWER_LEVEL>;

    fn next(&mut self) -> Option<Self::Item> {
        if self.current >= self.end {
            None
        } else {
            let current = self.current;

            let lower_node_size = 1 << (LOWER_LEVEL * 3);
            self.current += lower_node_size;

            Some(LocalNodeIndex(current))
        }
    }
}

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
    pub iter_node_origin_index: LocalNodeIndex<3>,
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
            section_view_distance <= MAX_VIEW_DISTANCE,
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
        let iter_node_origin_index = LocalNodeIndex::pack(iter_node_origin_coords);

        let view_cube_length = (section_view_distance * 2) + 1;
        // convert to i32 to avoid implicit wrapping, then explicitly wrap
        // todo: should the +1 be here?
        let world_height =
            ((world_top_section_y as i32) - (world_bottom_section_y as i32) + 1) as u8;

        assert!(
            world_height <= MAX_WORLD_HEIGHT,
            "World heights larger than 254 sections are not supported"
        );

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
            iter_node_origin_index,
            iter_node_origin_coords,
            level_3_node_iters,
            iter_region_origin_coords: todo!(),
            region_iters: todo!(),
        }
    }

    #[inline(always)]
    pub fn test_node<const LEVEL: u8>(
        &self,
        local_node_index: LocalNodeIndex<LEVEL>,
    ) -> BoundsCheckResult {
        let local_node_pos = local_node_index.unpack();

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

    // #[inline(always)]
    #[no_mangle]
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

            // if any outside lengths are greater than -w, return OUTSIDE
            // if all inside lengths are greater than -w, return INSIDE
            // otherwise, return PARTIAL
            // NOTE: it is impossible for a lane to be both inside and outside at the same time
            let none_outside = outside_length_sq.simd_ge(-self.plane_ws).to_bitmask() == 0b111111;
            let all_inside = inside_length_sq.simd_ge(-self.plane_ws).to_bitmask() == 0b111111;

            BoundsCheckResult::from_int_unchecked(none_outside as u8 + all_inside as u8)
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
