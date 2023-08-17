#![allow(unused)]
#![feature(portable_simd)]
#![feature(core_intrinsics)]
#![feature(cell_leak)]
#![feature(maybe_uninit_slice)]

mod collections;
mod ffi;
mod graph;
mod jni;
pub(crate) mod math;
mod mem;
mod panic;
mod region;
