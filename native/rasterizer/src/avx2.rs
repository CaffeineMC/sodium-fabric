use std::mem;
use std::arch::x86_64::*;
use bitflags::*;
use ultraviolet::{Mat4, Vec3, IVec2};

#[allow(unused)]
pub struct Rasterizer {
    width: usize,
    height: usize,

    tiles: Box<[__m256i]>,
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
                let tile = self.tiles[((y / 8) * self.tiles_x) + (x / 32)];
                let tile: [u32; 8] = unsafe { mem::transmute_copy(&tile) };

                let word = tile[7 - (y % 8)];

                let color = if (word & (1 << (31 - (x % 32)))) != 0 {
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
    end: IVec2
}

impl Edge {
    fn new(start: IVec2, end: IVec2) -> Self {
        Edge { start, end }
    }
}

impl Rasterizer {
    pub fn create(width: usize, height: usize) -> Self {
        let tiles_x = round_up_to_multiple(width, 32) / 32;
        let tiles_y = round_up_to_multiple(height, 8) / 8;

        Rasterizer {
            tiles: vec![unsafe { _mm256_set1_epi32(0) }; tiles_x * tiles_y].into_boxed_slice(),
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
        self.tiles.fill(unsafe { _mm256_set1_epi32(0) });
        self.stats = Stats::default();
    }

    pub fn set_camera(&mut self, position: Vec3, matrix: Mat4) {
        self.camera_matrix = matrix;
        self.camera_position = position;
    }


    pub fn draw_aabb<T, E>(&mut self, min: &Vec3, max: &Vec3, faces: BoxFace) -> bool
        where T: PixelFunction, E: ResultAccumulator
    {
        // Project the eight corners of the bounding box
        let c000 = project(&self.camera_matrix, &Vec3::new(min.x, min.y, min.z), &self.viewport);
        let c001 = project(&self.camera_matrix, &Vec3::new(min.x, min.y, max.z), &self.viewport);
        let c100 = project(&self.camera_matrix, &Vec3::new(max.x, min.y, min.z), &self.viewport);
        let c101 = project(&self.camera_matrix, &Vec3::new(max.x, min.y, max.z), &self.viewport);

        let c010 = project(&self.camera_matrix, &Vec3::new(min.x, max.y, min.z), &self.viewport);
        let c011 = project(&self.camera_matrix, &Vec3::new(min.x, max.y, max.z), &self.viewport);
        let c110 = project(&self.camera_matrix, &Vec3::new(max.x, max.y, min.z), &self.viewport);
        let c111 = project(&self.camera_matrix, &Vec3::new(max.x, max.y, max.z), &self.viewport);

        let mut result = false;

        if faces.contains(BoxFace::POSITIVE_Y) && self.camera_position.y > max.y {
            E::accumulate(&mut result, || self.draw_triangle::<T>(c110, c010, c111)
                || self.draw_triangle::<T>(c010, c011, c111));
        }

        if faces.contains(BoxFace::NEGATIVE_Y) && self.camera_position.y < min.y {
            E::accumulate(&mut result, || self.draw_triangle::<T>(c000, c100, c101)
                || self.draw_triangle::<T>(c001, c000, c101));
        }

        if faces.contains(BoxFace::POSITIVE_X) && self.camera_position.x > max.x {
            E::accumulate(&mut result, || self.draw_triangle::<T>(c101, c100, c111)
                || self.draw_triangle::<T>(c100, c110, c111));
        }

        if faces.contains(BoxFace::NEGATIVE_X) && self.camera_position.x < min.x {
            E::accumulate(&mut result, || self.draw_triangle::<T>(c000, c001, c011)
                || self.draw_triangle::<T>(c000, c011, c010));
        }

        if faces.contains(BoxFace::POSITIVE_Z) && self.camera_position.z > max.z {
            E::accumulate(&mut result, || self.draw_triangle::<T>(c001, c101, c111)
                || self.draw_triangle::<T>(c111, c011, c001));
        }

        if faces.contains(BoxFace::NEGATIVE_Z) && self.camera_position.z < min.z {
            E::accumulate(&mut result, || self.draw_triangle::<T>(c000, c010, c110)
                || self.draw_triangle::<T>(c100, c000, c110));
        }

        result
    }

    // The vertices need to be in clockwise order.
    pub fn draw_triangle<P>(&mut self, mut v1: IVec2, mut v2: IVec2, mut v3: IVec2) -> bool
        where P: PixelFunction
    {
        // If the top and bottom vertices share the same Y-coordinate, the triangle has no height, so skip it
        if v1.y == v2.y && v2.y == v3.y {
            return false;
        }

        // Sort the vertices from top to bottom
        // This is an unrolled bubble sort to help the compiler as much as possible
        // The generated machine code is very slightly faster when compared to the simple [slice].sort_by(|v| -v.y)
        if v1.y < v2.y { mem::swap(&mut v1, &mut v2); }
        if v2.y < v3.y { mem::swap(&mut v2, &mut v3); }
        if v1.y < v2.y { mem::swap(&mut v1, &mut v2); }

        // The vertices are now in the following configuration
        //
        //      v1
        //      #
        //      ##    
        // e13  # #  e12
        //      #  #            top-half
        //      #---#  v2       -------
        //      #  #            bottom-half
        // e13  # #  e23        
        //      ##
        //      #
        //      v3

        // The orientation of midpoint v2 relative to edge(v1, v3) 
        let orientation = (v3.x - v1.x) * (v2.y - v1.y) - (v3.y - v1.y) * (v2.x - v1.x);

        // Top-half case
        if v2.y != v1.y {
            let (left, right) = if orientation < 0 {
                (v2, v3)
            } else {
                (v3, v2)
            };

            if self.draw_spans::<P>(Edge::new(v1, left), Edge::new(v1, right)) { return true; }
        }

        // Bottom-half case
        if v3.y != v2.y {
            let (left, right) = if orientation < 0 {
                (v2, v1)
            } else {
                (v1, v2)
            };

            if self.draw_spans::<P>(Edge::new(left, v3), Edge::new(right, v3)) { return true; }
        }
        
        false
    }

    #[inline(always)]
    #[allow(overflowing_literals)]
    fn draw_spans<P>(&mut self, left: Edge, right: Edge) -> bool
        where P: PixelFunction
    {
        // Find the bounding box
        let bounds_min_y = i32::max(left.end.y, right.end.y);
        let bounds_min_x = i32::min(left.start.x, left.end.x);
        let bounds_max_y = i32::min(left.start.y, right.start.y);
        let bounds_max_x = i32::max(right.start.x, right.end.x);
        
        // Check if the bounding box is outside the viewport, so that we can avoid clamping later
        if outside_viewport(bounds_min_y, bounds_max_y, bounds_min_x, bounds_max_x, self.width as i32, self.height as i32) {
            return false;
        }

        // Clamp the bounding box to the viewport
        let bounds_min_x = i32::max(bounds_min_x, 0);
        let bounds_min_y = i32::max(bounds_min_y, 0);
        let bounds_max_x = i32::min(bounds_max_x, self.width as i32 - 1);
        let bounds_max_y = i32::min(bounds_max_y, self.height as i32 - 1);
                
        // Find the tiles which the bounding box overlaps
        let tile_min_x = bounds_min_x >> 5;
        let tile_min_y = bounds_min_y >> 3;
        let tile_max_x = bounds_max_x >> 5;
        let tile_max_y = bounds_max_y >> 3;

        let left_init = left.start.x << 16;
        let left_inc = ((left.start.x - left.end.x) << 16) / (left.end.y - left.start.y);

        let right_init = right.start.x << 16;
        let right_inc = ((right.start.x - right.end.x) << 16) / (right.end.y - right.start.y);

        unsafe {
            // The raster x-coordinate which each scanline starts at
            let line_x_offset = _mm256_set1_epi32((tile_min_x * 32) << 16);

            // The raster y-coordinate for each scanline in the tile 
            let mut y_coord = _mm256_add_epi32(_mm256_set1_epi32(tile_max_y * 8), _mm256_set_epi32(0, 1, 2, 3, 4, 5, 6, 7));

            // The starting value for the left edge
            let mut left_x = _mm256_add_epi32(_mm256_set1_epi32(left_init), _mm256_mullo_epi32(_mm256_sub_epi32(_mm256_set1_epi32(left.start.y), y_coord), _mm256_set1_epi32(left_inc)));
            left_x = _mm256_sub_epi32(left_x, line_x_offset);

            // The starting value for the right edge
            let mut right_x = _mm256_add_epi32(_mm256_set1_epi32(right_init), _mm256_mullo_epi32(_mm256_sub_epi32(_mm256_set1_epi32(right.start.y), y_coord), _mm256_set1_epi32(right_inc)));
            right_x = _mm256_sub_epi32(right_x, line_x_offset);

            // The value by which left/right are advanced each tile
            let left_x_step = _mm256_mullo_epi32(_mm256_set1_epi32(left_inc), _mm256_set1_epi32(8));
            let right_x_step = _mm256_mullo_epi32(_mm256_set1_epi32(right_inc), _mm256_set1_epi32(8));

            let mut tile_y = tile_max_y;

            // Step downward in parallel for each scanline
            while tile_y >= tile_min_y {
                // The bounds of the rendered scanline
                let mut left_bound = _mm256_srai_epi32(left_x, 16);
                let mut right_bound = _mm256_srai_epi32(right_x, 16);

                // Since we render tiles, it's possible for rendering to start or end outside of the bounds. To avoid this,
                // we generate a mask for each y-coordinate depending on whether or not it's within bounds.
                let y_mask = _mm256_and_si256(_mm256_cmpgt_epi32(y_coord, _mm256_set1_epi32(bounds_min_y - 1)),
                    _mm256_cmpgt_epi32(_mm256_set1_epi32(bounds_max_y), y_coord));

                let mut tile_x = tile_min_x;

                // Step across each word in a scanline
                while tile_x <= tile_max_x {
                    // Create a bitmask for the current tile given the scan line bounds 
                    let mask = {
                        // left_mask = (~0 >> max(0, left - x))
                        let left_mask = _mm256_srlv_epi32(y_mask, _mm256_max_epi32(left_bound, _mm256_set1_epi32(0)));

                        // right_mask = (~0 >> max(0, right - x))
                        let right_mask = _mm256_srlv_epi32(y_mask, _mm256_max_epi32(right_bound, _mm256_set1_epi32(0)));

                        // mask = left_mask & ~right_mask
                        _mm256_and_si256(left_mask, _mm256_xor_si256(right_mask, _mm256_set1_epi32(0xFFFFFFFFi32)))
                    };

                    // Apply the bitmask to the tile using the pixel function
                    // Depending on the implementation, this may or may not write data
                    let result = {
                        let tile_index = (tile_y as usize * self.tiles_x) + tile_x as usize;

                        P::apply(self.tiles.as_mut_ptr()
                            .add(tile_index as usize), mask)
                    };
                    
                    // The pixel function decides whether we should exit early or not. Depending on the pixel function used,
                    // the compiler may optimize this away entirely, such as for the write-only function which does not return early.
                    if result {
                        return true;
                    }

                    // Advance the left/right bounds by one tile
                    left_bound = _mm256_sub_epi32(left_bound, _mm256_set1_epi32(32));
                    right_bound = _mm256_sub_epi32(right_bound, _mm256_set1_epi32(32));
                    
                    tile_x += 1;
                }

                // Step the y-coordinates for the next scanlines
                y_coord = _mm256_sub_epi32(y_coord, _mm256_set1_epi32(8));

                // Step the left/right bounds for the next scanlines
                left_x = _mm256_add_epi32(left_x, left_x_step);
                right_x = _mm256_add_epi32(right_x, right_x_step);

                tile_y -= 1;
            }
        }

        false
    }

    pub fn tiles(&self) -> &[__m256i] {
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

#[inline(always)]
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
    unsafe fn apply(pixel: *mut __m256i, mask: __m256i) -> bool;
}

pub struct SamplePixelFunction;
pub struct RasterPixelFunction;

impl PixelFunction for SamplePixelFunction {
    #[inline(always)]
    unsafe fn apply(pixel: *mut __m256i, mask: __m256i) -> bool {
        _mm256_movemask_epi8(_mm256_xor_si256(_mm256_and_si256(_mm256_load_si256(pixel), mask), mask)) != 0x0
    }
}

impl PixelFunction for RasterPixelFunction {
    #[inline(always)]
    unsafe fn apply(pixel: *mut __m256i, mask: __m256i) -> bool {
        _mm256_store_si256(pixel, _mm256_or_si256(_mm256_load_si256(pixel), mask));
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
