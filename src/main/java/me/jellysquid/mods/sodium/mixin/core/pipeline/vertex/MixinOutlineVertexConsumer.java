package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.transform.VertexTransform;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/client/render/OutlineVertexConsumerProvider$OutlineVertexConsumer")
public abstract class MixinOutlineVertexConsumer extends FixedColorVertexConsumer implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        VertexTransform.transformColor(ptr, count, format,
                ColorABGR.pack(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha));

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }
}