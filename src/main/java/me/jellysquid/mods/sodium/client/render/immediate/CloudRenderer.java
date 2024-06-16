package me.jellysquid.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;

public class CloudRenderer {
    private static final Identifier CLOUDS_TEXTURE_ID = Identifier.of("textures/environment/clouds.png");

    private static final int CLOUD_COLOR_NEG_Y = ColorABGR.pack(0.7F, 0.7F, 0.7F, 1.0f);
    private static final int CLOUD_COLOR_POS_Y = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);
    private static final int CLOUD_COLOR_NEG_X = ColorABGR.pack(0.9F, 0.9F, 0.9F, 1.0f);
    private static final int CLOUD_COLOR_POS_X = ColorABGR.pack(0.9F, 0.9F, 0.9F, 1.0f);
    private static final int CLOUD_COLOR_NEG_Z = ColorABGR.pack(0.8F, 0.8F, 0.8F, 1.0f);
    private static final int CLOUD_COLOR_POS_Z = ColorABGR.pack(0.8F, 0.8F, 0.8F, 1.0f);

    private static final int DIR_NEG_Y = 1 << 0;
    private static final int DIR_POS_Y = 1 << 1;
    private static final int DIR_NEG_X = 1 << 2;
    private static final int DIR_POS_X = 1 << 3;
    private static final int DIR_NEG_Z = 1 << 4;
    private static final int DIR_POS_Z = 1 << 5;

    private VertexBuffer vertexBuffer;
    private CloudEdges edges;
    private ShaderProgram shader;
    private final BackgroundRenderer.FogData fogData = new BackgroundRenderer.FogData(BackgroundRenderer.FogType.FOG_TERRAIN);

    private int prevCenterCellX, prevCenterCellY, cachedRenderDistance;
    private CloudRenderMode cloudRenderMode;

    public CloudRenderer(ResourceManager resourceManager) {
        this.reloadTextures(resourceManager);
    }

    public void render(@Nullable ClientWorld level, ClientPlayerEntity player, MatrixStack matrices, Matrix4f projectionMatrix, float ticks, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (level == null || edges.isBlank) {
            return;
        }

        float cloudHeight = level.getDimensionEffects().getCloudsHeight();

        // Vanilla uses NaN height as a way to disable cloud rendering
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        Vec3d color = level.getCloudsColor(tickDelta);

        double cloudTime = (ticks + tickDelta) * 0.03F;
        double cloudCenterX = (cameraX + cloudTime);
        double cloudCenterZ = (cameraZ) + 0.33D;

        int renderDistance = MinecraftClient.getInstance().options.getClampedViewDistance();
        int cloudDistance = Math.max(32, (renderDistance * 2) + 9);

        int centerCellX = (int) (Math.floor(cloudCenterX / 12));
        int centerCellZ = (int) (Math.floor(cloudCenterZ / 12));

        if (this.vertexBuffer == null || this.prevCenterCellX != centerCellX || this.prevCenterCellY != centerCellZ || this.cachedRenderDistance != renderDistance || cloudRenderMode != MinecraftClient.getInstance().options.getCloudRenderModeValue()) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            this.cloudRenderMode = MinecraftClient.getInstance().options.getCloudRenderModeValue();

            this.rebuildGeometry(bufferBuilder, cloudDistance, centerCellX, centerCellZ);

            if (this.vertexBuffer == null) {
                this.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }

            this.vertexBuffer.bind();
            this.vertexBuffer.upload(bufferBuilder.end());

            Tessellator.getInstance().clear();

            VertexBuffer.unbind();

            this.prevCenterCellX = centerCellX;
            this.prevCenterCellY = centerCellZ;
            this.cachedRenderDistance = renderDistance;
        }

        float previousEnd = RenderSystem.getShaderFogEnd();
        float previousStart = RenderSystem.getShaderFogStart();
        this.fogData.fogEnd = cloudDistance * 8;
        this.fogData.fogStart = (cloudDistance * 8) - 16;

        applyFogModifiers(level, this.fogData, player, cloudDistance * 8, tickDelta);


        RenderSystem.setShaderFogEnd(this.fogData.fogEnd);
        RenderSystem.setShaderFogStart(this.fogData.fogStart);

        float translateX = (float) (cloudCenterX - (centerCellX * 12));
        float translateZ = (float) (cloudCenterZ - (centerCellZ * 12));

        RenderSystem.enableDepthTest();

        this.vertexBuffer.bind();

        boolean insideClouds = cameraY < cloudHeight + 4.5f && cameraY > cloudHeight - 0.5f;
        boolean fastClouds = cloudRenderMode == CloudRenderMode.FAST;

        if (insideClouds || fastClouds) {
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableCull();
        }

        if (MinecraftClient.isFabulousGraphicsOrBetter()) {
            MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer().beginWrite(false);
        }

        RenderSystem.setShaderColor((float) color.x, (float) color.y, (float) color.z, 0.8f);

        matrices.push();

        Matrix4f modelViewMatrix = matrices.peek().getPositionMatrix();
        modelViewMatrix.translate(-translateX, cloudHeight - (float) cameraY + 0.33F, -translateZ);

        // PASS 1: Set up depth buffer
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        this.vertexBuffer.draw(modelViewMatrix, projectionMatrix, this.shader);

        // PASS 2: Render geometry
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL30C.GL_EQUAL);
        RenderSystem.colorMask(true, true, true, true);

        this.vertexBuffer.draw(modelViewMatrix, projectionMatrix, this.shader);

        matrices.pop();

        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.depthFunc(GL30C.GL_LEQUAL);

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (MinecraftClient.isFabulousGraphicsOrBetter()) {
            MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
        }

        RenderSystem.setShaderFogEnd(previousEnd);
        RenderSystem.setShaderFogStart(previousStart);
    }

    private void applyFogModifiers(ClientWorld world, BackgroundRenderer.FogData fogData, ClientPlayerEntity player, int cloudDistance, float tickDelta) {
        GameRenderer renderer = MinecraftClient.getInstance().gameRenderer;
        Camera camera = renderer.getCamera();
        CameraSubmersionType fogType = camera.getSubmersionType();

        if (fogType == CameraSubmersionType.LAVA) {
            if (player.isSpectator()) {
                fogData.fogStart = -8.0f;
                fogData.fogEnd = (cloudDistance) * 0.5f;
            } else if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                fogData.fogStart = 0.0f;
                fogData.fogEnd = 3.0f;
            } else {
                fogData.fogStart = 0.25f;
                fogData.fogEnd = 1.0f;
            }
        } else if (fogType == CameraSubmersionType.POWDER_SNOW) {
            if (player.isSpectator()) {
                fogData.fogStart = -8.0f;
                fogData.fogEnd = (cloudDistance) * 0.5f;
            } else {
                fogData.fogStart = 0.0f;
                fogData.fogEnd = 2.0f;
            }
        } else if (fogType == CameraSubmersionType.WATER) {
            fogData.fogStart = -8.0f;
            fogData.fogEnd = 96.0f;
            fogData.fogEnd *= Math.max(0.25f, player.getUnderwaterVisibility());

            if (fogData.fogEnd > cloudDistance) {
                fogData.fogEnd = cloudDistance;
                fogData.fogShape = FogShape.CYLINDER;
            }
        } else {
            Vec3d position = camera.getPos();

            if (world.getDimensionEffects().useThickFog(MathHelper.floor(position.x), MathHelper.floor(position.z)) ||
                    MinecraftClient.getInstance().inGameHud.getBossBarHud().shouldThickenFog()) {
                fogData.fogStart = (cloudDistance) * 0.05f;
                fogData.fogEnd = Math.min((cloudDistance), 192.0f) * 0.5f;
            }
        }

        BackgroundRenderer.StatusEffectFogModifier fogModifier = BackgroundRenderer.getFogModifier(player, tickDelta);

        if (fogModifier != null) {
            StatusEffectInstance statusEffectInstance = player.getStatusEffect(fogModifier.getStatusEffect());

            if (statusEffectInstance != null) {
                fogModifier.applyStartEndModifier(fogData, player, statusEffectInstance, (cloudDistance * 8), tickDelta);
            }
        }
    }

    private void rebuildGeometry(BufferBuilder bufferBuilder, int cloudDistance, int centerCellX, int centerCellZ) {
        var writer = VertexBufferWriter.of(bufferBuilder);
        var fastClouds = this.cloudRenderMode == CloudRenderMode.FAST;

        for (int offsetCellX = -cloudDistance; offsetCellX < cloudDistance; offsetCellX++) {
            for (int offsetCellZ = -cloudDistance; offsetCellZ < cloudDistance; offsetCellZ++) {
                int cellIndex = this.edges.getCellIndexWrapping(centerCellX + offsetCellX, centerCellZ + offsetCellZ);
                int cellFaces = this.edges.getCellFaces(cellIndex);

                if (cellFaces == 0) {
                    continue;
                }

                int cellColor = this.edges.getCellColor(cellIndex);

                float x = offsetCellX * 12;
                float z = offsetCellZ * 12;

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    final long buffer = stack.nmalloc((fastClouds ? 4 : (6 * 4)) * ColorVertex.STRIDE);

                    long ptr = buffer;
                    int count = 0;

                    // -Y
                    if ((cellFaces & DIR_NEG_Y) != 0) {
                        int mixedColor = ColorMixer.mul(cellColor, fastClouds ? CLOUD_COLOR_POS_Y : CLOUD_COLOR_NEG_Y);

                        ptr = writeVertex(ptr, x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);

                        count += 4;
                    }

                    // Only emit -Y geometry to emulate vanilla fast clouds
                    if (fastClouds) {
                        writer.push(stack, buffer, count, ColorVertex.FORMAT);
                        continue;
                    }

                    // +Y
                    if ((cellFaces & DIR_POS_Y) != 0) {
                        int mixedColor = ColorMixer.mul(cellColor, CLOUD_COLOR_POS_Y);

                        ptr = writeVertex(ptr, x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 4.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 4.0f, z +  0.0f, mixedColor);

                        count += 4;
                    }

                    // -X
                    if ((cellFaces & DIR_NEG_X) != 0) {
                        int mixedColor = ColorMixer.mul(cellColor, CLOUD_COLOR_NEG_X);

                        ptr = writeVertex(ptr, x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 4.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);

                        count += 4;
                    }

                    // +X
                    if ((cellFaces & DIR_POS_X) != 0) {
                        int mixedColor = ColorMixer.mul(cellColor, CLOUD_COLOR_POS_X);

                        ptr = writeVertex(ptr, x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 4.0f, z +  0.0f, mixedColor);

                        count += 4;
                    }

                    // -Z
                    if ((cellFaces & DIR_NEG_Z) != 0) {
                        int mixedColor = ColorMixer.mul(cellColor, CLOUD_COLOR_NEG_Z);

                        ptr = writeVertex(ptr, x + 12.0f, 4.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 4.0f, z +  0.0f, mixedColor);

                        count += 4;
                    }

                    // +Z
                    if ((cellFaces & DIR_POS_Z) != 0) {
                        int mixedColor = ColorMixer.mul(cellColor, CLOUD_COLOR_POS_Z);

                        ptr = writeVertex(ptr, x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                        ptr = writeVertex(ptr, x +  0.0f, 0.0f, z + 12.0f, mixedColor);

                        count += 4;
                    }

                    if (count > 0) {
                        writer.push(stack, buffer, count, ColorVertex.FORMAT);
                    }
                }
            }
        }
    }

    private static long writeVertex(long buffer, float x, float y, float z, int color) {
        ColorVertex.put(buffer, x, y, z, color);
        return buffer + ColorVertex.STRIDE;
    }

    public void reloadTextures(ResourceManager resourceManager) {
        this.destroy();

        this.edges = createCloudEdges();

        try {
            this.shader = new ShaderProgram(resourceManager, "clouds", VertexFormats.POSITION_COLOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        if (this.shader != null) {
            this.shader.close();
            this.shader = null;
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    private static CloudEdges createCloudEdges() {
        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID)
                .orElseThrow();

        try (InputStream inputStream = resource.getInputStream()){
            try (NativeImage nativeImage = NativeImage.read(inputStream)) {
                return new CloudEdges(nativeImage);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load texture data", ex);
        }
    }

    private static class CloudEdges {
        private final byte[] faces;
        private final int[] colors;
        private final int width, height;
        private boolean isBlank;

        public CloudEdges(NativeImage texture) {
            int width = texture.getWidth();
            int height = texture.getHeight();

            this.faces = new byte[width * height];
            this.colors = new int[width * height];

            this.width = width;
            this.height = height;

            this.loadTextureData(texture, width, height);
        }

        private void loadTextureData(NativeImage texture, int width, int height) {
            this.isBlank = true;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    int index = this.getCellIndex(x, z);
                    int color = texture.getColor(x, z);

                    this.colors[index] = color;

                    if (!isTransparentCell(color)) {
                        this.isBlank = false;
                        this.faces[index] = (byte) getVisibleFaces(texture, color, x, z);
                    }
                }
            }
        }

        private static int getVisibleFaces(NativeImage image, int color, int x, int z) {
            // Since the cloud texture is only 2D, nothing can hide the top or bottom faces
            int faces = DIR_NEG_Y | DIR_POS_Y;

            // Generate faces where the neighbor cell is a different color
            // Do not generate duplicate faces between two cells
            {
                // -X face
                int neighbor = getNeighborTexel(image, x - 1, z);

                if (color != neighbor) {
                    faces |= DIR_NEG_X;
                }
            }

            {
                // +X face
                int neighbor = getNeighborTexel(image, x + 1, z);

                if (isTransparentCell(neighbor) && color != neighbor) {
                    faces |= DIR_POS_X;
                }
            }

            {
                // -Z face
                int neighbor = getNeighborTexel(image, x, z - 1);

                if (color != neighbor) {
                    faces |= DIR_NEG_Z;
                }
            }

            {
                // +Z face
                int neighbor = getNeighborTexel(image, x, z + 1);

                if (isTransparentCell(neighbor) && color != neighbor) {
                    faces |= DIR_POS_Z;
                }
            }

            return faces;
        }

        private static int getNeighborTexel(NativeImage image, int x, int z) {
            x = wrapTexelCoord(x, 0, image.getWidth() - 1);
            z = wrapTexelCoord(z, 0, image.getHeight() - 1);

            return image.getColor(x, z);
        }

        private static int wrapTexelCoord(int coord, int min, int max) {
            if (coord < min) {
                coord = max;
            }

            if (coord > max) {
                coord = min;
            }

            return coord;
        }

        private static boolean isTransparentCell(int color) {
            return ColorARGB.unpackAlpha(color) <= 1;
        }

        public int getCellFaces(int index) {
            return this.faces[index];
        }

        public int getCellColor(int index) {
            return this.colors[index];
        }

        private int getCellIndexWrapping(int x, int z) {
            return this.getCellIndex(
                    Math.floorMod(x, this.width),
                    Math.floorMod(z, this.height)
            );
        }

        private int getCellIndex(int x, int z) {
            return (x * this.width) + z;
        }
    }
}
