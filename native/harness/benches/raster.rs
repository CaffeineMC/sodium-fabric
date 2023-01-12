use criterion::{black_box, criterion_group, criterion_main, Criterion, Bencher};
use rasterizer::{Rasterizer, RasterPixelFunction, AllExecutionsFunction, BoxFace, SamplePixelFunction, EarlyExitFunction};
use ultraviolet::{Mat4, Vec3};

fn draw_cube(bencher: &mut Bencher, width: usize, height: usize) {
    let camera_target = Vec3::new(0.0, 0.0, 0.0);
    let camera_position = Vec3::new(1.0, 3.0, 3.0);
    let camera = Camera::create(camera_target, camera_position, width, height);

    let mut rasterizer = Rasterizer::create(width, height);
    rasterizer.clear();
    rasterizer.set_camera(camera.position, camera.proj_matrix * camera.view_matrix);

    bencher.iter(|| {
        let result = rasterizer.draw_aabb::<RasterPixelFunction, AllExecutionsFunction>(
            &Vec3::new(-1.0, -1.0, -1.0),
            &Vec3::new(1.0, 1.0, 1.0),
            BoxFace::ALL);

        black_box(result);
        black_box(rasterizer.tiles());
    });
}

fn draw_small_cubes(bencher: &mut Bencher, width: usize, height: usize) {
    let camera_target = Vec3::new(0.0, 0.0, 0.0);
    let camera_position = Vec3::new(1.0, 3.0, 3.0);
    let camera = Camera::create(camera_target, camera_position, width, height);
    let steps = 5;
    let start = -1.0;
    let end = 1.0;
    let size = ((end - start) / steps as f32) * 0.5;
    let size_steps = 4;

    let mut rasterizer = Rasterizer::create(width, height);
    rasterizer.clear();
    rasterizer.set_camera(camera.position, camera.proj_matrix * camera.view_matrix);

    bencher.iter(|| {
        let mut result = false;
        for size_factor in 1..=size_steps {
            let real_size = size * size_factor as f32;
            for x in 0..steps {
                for y in 0..steps {
                    for z in 0..steps {
                        let x = start + ((steps - x) as f32 / steps as f32) * (end - start);
                        let y = start + (y as f32 / steps as f32) * (end - start);
                        let z = start + ((steps - z) as f32 / steps as f32) * (end - start);

                        result |= rasterizer.draw_aabb::<RasterPixelFunction, AllExecutionsFunction>(
                            &Vec3::new(x, y, z),
                            &Vec3::new(x + real_size, y + real_size, z + real_size),
                            BoxFace::ALL
                        );
                    }
                }
            }
        }
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
        &Vec3::new(-1.0, -1.0, -1.0),
        &Vec3::new(1.0, 1.0, 1.0),
        BoxFace::ALL);

    bencher.iter(|| {
        let result = rasterizer.draw_aabb::<SamplePixelFunction, EarlyExitFunction>(
            &Vec3::new(-1.0, -1.0, -1.0),
            &Vec3::new(1.0, 1.0, 1.0),
            BoxFace::ALL);

        black_box(result);
        black_box(rasterizer.tiles());
    });
}

fn criterion_benchmark(c: &mut Criterion) {
    c.bench_function("draw_cube_200px", |b| draw_cube(b, 200, 200));
    c.bench_function("draw_cube_400px", |b| draw_cube(b, 400, 400));
    c.bench_function("draw_cube_800px", |b| draw_cube(b, 800, 800));

    c.bench_function("draw_small_cubes_200px", |b| draw_small_cubes(b, 200, 200));
    c.bench_function("draw_small_cubes_400px", |b| draw_small_cubes(b, 400, 400));
    c.bench_function("draw_small_cubes_800px", |b| draw_small_cubes(b, 800, 800));

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
