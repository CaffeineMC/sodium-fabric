package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_advance;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer implements BufferVertexConsumer {
    @Shadow
    private VertexFormat format;

    @Shadow
    private VertexFormatElement currentElement;

    @Shadow
    private int elementOffset;

    @Shadow
    private int currentElementId;

    /**
     * @author JellySquid
     * @reason Remove modulo operations and recursion
     */
    @Override
    @Overwrite
    public void nextElement() {
        ImmutableList<VertexFormatElement> elements = this.format.getElements();

        do {
            this.elementOffset += this.currentElement.getByteLength();

            // Wrap around the element pointer without using modulo
            if (++this.currentElementId >= elements.size()) {
                this.currentElementId -= elements.size();
            }

            this.currentElement = elements.get(this.currentElementId);
        } while (this.currentElement.getType() == VertexFormatElement.Type.PADDING);

        if (this.colorFixed && this.currentElement.getType() == VertexFormatElement.Type.COLOR) {
            BufferVertexConsumer.super.color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha);
        }
    }
}
