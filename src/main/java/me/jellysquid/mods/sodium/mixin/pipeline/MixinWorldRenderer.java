package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ExtendedBuiltChunkStorage;
import me.jellysquid.mods.sodium.client.render.chunk.ExtendedWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements ExtendedWorldRenderer {
    @Shadow
    private BuiltChunkStorage chunks;


    @Shadow
    private ChunkBuilder chunkBuilder;

    @Shadow
    @Final
    private Vector4f[] capturedFrustrumOrientation;

    @Shadow
    @Final
    private Vector3d capturedFrustumPosition;

    @Shadow
    private Frustum capturedFrustum;

    private ChunkRenderer chunkRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
        this.chunkRenderer = new ChunkRenderer(client, (WorldRenderer) (Object) this);
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(ClientWorld world, CallbackInfo ci) {
        this.chunkRenderer.setWorld(world);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public int getCompletedChunkCount() {
        return this.chunkRenderer.getCompletedChunkCount();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void updateChunks(long limitTime) {
        this.chunkRenderer.updateChunks(limitTime);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public boolean isTerrainRenderComplete() {
        return this.chunkRenderer.isTerrainRenderComplete();
    }

    @Inject(method = "scheduleTerrainUpdate", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.chunkRenderer.scheduleTerrainUpdate();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
        this.chunkRenderer.renderLayer(renderLayer, matrixStack, d, e, f);
    }


    @Inject(method = "captureFrustum", at = @At("RETURN"))
    private void onFrustumUpdated(Matrix4f modelMatrix, Matrix4f matrix4f, double x, double y, double z, Frustum frustum, CallbackInfo ci) {
        this.chunkRenderer.onFrustumUpdated(this.capturedFrustrumOrientation, this.capturedFrustumPosition, this.capturedFrustum);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderChunkDebugInfo(Camera camera) {
        this.chunkRenderer.renderChunkDebugInfo(camera);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.chunkRenderer.update(camera, frustum, hasForcedFrustum, frame, spectator);
    }

    @Override
    public ChunkBuilder getChunkBuilder() {
        return this.chunkBuilder;
    }

    @Override
    public ExtendedBuiltChunkStorage getBuiltChunkStorage() {
        return (ExtendedBuiltChunkStorage) this.chunks;
    }

    @Inject(method = "clearChunkRenderers", at = @At("RETURN"))
    private void onChunkRenderersCleared(CallbackInfo ci) {
        this.chunkRenderer.clearRenderers();
    }

    @Inject(method = "reload", at = @At("RETURN"))
    private void reload(CallbackInfo ci) {
        this.chunkRenderer.reload();
    }

}
