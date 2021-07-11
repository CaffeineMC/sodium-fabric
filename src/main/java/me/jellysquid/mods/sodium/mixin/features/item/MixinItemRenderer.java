package me.jellysquid.mods.sodium.mixin.features.item;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.ModelQuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionColorSink;
import me.jellysquid.mods.sodium.client.render.ItemRenderBatch;
import me.jellysquid.mods.sodium.client.render.ItemRendererExtended;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.client.world.biome.ItemColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer implements ItemRendererExtended {
    private final XoRoShiRoRandom random = new XoRoShiRoRandom();

    @Shadow
    @Final
    private ItemColors colorMap;

    @Shadow
    public float zOffset;

    @Shadow
    public abstract void renderItem(ItemStack stack, ModelTransformation.Mode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, BakedModel model);

    @Shadow
    public abstract BakedModel getHeldItemModel(ItemStack stack, @Nullable World world, @Nullable LivingEntity entity, int seed);

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    private void renderBakedItemModel(BakedModel model, ItemStack stack, int light, int overlay, MatrixStack matrices, VertexConsumer vertices) {
        XoRoShiRoRandom random = this.random;

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = model.getQuads(null, direction, random.setSeedAndReturn(42L));

            if (!quads.isEmpty()) {
                this.renderBakedItemQuads(matrices, vertices, quads, stack, light, overlay);
            }
        }

        List<BakedQuad> quads = model.getQuads(null, null, random.setSeedAndReturn(42L));

        if (!quads.isEmpty()) {
            this.renderBakedItemQuads(matrices, vertices, quads, stack, light, overlay);
        }
    }

    /**
     * @reason Use vertex building intrinsics
     * @author JellySquid
     */
    @Overwrite
    private void renderBakedItemQuads(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, ItemStack stack, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();

        ItemColorProvider colorProvider = null;

        ModelQuadVertexSink drain = VertexDrain.of(vertexConsumer)
                .createSink(VanillaVertexTypes.QUADS);
        drain.ensureCapacity(quads.size() * 4);

        for (BakedQuad bakedQuad : quads) {
            int color = 0xFFFFFFFF;

            if (!stack.isEmpty() && bakedQuad.hasColor()) {
                if (colorProvider == null) {
                    colorProvider = ((ItemColorsExtended) this.colorMap).getColorProvider(stack);
                }

                color = ColorARGB.toABGR((colorProvider.getColor(stack, bakedQuad.getColorIndex())), 255);
            }

            ModelQuadView quad = ((ModelQuadView) bakedQuad);

            for (int i = 0; i < 4; i++) {
                drain.writeQuad(entry, quad.getX(i), quad.getY(i), quad.getZ(i), color, quad.getTexU(i), quad.getTexV(i),
                        light, overlay, bakedQuad.getFace());
            }

            SpriteUtil.markSpriteActive(quad.getSprite());
        }

        drain.flush();
    }

    @Override
    public void renderItemModel(ItemRenderBatch batch, MatrixStack matrixStack, int x, int y, ItemStack stack, LivingEntity entity, int seed) {
        if (stack.isEmpty()) {
            return;
        }

        BakedModel model = this.getHeldItemModel(stack, null, entity, seed);
        this.zOffset = model.hasDepth() ? this.zOffset + 50.0F + (float) 0 : this.zOffset + 50.0F;

        try {
            this.renderGuiItemModel(stack, x, y, model, batch, matrixStack);
        } catch (Throwable e) {
            CrashReport report = CrashReport.create(e, "Rendering item");

            CrashReportSection section = report.addElement("Item being rendered");
            section.add("Item Type", () -> String.valueOf(stack.getItem()));
            section.add("Item Damage", () -> String.valueOf(stack.getDamage()));
            section.add("Item NBT", () -> String.valueOf(stack.getTag()));
            section.add("Item Foil", () -> String.valueOf(stack.hasGlint()));

            throw new CrashException(report);
        }

        this.zOffset = model.hasDepth() ? this.zOffset - 50.0F - (float) 0 : this.zOffset - 50.0F;
    }

    protected void renderGuiItemModel(ItemStack stack, int x, int y, BakedModel model, ItemRenderBatch batcher, MatrixStack matrixStack) {
        matrixStack.push();
        matrixStack.translate(x + 8.0D, y + 8.0D, 100.0F + this.zOffset);
        matrixStack.scale(16.0F, -16.0F, 16.0F);

        this.renderItem(stack, ModelTransformation.Mode.GUI, false, matrixStack,
                batcher.getItemRendererVertexConsumer(model.isSideLit()), 0xf000f0, OverlayTexture.DEFAULT_UV, model);

        matrixStack.pop();
    }

    @Override
    public void renderItemLabel(ItemRenderBatch batch, TextRenderer textRenderer, MatrixStack matrixStack, int x, int y, ItemStack stack, String countLabel) {
        if (stack.isEmpty()) {
            return;
        }

        if (stack.getCount() != 1 || countLabel != null) {
            String label = countLabel == null ? String.valueOf(stack.getCount()) : countLabel;

            matrixStack.push();
            matrixStack.translate(0.0D, 0.0D, this.zOffset + 200.0F);

            textRenderer.draw(label, (x + 19 - 2 - textRenderer.getWidth(label)), (y + 6 + 3), 0xffffff, true,
                    matrixStack.peek().getModel(), batch.getItemFontBuffer(), false, 0, 0xf000f0);

            matrixStack.pop();
        }
    }
    
    @Override
    public void renderItemOverlays(ItemRenderBatch batch, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }

        if (stack.isItemBarVisible()) {
            int step = stack.getItemBarStep();
            int color = stack.getItemBarColor();

            writeColoredRect(batch.getItemOverlayBuffer(), x + 2, y + 13,
                    13, 2, 0, 0, 0, 255);

            writeColoredRect(batch.getItemOverlayBuffer(), x + 2, y + 13, step,
                    1, color >> 16 & 255, color >> 8 & 255, color & 255, 255);
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float cooldown;

        if (player != null) {
            cooldown = player.getItemCooldownManager().getCooldownProgress(stack.getItem(), MinecraftClient.getInstance().getTickDelta());
        } else {
            cooldown = 0.0F;
        }

        if (cooldown > 0.0F) {
            writeColoredRect(batch.getItemOverlayBuffer(), x, y + MathHelper.floor(16.0F * (1.0F - cooldown)),
                    16, MathHelper.ceil(16.0F * cooldown), 255, 255, 255, 127);
        }
    }

    private static void writeColoredRect(BufferBuilder buffer, int x, int y, int width, int height, int red, int green, int blue, int alpha) {
        int color = ColorABGR.pack(red, green, blue, alpha);

        PositionColorSink sink = VertexDrain.of(buffer)
                .createSink(VanillaVertexTypes.POSITION_COLOR);
        sink.ensureCapacity(4);
        sink.writeQuad(x, y, 0.0f, color);
        sink.writeQuad(x, y + height, 0.0f, color);
        sink.writeQuad(x + width, y + height, 0.0f, color);
        sink.writeQuad(x + width, y, 0.0f, color);
        sink.flush();
    }
}
