package me.jellysquid.mods.sodium.mixin.chunk_rendering;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
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

    @Shadow
    public abstract void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f);

    private SodiumWorldRenderer renderer;

    @Redirect(method = "reload", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;viewDistance:I", ordinal = 1))
    private int nullifyBuiltChunkStorage(GameOptions options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
        this.renderer = SodiumWorldRenderer.create();
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(ClientWorld world, CallbackInfo ci) {
        this.renderer.setWorld(world);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public int getCompletedChunkCount() {
        return this.renderer.getVisibleChunkCount();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public boolean isTerrainRenderComplete() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "scheduleTerrainUpdate", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
        this.renderer.drawChunkLayer(renderLayer, matrixStack, d, e, f);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderChunkDebugInfo(Camera camera) {
        this.renderer.renderChunkDebugInfo(camera);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.renderer.updateChunks(camera, frustum, hasForcedFrustum, frame, spectator);
    }

    @Inject(method = "reload", at = @At("RETURN"))
    private void reload(CallbackInfo ci) {
        this.renderer.reload();
    }

    /**
     * @reason Avoid updating the same chunk multiple times
     * @author JellySquid
     */
    @Overwrite
    public void scheduleBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    /**
     * @reason Avoid updating the same chunk multiple times
     * @author JellySquid
     */
    @Overwrite
    public void scheduleBlockRenders(int x, int y, int z) {
        this.renderer.scheduleRebuildForChunks(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
    }

    /**
     * @reason Avoid updating the same chunk multiple times
     * @author JellySquid
     */
    @Overwrite
    private void scheduleSectionRender(BlockPos pos, boolean important) {
        this.renderer.scheduleRebuildForBlockArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, important);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void scheduleChunkRender(int x, int y, int z, boolean important) {
        this.renderer.scheduleRebuildForChunk(x, y, z, important);
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/WorldRenderer;noCullingBlockEntities:Ljava/util/Set;", shift = At.Shift.BEFORE, ordinal = 0))
    private void renderTileEntities(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        this.renderer.renderTileEntities(matrices, this.bufferBuilders, this.blockBreakingProgressions, camera, tickDelta);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public String getChunksDebugString() {
        return this.renderer.getChunksDebugString();
    }
}
