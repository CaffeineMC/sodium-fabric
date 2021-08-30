package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_sort;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.util.geometry.BufferBuilderParametersAccess;
import me.jellysquid.mods.sodium.client.util.geometry.GeometrySort;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {
    private static final Vec3f[] MAGIC_ARRAY = new Vec3f[0];

    private float[] currentParametersFlat;

    @Shadow
    private int buildStart;

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private VertexFormat format;

    @Shadow
    private VertexFormat.DrawMode drawMode;

    @Shadow
    private int vertexCount;

    @Shadow
    protected abstract IntConsumer createConsumer(VertexFormat.IntType elementFormat);

    @Shadow
    private int elementOffset;

    @Shadow
    private float cameraX;

    @Shadow
    private float cameraY;

    @Shadow
    private float cameraZ;

    @Redirect(method = "setCameraPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;buildParameterVector()[Lnet/minecraft/util/math/Vec3f;"))
    private Vec3f[] redirectBuildParameterVector(BufferBuilder bufferBuilder) {
        this.currentParametersFlat = this.buildParameterVector();

        return MAGIC_ARRAY;
    }

    @Inject(method = "popState", at = @At("RETURN"))
    private void injectPopData(CallbackInfoReturnable<BufferBuilder.State> cir) {
        BufferBuilderParametersAccess access = (BufferBuilderParametersAccess) cir.getReturnValue();
        access.setParametersFlat(this.currentParametersFlat);
    }

    @Inject(method = "restoreState", at = @At("RETURN"))
    private void restoreState(BufferBuilder.State state, CallbackInfo ci) {
        BufferBuilderParametersAccess access = (BufferBuilderParametersAccess) state;
        this.currentParametersFlat = access.getParametersFlat();
    }

    private float[] buildParameterVector() {
        int primitiveCount = this.vertexCount / this.drawMode.size;

        float[] arr = new float[primitiveCount * 3];
        ByteBuffer buf = this.buffer;

        if (SodiumClientMod.isDirectMemoryAccessEnabled()) {
            long start = MemoryUtil.memAddress(this.buffer, this.buildStart);
            long vertexStride = this.format.getVertexSize();
            long primitiveStride = vertexStride * this.drawMode.size;
            long midpoint = (vertexStride * 2);

            int k = 0;

            for(int index = 0; index < primitiveCount; ++index) {
                long c1 = start + (index * primitiveStride);
                long c2 = c1 + midpoint;

                float x1 = MemoryUtil.memGetFloat(c1);
                float y1 = MemoryUtil.memGetFloat(c1 + 4);
                float z1 = MemoryUtil.memGetFloat(c1 + 8);

                float x2 = MemoryUtil.memGetFloat(c2);
                float y2 = MemoryUtil.memGetFloat(c2 + 4);
                float z2 = MemoryUtil.memGetFloat(c2 + 8);

                arr[k++] = (x1 + x2) * 0.5F;
                arr[k++] = (y1 + y2) * 0.5F;
                arr[k++] = (z1 + z2) * 0.5F;
            }
        } else {
            int start = this.buildStart / 4;
            int vertexStride = this.format.getVertexSizeInteger();
            int primitiveStride = vertexStride * this.drawMode.size;
            int midpoint = (vertexStride * 2);

            FloatBuffer floatBuffer = this.buffer.asFloatBuffer();
            int k = 0;

            for(int index = 0; index < primitiveCount; ++index) {
                int c1 = start + (index * primitiveStride);
                int c2 = c1 + midpoint;

                float x1 = floatBuffer.get(c1);
                float y1 = floatBuffer.get(c1 + 1);
                float z1 = floatBuffer.get(c1 + 2);

                float x2 = floatBuffer.get(c2);
                float y2 = floatBuffer.get(c2 + 1);
                float z2 = floatBuffer.get(c2 + 2);

                arr[k++] = (x1 + x2) * 0.5F;
                arr[k++] = (y1 + y2) * 0.5F;
                arr[k++] = (z1 + z2) * 0.5F;
            }
        }

        return arr;
    }

    /**
     * @author JellySquid
     * @reason Use direct memory access, avoid NIO/IntConsumer indirection, access flattened position attributes
     */
    @Overwrite
    private void writeCameraOffset(VertexFormat.IntType elementFormat) {
        int count = this.currentParametersFlat.length / 3;

        float[] vectors = this.currentParametersFlat;
        float[] distance = new float[count];
        int[] indices = new int[count];

        float x = this.cameraX;
        float y = this.cameraY;
        float z = this.cameraZ;

        for(int i = 0; i < count; indices[i] = i++) {
            int j = i * 3;

            float xDist = vectors[j] - x;
            float yDist = vectors[j + 1] - y;
            float zDist = vectors[j + 2] - z;

            distance[i] = xDist * xDist + yDist * yDist + zDist * zDist;
        }

        GeometrySort.mergeSort(indices, distance);

        if (SodiumClientMod.isDirectMemoryAccessEnabled()) {
            // Subtract one base unit as we're using += below
            long p = MemoryUtil.memAddress(this.buffer, this.elementOffset) - elementFormat.size;

            for (int index : indices) {
                int start = index * this.drawMode.size;

                switch (elementFormat) {
                    case BYTE -> {
                        MemoryUtil.memPutByte(p += 1, (byte) (start + 0));
                        MemoryUtil.memPutByte(p += 1, (byte) (start + 1));
                        MemoryUtil.memPutByte(p += 1, (byte) (start + 2));
                        MemoryUtil.memPutByte(p += 1, (byte) (start + 2));
                        MemoryUtil.memPutByte(p += 1, (byte) (start + 3));
                        MemoryUtil.memPutByte(p += 1, (byte) (start + 0));
                    }
                    case SHORT -> {
                        MemoryUtil.memPutShort(p += 2, (short) (start + 0));
                        MemoryUtil.memPutShort(p += 2, (short) (start + 1));
                        MemoryUtil.memPutShort(p += 2, (short) (start + 2));
                        MemoryUtil.memPutShort(p += 2, (short) (start + 2));
                        MemoryUtil.memPutShort(p += 2, (short) (start + 3));
                        MemoryUtil.memPutShort(p += 2, (short) (start + 0));
                    }
                    case INT -> {
                        MemoryUtil.memPutInt(p += 4, start + 0);
                        MemoryUtil.memPutInt(p += 4, start + 1);
                        MemoryUtil.memPutInt(p += 4, start + 2);
                        MemoryUtil.memPutInt(p += 4, start + 2);
                        MemoryUtil.memPutInt(p += 4, start + 3);
                        MemoryUtil.memPutInt(p += 4, start + 0);
                    }
                }
            }

            this.buffer.position(this.elementOffset + (indices.length * elementFormat.size * 6));
        } else {
            IntConsumer intConsumer = this.createConsumer(elementFormat);

            this.buffer.position(this.elementOffset);

            for (int index : indices) {
                int p = index * this.drawMode.size;

                intConsumer.accept(p + 0);
                intConsumer.accept(p + 1);
                intConsumer.accept(p + 2);
                intConsumer.accept(p + 2);
                intConsumer.accept(p + 3);
                intConsumer.accept(p + 0);
            }

        }
    }


    @Mixin(BufferBuilder.State.class)
    private static class MixinState implements BufferBuilderParametersAccess {
        private float[] parametersFlat;

        @Override
        public float[] getParametersFlat() {
            return this.parametersFlat;
        }

        @Override
        public void setParametersFlat(float[] parametersFlat) {
            this.parametersFlat = parametersFlat;
        }
    }
}
