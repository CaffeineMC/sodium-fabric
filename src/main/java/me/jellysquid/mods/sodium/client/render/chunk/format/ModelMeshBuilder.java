package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType.ModelBufferTarget;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class ModelMeshBuilder implements ChunkMeshBuilder<ModelBufferTarget> {
    private final StructBufferBuilder vertices = new StructBufferBuilder(756 * 20, 24);

    private int primitiveCount = 0;

    @Override
    public void reset() {
        this.vertices.reset();

        this.primitiveCount = 0;
    }

    @Override
    public void destroy() {
        this.vertices.destroy();
    }

    @Override
    public ByteBuffer getBuffer(ModelBufferTarget target) {
        return switch (target) {
            case VERTICES -> this.vertices.window();
        };
    }

    @Override
    public int getPrimitiveCount() {
        return this.primitiveCount;
    }

    @Override
    public void add(Vec3i pos, ModelQuadView quad, ModelQuadFacing facing) {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer vertexBuf = memoryStack.calloc(4, 24);

            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                vertexBuf.putFloat(0, pos.getX() + quad.getX(vertexIndex));
                vertexBuf.putFloat(4, pos.getY() + quad.getY(vertexIndex));
                vertexBuf.putFloat(8, pos.getZ() + quad.getZ(vertexIndex));

                vertexBuf.putInt(12, quad.getColor(vertexIndex));

                vertexBuf.putShort(16, encodeBlockTexture(quad.getTexU(vertexIndex)));
                vertexBuf.putShort(18, encodeBlockTexture(quad.getTexV(vertexIndex)));

                vertexBuf.putInt(20, encodeLightMapTexCoord(quad.getLight(vertexIndex)));

                this.vertices.add(vertexBuf);
            }
        }

        this.primitiveCount++;
    }

    private static final int TEXTURE_MAX_VALUE = 65536;

    private static int encodeLightMapTexCoord(int light) {
        int r = light;

        // Mask off coordinate values outside 0..255
        r &= 0x00FF_00FF;

        // Light coordinates are normalized values, so upcasting requires a shift
        // Scale the coordinates from the range of 0..255 (unsigned byte) into 0..65535 (unsigned short)
        r <<= 8;

        // Add a half-texel offset to each coordinate so we sample from the center of each texel
        r += 0x0800_0800;

        return r;
    }

    private static short encodeBlockTexture(float value) {
        return (short) (value * TEXTURE_MAX_VALUE);
    }

}
