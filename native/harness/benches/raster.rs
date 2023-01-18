use criterion::{black_box, criterion_group, criterion_main, Criterion, Bencher};
use rasterizer::{Rasterizer, RasterPixelFunction, AllExecutionsFunction, BoxFace, SamplePixelFunction, EarlyExitFunction};
use ultraviolet::{Mat4, Vec3, IVec2};

fn draw_cube(bencher: &mut Bencher, width: usize, height: usize) {
    let camera_target = Vec3::new(0.0, 0.0, 0.0);
    let camera_position = Vec3::new(1.0, 3.0, 3.0);
    let camera = Camera::create(camera_target, camera_position, width, height);

    let mut rasterizer = Rasterizer::create(width, height);
    rasterizer.clear();
    rasterizer.set_camera(camera.position, camera.proj_matrix * camera.view_matrix);

    let positions = black_box([
        IVec2::new(25 * 16, 0 * 16),
        IVec2::new(50 * 16, 16 * 16),
        IVec2::new(50 * 16, 30 * 16),
        IVec2::new(25 * 16, 45 * 16),
        IVec2::new(0 * 16, 31 * 16),
        IVec2::new(0 * 16, 15 * 16)
    ]);

    bencher.iter(|| {
        let result = rasterizer.draw_hex::<RasterPixelFunction>(positions);
        
        black_box(result);
        black_box(rasterizer.tiles());
    });
}

fn test_cube(bencher: &mut Bencher, width: usize, height: usize) {
    let camera_target = Vec3::new(0.0, 0.0, 0.0);
    let camera_position = Vec3::new(1.0, 3.0, 3.0);
    let camera = Camera::create(camera_target, camera_position, width, height);

    let mut rasterizer = Rasterizer::create(width, height);
    rasterizer.clear();
    rasterizer.set_camera(camera.position, camera.proj_matrix * camera.view_matrix);

    rasterizer.draw_aabb::<RasterPixelFunction, AllExecutionsFunction>(
        Vec3::new(-1.0, -1.0, -1.0),
        Vec3::new(1.0, 1.0, 1.0),
        BoxFace::all());

    bencher.iter(|| {
        let result = rasterizer.draw_aabb::<SamplePixelFunction, EarlyExitFunction>(
            Vec3::new(-1.0, -1.0, -1.0),
            Vec3::new(1.0, 1.0, 1.0),
            BoxFace::all());

        black_box(result);
        black_box(rasterizer.tiles());
    });
}

fn criterion_benchmark(c: &mut Criterion) {
    c.bench_function("draw_cube_200px", |b| draw_cube(b, 200, 200));
    c.bench_function("draw_cube_400px", |b| draw_cube(b, 400, 400));
    c.bench_function("draw_cube_800px", |b| draw_cube(b, 800, 800));

    c.bench_function("test_cube_200px", |b| test_cube(b, 200, 200));
    c.bench_function("test_cube_400px", |b| test_cube(b, 400, 400));
    c.bench_function("test_cube_800px", |b| test_cube(b, 800, 800));
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
