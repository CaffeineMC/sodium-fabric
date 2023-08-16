#![allow(unused)]
#![feature(portable_simd)]
#![feature(core_intrinsics)]
#![feature(cell_leak)]
// will be stabilized very soon, see https://github.com/rust-lang/rust/issues/88581
#![feature(int_roundings)]

mod collections;
mod ffi;
mod graph;
mod jni;
pub(crate) mod math;
mod mem;
mod panic;
mod region;
