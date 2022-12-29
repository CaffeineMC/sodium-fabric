use std::simd::{i16x16, SimdInt, ToBitMask};

use bitflags::*;
use ultraviolet::{Mat4, Vec3, IVec2};

pub struct Rasterizer {
    width: usize,
    height: usize,

    tiles: Box<[u16]>,
    tiles_stride: usize,

    camera_matrix: Mat4,
    camera_position: Vec3,
    viewport: Viewport
}

impl Rasterizer {
    // This is not meant to be fast. It's simply a debug function for getting out the framebuffer to a usable surface.
    pub fn get_depth_buffer(&self, data: &mut [u32]) {
        let src_len = self.width * self.height;
        let dst_len = data.len();

        if dst_len < src_len {
            panic!("Destination buffer too small (needed {} elements, destination holds {} elements)",
                   src_len, dst_len);
        }

        for word_index in 0..self.tiles.len() {
            let word = self.tiles[word_index];

            let x = (word_index % self.tiles_stride) * Edge::STEP_X_SIZE as usize;
            let y = (word_index / self.tiles_stride) * Edge::STEP_Y_SIZE as usize;

            for bit_index in 0..16 {
                let bit = word & (1 << bit_index);

                let x = x + Edge::OFFSETS_X[bit_index] as usize;
                let y = y + Edge::OFFSETS_Y[bit_index] as usize;

                if x >= self.width || y >= self.height {
                    continue;
                }

                // Flipped Y
                data[((self.height - y - 1) * self.width) + x] = if bit == 0 { 0x00 } else { 0xFFFFFFFF };
            }
        }
    }

}

impl Rasterizer {
    pub fn create(width: usize, height: usize) -> Self {
        let tiles_x = width / 4;
        let tiles_y = height / 4;

        Rasterizer {
            tiles: vec![0u16; tiles_x * tiles_y].into_boxed_slice(),
            tiles_stride: tiles_x,
            width,
            height,
            camera_matrix: Mat4::identity(),
            camera_position: Vec3::default(),
            viewport: Viewport {
                x: 0.0,
                y: 0.0,
                width: width as f32,
                height: height as f32
            }
        }
    }

    pub fn clear(&mut self) {
        self.tiles.fill(0u16);
    }

    pub fn set_camera(&mut self, position: &Vec3, matrix: &Mat4) {
        self.camera_matrix = *matrix;
        self.camera_position = *position;
    }

    pub fn draw_aabb<T, E>(&mut self, min: &Vec3, max: &Vec3, faces: BoxFace) -> bool
        where T: PixelFunction, E: ResultAccumulator
    {
        let pos_y = faces.contains(BoxFace::POSITIVE_Y) && self.camera_position.y > max.y;
        let neg_y = faces.contains(BoxFace::NEGATIVE_Y) && self.camera_position.y < min.y;

        let pos_x = faces.contains(BoxFace::POSITIVE_X) && self.camera_position.x > max.x;
        let neg_x = faces.contains(BoxFace::NEGATIVE_X) && self.camera_position.x < min.x;

        let pos_z = faces.contains(BoxFace::POSITIVE_Z) && self.camera_position.z > max.z;
        let neg_z = faces.contains(BoxFace::NEGATIVE_Z) && self.camera_position.z < min.z;
        
        if !(pos_y | neg_y | pos_x | neg_x | pos_z | neg_z) {
            return false;
        }

        let c000 = project(&self.camera_matrix, &Vec3::new(min.x, min.y, min.z), &self.viewport);
        let c001 = project(&self.camera_matrix, &Vec3::new(min.x, min.y, max.z), &self.viewport);
        let c100 = project(&self.camera_matrix, &Vec3::new(max.x, min.y, min.z), &self.viewport);
        let c101 = project(&self.camera_matrix, &Vec3::new(max.x, min.y, max.z), &self.viewport);

        let c010 = project(&self.camera_matrix, &Vec3::new(min.x, max.y, min.z), &self.viewport);
        let c011 = project(&self.camera_matrix, &Vec3::new(min.x, max.y, max.z), &self.viewport);
        let c110 = project(&self.camera_matrix, &Vec3::new(max.x, max.y, min.z), &self.viewport);
        let c111 = project(&self.camera_matrix, &Vec3::new(max.x, max.y, max.z), &self.viewport);

        let mut result = false;

        if pos_y {
            E::accumulate(&mut result, || self.draw_quad::<T>(c010, c011, c111, c110));
        } else if neg_y {
            E::accumulate(&mut result, || self.draw_quad::<T>(c001, c000, c100, c101));
        }

        if pos_x {
            E::accumulate(&mut result, || self.draw_quad::<T>(c101, c100, c110, c111));
        } else if neg_x {
            E::accumulate(&mut result, || self.draw_quad::<T>(c000, c001, c011, c010));
        }

        if pos_z {
            E::accumulate(&mut result, || self.draw_quad::<T>(c001, c101, c111, c011));
        } else if neg_z {
            E::accumulate(&mut result, || self.draw_quad::<T>(c100, c000, c010, c110));
        }

        return result;
    }

    pub fn draw_quad<T>(&mut self, v0: IVec2, v1: IVec2, v2: IVec2, v3: IVec2) -> bool
        where T: PixelFunction
    {
        let min_x = i32::max(min4(v0.x, v1.x, v2.x, v3.x), 0) & !(Edge::STEP_X_SIZE - 1);
        let max_x = i32::min(max4(v0.x, v1.x, v2.x, v3.x), (self.width - 1) as i32);

        let min_y = i32::max(min4(v0.y, v1.y, v2.y, v3.y), 0) & !(Edge::STEP_Y_SIZE - 1);
        let max_y = i32::min(max4(v0.y, v1.y, v2.y, v3.y), (self.height - 1) as i32);

        let top_left_corner = IVec2::new(min_x, min_y);

        let (e01, mut w0_row) = Edge::init(v0, v1, top_left_corner);
        let (e12, mut w1_row) = Edge::init(v1, v2, top_left_corner);
        let (e23, mut w2_row) = Edge::init(v2, v3, top_left_corner);
        let (e30, mut w3_row) = Edge::init(v3, v0, top_left_corner);

        let mut y = min_y;
        let mut result = false;

        while y <= max_y {
            let mut w0 = w0_row;
            let mut w1 = w1_row;
            let mut w2 = w2_row;
            let mut w3 = w3_row;

            let mut x = min_x;

            while x <= max_x {
                let pixel_coord = ((y as usize >> 2) * self.tiles_stride) + (x as usize >> 2);
                
                let mask = w0 | w1 | w2 | w3;
                let bits = mask.is_positive()
                        .to_bitmask();

                unsafe {
                    result |= T::apply(&mut self.tiles.get_unchecked_mut(pixel_coord), bits);
                }

                if result { return true; }

                w0 += e01.one_step_x;
                w1 += e12.one_step_x;
                w2 += e23.one_step_x;
                w3 += e30.one_step_x;

                x += Edge::STEP_X_SIZE;
            }

            w0_row += e01.one_step_y;
            w1_row += e12.one_step_y;
            w2_row += e23.one_step_y;
            w3_row += e30.one_step_y;

            y += Edge::STEP_Y_SIZE;
        }

        result
    }

    pub fn pixels(&self) -> &[u16] {
        &self.tiles
    }

    pub fn height(&self) -> usize {
        self.height
    }

    pub fn width(&self) -> usize {
        self.width
    }
}

struct Edge {
    one_step_x: i16x16,
    one_step_y: i16x16
}

impl Edge {
    const STEP_X_SIZE: i32 = 4;
    const STEP_Y_SIZE: i32 = 4;

    const OFFSETS_X: i16x16 = i16x16::from_array([0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3]);
    const OFFSETS_Y: i16x16 = i16x16::from_array([0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3]);

    pub fn init(v0: IVec2, v1: IVec2, origin: IVec2) -> (Edge, i16x16) {
        let a = v0.y - v1.y;
        let b = v1.x - v0.x;
        let c = (v0.x * v1.y) - (v0.y * v1.x);

        let x = i16x16::splat(origin.x as i16) + Edge::OFFSETS_X;
        let y = i16x16::splat(origin.y as i16) + Edge::OFFSETS_Y;

        let edge = Edge {
            one_step_x: i16x16::splat((a * Edge::STEP_X_SIZE) as i16),
            one_step_y: i16x16::splat((b * Edge::STEP_Y_SIZE) as i16)
        };

        let row = (i16x16::splat(a as i16) * x) + (i16x16::splat(b as i16) * y) + i16x16::splat(c as i16);
        (edge, row)
    }
}

pub trait PixelFunction {
    fn apply(pixel: &mut u16, mask: u16) -> bool;
}

pub struct ReadOnlyPixelFunction;
pub struct WriteOnlyPixelFunction;

impl PixelFunction for ReadOnlyPixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u16, mask: u16) -> bool {
        (*pixel & mask) != mask
    }
}

impl PixelFunction for WriteOnlyPixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u16, mask: u16) -> bool {
        *pixel |= mask;
        false
    }
}

pub trait ResultAccumulator {
    fn accumulate<F>(result: &mut bool, func: F)
        where F: FnOnce() -> bool;
}

pub struct EarlyExitFunction;
pub struct AllExecutionsFunction;

impl ResultAccumulator for EarlyExitFunction {
    #[inline(always)]
    fn accumulate<F>(result: &mut bool, func: F)
        where F: FnOnce() -> bool
    {
        if !*result {
            *result |= func()
        }
    }
}

impl ResultAccumulator for AllExecutionsFunction {
    #[inline(always)]
    fn accumulate<F>(result: &mut bool, func: F)
        where F: FnOnce() -> bool
    {
        *result |= func();
    }
}

bitflags! {
    pub struct BoxFace: u32 {
        const NEGATIVE_Y = 0b000001;
        const POSITIVE_Y = 0b000010;
        const NEGATIVE_Z = 0b000100;
        const POSITIVE_Z = 0b001000;
        const NEGATIVE_X = 0b010000;
        const POSITIVE_X = 0b100000;

        const ALL = BoxFace::POSITIVE_X.bits() | BoxFace::POSITIVE_Y.bits() | BoxFace::POSITIVE_Z.bits() |
                    BoxFace::NEGATIVE_X.bits() | BoxFace::NEGATIVE_Y.bits() | BoxFace::NEGATIVE_Z.bits();
    }
}

#[derive(Clone)]
pub struct Viewport {
    x: f32,
    y: f32,
    width: f32,
    height: f32
}

pub fn project(mat: &Mat4, pos: &Vec3, viewport: &Viewport) -> IVec2 {
    let inv_w = 1.0 / ((mat[0][3] * pos.x) + (mat[1][3] * pos.y) + (mat[2][3] * pos.z) + mat[3][3]);

    let nx = ((mat[0][0] * pos.x) + (mat[1][0] * pos.y) + (mat[2][0] * pos.z) + mat[3][0]) * inv_w;
    let ny = ((mat[0][1] * pos.x) + (mat[1][1] * pos.y) + (mat[2][1] * pos.z) + mat[3][1]) * inv_w;

    let x = ((((nx * 0.5) + 0.5) * viewport.width) + viewport.x).floor() as i32;
    let y = ((((ny * 0.5) + 0.5) * viewport.height) + viewport.y).floor() as i32;

    IVec2 {
        x, y
    }
}

fn max4(a: i32, b: i32, c: i32, d: i32) -> i32 {
    i32::max(i32::max(a, b), i32::max(c, d))
}

fn min4(a: i32, b: i32, c: i32, d: i32) -> i32 {
    i32::min(i32::min(a, b), i32::min(c, d))
}