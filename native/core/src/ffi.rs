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
    pub fn new() -> Self
    where
        T: Copy,
    {
        CInlineVec {
            count: 0,
            data: [MaybeUninit::uninit(); LEN],
        }
    }

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
}

// #[allow(non_snake_case)]
// mod java {
//     use std::boxed::Box;
//     use std::mem::MaybeUninit;
//     use std::ptr;
//
//     use core_simd::simd::f32x4;
//
//     use crate::ffi::*;
//     use crate::graph::frustum::LocalFrustum;
//     use crate::graph::*;
//     use crate::jni::types::*;
//     use crate::math::*;
//     use crate::mem::LibcAllocVtable;
//     use crate::panic::PanicHandlerFn;
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_setAllocator(
//         _: *mut JEnv,
//         _: *mut JClass,
//         vtable: JPtr<LibcAllocVtable>,
//     ) {
//         let vtable = vtable.as_ref();
//
//         crate::mem::set_allocator(vtable);
//     }
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_setPanicHandler(
//         _: *mut JEnv,
//         _: *mut JClass,
//         pfn: JPtr<PanicHandlerFn>,
//     ) {
//         let pfn = *pfn.as_ref();
//
//         crate::panic::set_panic_handler(pfn);
//     }
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphCreate(
//         _: *mut JEnv,
//         _: *mut JClass,
//         out_graph: JPtrMut<*const Graph>,
//     ) {
//         let graph = Box::new(Graph::new());
//
//         let out_graph = out_graph.into_mut_ref();
//         *out_graph = Box::into_raw(graph);
//     }
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphAddSection(
//         _: *mut JEnv,
//         _: *mut JClass,
//         graph: JPtrMut<Graph>,
//         x: Jint,
//         y: Jint,
//         z: Jint,
//         node: JPtr<CSectionData>,
//     ) {
//         let node = node.as_ref();
//
//         let graph = graph.into_mut_ref();
//         graph.add_chunk(
//             LocalSectionCoord::from_xyz(x, y, z),
//             Node::new(VisibilityData::pack(node.connections), node.flags as u8),
//         );
//     }
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphRemoveSection(
//         _: *mut JEnv,
//         _: *mut JClass,
//         graph: JPtrMut<Graph>,
//         x: Jint,
//         y: Jint,
//         z: Jint,
//     ) {
//         let graph = graph.into_mut_ref();
//         graph.remove_chunk(LocalSectionCoord::from_xyz(x, y, z));
//     }
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphSearch(
//         _: *mut JEnv,
//         _: *mut JClass,
//         graph: JPtrMut<Graph>,
//         frustum: JPtr<[[f32; 6]; 4]>,
//         view_distance: Jint,
//         out_results: JPtrMut<CVec<RegionDrawBatch>>,
//     ) {
//         let graph = graph.into_mut_ref();
//         let frustum = frustum.as_ref();
//
//         *out_results.into_mut_ref() = graph.search(frustum, view_distance);
//     }
//
// TODO: deleteSearchResults
//
//     #[no_mangle]
//     pub unsafe extern "C" fn Java_me_jellysquid_mods_sodium_core_CoreLibFFI_graphDelete(
//         _: *mut JEnv,
//         _: *mut JClass,
//         graph: JPtrMut<Graph>,
//     ) {
//         let graph = Box::from_raw(graph.into_mut_ref());
//         std::mem::drop(graph);
//     }
// }
