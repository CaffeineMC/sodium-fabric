#![allow(non_snake_case)]

use std::mem::MaybeUninit;

use std::boxed::Box;
use std::ptr;

use crate::frustum::Frustum;
use crate::graph::*;
use crate::math::*;

use crate::jni::types::*;
use crate::mem::LibcAllocVtable;
use crate::panic::PanicHandlerFn;

use sodium_macros::jni_export;

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

    pub fn slice(&self) -> &[T] {
        unsafe { MaybeUninit::slice_assume_init_ref(&self.data[0..(self.count as usize)]) }
    }

    pub fn is_empty(&self) -> bool {
        self.count == 0
    }
}

#[repr(C)]
pub struct CGraphNode {
    connections: u64,
    flags: u32,
}

#[jni_export(me.jellysquid.mods.sodium.core.CoreLibFFI)]
mod java {
    use std::mem::MaybeUninit;

    use std::boxed::Box;
    use std::ptr;

    use crate::frustum::Frustum;
    use crate::graph::*;
    use crate::math::*;

    use crate::ffi::*;
    use crate::jni::types::*;
    use crate::mem::LibcAllocVtable;
    use crate::panic::PanicHandlerFn;

    pub unsafe fn setAllocator(vtable: JPtr<LibcAllocVtable>) {
        let vtable = vtable.as_ref();

        crate::mem::set_allocator(vtable);
    }

    pub unsafe fn setPanicHandler(pfn: JPtr<PanicHandlerFn>) {
        let pfn = *pfn.as_ref();

        crate::panic::set_panic_handler(pfn);
    }

    pub unsafe fn graphCreate(out_graph: JPtrMut<*const Graph>) {
        let graph = Box::new(Graph::new());

        let out_graph = out_graph.as_mut_ref();
        *out_graph = Box::into_raw(graph);
    }

    pub unsafe fn graphAddChunk(graph: JPtrMut<Graph>, x: Jint, y: Jint, z: Jint) {
        let graph = graph.as_mut_ref();
        graph.add_chunk(IVec3::new(x, y, z));
    }

    pub unsafe fn graphUpdateChunk(
        graph: JPtrMut<Graph>,
        x: Jint,
        y: Jint,
        z: Jint,
        node: JPtr<CGraphNode>,
    ) {
        let node = node.as_ref();

        let graph = graph.as_mut_ref();
        graph.update_chunk(
            IVec3::new(x, y, z),
            Node::new(VisibilityData::from_u64(node.connections), node.flags as u8),
        );
    }

    pub unsafe fn graphRemoveChunk(graph: JPtrMut<Graph>, x: Jint, y: Jint, z: Jint) {
        let graph = graph.as_mut_ref();
        graph.remove_chunk(IVec3::new(x, y, z));
    }

    pub unsafe fn graphSearch(
        graph: JPtrMut<Graph>,
        frustum: JPtr<Frustum>,
        view_distance: Jint,
        out_results: JPtrMut<CVec<RegionDrawBatch>>,
    ) {
        let graph = graph.as_mut_ref();
        let frustum = frustum.as_ref();

        *out_results.as_mut_ref() = graph.search(frustum, view_distance);
    }

    pub unsafe fn graphDelete(graph: JPtrMut<Graph>) {
        let graph = Box::from_raw(graph.as_mut_ref());
        std::mem::drop(graph);
    }

    pub unsafe fn frustumCreate(
        out_frustum: JPtrMut<*const Frustum>,
        planes: JPtr<[[f32; 4]; 6]>,
        offset: JPtr<[f32; 3]>,
    ) {
        let planes = planes.as_ref().map(|vec| Vec4::from_array(vec));

        let offset = Vec3::from_array(*offset.as_ref());

        let frustum = Box::new(Frustum::new(planes, offset));

        let out_frustum = out_frustum.as_mut_ref();
        *out_frustum = Box::into_raw(frustum);
    }

    pub unsafe fn frustumDelete(frustum: JPtrMut<Frustum>) {
        std::mem::drop(Box::from_raw(frustum.as_mut_ref()));
    }
}
