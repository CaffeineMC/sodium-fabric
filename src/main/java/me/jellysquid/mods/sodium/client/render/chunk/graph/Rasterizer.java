package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.ffi.RustBindings;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class Rasterizer {
    private final long handle;

    private final int width, height;

    public Rasterizer(int width, int height) {
        this.handle = RustBindings.r_create(width, height);
        this.width = width;
        this.height = height;
    }

    public void setCamera(Camera camera, Frustum frustum) {
        var position = camera.getPos().toVector3f();
        var matrix = new Matrix4f(frustum.getMatrix())
                .translate(new Vector3f().sub(position));

        try (var stack = MemoryStack.stackPush()) {
            var positionBuf = stack.mallocFloat(3);
            position.get(positionBuf);

            var matrixBuf = stack.mallocFloat(4 * 4);
            matrix.get(matrixBuf);

            RustBindings.r_set_camera(this.handle, MemoryUtil.memAddress(positionBuf), MemoryUtil.memAddress(matrixBuf));
        }
    }

    public void saveDebugInformation(String filename) {
        ByteBuffer buffer = MemoryUtil.memAlloc(this.width * this.height * 4);
        RustBindings.r_get_depth_buffer(this.handle, MemoryUtil.memAddress(buffer));

        STBImageWrite.stbi_flip_vertically_on_write(true);
        STBImageWrite.stbi_write_png(filename, this.width, this.height, 4, buffer, (this.width * 4));
        STBImageWrite.stbi_flip_vertically_on_write(false);

        MemoryUtil.memFree(buffer);

        RustBindings.r_print_stats(this.handle);
    }

    public void drawBoxes(VoxelBoxList boxes, int x, int y, int z) {
        RustBindings.r_draw_boxes(this.handle, MemoryUtil.memAddress(boxes.buffer), boxes.count, x, y, z);
    }

    public boolean testBox(float x1, float y1, float z1, float x2, float y2, float z2, int faces) {
        return RustBindings.r_test_box(this.handle, x1, y1, z1, x2, y2, z2, faces);
    }

    public void clear() {
        RustBindings.r_clear(this.handle);
    }

    public void destroy() {
        RustBindings.r_destroy(this.handle);
    }
}
