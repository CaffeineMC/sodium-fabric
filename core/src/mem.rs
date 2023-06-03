use std::alloc::{GlobalAlloc, Layout};

#[global_allocator]
static mut GLOBAL_ALLOC: GlobalLibcAllocator = GlobalLibcAllocator::uninit();

pub fn set_allocator(vtable: &LibcAllocVtable) {
    unsafe {
        GLOBAL_ALLOC = GlobalLibcAllocator::new(vtable.clone());
    }
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct LibcAllocVtable {
    aligned_alloc: unsafe extern "C" fn(alignment: usize, size: usize) -> *mut u8,
    aligned_free: unsafe extern "C" fn(ptr: *mut u8),
}

pub struct GlobalLibcAllocator {
    vtable: Option<LibcAllocVtable>,
}

impl GlobalLibcAllocator {
    pub const fn uninit() -> Self {
        GlobalLibcAllocator { vtable: None }
    }

    pub fn new(allocator: LibcAllocVtable) -> GlobalLibcAllocator {
        GlobalLibcAllocator {
            vtable: Some(allocator),
        }
    }

    fn vtable(&self) -> &LibcAllocVtable {
        self.vtable
            .as_ref()
            .expect("Allocator functions not initialized")
    }
}

unsafe impl GlobalAlloc for GlobalLibcAllocator {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        (self.vtable().aligned_alloc)(layout.align(), layout.size())
    }

    unsafe fn dealloc(&self, ptr: *mut u8, _: Layout) {
        (self.vtable().aligned_free)(ptr)
    }
}
