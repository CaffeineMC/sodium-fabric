use std::cmp::PartialEq;
use bitflags::*;
use ultraviolet::{Mat4, Vec3, IVec2};

#[allow(unused)]
pub struct Rasterizer {
    width: usize,
    height: usize,

    tiles: Box<[u32]>,
    tiles_x: usize,
    tiles_y: usize,

    camera_matrix: Mat4,
    camera_position: Vec3,

    viewport: Viewport,

    stats: Stats
}

impl Rasterizer {
    // This is not meant to be fast. It's simply a debug function for getting out the framebuffer to a usable surface.
    pub fn get_depth_buffer(&self, data: &mut [u32]) {
        for x in 0..self.width {
            for y in 0..self.height { 
                let word = self.tiles[(y * self.tiles_x) + (x / 32)];

                let color = if (word & (1 << (x % 32))) != 0 {
                    0xFFFFFFFF
                } else {
                    0x00000000
                };

                data[(y * self.width) + x] = color;
            }
        }
    }
}

#[derive(Clone, Copy, Debug)]
struct Edge {
    start: IVec2,
    end: IVec2,
}

impl Edge {
    fn new(start: IVec2, end: IVec2) -> Self {
        Edge { start, end }
    }
}

impl Rasterizer {
    pub fn create(width: usize, height: usize) -> Self {
        let tiles_x = round_up_to_multiple(width, 32) / 32;
        let tiles_y = height;

        Rasterizer {
            tiles: vec![0u32; tiles_x * tiles_y].into_boxed_slice(),
            width,
            height,
            tiles_x,
            tiles_y,
            camera_matrix: Mat4::identity(),
            camera_position: Vec3::default(),
            viewport: Viewport {
                x: 0.0,
                y: 0.0,
                width: width as f32,
                height: height as f32
            },
            stats: Stats::default()
        }
    }

    pub fn clear(&mut self) {
        self.tiles.fill(0);
        self.stats = Stats::default();
    }

    pub fn set_camera(&mut self, position: Vec3, matrix: Mat4) {
        self.camera_matrix = matrix;
        self.camera_position = position;
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
            E::accumulate(&mut result, || self.draw_quad::<T>([c010, c011, c111, c110]));
        } else if neg_y {
            E::accumulate(&mut result, || self.draw_quad::<T>([c001, c000, c100, c101]));
        }

        if pos_x {
            E::accumulate(&mut result, || self.draw_quad::<T>([c101, c100, c110, c111]));
        } else if neg_x {
            E::accumulate(&mut result, || self.draw_quad::<T>([c000, c001, c011, c010]));
        }

        if pos_z {
            E::accumulate(&mut result, || self.draw_quad::<T>([c001, c101, c111, c011]));
        } else if neg_z {
            E::accumulate(&mut result, || self.draw_quad::<T>([c100, c000, c010, c110]));
        }

        return result;
    }

    // The vertices need to be in clockwise order.
    pub fn draw_quad<P>(&mut self, mut vertices: [IVec2; 4]) -> bool
        where P: PixelFunction
    {
        // Sort the vertices from bottom to top
        vertices.sort_by_key(|v| v.y);

        // Reverse the order of the vertices so they're in top to bottom order
        let v1 = vertices[3];
        let v2 = vertices[2];
        let v3 = vertices[1];
        let v4 = vertices[0];
        
        // The vertices are now in the following configuration...
        // 
        //            v1
        //            #
        //           # #             
        //          #   #            top-half triangle
        //         #-----# v2/v3     -----------------
        //        #     #            middle trapezoid
        // v3/v2 #-----#             -----------------
        //        #   #              bottom-half triangle
        //         # #
        //          #
        //         v4

        // Top-half triangle
        if v1.y != v2.y {
            // Orientation of v2 against edge(v1, v4)
            let orientation = (v4.x - v1.x) * (v2.y - v1.y) > (v4.y - v1.y) * (v2.x - v1.x);

            let (mid_left, mid_right) = if orientation {
                (v3, v2)
            } else {
                (v2, v3)
            };

            if self.draw_spans::<P>(Edge::new(v1, mid_left), Edge::new(v1, mid_right)) { return true; }
        }

        // Middle trapezoid
        if v2.y != v3.y {
            let (top_left, top_right) = if v1.x < v2.x {
                (v1, v2)
            } else {
                (v2, v1)
            };

            let (bottom_left, bottom_right) = if v3.x < v4.x {
                (v3, v4)
            } else {
                (v4, v3)
            };

            if self.draw_spans::<P>(Edge::new(top_left, bottom_left), Edge::new(top_right, bottom_right)) { return true; }
        }

        // Bottom-half triangle
        if v3.y != v4.y {
            // Orientation of v3 against edge(v1, v4)
            let orientation = (v4.x - v1.x) * (v3.y - v1.y) > (v4.y - v1.y) * (v3.x - v1.x);

            let (mid_left, mid_right) = if orientation {
                (v2, v3)
            } else {
                (v3, v2)
            };

            if self.draw_spans::<P>(Edge::new(mid_left, v4), Edge::new(mid_right, v4)) { return true; }
        }
        
        false
    }

    fn draw_spans<P>(&mut self, left: Edge, right: Edge) -> bool
        where P: PixelFunction
    {
        let bounds_min_y = i32::max(left.end.y, right.end.y);
        let bounds_max_y = i32::min(left.start.y, right.start.y);
        
        let bounds_min_x = i32::min(left.start.x, left.end.x);
        let bounds_max_x = i32::max(right.start.x, right.end.x);
        
        if outside_viewport(bounds_min_y, bounds_max_y, bounds_min_x, bounds_max_x, self.width as i32, self.height as i32) {
            return false;
        }

        // Clamp the render bounds to the viewport
        let min_y = i32::max(bounds_min_y, 0);
        let max_y = i32::min(bounds_max_y, self.height as i32 - 1);

        // This allows us to resume the walk of an existing line, useful for restarting the edge walk
        // of the triangle's longest edge to avoid unnecessary setup.
        let left_offset_y = left.start.y - max_y;
        let right_offset_y = right.start.y - max_y;

        let left_init = left.start.x << 16;
        let left_inc = ((left.start.x - left.end.x) << 16) / (left.end.y - left.start.y);

        let right_init = right.start.x << 16;
        let right_inc = ((right.start.x - right.end.x) << 16) / (right.end.y - right.start.y);

        let mut left_x = left_init + (left_offset_y * left_inc);
        let mut right_x = right_init + (right_offset_y * right_inc);
        
        // Process scanlines in descending Y order from max_y to min_y
        let mut y = max_y;
        
        while y > min_y {
            // Calculate the left/right entry events for the scan line
            // Since we discarded triangles outside the viewport just above, we alawys know that the left
            // and right events are within certain edges of the viewport, which avoids an additional
            // min and max in this loop.
            let left_bound = i32::max(left_x >> 16, 0); // (left_x <= width) is always true  
            let right_bound = i32::min(right_x >> 16, self.width as i32 - 1); // (right_x > 0) is always true  

            // Calculate the range of tiles which this scanline will overlap
            let tile_left_bound = left_bound >> 5;
            let tile_right_bound = right_bound >> 5;

            let mut left_bound = left_bound - (tile_left_bound * 32);
            let mut right_bound = right_bound - (tile_left_bound * 32) + 1;

            let mut tile_x = tile_left_bound;

            while tile_x <= tile_right_bound {
                // Clamp the left/right entry events of the scanline to this tile's bounding box
                let left_bit = i32::max(left_bound, 0);
                let right_bit = i32::min(right_bound, 32);

                // Calculate a bit mask of left..right bits for the tile
                let mask = {
                    0xFFFFFFFFu32
                        .wrapping_shr(-(right_bit - left_bit) as u32)
                        .wrapping_shl(left_bit as u32)
                };

                // Process the tile
                // The exact behavior here is up to the pixel function which the caller provided
                let result = unsafe {
                    let index = (y as usize * self.tiles_x) + tile_x as usize;
                    P::apply(self.tiles.get_unchecked_mut(index), mask)
                };

	            self.stats.rasterized_pixels += 32;

                // If the pixel function returned true, it means it would like to exit early (most likely it has the result it needs)
                if result {
                    return true;
                }

                left_bound -= 32;
                right_bound -= 32;
                tile_x += 1;
            }
            
            // Step the left/right events forward one scan line
            // Calculating the events looks like (Init + (Y * Step)) but since we step only one
            // scan line at a time, we can simply add Step to Init on each iteration.
            left_x += left_inc;
            right_x += right_inc;

            y -= 1;
        }

        false
    }

    pub fn pixels(&self) -> &[u32] {
        &self.tiles
    }

    pub fn height(&self) -> usize {
        self.height
    }

    pub fn width(&self) -> usize {
        self.width
    }

    pub fn stats(&self) -> Stats {
        self.stats.clone()
    }
}

fn outside_viewport(min_y: i32, max_y: i32, min_x: i32, max_x: i32,
                    width: i32, height: i32) -> bool
{
    if max_x < 0 || min_x >= width {
        return true;
    }

    if max_y < 0 || min_y >= height {
        return true;
    }

    false
}

#[derive(Clone, Default)]
pub struct Stats {
    pub rasterized_pixels: u64
}

pub trait PixelFunction {
    fn apply(pixel: &mut u32, mask: u32) -> bool;
}

pub struct SamplePixelFunction;
pub struct RasterPixelFunction;
pub struct OverdrawPixelFunction;

impl PixelFunction for SamplePixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u32, mask: u32) -> bool {
        (*pixel & mask) != mask
    }
}

impl PixelFunction for RasterPixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u32, mask: u32) -> bool {
        *pixel |= mask;
        false
    }
}

impl PixelFunction for OverdrawPixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u32, _: u32) -> bool {
        *pixel ^= 0xFFFFFFFFu32;
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

// TODO: handle near-plane clipping
pub fn project(mat: &Mat4, pos: &Vec3, viewport: &Viewport) -> IVec2 {
    // TODO: Vectorize this, since the generated machine code is terrible and very slow
    let inv_w = 1.0 / ((mat[0][3] * pos.x) + (mat[1][3] * pos.y) + (mat[2][3] * pos.z) + mat[3][3]);

    let nx = ((mat[0][0] * pos.x) + (mat[1][0] * pos.y) + (mat[2][0] * pos.z) + mat[3][0]) * inv_w;
    let ny = ((mat[0][1] * pos.x) + (mat[1][1] * pos.y) + (mat[2][1] * pos.z) + mat[3][1]) * inv_w;

    let x = ((((nx * 0.5) + 0.5) * viewport.width) + viewport.x).floor() as i32;
    let y = ((((ny * 0.5) + 0.5) * viewport.height) + viewport.y).floor() as i32;

    IVec2 {
        x, y
    }
}

// Multiple must be a power-of-two!
fn round_up_to_multiple(number: usize, multiple: usize) -> usize {
    let additive = multiple - 1;
    let mask = !additive;
    (number + additive) & mask
}
