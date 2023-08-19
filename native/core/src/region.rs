use std::mem::MaybeUninit;

use core_simd::simd::*;

use crate::ffi::*;
use crate::graph::local::LocalCoordContext;
use crate::graph::SortedSearchResults;
use crate::math::*;

pub const SECTIONS_IN_REGION: usize = 8 * 4 * 8;
pub const REGION_COORD_SHIFT: u8x3 = Simd::from_array([3, 2, 3]);
pub const REGION_MASK: u8x3 = Simd::from_array([0b11111000, 0b11111100, 0b11111000]);

pub const UNDEFINED_REGION_COORDS: (i32, i32, i32) = (i32::MIN, i32::MIN, i32::MIN);

// the graph should be region-aligned, so this should always hold true
pub const REGIONS_IN_GRAPH: usize = (256 / 8) * (256 / 4) * (256 / 8);

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct LocalRegionIndex(u16);

impl LocalRegionIndex {
    const X_MASK_SINGLE: u16 = 0b11111000;
    const Y_MASK_SINGLE: u16 = 0b11111100;
    const Z_MASK_SINGLE: u16 = 0b11111000;

    const X_MASK_SHIFT_LEFT: u16 = 8;
    const Y_MASK_SHIFT_LEFT: u16 = 3;
    const Z_MASK_SHIFT_RIGHT: u16 = 3;

    #[inline(always)]
    pub fn from_local_section(local_section_coord: u8x3) -> Self {
        Self(
            ((local_section_coord.cast::<u16>()
                & u16x3::from_array([
                    Self::X_MASK_SINGLE,
                    Self::Y_MASK_SINGLE,
                    Self::Z_MASK_SINGLE,
                ]) << u16x3::from_array([Self::X_MASK_SHIFT_LEFT, Self::Y_MASK_SHIFT_LEFT, 0]))
                >> u16x3::from_array([0, 0, Self::Z_MASK_SHIFT_RIGHT]))
            .reduce_or(),
        )
    }
}

#[derive(Clone, Copy)]
#[repr(transparent)]
pub struct RegionSectionIndex(u8);

impl RegionSectionIndex {
    const X_MASK_SINGLE: u8 = 0b00000111;
    const Y_MASK_SINGLE: u8 = 0b00000011;
    const Z_MASK_SINGLE: u8 = 0b00000111;

    const X_MASK_SHIFT: u8 = 5;
    const Y_MASK_SHIFT: u8 = 3;
    const Z_MASK_SHIFT: u8 = 0;

    #[inline(always)]
    pub fn from_local_section(local_section_coord: u8x3) -> Self {
        Self(
            (local_section_coord
                & u8x3::from_array([
                    Self::X_MASK_SINGLE,
                    Self::Y_MASK_SINGLE,
                    Self::Z_MASK_SINGLE,
                ]) << u8x3::from_array([
                    Self::X_MASK_SHIFT,
                    Self::Y_MASK_SHIFT,
                    Self::Z_MASK_SHIFT,
                ]))
            .reduce_or(),
        )
    }
}

#[derive(Copy, Clone)]
#[repr(C)]
pub struct RegionDrawBatch {
    region_coord: (i32, i32, i32),
    sections: CInlineVec<RegionSectionIndex, SECTIONS_IN_REGION>,
}

impl Default for RegionDrawBatch {
    fn default() -> Self {
        Self {
            region_coord: UNDEFINED_REGION_COORDS,
            sections: Default::default(),
        }
    }
}

pub struct StagingRegionDrawBatches {
    draw_batches: [RegionDrawBatch; REGIONS_IN_GRAPH],
    ordered_batch_indices: CInlineVec<LocalRegionIndex, REGIONS_IN_GRAPH>,
}

impl StagingRegionDrawBatches {
    pub fn add_section(&mut self, coord_context: &LocalCoordContext, local_section_coord: u8x3) {
        let local_region_index = LocalRegionIndex::from_local_section(local_section_coord);

        let draw_batch = &mut self.draw_batches[local_region_index.0 as usize];

        let global_region_pos = (coord_context.origin_region_coords
            + (local_section_coord & REGION_MASK >> REGION_COORD_SHIFT).cast::<i32>())
        .into_tuple();
        draw_batch.region_coord = global_region_pos;

        let region_section_index = RegionSectionIndex::from_local_section(local_section_coord);
        draw_batch.sections.push(region_section_index);

        self.ordered_batch_indices.push(local_region_index);
    }

    pub fn get_sorted_batches(&self) -> SortedSearchResults {
        let mut sorted_batches = CInlineVec::<RegionDrawBatch, REGIONS_IN_GRAPH>::default();

        for &index in self.ordered_batch_indices.get_slice() {
            sorted_batches.push(self.draw_batches[index.0 as usize]);
        }

        sorted_batches
    }

    pub fn reset(&mut self) {
        self.ordered_batch_indices.clear();

        for batch in &mut self.draw_batches {
            batch.region_coord = UNDEFINED_REGION_COORDS;
            batch.sections.clear();
        }
    }
}

impl Default for StagingRegionDrawBatches {
    fn default() -> Self {
        // don't wanna impl copy trait for all RegionDrawBatches because it's probably not a good idea.
        // instead, create an array and manually set each to the default.
        let draw_batches = unsafe {
            let mut draw_batches_uninit =
                MaybeUninit::<[RegionDrawBatch; REGIONS_IN_GRAPH]>::uninit();

            for draw_batch_mut in (*draw_batches_uninit.as_mut_ptr()).iter_mut() {
                *draw_batch_mut = Default::default();
            }

            draw_batches_uninit.assume_init()
        };

        Self {
            draw_batches,
            ordered_batch_indices: Default::default(),
        }
    }
}
