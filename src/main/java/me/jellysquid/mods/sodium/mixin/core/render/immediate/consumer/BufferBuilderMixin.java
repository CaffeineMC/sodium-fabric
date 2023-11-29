package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.*;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.minecraft.client.render.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin extends FixedColorVertexConsumer implements VertexBufferWriter {
    @Shadow
    protected abstract void grow(int size);

    @Unique
    private static final int ATTRIBUTE_NOT_PRESENT = -1;

    @Unique
    private static final int
            ATTRIBUTE_POSITION_BIT  = 1 << CommonVertexAttribute.POSITION.ordinal(),
            ATTRIBUTE_COLOR_BIT     = 1 << CommonVertexAttribute.COLOR.ordinal(),
            ATTRIBUTE_TEXTURE_BIT   = 1 << CommonVertexAttribute.TEXTURE.ordinal(),
            ATTRIBUTE_OVERLAY_BIT   = 1 << CommonVertexAttribute.OVERLAY.ordinal(),
            ATTRIBUTE_LIGHT_BIT     = 1 << CommonVertexAttribute.LIGHT.ordinal(),
            ATTRIBUTE_NORMAL_BIT    = 1 << CommonVertexAttribute.NORMAL.ordinal();

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private int elementOffset;

    @Shadow
    private VertexFormat.DrawMode drawMode;

    @Unique
    private VertexFormatDescription formatDescription;

    @Unique
    private int vertexStride;

    @Unique
    private int
            attributeOffsetPosition,
            attributeOffsetColor,
            attributeOffsetTexture,
            attributeOffsetOverlay,
            attributeOffsetLight,
            attributeOffsetNormal;

    @Unique
    private int requiredAttributes, writtenAttributes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int initialCapacity, CallbackInfo ci) {
        this.resetAttributeBindings();
    }

    @Unique
    private void resetAttributeBindings() {
        this.requiredAttributes = 0;

        this.attributeOffsetPosition = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetColor = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetTexture = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetOverlay = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetLight = ATTRIBUTE_NOT_PRESENT;
        this.attributeOffsetNormal = ATTRIBUTE_NOT_PRESENT;
    }

    @Inject(
            method = "setFormat",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/BufferBuilder;format:Lnet/minecraft/client/render/VertexFormat;",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.formatDescription = VertexFormatRegistry.instance()
                .get(format);
        this.vertexStride = this.formatDescription.stride();

        this.updateAttributeBindings(this.formatDescription);
    }

    @Unique
    private void updateAttributeBindings(VertexFormatDescription desc) {
        this.resetAttributeBindings();

        if (desc.containsElement(CommonVertexAttribute.POSITION)) {
            this.requiredAttributes |= ATTRIBUTE_POSITION_BIT;
            this.attributeOffsetPosition = desc.getElementOffset(CommonVertexAttribute.POSITION);
        }

        if (desc.containsElement(CommonVertexAttribute.COLOR)) {
            this.requiredAttributes |= ATTRIBUTE_COLOR_BIT;
            this.attributeOffsetColor = desc.getElementOffset(CommonVertexAttribute.COLOR);
        }

        if (desc.containsElement(CommonVertexAttribute.TEXTURE)) {
            this.requiredAttributes |= ATTRIBUTE_TEXTURE_BIT;
            this.attributeOffsetTexture = desc.getElementOffset(CommonVertexAttribute.TEXTURE);
        }

        if (desc.containsElement(CommonVertexAttribute.OVERLAY)) {
            this.requiredAttributes |= ATTRIBUTE_OVERLAY_BIT;
            this.attributeOffsetOverlay = desc.getElementOffset(CommonVertexAttribute.OVERLAY);
        }

        if (desc.containsElement(CommonVertexAttribute.LIGHT)) {
            this.requiredAttributes |= ATTRIBUTE_LIGHT_BIT;
            this.attributeOffsetLight = desc.getElementOffset(CommonVertexAttribute.LIGHT);
        }

        if (desc.containsElement(CommonVertexAttribute.NORMAL)) {
            this.requiredAttributes |= ATTRIBUTE_NORMAL_BIT;
            this.attributeOffsetNormal = desc.getElementOffset(CommonVertexAttribute.NORMAL);
        }
    }

    @Inject(
            method = "begin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BufferBuilder;setFormat(Lnet/minecraft/client/render/VertexFormat;)V"
            )
    )
    private void onBegin(VertexFormat.DrawMode drawMode, VertexFormat format, CallbackInfo ci) {
        this.writtenAttributes = 0;
    }

    @Inject(method = "resetBuilding", at = @At("RETURN"))
    private void onResetBuilding(CallbackInfo ci) {
        this.writtenAttributes = 0;
    }

    @Inject(method = "reset", at = @At("RETURN"))
    private void onReset(CallbackInfo ci) {
        this.writtenAttributes = 0;
    }

    @Unique
    private void putPositionAttribute(float x, float y, float z) {
        if (this.attributeOffsetPosition == ATTRIBUTE_NOT_PRESENT) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset + this.attributeOffsetPosition);
        PositionAttribute.put(offset, x, y, z);

        this.writtenAttributes |= ATTRIBUTE_POSITION_BIT;
    }


    @Unique
    private void putColorAttribute(int rgba) {
        if (this.attributeOffsetColor == ATTRIBUTE_NOT_PRESENT) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset + this.attributeOffsetColor);
        ColorAttribute.set(offset, rgba);

        this.writtenAttributes |= ATTRIBUTE_COLOR_BIT;
    }

    @Unique
    private void putTextureAttribute(float u, float v) {
        if (this.attributeOffsetTexture == ATTRIBUTE_NOT_PRESENT) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset + this.attributeOffsetTexture);
        TextureAttribute.put(offset, u, v);

        this.writtenAttributes |= ATTRIBUTE_TEXTURE_BIT;
    }

    @Unique
    private void putOverlayAttribute(int uv) {
        if (this.attributeOffsetOverlay == ATTRIBUTE_NOT_PRESENT) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset + this.attributeOffsetOverlay);
        OverlayAttribute.set(offset, uv);

        this.writtenAttributes |= ATTRIBUTE_OVERLAY_BIT;
    }

    @Unique
    private void putLightAttribute(int uv) {
        if (this.attributeOffsetLight == ATTRIBUTE_NOT_PRESENT) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset + this.attributeOffsetLight);
        LightAttribute.set(offset, uv);

        this.writtenAttributes |= ATTRIBUTE_LIGHT_BIT;
    }

    @Unique
    private void putNormalAttribute(int normal) {
        if (this.attributeOffsetNormal == ATTRIBUTE_NOT_PRESENT) {
            return;
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset + this.attributeOffsetNormal);
        NormalAttribute.set(offset, normal);

        this.writtenAttributes |= ATTRIBUTE_NORMAL_BIT;
    }

    @Override
    public void vertex(float x, float y, float z,
                       float red, float green, float blue, float alpha,
                       float u, float v,
                       int overlay, int light,
                       float normalX, float normalY, float normalZ
    ) {
        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        final long offset = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        if (this.attributeOffsetPosition != ATTRIBUTE_NOT_PRESENT) {
            PositionAttribute.put(offset + this.attributeOffsetPosition, x, y, z);
        }

        if (this.attributeOffsetColor != ATTRIBUTE_NOT_PRESENT) {
            ColorAttribute.set(offset + this.attributeOffsetColor, ColorABGR.pack(red, green, blue, alpha));
        }

        if (this.attributeOffsetTexture != ATTRIBUTE_NOT_PRESENT) {
            TextureAttribute.put(offset + this.attributeOffsetTexture, u, v);
        }

        if (this.attributeOffsetOverlay != ATTRIBUTE_NOT_PRESENT) {
            OverlayAttribute.set(offset + this.attributeOffsetOverlay, overlay);
        }

        if (this.attributeOffsetLight != ATTRIBUTE_NOT_PRESENT) {
            LightAttribute.set(offset + this.attributeOffsetLight, light);
        }

        if (this.attributeOffsetNormal != ATTRIBUTE_NOT_PRESENT) {
            NormalAttribute.set(offset + this.attributeOffsetNormal, NormI8.pack(normalX, normalY, normalZ));
        }

        // It's okay to mark elements as "written to" even if the vertex format does not specify those elements.
        this.writtenAttributes = ATTRIBUTE_POSITION_BIT | ATTRIBUTE_COLOR_BIT | ATTRIBUTE_TEXTURE_BIT |
                ATTRIBUTE_OVERLAY_BIT | ATTRIBUTE_LIGHT_BIT | ATTRIBUTE_NORMAL_BIT;

        this.next();
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.putPositionAttribute((float) x, (float) y, (float) z);

        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        this.putColorAttribute(ColorABGR.pack(red, green, blue, alpha));

        return this;
    }

    @Override
    public VertexConsumer color(int argb) { // No, this isn't a typo. One method takes RGBA, but this one takes ARGB.
        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        // This should be RGBA.
        // There is no reason it should be anything other than RGBA.
        // It should certainly never be ABGR.
        // But it is.
        // Why?
        this.putColorAttribute(ColorARGB.toABGR(argb));

        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        this.putTextureAttribute(u, v);

        return this;
    }

    @Override
    public VertexConsumer overlay(int uv) {
        this.putOverlayAttribute(uv);

        return this;
    }


    @Override
    public VertexConsumer light(int uv) {
        this.putLightAttribute(uv);

        return this;
    }
    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.putNormalAttribute(NormI8.pack(x, y, z));

        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this.light(packU16x2(u, v));
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this.overlay(packU16x2(u, v));
    }

    @Unique
    private static int packU16x2(int u, int v) {
        return (u & 0xFFFF) << 0 |
                (v & 0xFFFF) << 16;
    }

    /**
     * @author JellySquid
     * @reason Only used by BufferVertexConsumer's default implementations, which our patches remove
     */
    @Overwrite
    public VertexFormatElement getCurrentElement() {
        throw createBlockedUpcallException();
    }

    /**
     * @author JellySquid
     * @reason Only used by BufferVertexConsumer's default implementations, which our patches remove
     */
    @Overwrite
    public void nextElement() {
        throw createBlockedUpcallException();
    }

    /**
     * @author JellySquid
     * @reason Only used by BufferVertexConsumer's default implementations, which our patches remove
     */
    @Overwrite
    public void putByte(int index, byte value) {
        throw createBlockedUpcallException();
    }

    /**
     * @author JellySquid
     * @reason Only used by BufferVertexConsumer's default implementations, which our patches remove
     */
    @Overwrite
    public void putShort(int index, short value) {
        throw createBlockedUpcallException();
    }

    /**
     * @author JellySquid
     * @reason Only used by BufferVertexConsumer's default implementations, which our patches remove
     */
    @Overwrite
    public void putFloat(int index, float value) {
        throw createBlockedUpcallException();
    }

    /**
     * @author JellySquid
     * @reason The implementation no longer cares about the current element
     */
    @Overwrite
    @Override
    public void next() {
        if (this.colorFixed) {
            this.writeFixedColor();
        }

        if (!this.isVertexFinished()) {
            throw new IllegalStateException("Not filled all elements of the vertex");
        }

        this.vertexCount++;
        this.elementOffset += this.vertexStride;

        this.writtenAttributes = 0;

        this.grow(this.vertexStride);

        if (this.shouldDuplicateVertices()) {
            this.duplicateVertex();
        }
    }

    @Unique
    private boolean isVertexFinished() {
        return (this.writtenAttributes & this.requiredAttributes) == this.requiredAttributes;
    }

    @Unique
    private void writeFixedColor() {
        this.putColorAttribute(ColorABGR.pack(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha));
    }

    @Unique
    private boolean shouldDuplicateVertices() {
        return this.drawMode == VertexFormat.DrawMode.LINES || this.drawMode == VertexFormat.DrawMode.LINE_STRIP;
    }

    @Unique
    private void duplicateVertex() {
        MemoryIntrinsics.copyMemory(
                MemoryUtil.memAddress(this.buffer, this.elementOffset - this.vertexStride),
                MemoryUtil.memAddress(this.buffer, this.elementOffset),
                this.vertexStride);

        this.elementOffset += this.vertexStride;
        this.vertexCount++;

        this.grow(this.vertexStride);
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        var length = count * this.vertexStride;

        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        this.grow(length + this.vertexStride);

        // The buffer may change in the even, so we need to make sure that the
        // pointer is retrieved *after* the resize
        var dst = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        if (format == this.formatDescription) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertexCount += count;
        this.elementOffset += length;
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerRegistry.instance()
                .get(format, this.formatDescription)
                .serialize(src, dst, count);
    }

    @Unique
    private static RuntimeException createBlockedUpcallException() {
        return new UnsupportedOperationException("The internal methods provided by BufferVertexConsumer (as used to upcall into BufferBuilder) are unsupported");
    }
}
