package me.jellysquid.mods.sodium.client.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface ItemRendererExtended {
    static ItemRendererExtended cast(ItemRenderer itemRenderer) {
        return (ItemRendererExtended) itemRenderer;
    }

    void renderItemModel(ItemRenderBatch batch, MatrixStack matrixStack, int x, int y, ItemStack stack, LivingEntity entity, int seed);

    void renderItemLabel(ItemRenderBatch batch, TextRenderer textRenderer, MatrixStack matrixStack, int x, int y, ItemStack stack, String string);

    void renderItemOverlays(ItemRenderBatch batch, ItemStack stack, int x, int y);
}
