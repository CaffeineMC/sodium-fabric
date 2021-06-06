package me.jellysquid.mods.sodium.client.render.entity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public interface EntityLabelAccessor<T extends Entity> {
    boolean bridge$hasLabel(T entity);

    void bridge$renderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
}
