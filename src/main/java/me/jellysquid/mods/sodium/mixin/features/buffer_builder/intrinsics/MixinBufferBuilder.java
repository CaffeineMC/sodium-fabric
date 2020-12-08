package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.consumer.GlyphVertexConsumer;
import me.jellysquid.mods.sodium.client.model.consumer.LineVertexConsumer;
import me.jellysquid.mods.sodium.client.model.consumer.ParticleVertexConsumer;
import me.jellysquid.mods.sodium.client.model.consumer.QuadVertexConsumer;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorU8;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import sun.misc.Unsafe;

import java.nio.ByteBuffer;

@SuppressWarnings({ "SameParameterValue", "SuspiciousNameCombination" })
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer
        implements ParticleVertexConsumer, QuadVertexConsumer, GlyphVertexConsumer, LineVertexConsumer {
    @Shadow
    private VertexFormat format;

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int elementOffset;

    @Shadow
    private int vertexCount;

    @Shadow
    private boolean field_21595; // has overlay

    @Shadow
    private boolean field_21594; // is baked quad format

    @Shadow
    protected abstract void grow(int size);

    @Override
    public void vertexParticle(float x, float y, float z, float u, float v, int color, int light) {
        if (this.format != VertexFormats.POSITION_TEXTURE_COLOR_LIGHT) {
            this.vertexParticleFallback(x, y, z, u, v, color, light);
            return;
        }

        int size = this.format.getVertexSize();

        this.grow(size);

        if (UnsafeUtil.isAvailable()) {
            this.vertexParticleUnsafe(x, y, z, u, v, color, light);
        } else {
            this.vertexParticleSafe(x, y, z, u, v, color, light);
        }

        this.elementOffset += size;
        this.vertexCount++;
    }

    private void vertexParticleFallback(float x, float y, float z, float u, float v, int color, int light) {
        this.vertex(x, y, z);
        this.texture(u, v);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color),
                ColorABGR.unpackAlpha(color));
        this.light(light);
        this.next();
    }

    private void vertexParticleSafe(float x, float y, float z, float u, float v, int color, int light) {
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
    public void vertexQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        if (!this.field_21594) {
            this.vertexQuadFallback(x, y, z, color, u, v, overlay, light, normal);
            return;
        }

        int size = this.format.getVertexSize();

        this.grow(size);

        if (UnsafeUtil.isAvailable()) {
            this.vertexQuadUnsafe(x, y, z, color, u, v, overlay, light, normal);
        } else {
            this.vertexQuadSafe(x, y, z, color, u, v, overlay, light, normal);
        }

        this.elementOffset += size;
        this.vertexCount++;
    }

    private void vertexQuadFallback(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        this.vertex(x, y, z);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color),
                ColorABGR.unpackAlpha(color));
        this.texture(u, v);

        if (this.field_21595) {
            this.overlay(overlay);
        }

        this.light(light);
        this.normal(Norm3b.unpackX(normal), Norm3b.unpackY(normal), Norm3b.unpackZ(normal));
        this.next();
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void vertexQuadUnsafe(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
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

    private void vertexQuadSafe(float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
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

                float oR = ColorU8.normalize(ColorABGR.unpackRed(color));
                float oG = ColorU8.normalize(ColorABGR.unpackGreen(color));
                float oB = ColorU8.normalize(ColorABGR.unpackBlue(color));

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

            int color = ColorABGR.pack(fR, fG, fB, 1.0F);

            Vector4f pos = new Vector4f(x, y, z, 1.0F);
            pos.transform(modelMatrix);

            this.vertexQuad(pos.getX(), pos.getY(), pos.getZ(), color, u, v, light[i], overlay, norm);
        }
    }

    @Override
    public void vertexGlyph(Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        Matrix4fExtended matrixExt = MatrixUtil.getExtendedMatrix(matrix);

        float x2 = matrixExt.transformVecX(x, y, z);
        float y2 = matrixExt.transformVecY(x, y, z);
        float z2 = matrixExt.transformVecZ(x, y, z);

        if (this.format != VertexFormats.POSITION_COLOR_TEXTURE_LIGHT) {
            this.vertexGlyphFallback(x2, y2, z2, color, u, v, light);
            return;
        }

        int size = this.format.getVertexSize();

        this.grow(size);

        if (UnsafeUtil.isAvailable()) {
            this.vertexGlyphUnsafe(x2, y2, z2, color, u, v, light);
        } else {
            this.vertexGlyphSafe(x2, y2, z2, color, u, v, light);
        }

        this.elementOffset += size;
        this.vertexCount++;
    }

    private void vertexGlyphFallback(float x, float y, float z, int color, float u, float v, int light) {
        this.vertex(x, y, z);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color),
                ColorABGR.unpackAlpha(color));
        this.texture(u, v);
        this.light(light);
        this.next();
    }

    private void vertexGlyphSafe(float x, float y, float z, int color, float u, float v, int light) {
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

        buffer.putInt(i, light);
        i += 4;
    }

    private void vertexGlyphUnsafe(float x, float y, float z, int color, float u, float v, int light) {
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

        unsafe.putInt(i, light);
        i += 4;
    }

    @Override
    public void vertexLine(Matrix4f matrix, float x, float y, float z, int color) {
        Matrix4fExtended matrixExt = MatrixUtil.getExtendedMatrix(matrix);

        float x2 = matrixExt.transformVecX(x, y, z);
        float y2 = matrixExt.transformVecY(x, y, z);
        float z2 = matrixExt.transformVecZ(x, y, z);

        if (this.format != VertexFormats.POSITION_COLOR) {
            this.vertexLineFallback(x2, y2, z2, color);

            return;
        }

        int size = this.format.getVertexSize();

        this.grow(size);

        if (UnsafeUtil.isAvailable()) {
            this.vertexLineUnsafe(x2, y2, z2, color);
        } else {
            this.vertexLineSafe(x2, y2, z2, color);
        }

        this.elementOffset += size;
        this.vertexCount++;
    }

    private void vertexLineFallback(float x, float y, float z, int color) {
        this.vertex(x, y, z);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color),
                ColorABGR.unpackAlpha(color));
        this.next();
    }

    private void vertexLineSafe(float x, float y, float z, int color) {
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
    }

    private void vertexLineUnsafe(float x, float y, float z, int color) {
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
    }
}
