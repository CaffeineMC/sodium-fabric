#![feature(stdarch)]
#![feature(stdsimd)]
#![feature(core_intrinsics)]

// #[cfg(target_feature = "avx2")]
#[path = "avx2.rs"]
mod implementation;

// #[cfg(not(any(target_feature = "avx2")))]
// mod none {
//     compile_error!("No implementation found for rasterizer... you probably need to change your compile flags.");
// }

pub use implementation::*;
