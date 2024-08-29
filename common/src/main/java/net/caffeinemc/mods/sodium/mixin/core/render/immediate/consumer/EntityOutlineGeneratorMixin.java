package net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/renderer/OutlineBufferSource$EntityOutlineGenerator")
public abstract class EntityOutlineGeneratorMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private int color;
    @Unique
    private boolean canUseIntrinsics;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/vertex/VertexConsumer;I)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.canUseIntrinsics = VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.canUseIntrinsics;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
        transform(ptr, count, format,
                this.color);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the color element of each vertex to use the specified value.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param color  The packed color to use for transforming the vertices
     */
    @Unique
    private static void transform(long ptr, int count, VertexFormat format,
                                  int color) {
        long stride = format.getVertexSize();
        long offsetColor = format.getOffset(VertexFormatElement.COLOR);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            ColorAttribute.set(ptr + offsetColor, ColorARGB.toABGR(color));
            ptr += stride;
        }
    }

}