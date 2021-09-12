package me.jellysquid.mods.sodium.model.quad;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public interface QuadColorizer<T> {
    int getColor(T state, @Nullable BlockRenderView world, @Nullable BlockPos pos, QuadView quad);

    static <T> QuadColorizer<T> getDefault() {
        return (state, world, pos, quad) -> -1;
    }

    static QuadColorizer<BlockState> wrap(BlockColorProvider provider) {
        return (state, world, pos, quad) -> provider.getColor(state, world, pos, quad.colorIndex());
    }
}
