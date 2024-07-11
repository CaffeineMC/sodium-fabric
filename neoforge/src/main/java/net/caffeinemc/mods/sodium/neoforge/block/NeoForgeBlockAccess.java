package net.caffeinemc.mods.sodium.neoforge.block;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class NeoForgeBlockAccess implements PlatformBlockAccess {
    @Override
    public int getLightEmission(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return state.getLightEmission(level, pos);
    }

    @Override
    public boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, BlockPos otherPos, Direction facing) {
        return (otherState.hidesNeighborFace(level, otherPos, selfState, DirectionUtil.getOpposite(facing))) && selfState.supportsExternalFaceHiding();
    }

    @Override
    public boolean shouldShowFluidOverlay(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return block.shouldDisplayFluidOverlay(level, pos, fluidState);
    }

    @Override
    public @Nullable Object getBlockEntityData(BlockEntity blockEntity) {
        return null;
    }

    @Override
    public boolean platformHasBlockData() {
        return false;
    }

    @Override
    public float getAccurateBlockShade(ModelQuadView quad, BlockAndTintGetter level, boolean shade) {
        return level.getShade(NormI8.unpackX(quad.getFaceNormal()), NormI8.unpackY(quad.getFaceNormal()), NormI8.unpackZ(quad.getFaceNormal()), shade);
    }

    @Override
    public TriState usesAmbientOcclusion(BakedModel model, BlockState state, Object data, RenderType renderType, BlockAndTintGetter level, BlockPos pos) {
        return model.useAmbientOcclusion(state, renderType) ? TriState.DEFAULT : TriState.FALSE;
    }
}
