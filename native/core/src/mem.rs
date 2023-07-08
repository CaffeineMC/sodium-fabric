use std::alloc::{GlobalAlloc, Layout};
use std::ptr;

#[global_allocator]
static mut GLOBAL_ALLOC: GlobalLibcAllocator = GlobalLibcAllocator::uninit();

pub fn set_allocator(vtable: &LibcAllocVtable) {
    unsafe {
        GLOBAL_ALLOC = GlobalLibcAllocator::new(*vtable);
    }
}

#[repr(C)]
#[derive(Clone, Copy)]
pub struct LibcAllocVtable {
    aligned_alloc: unsafe extern "C" fn(alignment: usize, size: usize) -> *mut u8,
    aligned_free: unsafe extern "C" fn(ptr: *mut u8),
    realloc: unsafe extern "C" fn(ptr: *mut u8, new_size: usize) -> *mut u8,
    calloc: unsafe extern "C" fn(num_elements: usize, element_size: usize) -> *mut u8,
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

    /// Mirrors the unix libc impl for GlobalAlloc

    unsafe fn alloc_zeroed(&self, layout: Layout) -> *mut u8 {
        // See the comment above in `alloc` for why this check looks the way it does.
        if layout.align() <= MIN_ALIGN && layout.align() <= layout.size() {
            (self.vtable().calloc)(layout.size(), 1)
        } else {
            let ptr = self.alloc(layout);
            if !ptr.is_null() {
                ptr::write_bytes(ptr, 0, layout.size());
            }
            ptr
        }
    }

    unsafe fn realloc(&self, ptr: *mut u8, layout: Layout, new_size: usize) -> *mut u8 {
        if layout.align() <= MIN_ALIGN && layout.align() <= new_size {
            (self.vtable().realloc)(ptr, new_size)
        } else {
            // Docs for GlobalAlloc::realloc require this to be valid:
            let new_layout = Layout::from_size_align_unchecked(new_size, layout.align());

            let new_ptr = self.alloc(new_layout);
            if !new_ptr.is_null() {
                let size = new_size.min(layout.size());
                ptr::copy_nonoverlapping(ptr, new_ptr, size);
                self.dealloc(ptr, layout);
            }
            new_ptr
        }
    }
}

// The minimum alignment guaranteed by the architecture. This value is used to
// add fast paths for low alignment values.
#[cfg(any(
    target_arch = "x86",
    target_arch = "arm",
    target_arch = "m68k",
    target_arch = "mips",
    target_arch = "powerpc",
    target_arch = "powerpc64",
    target_arch = "sparc",
    target_arch = "asmjs",
    target_arch = "wasm32",
    target_arch = "hexagon",
    all(target_arch = "riscv32", not(target_os = "espidf")),
    all(target_arch = "xtensa", not(target_os = "espidf")),
))]
const MIN_ALIGN: usize = 8;
#[cfg(any(
    target_arch = "x86_64",
    target_arch = "aarch64",
    target_arch = "loongarch64",
    target_arch = "mips64",
    target_arch = "s390x",
    target_arch = "sparc64",
    target_arch = "riscv64",
    target_arch = "wasm64",
))]
const MIN_ALIGN: usize = 16;
// The allocator on the esp-idf platform guarantees 4 byte alignment.
#[cfg(any(
    all(target_arch = "riscv32", target_os = "espidf"),
    all(target_arch = "xtensa", target_os = "espidf"),
))]
const MIN_ALIGN: usize = 4;
