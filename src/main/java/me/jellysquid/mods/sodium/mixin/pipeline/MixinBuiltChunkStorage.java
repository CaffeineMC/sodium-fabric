package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ExtendedBuiltChunkStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BuiltChunkStorage.class)
public abstract class MixinBuiltChunkStorage implements ExtendedBuiltChunkStorage {
    @Shadow
    protected abstract ChunkBuilder.BuiltChunk getRenderedChunk(BlockPos pos);

    @Shadow
    public abstract void updateCameraPosition(double x, double z);

    @Shadow
    public ChunkBuilder.BuiltChunk[] chunks;

    @Override
    public ChunkBuilder.BuiltChunk bridge$getRenderedChunk(BlockPos pos) {
        return this.getRenderedChunk(pos);
    }

    @Override
    public void bridge$updateCameraPosition(double x, double z) {
        this.updateCameraPosition(x, z);
    }

    @Override
    public ChunkBuilder.BuiltChunk[] getData() {
        return this.chunks;
    }
}
