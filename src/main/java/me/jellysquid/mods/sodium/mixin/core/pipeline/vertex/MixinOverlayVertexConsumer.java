package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
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
    public void push(long ptr, int count, VertexFormatDescription format) {
        this.writeVerticesSlow(ptr, count, format);
    }

    private void writeVerticesSlow(long ptr, int count, VertexFormatDescription format) {
        var offsetPosition = format.getOffset(VertexFormats.POSITION_ELEMENT);
        var offsetNormal = format.getOffset(VertexFormats.NORMAL_ELEMENT);
        var offsetOverlay = format.getOffset(VertexFormats.OVERLAY_ELEMENT);
        var offsetLight = format.getOffset(VertexFormats.LIGHT_ELEMENT);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            float positionX = MemoryUtil.memGetFloat(ptr + offsetPosition + 0);
            float positionY = MemoryUtil.memGetFloat(ptr + offsetPosition + 4);
            float positionZ = MemoryUtil.memGetFloat(ptr + offsetPosition + 8);

            int overlay = MemoryUtil.memGetInt(ptr + offsetOverlay);
            int light = MemoryUtil.memGetInt(ptr + offsetLight);
            int normal = MemoryUtil.memGetInt(ptr + offsetNormal);

            float normalX = Norm3b.unpackX(normal);
            float normalY = Norm3b.unpackY(normal);
            float normalZ = Norm3b.unpackZ(normal);

            Vector3f normalCoord = this.inverseNormalMatrix.transform(new Vector3f(normalX, normalY, normalZ));
            Direction direction = Direction.getFacing(normalCoord.x(), normalCoord.y(), normalCoord.z());

            Vector4f textureCoord = this.inverseTextureMatrix.transform(new Vector4f(positionX, positionY, positionZ, 1.0F));
            textureCoord.rotateY(3.1415927F);
            textureCoord.rotateX(-1.5707964F);
            textureCoord.rotate(direction.getRotationQuaternion());

            float textureU = -textureCoord.x() * this.textureScale;
            float textureV = -textureCoord.y() * this.textureScale;

            this.delegate.vertex(positionX, positionY, positionZ)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .texture(textureU, textureV)
                    .overlay(overlay)
                    .light(light)
                    .normal(normalX, normalY, normalZ)
                    .next();

            ptr += format.stride;
        }
    }
}
