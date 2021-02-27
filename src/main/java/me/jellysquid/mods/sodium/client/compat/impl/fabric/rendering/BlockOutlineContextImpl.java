package me.jellysquid.mods.sodium.client.compat.impl.fabric.rendering;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class BlockOutlineContextImpl implements WorldRenderContext.BlockOutlineContext {
    public VertexConsumer vertexConsumer;
    public Entity entity;
    public double cameraX, cameraY, cameraZ;
    public BlockPos blockPos;
    public BlockState blockState;

    @Override
    public VertexConsumer vertexConsumer() {
        return vertexConsumer;
    }

    @Override
    public Entity entity() {
        return entity;
    }

    @Override
    public double cameraX() {
        return cameraX;
    }

    @Override
    public double cameraY() {
        return cameraY;
    }

    @Override
    public double cameraZ() {
        return cameraZ;
    }

    @Override
    public BlockPos blockPos() {
        return blockPos;
    }

    @Override
    public BlockState blockState() {
        return blockState;
    }
}
