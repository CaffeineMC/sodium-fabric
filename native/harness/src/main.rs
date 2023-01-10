use minifb::{Key, Window, WindowOptions};
use ultraviolet::{Mat4, Vec3};

use rasterizer::{AllExecutionsFunction, BoxFace, Rasterizer, WriteOnlyPixelFunction};

const WIDTH: usize = 1280;
const HEIGHT: usize = 720;

fn main() {
    let mut window = Window::new("Rasterizer test harness", WIDTH, HEIGHT, WindowOptions::default())
        .unwrap_or_else(|err| { panic!("Failed to create window: {err}"); });
    window.limit_update_rate(Some(std::time::Duration::from_micros(16600)));

    let mut framebuffer = vec![0u32; WIDTH * HEIGHT].into_boxed_slice();
    let mut rasterizer = Rasterizer::create(WIDTH, HEIGHT);

    let mut time: f32 = 0.5;
    
    while window.is_open() && !window.is_key_down(Key::Escape) {
        if window.is_key_down(Key::A) {
            time -= 0.01;
        } else if window.is_key_down(Key::D) {
            time += 0.01;
        }

        let camera_target = Vec3::new(0.0, 0.0, 0.0);
        let camera_position = Vec3::new(time.cos() * 3.0, 3.0, time.sin() * 3.0);
        
        let view_matrix = Mat4::look_at(camera_position, camera_target, Vec3::new(0.0, 1.0, 0.0));
        let proj_matrix = ultraviolet::projection::perspective_gl(45.0f32.to_radians(), WIDTH as f32 / HEIGHT as f32, 0.01, 1000.0);
        
        rasterizer.clear();
        
        rasterizer.set_camera(camera_position, proj_matrix * view_matrix);
        rasterizer.draw_aabb::<WriteOnlyPixelFunction, AllExecutionsFunction>(
            &Vec3::new(-1.0, -1.0, -1.0),
            &Vec3::new(1.0, 1.0, 1.0),
            BoxFace::ALL);

        rasterizer.get_depth_buffer(&mut framebuffer[..]);

        window.update_with_buffer(&framebuffer[..], WIDTH, HEIGHT)
                .unwrap();
    }
}