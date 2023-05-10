package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltChunkStorage.class)
public class MixinBuiltChunkStorage {
    @Shadow
    public ChunkBuilder.BuiltChunk[] chunks;

    @Shadow
    protected int sizeX;

    @Shadow
    protected int sizeY;

    @Shadow
    protected int sizeZ;

    /**
     * @author IMS
     * @reason Disable vanilla chunk management
     */
    @Overwrite
    public void createChunks(ChunkBuilder chunkBuilder) {
        this.chunks = new ChunkBuilder.BuiltChunk[0];
        this.sizeX = 0;
        this.sizeY = 0;
        this.sizeZ = 0;
    }

    /**
     * @author IMS
     * @reason Avoid a possible out of bounds exception
     */
    @Overwrite
    protected ChunkBuilder.BuiltChunk getRenderedChunk(BlockPos pos) {
        return null;
    }
}
