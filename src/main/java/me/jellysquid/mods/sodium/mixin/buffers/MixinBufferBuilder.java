package me.jellysquid.mods.sodium.mixin.buffers;

import me.jellysquid.mods.sodium.client.render.pipeline.DirectVertexConsumer;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer implements DirectVertexConsumer {
    @Shadow
    private int elementOffset;

    @Shadow
    public abstract void next();

    @Shadow
    private boolean field_21594; // canUseVertexPath

    @Shadow
    private boolean field_21595; // hasOverlay

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private VertexFormat format;

    @Shadow
    private int vertexCount;

    @Shadow
    protected abstract void grow(int size);

    /**
     * @reason Use faster implementation which works directly with Unsafe
     * @author JellySquid
     */
    @Overwrite
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int light1, int light2, float normX, float normY, float normZ) {
        if (!this.field_21594) {
            super.vertex(x, y, z, r, g, b, a, u, v, light1, light2, normX, normY, normZ);

            return;
        }

        this.vertex(x, y, z, ColorUtil.encodeRGBA(r, g, b, a), u, v, light1, light2, QuadUtil.encodeNormal(normX, normY, normZ));
    }

    @Override
    public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        if (UnsafeUtil.isAvailable()) {
            this.vertexUnsafe(x, y, z, color, u, v, overlay, light, normal);
        } else {
            this.vertexFallback(x, y, z, color, u, v, overlay, light, normal);
        }

        int size = this.format.getVertexSize();

        this.elementOffset += size;
        this.vertexCount++;

        this.grow(size);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void vertexUnsafe(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        long i = ((DirectBuffer) this.buffer).address() + this.elementOffset;

        Unsafe unsafe = UnsafeUtil.instance();
        unsafe.putFloat(i, x);
        i += 4;

        unsafe.putFloat(i, y);
        i += 4;

        unsafe.putFloat(i, z);
        i += 4;

        unsafe.putInt(i, color);
        i += 4;

        unsafe.putFloat(i, u);
        i += 4;

        unsafe.putFloat(i, v);
        i += 4;

        if (this.field_21595) {
            unsafe.putInt(i, overlay);
            i += 4;
        }

        unsafe.putInt(i, light);
        i += 4;

        unsafe.putInt(i, normal);
    }

    private void vertexFallback(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        int i = this.elementOffset;

        ByteBuffer buffer = this.buffer;
        buffer.putFloat(i, x);
        i += 4;

        buffer.putFloat(i, y);
        i += 4;

        buffer.putFloat(i, z);
        i += 4;

        buffer.putInt(i, color);
        i += 4;

        buffer.putFloat(i, u);
        i += 4;

        buffer.putFloat(i, v);
        i += 4;

        if (this.field_21595) {
            buffer.putInt(i, overlay);
            i += 4;
        }

        buffer.putInt(i, light);
        i += 4;

        buffer.putInt(i, normal);
    }
}
