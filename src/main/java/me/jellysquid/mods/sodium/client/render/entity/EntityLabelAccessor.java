package me.jellysquid.mods.sodium.client.render.entity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public interface EntityLabelAccessor {
    boolean bridge$hasLabel(Entity entity);

    void bridge$renderLabelIfPresent(Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);
}
