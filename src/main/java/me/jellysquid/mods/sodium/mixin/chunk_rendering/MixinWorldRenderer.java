package me.jellysquid.mods.sodium.mixin.chunk_rendering;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;
    private ChunkRenderManager chunkManager;

    @Redirect(method = "reload", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;viewDistance:I", ordinal = 1))
    private int nullifyBuiltChunkStorage(GameOptions options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
        this.chunkManager = new ChunkRenderManager(client);
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(ClientWorld world, CallbackInfo ci) {
        this.chunkManager.setWorld(world);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public int getCompletedChunkCount() {
        return this.chunkManager.getCompletedChunkCount();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public boolean isTerrainRenderComplete() {
        return this.chunkManager.isTerrainRenderComplete();
    }

    @Inject(method = "scheduleTerrainUpdate", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.chunkManager.scheduleTerrainUpdate();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
        this.chunkManager.renderLayer(renderLayer, matrixStack, d, e, f);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderChunkDebugInfo(Camera camera) {
        this.chunkManager.renderChunkDebugInfo(camera);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.chunkManager.update(camera, frustum, hasForcedFrustum, frame, spectator);
    }

    @Inject(method = "reload", at = @At("RETURN"))
    private void reload(CallbackInfo ci) {
        this.chunkManager.reload();
    }

    /**
     * @reason Avoid updating the same chunk multiple times
     * @author JellySquid
     */
    @Overwrite
    public void scheduleBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minChunkX = minX >> 4;
        int minChunkY = minY >> 4;
        int minChunkZ = minZ >> 4;

        int maxChunkX = maxX >> 4;
        int maxChunkY = maxY >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    this.scheduleChunkRender(chunkX, chunkY, chunkZ, false);
                }
            }
        }
    }


    /**
     * @author JellySquid
     */
    @Overwrite
    private void scheduleChunkRender(int x, int y, int z, boolean important) {
        this.chunkManager.scheduleRebuildForBlock(x, y, z);
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;noCullingBlockEntities:Ljava/util/Set;", shift = At.Shift.BEFORE, ordinal = 0))
    private void renderTileEntities(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        this.chunkManager.renderTileEntities(matrices, this.bufferBuilders, this.blockBreakingProgressions, camera, tickDelta);
    }
}
