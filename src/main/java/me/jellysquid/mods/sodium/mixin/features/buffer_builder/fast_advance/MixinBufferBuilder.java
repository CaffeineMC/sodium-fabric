package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_advance;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.minecraft.client.render.*;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Unique
    private VertexFormatElement[] formatElements;

    @Inject(method = "setFormat",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/BufferBuilder;format:Lnet/minecraft/client/render/VertexFormat;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.formatElements = format.getElements().toArray(new VertexFormatElement[0]);
    }

    /**
     * @author JellySquid
     * @reason Remove modulo operations, recursion and use array instead of list
     */
    @Override
    @Overwrite
    public void nextElement() {
        do {
            this.elementOffset += this.currentElement.getByteLength();

            // Wrap around the element pointer without using modulo
            if (++this.currentElementId >= this.formatElements.length) {
                this.currentElementId -= this.formatElements.length;
            }

            this.currentElement = this.formatElements[this.currentElementId];
        } while (this.currentElement.getType() == VertexFormatElement.Type.PADDING);

        if (this.colorFixed && this.currentElement.getType() == VertexFormatElement.Type.COLOR) {
            BufferVertexConsumer.super.color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha);
        }
    }
}
