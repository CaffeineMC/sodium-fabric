package me.jellysquid.mods.sodium.mixin.buffers;

import me.jellysquid.mods.sodium.client.model.ParticleVertexConsumer;
import me.jellysquid.mods.sodium.client.model.QuadVertexConsumer;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import me.jellysquid.mods.sodium.common.util.matrix.MatrixUtil;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer implements
        ParticleVertexConsumer, QuadVertexConsumer, BufferVertexConsumer {
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
    public void vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v, int overlay, int light, float normX, float normY, float normZ) {
        if (!this.field_21594) {
            super.vertex(x, y, z, r, g, b, a, u, v, overlay, light, normX, normY, normZ);

            return;
        }

        this.vertexQuad(x, y, z, ColorARGB.pack(r, g, b, a), u, v, overlay, light, Norm3b.pack(normX, normY, normZ));
    }

    @Override
    public void vertexQuad(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        int size = this.format.getVertexSize();

        this.grow(size);

        if (UnsafeUtil.isAvailable()) {
            this.vertexUnsafe(x, y, z, color, u, v, overlay, light, normal);
        } else {
            this.vertexFallback(x, y, z, color, u, v, overlay, light, normal);
        }

        this.elementOffset += size;
        this.vertexCount++;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void vertexUnsafe(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        long i = MemoryUtil.memAddress(this.buffer, this.elementOffset);

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

    @Override
    public void vertexParticle(float x, float y, float z, float u, float v, int color, int light) {
        if (this.format != VertexFormats.POSITION_TEXTURE_COLOR_LIGHT) {
            throw new IllegalStateException("Invalid vertex format");
        }

        int size = this.format.getVertexSize();

        this.grow(size);

        if (UnsafeUtil.isAvailable()) {
            this.vertexParticleUnsafe(x, y, z, u, v, color, light);
        } else {
            this.vertexParticleFallback(x, y, z, u, v, color, light);
        }

        this.elementOffset += size;
        this.vertexCount++;
    }

    private void vertexParticleFallback(float x, float y, float z, float u, float v, int color, int light) {
        int i = this.elementOffset;

        ByteBuffer buffer = this.buffer;
        buffer.putFloat(i, x);
        i += 4;

        buffer.putFloat(i, y);
        i += 4;

        buffer.putFloat(i, z);
        i += 4;

        buffer.putFloat(i, u);
        i += 4;

        buffer.putFloat(i, v);
        i += 4;

        buffer.putInt(i, color);
        i += 4;

        buffer.putInt(i, light);
        i += 4;
    }

    private void vertexParticleUnsafe(float x, float y, float z, float u, float v, int color, int light) {
        long i = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        Unsafe unsafe = UnsafeUtil.instance();
        unsafe.putFloat(i, x);
        i += 4;

        unsafe.putFloat(i, y);
        i += 4;

        unsafe.putFloat(i, z);
        i += 4;

        unsafe.putFloat(i, u);
        i += 4;

        unsafe.putFloat(i, v);
        i += 4;

        unsafe.putInt(i, color);
        i += 4;

        unsafe.putInt(i, light);
        i += 4;
    }

    @Override
    public void quad(MatrixStack.Entry matrices, BakedQuad quad, float[] brightnessTable, float r, float g, float b, int[] light, int overlay, boolean colorize) {
        if (!this.field_21594) {
            super.quad(matrices, quad, brightnessTable, r, g, b, light, overlay, colorize);

            return;
        }

        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        ModelQuadView quadView = (ModelQuadView) quad;

        Matrix4f modelMatrix = matrices.getModel();
        Matrix3f normalMatrix = matrices.getNormal();

        int norm = MatrixUtil.computeNormal(normalMatrix, quad.getFace());

        for (int i = 0; i < 4; i++) {
            float x = quadView.getX(i);
            float y = quadView.getY(i);
            float z = quadView.getZ(i);

            float fR;
            float fG;
            float fB;

            float brightness = brightnessTable[i];

            if (colorize) {
                int color = quadView.getColor(i);

                float oR = ColorARGB.normalize(ColorARGB.unpackRed(color));
                float oG = ColorARGB.normalize(ColorARGB.unpackGreen(color));
                float oB = ColorARGB.normalize(ColorARGB.unpackBlue(color));

                fR = oR * brightness * r;
                fG = oG * brightness * g;
                fB = oB * brightness * b;
            } else {
                fR = brightness * r;
                fG = brightness * g;
                fB = brightness * b;
            }

            float u = quadView.getTexU(i);
            float v = quadView.getTexV(i);

            int color = ColorARGB.pack(fR, fG, fB, 1.0F);

            Vector4f pos = new Vector4f(x, y, z, 1.0F);
            pos.transform(modelMatrix);

            this.vertexQuad(pos.getX(), pos.getY(), pos.getZ(), color, u, v, overlay, light[i], norm);
        }
    }
}
