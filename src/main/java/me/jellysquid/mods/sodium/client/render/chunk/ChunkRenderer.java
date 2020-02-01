package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class ChunkRenderer {
    private final MinecraftClient client;

    private final WorldRenderer worldRenderer;

    private final ObjectList<ChunkBuilder.BuiltChunk> chunksToRebuild = new ObjectArrayList<>();

    private ClientWorld world;

    private int renderDistance;

    private ChunkBuilder chunkBuilder;
    private ExtendedBuiltChunkStorage chunks;

    private Vector4f[] capturedFrustumOrientation;
    private Vector3d capturedFrustumPosition;
    private Frustum capturedFrustum;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;

    private double lastCameraChunkUpdateX;
    private double lastCameraChunkUpdateY;
    private double lastCameraChunkUpdateZ;

    private int cameraChunkY;
    private int cameraChunkZ;
    private int cameraChunkX;

    private double lastCameraX;
    private double lastCameraY;
    private double lastCameraZ;

    private double lastCameraPitch;
    private double lastCameraYaw;

    private boolean isRenderGraphDirty;

    private ChunkGraph chunkGraph;
    private ChunkStatusTracker chunkStatusTracker;

    public ChunkRenderer(MinecraftClient client, WorldRenderer worldRenderer) {
        this.client = client;
        this.worldRenderer = worldRenderer;
    }

    public void setWorld(ClientWorld world) {
        this.world = world;

        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        this.cameraChunkX = Integer.MIN_VALUE;
        this.cameraChunkY = Integer.MIN_VALUE;
        this.cameraChunkZ = Integer.MIN_VALUE;

        this.isRenderGraphDirty = true;

        this.chunks = ((ExtendedWorldRenderer) this.worldRenderer).getBuiltChunkStorage();
        this.chunkBuilder = ((ExtendedWorldRenderer) this.worldRenderer).getChunkBuilder();

        this.renderDistance = this.client.options.viewDistance;

        if (world == null) {
            this.chunksToRebuild.clear();

            if (this.chunkGraph != null) {
                this.chunkGraph.reset();
                this.chunkGraph = null;
            }
        } else {
            this.chunkStatusTracker = new ChunkStatusTracker(this.renderDistance);
            this.chunkGraph = new ChunkGraph(this.world, this.chunks, this.chunkStatusTracker, this.renderDistance);

            ((ExtendedClientChunkManager) world.getChunkManager()).setListener(this.chunkStatusTracker);
        }
    }

    public void onFrustumUpdated(Vector4f[] capturedFrustumOrientation, Vector3d capturedFrustumPosition, Frustum capturedFrustum) {
        this.capturedFrustumOrientation = capturedFrustumOrientation;
        this.capturedFrustum = capturedFrustum;
        this.capturedFrustumPosition = capturedFrustumPosition;
    }

    public int getCompletedChunkCount() {
        int i = 0;

        for (ChunkGraphNode info : this.chunkGraph.getVisibleChunks()) {
            if (!info.chunk.getData().isEmpty()) {
                i++;
            }
        }

        return i;
    }

    public void scheduleTerrainUpdate() {
        this.isRenderGraphDirty = true;
    }

    public void updateChunks(long limitTime) {
        this.isRenderGraphDirty |= this.chunkBuilder.upload();

        long startTime = Util.getMeasuringTimeNano();

        int uploaded = 0;

        if (!this.chunksToRebuild.isEmpty()) {
            Iterator<ChunkBuilder.BuiltChunk> iterator = this.chunksToRebuild.iterator();

            while (iterator.hasNext()) {
                ChunkBuilder.BuiltChunk chunk = iterator.next();

                if (chunk.needsImportantRebuild()) {
                    this.chunkBuilder.rebuild(chunk);
                } else {
                    chunk.scheduleRebuild(this.chunkBuilder);
                }

                chunk.cancelRebuild();

                iterator.remove();

                ++uploaded;

                long currentTime = Util.getMeasuringTimeNano();
                long passedTime = currentTime - startTime;
                long remainingTime = limitTime - currentTime;
                long timePerChunk = passedTime / (long) uploaded;

                if (remainingTime < timePerChunk) {
                    break;
                }
            }
        }
    }

    public boolean isTerrainRenderComplete() {
        return this.chunksToRebuild.isEmpty() && this.chunkBuilder.isEmpty();
    }

    public void update(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        Vec3d cameraPos = camera.getPos();

        if (this.client.options.viewDistance != this.renderDistance) {
            this.reload();
        }

        this.world.getProfiler().push("camera");

        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        double deltaX = player.getX() - this.lastCameraChunkUpdateX;
        double deltaY = player.getY() - this.lastCameraChunkUpdateY;
        double deltaZ = player.getZ() - this.lastCameraChunkUpdateZ;

        if (this.cameraChunkX != player.chunkX || this.cameraChunkY != player.chunkY || this.cameraChunkZ != player.chunkZ || deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 16.0D) {
            this.lastCameraChunkUpdateX = player.getX();
            this.lastCameraChunkUpdateY = player.getY();
            this.lastCameraChunkUpdateZ = player.getZ();

            this.cameraChunkX = player.chunkX;
            this.cameraChunkY = player.chunkY;
            this.cameraChunkZ = player.chunkZ;

            this.chunks.bridge$updateCameraPosition(player.getX(), player.getZ());
        }

        this.chunkBuilder.setCameraPosition(cameraPos);

        this.world.getProfiler().swap("cull");
        this.client.getProfiler().swap("culling");

        float pitch = camera.getPitch();
        float yaw = camera.getYaw();

        this.isRenderGraphDirty = this.isRenderGraphDirty || !this.chunksToRebuild.isEmpty() ||
                cameraPos.x != this.lastCameraX || cameraPos.y != this.lastCameraY || cameraPos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw;

        this.lastCameraX = cameraPos.x;
        this.lastCameraY = cameraPos.y;
        this.lastCameraZ = cameraPos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;

        this.client.getProfiler().swap("update");

        BlockPos blockPos = camera.getBlockPos();

        if (!hasForcedFrustum && this.isRenderGraphDirty) {
            this.isRenderGraphDirty = false;

            this.client.getProfiler().push("iteration");

            this.chunkGraph.calculateVisible(camera, cameraPos, blockPos, frame, frustum, spectator);

            this.client.getProfiler().pop();
        }

        Entity.setRenderDistanceMultiplier(MathHelper.clamp((double) client.options.viewDistance / 8.0D, 1.0D, 2.5D));

        this.client.getProfiler().swap("rebuildNear");

        this.performRebuilds(blockPos);

        this.client.getProfiler().pop();
    }

    private void performRebuilds(BlockPos blockPos) {
        List<ChunkBuilder.BuiltChunk> chunksToRebuildNow = new ArrayList<>();

        List<ChunkBuilder.BuiltChunk> chunksToRebuild = this.chunksToRebuild;
        this.chunksToRebuild.clear();

        for (ChunkGraphNode info : this.chunkGraph.getVisibleChunks()) {
            ChunkBuilder.BuiltChunk chunk = info.chunk;

            if (!chunk.needsRebuild()) {
                continue;
            }

            this.isRenderGraphDirty = true;

            BlockPos center = chunk.getOrigin().add(8, 8, 8);

            boolean nearby = center.getSquaredDistance(blockPos) < 768.0D;

            if (!chunk.needsImportantRebuild() && !nearby) {
                chunksToRebuild.add(chunk);
            } else {
                chunksToRebuildNow.add(chunk);
            }
        }

        for (ChunkBuilder.BuiltChunk chunk : chunksToRebuildNow) {
            chunk.rebuild();
            chunk.cancelRebuild();
        }
    }

    public void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
        Profiler profiler = this.client.getProfiler();

        renderLayer.startDrawing();

        if (renderLayer == RenderLayer.getTranslucent()) {
            profiler.push("translucent_sort");

            double g = d - this.lastTranslucentSortX;
            double h = e - this.lastTranslucentSortY;
            double i = f - this.lastTranslucentSortZ;

            if (g * g + h * h + i * i > 1.0D) {
                this.lastTranslucentSortX = d;
                this.lastTranslucentSortY = e;
                this.lastTranslucentSortZ = f;

                int j = 0;

                for (ChunkGraphNode chunkInfo : this.chunkGraph.getVisibleChunks()) {
                    if (j < 15 && chunkInfo.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
                        ++j;
                    }
                }
            }

            profiler.pop();
        }

        profiler.push("filterempty");
        profiler.swap(() -> "render_" + renderLayer);

        boolean notTranslucent = renderLayer != RenderLayer.getTranslucent();

        ObjectListIterator<ChunkGraphNode> it = this.chunkGraph.getVisibleChunks().listIterator(notTranslucent ? 0 : this.chunkGraph.getVisibleChunkCount());

        while (true) {
            if (notTranslucent) {
                if (!it.hasNext()) {
                    break;
                }
            } else if (!it.hasPrevious()) {
                break;
            }

            ChunkGraphNode info = notTranslucent ? it.next() : it.previous();
            ChunkBuilder.BuiltChunk chunk = info.chunk;

            if (!chunk.getData().isEmpty(renderLayer)) {
                BlockPos origin = chunk.getOrigin();

                matrixStack.push();
                matrixStack.translate((double) origin.getX() - d, (double) origin.getY() - e, (double) origin.getZ() - f);

                this.renderChunk(matrixStack, chunk, renderLayer);

                matrixStack.pop();
            }
        }

        VertexBufferWithArray.unbind();
        RenderSystem.clearCurrentColor();

        profiler.pop();

        renderLayer.endDrawing();
    }

    private void renderChunk(MatrixStack matrixStack, ChunkBuilder.BuiltChunk chunk, RenderLayer renderLayer) {
        ExtendedBuiltChunk echunk = ((ExtendedBuiltChunk) chunk);

        if (echunk.usesVAORendering()) {
            VertexBufferWithArray data = ((ExtendedBuiltChunk) chunk).getBufferWithArray(renderLayer);
            data.bind();
            data.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);
        } else {
            VertexBuffer vertexBuffer = chunk.getBuffer(renderLayer);
            vertexBuffer.bind();

            VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.startDrawing(0L);

            vertexBuffer.draw(matrixStack.peek().getModel(), GL11.GL_QUADS);
        }
    }

    public void renderChunkDebugInfo(Camera camera) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        if (this.client.debugChunkInfo || this.client.debugChunkOcculsion) {
            double x = camera.getPos().getX();
            double y = camera.getPos().getY();
            double z = camera.getPos().getZ();

            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableTexture();

            for (ObjectListIterator<ChunkGraphNode> var10 = this.chunkGraph.getVisibleChunks().iterator(); var10.hasNext(); RenderSystem.popMatrix()) {
                ChunkGraphNode chunkInfo = var10.next();
                ChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;
                RenderSystem.pushMatrix();
                BlockPos blockPos = builtChunk.getOrigin();
                RenderSystem.translated((double) blockPos.getX() - x, (double) blockPos.getY() - y, (double) blockPos.getZ() - z);
                int m;
                int k;
                int l;
                Direction direction2;
                if (this.client.debugChunkInfo) {
                    bufferBuilder.begin(1, VertexFormats.POSITION_COLOR);
                    RenderSystem.lineWidth(10.0F);
                    m = chunkInfo.propagationLevel == 0 ? 0 : MathHelper.hsvToRgb((float) chunkInfo.propagationLevel / 50.0F, 0.9F, 0.9F);
                    int j = m >> 16 & 255;
                    k = m >> 8 & 255;
                    l = m & 255;
                    direction2 = chunkInfo.direction;
                    if (direction2 != null) {
                        bufferBuilder.vertex(8.0D, 8.0D, 8.0D).color(j, k, l, 255).next();
                        bufferBuilder.vertex(8 - 16 * direction2.getOffsetX(), 8 - 16 * direction2.getOffsetY(), 8 - 16 * direction2.getOffsetZ()).color(j, k, l, 255).next();
                    }

                    tessellator.draw();
                    RenderSystem.lineWidth(1.0F);
                }

                if (this.client.debugChunkOcculsion && !builtChunk.getData().isEmpty()) {
                    bufferBuilder.begin(1, VertexFormats.POSITION_COLOR);
                    RenderSystem.lineWidth(10.0F);
                    m = 0;
                    Direction[] var24 = Direction.values();
                    k = var24.length;

                    for (l = 0; l < k; ++l) {
                        direction2 = var24[l];

                        for (Direction direction3 : Direction.values()) {
                            boolean bl = builtChunk.getData().isVisibleThrough(direction2, direction3);
                            if (!bl) {
                                ++m;
                                bufferBuilder.vertex(8 + 8 * direction2.getOffsetX(), 8 + 8 * direction2.getOffsetY(), 8 + 8 * direction2.getOffsetZ()).color(1, 0, 0, 1).next();
                                bufferBuilder.vertex(8 + 8 * direction3.getOffsetX(), 8 + 8 * direction3.getOffsetY(), 8 + 8 * direction3.getOffsetZ()).color(1, 0, 0, 1).next();
                            }
                        }
                    }

                    tessellator.draw();
                    RenderSystem.lineWidth(1.0F);

                    if (m > 0) {
                        bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
                        bufferBuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        bufferBuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
                        tessellator.draw();
                    }
                }
            }

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
        }

        if (this.capturedFrustum != null) {
            RenderSystem.disableCull();
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(10.0F);
            RenderSystem.pushMatrix();
            RenderSystem.translatef((float) (this.capturedFrustumPosition.x - camera.getPos().x), (float) (this.capturedFrustumPosition.y - camera.getPos().y), (float) (this.capturedFrustumPosition.z - camera.getPos().z));
            RenderSystem.depthMask(true);
            bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
            this.method_22985(bufferBuilder, 0, 1, 2, 3, 0, 1, 1);
            this.method_22985(bufferBuilder, 4, 5, 6, 7, 1, 0, 0);
            this.method_22985(bufferBuilder, 0, 1, 5, 4, 1, 1, 0);
            this.method_22985(bufferBuilder, 2, 3, 7, 6, 0, 0, 1);
            this.method_22985(bufferBuilder, 0, 4, 7, 3, 0, 1, 0);
            this.method_22985(bufferBuilder, 1, 5, 6, 2, 1, 0, 1);
            tessellator.draw();
            RenderSystem.depthMask(false);
            bufferBuilder.begin(1, VertexFormats.POSITION);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.method_22984(bufferBuilder, 0);
            this.method_22984(bufferBuilder, 1);
            this.method_22984(bufferBuilder, 1);
            this.method_22984(bufferBuilder, 2);
            this.method_22984(bufferBuilder, 2);
            this.method_22984(bufferBuilder, 3);
            this.method_22984(bufferBuilder, 3);
            this.method_22984(bufferBuilder, 0);
            this.method_22984(bufferBuilder, 4);
            this.method_22984(bufferBuilder, 5);
            this.method_22984(bufferBuilder, 5);
            this.method_22984(bufferBuilder, 6);
            this.method_22984(bufferBuilder, 6);
            this.method_22984(bufferBuilder, 7);
            this.method_22984(bufferBuilder, 7);
            this.method_22984(bufferBuilder, 4);
            this.method_22984(bufferBuilder, 0);
            this.method_22984(bufferBuilder, 4);
            this.method_22984(bufferBuilder, 1);
            this.method_22984(bufferBuilder, 5);
            this.method_22984(bufferBuilder, 2);
            this.method_22984(bufferBuilder, 6);
            this.method_22984(bufferBuilder, 3);
            this.method_22984(bufferBuilder, 7);
            tessellator.draw();

            RenderSystem.popMatrix();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
            RenderSystem.lineWidth(1.0F);
        }
    }


    private void method_22984(VertexConsumer vertexConsumer, int i) {
        Vector4f[] f = this.capturedFrustumOrientation;

        vertexConsumer.vertex(f[i].getX(), f[i].getY(), f[i].getZ());
        vertexConsumer.next();
    }

    private void method_22985(VertexConsumer vertexConsumer, int i, int j, int k, int l, int m, int n, int o) {
        Vector4f[] f = this.capturedFrustumOrientation;

        vertexConsumer.vertex(f[i].getX(), f[i].getY(), f[i].getZ()).color((float) m, (float) n, (float) o, 0.25F).next();
        vertexConsumer.vertex(f[j].getX(), f[j].getY(), f[j].getZ()).color((float) m, (float) n, (float) o, 0.25F).next();
        vertexConsumer.vertex(f[k].getX(), f[k].getY(), f[k].getZ()).color((float) m, (float) n, (float) o, 0.25F).next();
        vertexConsumer.vertex(f[l].getX(), f[l].getY(), f[l].getZ()).color((float) m, (float) n, (float) o, 0.25F).next();
    }

    public void clearRenderers() {
        this.chunksToRebuild.clear();
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.isRenderGraphDirty = true;
        this.renderDistance = this.client.options.viewDistance;

        this.chunks = ((ExtendedWorldRenderer) this.worldRenderer).getBuiltChunkStorage();
        this.chunkBuilder = ((ExtendedWorldRenderer) this.worldRenderer).getChunkBuilder();

        this.chunkGraph = new ChunkGraph(this.world, this.chunks, this.chunkStatusTracker, this.renderDistance);
    }
}
