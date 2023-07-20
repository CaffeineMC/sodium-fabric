package me.jellysquid.mods.sodium.mixin.features.item;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
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
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    private final Random random = new LocalRandom(42L);

    @Shadow
    @Final
    private ItemColors colors;

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    private void renderBakedItemModel(BakedModel model, ItemStack itemStack, int light, int overlay, MatrixStack matricStack, VertexConsumer vertexConsumer) {
        var writer = VertexBufferWriter.of(vertexConsumer);

        Random random = this.random;
        MatrixStack.Entry matrices = matricStack.peek();

        ItemColorProvider colorProvider = null;

        if (!itemStack.isEmpty()) {
            colorProvider = ((ItemColorsExtended) this.colors).getColorProvider(itemStack);
        }

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);

            if (!quads.isEmpty()) {
                this.sodium$renderBakedItemQuads(matrices, writer, quads, itemStack, colorProvider, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);

        if (!quads.isEmpty()) {
            this.sodium$renderBakedItemQuads(matrices, writer, quads, itemStack, colorProvider, light, overlay);
        }
    }

    /**
     * @reason Use vertex building intrinsics
     * @author JellySquid
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void sodium$renderBakedItemQuads(MatrixStack.Entry matrices, VertexBufferWriter writer, List<BakedQuad> quads, ItemStack itemStack, ItemColorProvider colorProvider, int light, int overlay) {

        for (int j = 0; j < quads.size(); j++) {
            BakedQuad bakedQuad = quads.get(j);

            int color = 0xFFFFFFFF;

            if (colorProvider != null && bakedQuad.hasColor()) {
                color = ColorARGB.toABGR((colorProvider.getColor(itemStack, bakedQuad.getColorIndex())), 255);
            }

            ModelQuadView quad = ((ModelQuadView) bakedQuad);

            ModelVertex.writeQuadVertices(writer, matrices, quad, light, overlay, color);

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
