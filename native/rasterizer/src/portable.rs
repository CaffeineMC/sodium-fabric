use std::cmp::PartialEq;
use std::mem;
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

    viewport: Viewport
}

impl Rasterizer {
    // This is not meant to be fast. It's simply a debug function for getting out the framebuffer to a usable surface.
    pub fn get_depth_buffer(&self, data: &mut [u32]) {
        let mut k = 0;

        for word in self.tiles.iter() {
            for i in 0..32 {
                if (word & (1 << i)) != 0 {
                    data[k] = 0xFFFFFFFF;
                } else {
                    data[k] = 0x00000000;
                }

                k += 1;
            }
        }
    }
}

#[derive(Clone, Copy, Debug)]
struct Edge {
    start_y: i32, // The top-most y-coordinate of the edge

    init: i32,  // The initial state at the line origin
    inc: i32,   // The increment applied to init for each scanline after start_y
}

impl Edge {
    fn new(start: IVec2, end: IVec2) -> Self {
        // We use fixed point math here, in i16.i16 format
        // There is some chance for losing precision here since we project into raster space (integer)
        //
        // For some additional instructions, we can work from floating point and take the whole/fractional parts to assemble
        // a fixed-point number. This would also allow us to vectorize the division here since there are no SIMD instructions
        // for integer divisions, which would likely end up being a net-positive.
        
        let init = start.x << 16;
        let inc = ((start.x - end.x) << 16) / (end.y - start.y);

        Edge { start_y: start.y, init, inc }
    }
}

impl Rasterizer {
    pub fn create(width: usize, height: usize) -> Self {
        let tiles_x = width / 32;
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
            }
        }
    }

    pub fn clear(&mut self) {
        self.tiles.fill(0);
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

        // If the top and bottom vertices share the same Y-coordinate, the triangle has no height, so skip it
        if v1.y == v3.y {
            return false;
        }

        // The longest edge of the triangle (from the top-most to the bottom-most vertices)
        let e13 = Edge::new(v1, v3);

        // Top-half case
        if v2.y != v1.y {
            let e12 = Edge::new(v1, v2);
            if self.draw_spans::<P>(e12, e13, v2.y, v1.y) { return true; }
        }

        // Bottom-half case
        if v3.y != v2.y {
            let e23 = Edge::new(v2, v3);
            if self.draw_spans::<P>(e23, e13, v3.y, v2.y) { return true; }
        }
        
        false
    }

    fn draw_spans<P>(&mut self, e0: Edge, e1: Edge, min_y: i32, max_y: i32) -> bool
        where P: PixelFunction
    {
        // Clamp the render bounds to the viewport
        let min_y = i32::max(min_y, 0);
        let max_y = i32::min(max_y, self.height as i32 - 1);

        // This allows us to resume the walk of an existing line
        let mut e0_y = e0.start_y - max_y;
        let mut e1_y = e1.start_y - max_y;

        // Iterate in descending Y order from max_y to min_y
        let mut y = max_y;

        while y > min_y {
            // Calculate (init + (y * step)) for each edge
            // TODO: Vectorize this so we can process 8 rows in parallel
            let e0_px = (e0.init + (e0_y * e0.inc)) >> 16;
            let e1_px = (e1.init + (e1_y * e1.inc)) >> 16;

            // Calculate the left/right entry events for the scan line
            // TODO: Simplify this by ensuring e0 is always the left edge, and e1 is always the right edge
            let left_bound = i32::min(e0_px, e1_px);
            let right_bound = i32::max(e0_px, e1_px);

            // Clamp the entry events to the viewport
            // TODO: Simplify this by discarding triangles which are outside the viewport
            let left_bound = i32::min(i32::max(left_bound, 0), self.width as i32 - 1);
            let right_bound = i32::min(i32::max(right_bound, 0), self.width as i32 - 1);

            // Calculate the range of tiles which this scanline will overlap
            let tile_left_bound = left_bound / 32;
            let tile_right_bound = right_bound / 32;

            for tile_x in tile_left_bound..=tile_right_bound {
                let start_x = tile_x * 32;
                
                // Clamp the left/right entry events to this tile
                let left_bit = i32::clamp(left_bound - start_x, 0, 32);
                let right_bit = i32::clamp(right_bound - start_x + 1, 0, 32);
                
                // Calculate a bit mask of left..right bits for the tile
                let mask = unsafe { 0xFFFFFFFFu32.unchecked_shr(-(right_bit - left_bit) as u32) } << left_bit;

                // Process the tile
                // The exact behavior here is up to the pixel function which the caller provided
                let result = unsafe {
                    let index = (y as usize * self.tiles_x) + tile_x as usize;
                    P::apply(self.tiles.get_unchecked_mut(index), mask)
                };

                // If the pixel function returned true, it means it would like to exit early (most likely it has the result it needs)
                if result {
                    return true;
                }
            }
            
            e0_y += 1;
            e1_y += 1;

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
}

pub trait PixelFunction {
    fn apply(pixel: &mut u32, mask: u32) -> bool;
}

pub struct ReadOnlyPixelFunction;
pub struct WriteOnlyPixelFunction;

impl PixelFunction for ReadOnlyPixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u32, mask: u32) -> bool {
        (*pixel & mask) != mask
    }
}

impl PixelFunction for WriteOnlyPixelFunction {
    #[inline(always)]
    fn apply(pixel: &mut u32, mask: u32) -> bool {
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