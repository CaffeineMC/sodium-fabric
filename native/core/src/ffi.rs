#![allow(non_snake_case)]

use std::boxed::Box;
use std::mem::MaybeUninit;
use std::pin::Pin;
use std::ptr;

use crate::graph::local::LocalFrustum;
use crate::graph::*;
use crate::jni::types::*;
use crate::math::*;
use crate::mem::LibcAllocVtable;
use crate::panic::PanicHandlerFn;

#[repr(C)]
pub struct CVec<T> {
    count: u32,
    data: *mut T,
}

impl<T> CVec<T> {
    pub fn from_boxed_slice(data: Box<[T]>) -> Self {
        CVec {
            count: data.len().try_into().expect("len is not a valid u32"),
            data: if data.len() == 0 {
                ptr::null_mut()
            } else {
                Box::leak(data).as_mut_ptr()
            },
        }
    }
}

#[repr(C)]
pub struct CInlineVec<T, const LEN: usize> {
    count: u32,
    data: [MaybeUninit<T>; LEN],
}

impl<T, const LEN: usize> CInlineVec<T, LEN> {
    pub fn push(&mut self, value: T) {
        self.data[self.count as usize] = MaybeUninit::new(value);
        self.count += 1;
    }

    pub fn clear(&mut self) {
        unsafe {
            for i in 0..self.count {
                self.data[i as usize].assume_init_drop();
            }
        }

        self.count = 0;
    }

    pub fn is_empty(&self) -> bool {
        self.count == 0
    }

    pub fn get_slice(&self) -> &[T] {
        // SAFETY: count shouldn't ever be able to be incremented past LEN, and the contents should
        // be initialized
        unsafe {
            MaybeUninit::slice_assume_init_ref(self.data.get_unchecked(0..(self.count as usize)))
        }
    }
}

impl<T, const LEN: usize> Default for CInlineVec<T, LEN> {
    fn default() -> Self {
        CInlineVec {
            count: 0,
            data: unsafe { MaybeUninit::<[MaybeUninit<T>; LEN]>::uninit().assume_init() },
        }
    }
}

impl<T, const LEN: usize> Clone for CInlineVec<T, LEN>
where
    MaybeUninit<T>: Clone,
{
    fn clone(&self) -> Self {
        Self {
            count: self.count,
            data: self.data.clone(),
        }
    }
}

impl<T, const LEN: usize> Copy for CInlineVec<T, LEN> where T: Copy {}

#[allow(non_snake_case)]
mod java {
    use std::boxed::Box;
    use std::mem::MaybeUninit;
    use std::ptr;

    use core_simd::simd::f32x4;

    use crate::ffi::*;
    use crate::graph::local::index::LocalNodeIndex;
    use crate::graph::local::LocalCoordContext;
    use crate::graph::visibility::VisibilityData;
    use crate::graph::*;
    use crate::jni::types::*;
    use crate::math::*;
    use crate::mem::LibcAllocVtable;
    use crate::panic::PanicHandlerFn;

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_setAllocator(
        _: *mut JEnv,
        _: *mut JClass,
        vtable: JPtr<LibcAllocVtable>,
    ) {
        let vtable = vtable.as_ref();

        crate::mem::set_allocator(vtable);
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_setPanicHandler(
        _: *mut JEnv,
        _: *mut JClass,
        pfn: JPtr<PanicHandlerFn>,
    ) {
        let pfn = *pfn.as_ref();

        crate::panic::set_panic_handler(pfn);
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphCreate(
        _: *mut JEnv,
        _: *mut JClass,
        out_graph: JPtrMut<*const Graph>,
    ) {
        let graph = Box::new(Graph::new());

        let out_graph = out_graph.into_mut_ref();
        *out_graph = Box::into_raw(graph);
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphAddSection(
        _: *mut JEnv,
        _: *mut JClass,
        graph: JPtrMut<Graph>,
        x: Jint,
        y: Jint,
        z: Jint,
        has_geometry: Jboolean,
        visibility_data: Jlong,
    ) {
        let graph = graph.into_mut_ref();
        graph.add_section(
            i32x3::from_xyz(x, y, z),
            has_geometry,
            VisibilityData::pack(visibility_data as u64),
        );
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphRemoveSection(
        _: *mut JEnv,
        _: *mut JClass,
        graph: JPtrMut<Graph>,
        x: Jint,
        y: Jint,
        z: Jint,
    ) {
        let graph = graph.into_mut_ref();
        graph.remove_section(i32x3::from_xyz(x, y, z));
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphSearch(
        _: *mut JEnv,
        _: *mut JClass,
        graph: JPtrMut<Graph>,
        camera_world_pos: JPtr<[Jdouble; 3]>,
        section_view_distance: Jint,
        frustum_planes: JPtr<[[Jfloat; 6]; 4]>,
        fog_distance: Jfloat,
        world_bottom_section_y: Jbyte,
        world_top_section_y: Jbyte,
        disable_occlusion_culling: Jboolean,
        out_results: JPtrMut<SortedSearchResults>,
    ) {
        let graph = graph.into_mut_ref();
        let frustum_planes = frustum_planes.as_ref();

        let simd_camera_world_pos = f64x3::from_array(*camera_world_pos.as_ref());
        let simd_frustum_planes = [
            f32x6::from_array(frustum_planes[0]),
            f32x6::from_array(frustum_planes[1]),
            f32x6::from_array(frustum_planes[2]),
            f32x6::from_array(frustum_planes[3]),
        ];

        let coord_context = LocalCoordContext::new(
            simd_camera_world_pos,
            section_view_distance as u8,
            simd_frustum_planes,
            fog_distance,
            world_bottom_section_y,
            world_top_section_y,
        );

        *out_results.into_mut_ref() = graph.cull(&coord_context, disable_occlusion_culling);
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_searchResultsDelete(
        _: *mut JEnv,
        _: *mut JClass,
        results: JPtrMut<SortedSearchResults>,
    ) {
        let graph = Box::from_raw(results.into_mut_ref());
        drop(graph);
    }

    #[no_mangle]
    pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphDelete(
        _: *mut JEnv,
        _: *mut JClass,
        graph: JPtrMut<Graph>,
    ) {
        let graph = Box::from_raw(graph.into_mut_ref());
        drop(graph);
    }
}
