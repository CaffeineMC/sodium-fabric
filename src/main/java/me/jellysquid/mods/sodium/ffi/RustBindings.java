package me.jellysquid.mods.sodium.ffi;

public class RustBindings {
    public static native long r_create(int width, int height);
    public static native void r_set_camera(long handle, long position_ptr, long matrix_ptr);
    public static native boolean r_draw_boxes(long handle, long buffer, int count, int x, int y, int z);
    public static native boolean r_test_box(long handle, float x1, float y1, float z1, float x2, float y2, float z2, int faces);
    public static native void r_clear(long handle);
    public static native void r_destroy(long handle);
    public static native void r_get_depth_buffer(long handle, long buffer);

    static {
        System.loadLibrary("rasterizer_jni");
    }
}
