package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.MatrixHelper;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class MixinModelPart {
    @Shadow public float pivotX;
    @Shadow public float pivotY;
    @Shadow public float pivotZ;

    @Shadow public float yaw;
    @Shadow public float pitch;
    @Shadow public float roll;

    @Shadow public float xScale;
    @Shadow public float yScale;
    @Shadow public float zScale;

    private ModelCuboid[] sodium$cuboids;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(List<ModelPart.Cuboid> cuboids, Map<String, ModelPart> children, CallbackInfo ci) {
        var copies = new ModelCuboid[cuboids.size()];

        for (int i = 0; i < cuboids.size(); i++) {
            var accessor = (ModelCuboidAccessor) cuboids.get(i);
            copies[i] = accessor.copy();
        }

        this.sodium$cuboids = copies;
    }

    /**
     * @author JellySquid
     * @reason Use optimized vertex writer, avoid allocations, use quick matrix transformations
     */
    @Overwrite
    private void renderCuboids(MatrixStack.Entry matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        var writer = VertexBufferWriter.of(vertexConsumer);
        int color = ColorABGR.pack(red, green, blue, alpha);

        for (ModelCuboid cuboid : this.sodium$cuboids) {
            cuboid.updateVertices(matrices.getPositionMatrix());

            try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
                long buffer = stack.nmalloc(4 * 6 * ModelVertex.STRIDE);
                long ptr = buffer;

                for (ModelCuboid.Quad quad : cuboid.quads) {
                    if (quad == null) continue;

                    var normal = quad.getNormal(matrices.getNormalMatrix());

                    for (int i = 0; i < 4; i++) {
                        var pos = quad.positions[i];
                        var tex = quad.textures[i];

                        ModelVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, light, overlay, normal);

                        ptr += ModelVertex.STRIDE;
                    }
                }

                writer.push(stack, buffer, 4 * 6, ModelVertex.FORMAT);
            }
        }
    }

    /**
     * @author JellySquid
     * @reason Apply transform more quickly
     */
    @Overwrite
    public void rotate(MatrixStack matrices) {
        matrices.translate(this.pivotX * (1.0F / 16.0F), this.pivotY * (1.0F / 16.0F), this.pivotZ * (1.0F / 16.0F));

        if (this.pitch != 0.0F || this.yaw != 0.0F || this.roll != 0.0F) {
            MatrixHelper.rotateZYX(matrices.peek(), this.roll, this.yaw, this.pitch);
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            matrices.scale(this.xScale, this.yScale, this.zScale);
        }
    }
}
