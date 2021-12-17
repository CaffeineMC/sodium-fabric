package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType.CubeBufferTarget;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public class CubeMeshBuilder implements ChunkMeshBuilder<CubeBufferTarget> {
    private final StructBufferBuilder quads = new StructBufferBuilder(756 * 20, 20);
    private final StructBufferBuilder vertices = new StructBufferBuilder(756 * 20, 4);

    private int primitiveCount = 0;

    @Override
    public void reset() {
        this.quads.reset();
        this.vertices.reset();

        this.primitiveCount = 0;
    }

    @Override
    public void destroy() {
        this.quads.destroy();
        this.vertices.destroy();
    }

    @Override
    public ByteBuffer getBuffer(CubeBufferTarget target) {
        return switch (target) {
            case QUADS -> this.quads.window();
            case VERTICES -> this.vertices.window();
        };
    }

    @Override
    public int getPrimitiveCount() {
        return this.primitiveCount;
    }

    @Override
    public void add(Vec3i pos, ModelQuadView quad, ModelQuadFacing facing) {
        int lightBlock = 0;
        int lightSky = 0;

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            lightBlock |= MathHelper.clamp(((quad.getLight(vertexIndex)) & 0xFF), 8, 248) << (vertexIndex * 8);
            lightSky |= MathHelper.clamp(((quad.getLight(vertexIndex) >> 16) & 0xFF), 8, 248) << (vertexIndex * 8);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bufVertex = stack.calloc(4, 4);

            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                bufVertex.putInt(0, quad.getColor(vertexIndex));

                this.vertices.add(bufVertex);
            }

            ByteBuffer bufQuad = stack.malloc(4, 20);
            bufQuad.put(3, (byte) pos.getX());
            bufQuad.put(2, (byte) pos.getY());
            bufQuad.put(1, (byte) pos.getZ());
            bufQuad.put(0, (byte) facing.ordinal());

            bufQuad.putInt(4, lightBlock);
            bufQuad.putInt(8, lightSky);

            bufQuad.putShort(12, encodeBlockTexture(quad.getTexU(0)));
            bufQuad.putShort(14, encodeBlockTexture(quad.getTexV(0)));

            bufQuad.putShort(16, encodeBlockTexture(quad.getTexU(2)));
            bufQuad.putShort(18, encodeBlockTexture(quad.getTexV(2)));

            this.quads.add(bufQuad);
        }

        this.primitiveCount++;
    }

    private static final int TEXTURE_MAX_VALUE = 65536;
    private static short encodeBlockTexture(float value) {
        return (short) (value * TEXTURE_MAX_VALUE);
    }
}
