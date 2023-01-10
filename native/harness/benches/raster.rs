use criterion::{black_box, criterion_group, criterion_main, Criterion, Bencher};
use rasterizer::{Rasterizer, WriteOnlyPixelFunction, AllExecutionsFunction, BoxFace};
use ultraviolet::{Mat4, Vec3};

fn raster_cube(bencher: &mut Bencher, width: usize, height: usize) {
    let camera_target = Vec3::new(0.0, 0.0, 0.0);
    let camera_position = Vec3::new(3.0, 3.0, 3.0);
    let camera = Camera::create(camera_target, camera_position, width, height);

    let mut rasterizer = Rasterizer::create(width, height);
    rasterizer.clear();
    rasterizer.set_camera(camera.position, camera.proj_matrix * camera.view_matrix);

    bencher.iter(|| {
        rasterizer.draw_aabb::<WriteOnlyPixelFunction, AllExecutionsFunction>(
            &Vec3::new(-1.0, -1.0, -1.0),
            &Vec3::new(1.0, 1.0, 1.0),
            BoxFace::ALL);    
    });

    black_box(rasterizer.pixels());
}

fn criterion_benchmark(c: &mut Criterion) {
    c.bench_function("cube  200x200 ", |b| raster_cube(b, 200, 200));
    c.bench_function("cube  400x400 ", |b| raster_cube(b, 400, 400));
    c.bench_function("cube  800x800 ", |b| raster_cube(b, 800, 800));
    c.bench_function("cube 1600x1600", |b| raster_cube(b, 1600, 1600));
}

criterion_group!(benches, criterion_benchmark);
criterion_main!(benches);

struct Camera {
    position: Vec3,

    view_matrix: Mat4,
    proj_matrix: Mat4
}

impl Camera {
    pub fn create(target: Vec3, position: Vec3, width: usize, height: usize) -> Self {
        let view_matrix = Mat4::look_at(position, target, Vec3::new(0.0, 1.0, 0.0));
        let proj_matrix = ultraviolet::projection::perspective_gl(45.0f32.to_radians(), width as f32 / height as f32, 0.01, 1000.0);

        Self { position, view_matrix, proj_matrix }
    }
}
