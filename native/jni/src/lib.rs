#![allow(unused)]

use rasterizer::{Rasterizer, BoxFace, RasterPixelFunction, AllExecutionsFunction, SamplePixelFunction, EarlyExitFunction};
use ultraviolet::{Vec3, Mat4};
use std::ffi::c_void;

type JNIEnv = *const c_void;
type JClass = *const c_void;

type JBool = bool;
type JLong = i64;
type JInt = i32;
type JShort = i16;
type JByte = i8;
type JFloat = f32;
type JDouble = f64;

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1create(
    _: JNIEnv,
    _: JClass,
    width: JInt,
    height: JInt,
) -> JLong {
    let rasterizer = Box::new(Rasterizer::create(
        width as usize,
        height as usize,
    ));
    (Box::leak(rasterizer) as *mut Rasterizer) as JLong
}

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1set_1camera(
    _: JNIEnv,
    _: JClass,
    handle: JLong,
    position_ptr: JLong,
    matrix_ptr: JLong,
) {
    let rasterizer = unsafe {
        Box::leak(Box::from_raw(handle as *mut Rasterizer))
    };
    let position: &[f32; 3] = unsafe {
        &*(position_ptr as *const [f32; 3])
    };
    let matrix: &[f32; 16] = unsafe {
        &*(matrix_ptr as *const [f32; 16])
    };

    rasterizer.set_camera(Vec3::from(position), Mat4::from(matrix));
}

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1get_1depth_1buffer(
    _: JNIEnv,
    _: JClass,
    handle: JLong,
    data_ptr: JLong,
) {
    let rasterizer = unsafe {
        Box::leak(Box::from_raw(handle as *mut Rasterizer))
    };

    let data_ptr: &mut [u32] = unsafe {
        std::slice::from_raw_parts_mut(data_ptr as *mut u32, rasterizer.width() * rasterizer.height())
    };
    
    rasterizer.get_depth_buffer(data_ptr);
}

#[repr(packed)]
struct Aabb {
    min_x: i8,
    min_y: i8,
    min_z: i8,
    _padding1: i8,
    
    max_x: i8,
    max_y: i8,
    max_z: i8,
    _padding2: i8,

    face: u32
}

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1draw_1boxes(
    _: JNIEnv,
    _: JClass,
    handle: JLong,
    boxes_ptr: JLong,
    boxes_count: JInt,
    offset_x: JInt,
    offset_y: JInt,
    offset_z: JInt,
) {
    let rasterizer = unsafe {
        Box::leak(Box::from_raw(handle as *mut Rasterizer))
    };
    let boxes = unsafe {
        std::slice::from_raw_parts(boxes_ptr as *const Aabb, boxes_count as usize)
    };

    let offset = Vec3::new(offset_x as f32, offset_y as f32, offset_z as f32);

    for b in boxes {
        let min = Vec3::new(b.min_x as f32, b.min_y as f32, b.min_z as f32) + offset;
        let max = Vec3::new(b.max_x as f32, b.max_y as f32, b.max_z as f32) + offset;
        
        rasterizer.draw_aabb::<RasterPixelFunction, AllExecutionsFunction>(min, max, BoxFace::from_bits_truncate(b.face));
    }
}

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1test_1box(
    _: JNIEnv,
    _: JClass,
    handle: JLong,
    x1: JFloat,
    y1: JFloat,
    z1: JFloat,
    x2: JFloat,
    y2: JFloat,
    z2: JFloat,
    faces: JInt
) -> JBool {
    let rasterizer = unsafe {
        Box::leak(Box::from_raw(handle as *mut Rasterizer))
    };
    rasterizer.draw_aabb::<SamplePixelFunction, EarlyExitFunction>(Vec3::new(x1, y1, z1), Vec3::new(x2, y2, z2), BoxFace::from_bits_truncate(faces as u32))
}

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1clear(
    _: JNIEnv,
    _: JClass,
    handle: JLong
) {
    let rasterizer = unsafe {
        Box::leak(Box::from_raw(handle as *mut Rasterizer))
    };
    rasterizer.clear();
}

#[no_mangle]
pub extern "system" fn Java_me_jellysquid_mods_sodium_ffi_RustBindings_r_1destroy(
    _: JNIEnv,
    _: JClass,
    handle: JLong
) {
    drop(unsafe {
        Box::from_raw(handle as *mut Rasterizer)
    });
}