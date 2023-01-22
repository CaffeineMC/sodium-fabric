package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.ModelCuboid;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class MixinModelPart {
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

            try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
                long buffer = writer.buffer(stack, 4 * 6, ModelVertex.STRIDE, ModelVertex.FORMAT);
                long ptr = buffer;

                for (ModelCuboid.Quad quad : cuboid.quads) {
                    var normal = quad.getNormal(matrices.getNormalMatrix());

                    for (int i = 0; i < 4; i++) {
                        var pos = quad.positions[i];
                        var tex = quad.textures[i];

                        ModelVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, light, overlay, normal);

                        ptr += ModelVertex.STRIDE;
                    }
                }

                writer.push(buffer, 4 * 6, ModelVertex.STRIDE, ModelVertex.FORMAT);
            }
        }
    }
}
