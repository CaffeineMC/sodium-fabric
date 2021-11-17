package me.jellysquid.mods.sodium.interop.vanilla.consumer;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.jellysquid.mods.sodium.SodiumClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class SmartBufferBuilderWrapper implements VertexConsumer {
    private final BufferBuilder internalBufferBuilder;
    private final FloatList primitivePositions;
    private final IntList primitivePartIds;

    private VertexFormat.DrawMode drawMode;
    private float[] primitiveVertexPositions; // vertex positions in the current primitive
    private int currentPosIdx;
    private int currentVert; // TODO: need better name for this
    private boolean firstPrimFinished;

    public SmartBufferBuilderWrapper(BufferBuilder bufferBuilder, int initialSize) {
        this.internalBufferBuilder = bufferBuilder;
        this.primitivePositions = new FloatArrayList(initialSize);
        this.primitivePartIds = new IntArrayList(initialSize);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        // Keep track of the vertex positions with an on-CPU buffer
        // Only store them as single-precision floats, that's plenty for transparency sorting

        primitiveVertexPositions[currentPosIdx++] = (float) x;
        primitiveVertexPositions[currentPosIdx++] = (float) y;
        primitiveVertexPositions[currentPosIdx++] = (float) z;

        // for modes like triangle strip and line strip, the primitive ends after the vertex count
        // fills, and from then it's every time the size fills. NOTE: this doesn't account for
        // primitive restarts, but minecraft doesn't use them anyway.
        boolean primFinished = false;
        if (firstPrimFinished) {
            currentVert++;
            if (currentVert >= drawMode.size) {
                currentVert = 0;
                primFinished = true;
            }
        }

        if (currentPosIdx >= primitiveVertexPositions.length) {
            currentPosIdx = 0;
            if (!firstPrimFinished) {
                firstPrimFinished = true;
                primFinished = true;
            }
        }

        if (primFinished) {
            // average vertex positions in primitive
            float totalX = 0.0f;
            float totalY = 0.0f;
            float totalZ = 0.0f;

            for (int vert = 0; vert < drawMode.vertexCount; vert++) {
                int startingPos = vert * 3;
                totalX += primitiveVertexPositions[startingPos];
                totalY += primitiveVertexPositions[startingPos + 1];
                totalZ += primitiveVertexPositions[startingPos + 2];
            }

            primitivePositions.add(totalX / drawMode.vertexCount);
            primitivePositions.add(totalY / drawMode.vertexCount);
            primitivePositions.add(totalZ / drawMode.vertexCount);
            primitivePartIds.add(partId);
        }

        return internalBufferBuilder.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return internalBufferBuilder.color(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return internalBufferBuilder.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return internalBufferBuilder.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return internalBufferBuilder.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return internalBufferBuilder.normal(x, y, z);
    }

    @Override
    public void next() {
        internalBufferBuilder.next();
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha) {
        internalBufferBuilder.fixedColor(red, green, blue, alpha);
    }

    @Override
    public void unfixColor() {
        internalBufferBuilder.unfixColor();
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        BufferBuilderAccessor originalAccessor = (BufferBuilderAccessor) internalBufferBuilder;

        vertex(x, y, z); // Make sure we call this, to record the verts for the transparency stuff
        internalBufferBuilder.texture(u, v).normal(normalX, normalY, normalZ);
        originalAccessor.getBuffer().putInt(originalAccessor.getElementOffset(), partId);
        internalBufferBuilder.nextElement();
        internalBufferBuilder.next();
    }

    private int partId;

    public void setId(int partId) {
        this.partId = partId;
    }

    public void begin(VertexFormat.DrawMode drawMode, VertexFormat vertexFormat) {
        internalBufferBuilder.begin(drawMode, vertexFormat);
        this.drawMode = drawMode;
        primitiveVertexPositions = new float[drawMode.vertexCount * 3];
    }

    public int getVertexCount() {
        return ((BufferBuilderAccessor) internalBufferBuilder).getVertexCount();
    }

    public void end() {
        internalBufferBuilder.end();
        if (currentPosIdx != 0) {
            SodiumClient.logger().warn("Primitive not finished! Pos idx at: " + currentPosIdx);
            currentPosIdx = 0;
        }
        primitiveVertexPositions = null;
        currentVert = 0;
        firstPrimFinished = false;
    }

    public float[] getPrimitivePositions() {
        return primitivePositions.toFloatArray();
    }

    public int[] getPrimitivePartIds() {
        return primitivePartIds.toIntArray();
    }

    public void clear() {
        internalBufferBuilder.clear();
        primitivePositions.clear();
        primitivePartIds.clear();
    }

    public BufferBuilder getInternalBufferBuilder() {
        return internalBufferBuilder;
    }

}
