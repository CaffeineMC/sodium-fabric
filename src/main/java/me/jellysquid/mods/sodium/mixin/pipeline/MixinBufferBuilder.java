package me.jellysquid.mods.sodium.mixin.pipeline;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadSink;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer implements ModelQuadSink {
    @Shadow
    private VertexFormat format;

    @Shadow
    private int currentElementId;

    @Shadow
    private int elementOffset;

    @Shadow
    private VertexFormatElement currentElement;

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    protected abstract void grow(int size);

    @Shadow
    public abstract void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

    @Shadow
    private int vertexCount;

    /**
     * @author JellySquid
     */
    @Overwrite
    public void nextElement() {
        ImmutableList<VertexFormatElement> elements = this.format.getElements();

        // avoid the modulo!
        if (++this.currentElementId >= elements.size()) {
            this.currentElementId -= elements.size();
        }

        this.elementOffset += this.currentElement.getSize();
        this.currentElement = elements.get(this.currentElementId);

        if (this.currentElement.getType() == VertexFormatElement.Type.PADDING) {
            this.nextElement();
        }

        if (this.colorFixed && this.currentElement.getType() == VertexFormatElement.Type.COLOR) {
            this.color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha);
        }
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        this.grow(32);

        quad.copyInto(this.buffer, this.elementOffset);

        this.vertexCount += 4;
        this.elementOffset += 32;
    }
}
