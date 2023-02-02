package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.transform.VertexTransform;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteTexturedVertexConsumer.class)
public class MixinSpriteTexturedVertexConsumer implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;
    private float minU, minV;
    private float maxU, maxV;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(VertexConsumer delegate, Sprite sprite, CallbackInfo ci) {
        this.minU = sprite.getMinU();
        this.minV = sprite.getMinV();

        this.maxU = sprite.getMaxU();
        this.maxV = sprite.getMaxV();
    }

    @Override
    public void push(MemoryStack stack, final long ptr, int count, VertexFormatDescription format) {
        VertexTransform.transformSprite(ptr, count, format,
                this.minU, this.minV, this.maxU, this.maxV);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }
}
