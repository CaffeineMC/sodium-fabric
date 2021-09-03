package me.jellysquid.mods.sodium.client.interop.vanilla.item;

import me.jellysquid.mods.sodium.client.render.batch.ItemRenderBatch;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface ItemRendererBatched {
    static ItemRendererBatched cast(ItemRenderer itemRenderer) {
        return (ItemRendererBatched) itemRenderer;
    }

    void renderItemModel(ItemRenderBatch batch, MatrixStack matrixStack, int x, int y, ItemStack stack, LivingEntity entity, int seed);

    void renderItemLabel(ItemRenderBatch batch, MatrixStack matrixStack, int x, int y, ItemStack stack, TextRenderer textRenderer, String string);

    void renderItemOverlays(ItemRenderBatch batch, MatrixStack matrices, int x, int y, ItemStack stack);
}
