package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.transform.VertexTransform;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OverlayVertexConsumer.class)
public class MixinOverlayVertexConsumer implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private Matrix3f inverseNormalMatrix;

    @Shadow
    @Final
    private Matrix4f inverseTextureMatrix;

    @Shadow
    @Final
    private float textureScale;

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        VertexTransform.transformOverlay(ptr, count, format,
                this.inverseNormalMatrix, this.inverseTextureMatrix, this.textureScale);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }
}
