use core_simd::simd::*;

use crate::ffi::*;
use crate::math::*;

pub const SECTIONS_IN_REGION: usize = 8 * 4 * 8;
pub const UNDEFINED_REGION_COORDS: (i32, i32, i32) = (i32::MIN, i32::MIN, i32::MIN);

// THE PLAN:
// 1. Figure out a way to calculate the maximum amount of regions the local coords can have.Have a
//    const like MAX_LOCAL_REGION_COUNT.
// 2. Have a struct like StagingRegionDrawBatches that's kept in the BfsCachedState. This will
//    include a RegionDrawBatch array of MAX_LOCAL_REGION_COUNT size, and a u32 CInlineVec of the
//    same size. The first array is indexed with the linearly-encoded local region coords (can be
//    merged with RegionSectionIndex::from_local and moved or smth). The u32 vec will have the index
//    of the RegionDrawBatch pushed to it when the first section is added to it, which keeps track
//    of the order.
// 3. When we're done adding everything to the stage, we can then dump the RegionDrawBatch array
//    to a new CInlineVec (heap allocated) of the same type and the same size, ordered with the u32
//    vec.
// 4. Return and leak this CInlineVec back to java, and when we're done iterating through it on the
//    java side, we give it back to rust where it will be destroyed. Java will also manage creating
//    whatever visibility array it needs in a format that it won't choke on (ex. morton without
//    pdep or vmovmskps), allowing things like entities to be culled efficiently.

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
    pub fn from_local(local_section_coord: u8x3) -> Self {
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

#[repr(C)]
pub struct RegionDrawBatch {
    region_coord: (i32, i32, i32),
    sections: CInlineVec<RegionSectionIndex, SECTIONS_IN_REGION>,
}

impl RegionDrawBatch {
    pub fn new(region_coord: i32x3) -> Self {
        RegionDrawBatch {
            region_coord: region_coord.into_tuple(),
            sections: CInlineVec::new(),
        }
    }

    fn is_empty(&self) -> bool {
        self.sections.is_empty()
    }
}
