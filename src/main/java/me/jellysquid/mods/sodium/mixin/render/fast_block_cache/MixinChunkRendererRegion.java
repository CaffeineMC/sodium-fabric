package me.jellysquid.mods.sodium.mixin.render.fast_block_cache;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

// TODO: don't construct FluidState array
@Mixin(ChunkRendererRegion.class)
public abstract class MixinChunkRendererRegion {
    @Shadow
    @Final
    protected BlockState[] blockStates;

    @Shadow
    protected abstract int getIndex(int x, int y, int z);

    @Shadow
    protected abstract int getIndex(BlockPos pos);

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z"))
    private boolean voidLoop(Iterator<?> iterator) {
        return false;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void test(World world, int chunkX, int chunkZ, WorldChunk[][] chunks, BlockPos startPos, BlockPos endPos, CallbackInfo ci) {
        final BlockState defaultState = Blocks.AIR.getDefaultState();

        final int minX = startPos.getX();
        final int minY = startPos.getY();
        final int minZ = startPos.getZ();

        final int maxX = endPos.getX();
        final int maxY = endPos.getY();
        final int maxZ = endPos.getZ();

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                WorldChunk worldChunk = chunks[(x >> 4) - chunkX][(z >> 4) - chunkZ];

                for (int y = minY; y <= maxY; y++) {
                    BlockState state;

                    if (y >= 0 && y < 256) {
                        ChunkSection section = worldChunk.getSectionArray()[y >> 4];

                        if (section != null) {
                            state = section.getBlockState(x & 15, y & 15, z & 15);
                        } else {
                            state = defaultState;
                        }
                    } else {
                        state = defaultState;
                    }

                    int i = this.getIndex(x, y, z);

                    this.blockStates[i] = state;
                }
            }
        }
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public FluidState getFluidState(BlockPos pos) {
        return this.blockStates[this.getIndex(pos)].getFluidState();
    }
}
