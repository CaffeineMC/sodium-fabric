package me.jellysquid.mods.sodium.mixin.features.item;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.world.biome.ItemColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    private final Xoroshiro128PlusPlusRandom random = new Xoroshiro128PlusPlusRandom(42L);

    @Shadow
    @Final
    private ItemColors colors;

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    private void renderBakedItemModel(BakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrices, VertexConsumer vertices) {
        Xoroshiro128PlusPlusRandom random = this.random;

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);

            if (!quads.isEmpty()) {
                this.renderBakedItemQuads(matrices, vertices, quads, stack, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);

        if (!quads.isEmpty()) {
            this.renderBakedItemQuads(matrices, vertices, quads, stack, light, overlay);
        }
    }

    /**
     * @reason Use vertex building intrinsics
     * @author JellySquid
     */
    @Overwrite
    private void renderBakedItemQuads(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, ItemStack itemStack, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();

        ItemColorProvider colorProvider = null;

        var writer = VertexBufferWriter.of(vertexConsumer);

        try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
            long buffer = stack.nmalloc(ModelVertex.STRIDE * 4);

            for (BakedQuad bakedQuad : quads) {
                int color = 0xFFFFFFFF;

                if (!itemStack.isEmpty() && bakedQuad.hasColor()) {
                    if (colorProvider == null) {
                        colorProvider = ((ItemColorsExtended) this.colors).getColorProvider(itemStack);
                    }

                    color = ColorARGB.toABGR((colorProvider.getColor(itemStack, bakedQuad.getColorIndex())), 255);
                }

                ModelQuadView quad = ((ModelQuadView) bakedQuad);
                long ptr = buffer;

                for (int i = 0; i < 4; i++) {
                    ModelVertex.write(ptr, entry, quad.getX(i), quad.getY(i), quad.getZ(i), color, quad.getTexU(i), quad.getTexV(i),
                            light, overlay, ModelQuadUtil.getFacingNormal(bakedQuad.getFace()));

                    ptr += ModelVertex.STRIDE;
                }

                writer.push(buffer, 4, ModelVertex.FORMAT);

                SpriteUtil.markSpriteActive(quad.getSprite());
            }
        }
    }
}
