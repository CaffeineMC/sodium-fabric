package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;

public class CloudRenderer {
    private static final Identifier CLOUDS_TEXTURE_ID = new Identifier("textures/environment/clouds.png");

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

    private int prevCenterCellX, prevCenterCellY;

    public CloudRenderer() {
        this.reloadTextures();
    }

    public void render(@Nullable ClientWorld world, MatrixStack matrices, Matrix4f projectionMatrix, float ticks, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (world == null) {
            return;
        }

        Vec3d color = world.getCloudsColor(tickDelta);

        float cloudHeight = world.getDimensionEffects().getCloudsHeight();

        double cloudTime = (ticks + tickDelta) * 0.03F;
        double cloudCenterX = (cameraX + cloudTime);
        double cloudCenterZ = (cameraZ) + 0.33D;

        int renderDistance = MinecraftClient.getInstance().options.getClampedViewDistance();
        int cloudDistance = (renderDistance * 2) + 1;

        int centerCellX = (int) (Math.floor(cloudCenterX / 8.0));
        int centerCellZ = (int) (Math.floor(cloudCenterZ / 8.0));

        if (this.vertexBuffer == null || this.prevCenterCellX != centerCellX || this.prevCenterCellY != centerCellZ) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            this.rebuildGeometry(bufferBuilder, cloudDistance, centerCellX, centerCellZ);

            if (this.vertexBuffer == null) {
                this.vertexBuffer = new VertexBuffer();
            }

            this.vertexBuffer.bind();
            this.vertexBuffer.upload(bufferBuilder.end());

            VertexBuffer.unbind();

            this.prevCenterCellX = centerCellX;
            this.prevCenterCellY = centerCellZ;
        }

        float translateX = (float) (cloudCenterX - (centerCellX * 8.0));
        float translateZ = (float) (cloudCenterZ - (centerCellZ * 8.0));

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableDepthTest();

        this.vertexBuffer.bind();

        boolean insideClouds = cameraY < cloudHeight + 4.0f && cameraY > cloudHeight;

        if (insideClouds) {
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableCull();
        }

        RenderSystem.disableTexture();
        RenderSystem.setShaderColor((float) color.x, (float) color.y, (float) color.z, 0.8f);

        matrices.push();

        Matrix4f modelViewMatrix = matrices.peek().getPositionMatrix();
        modelViewMatrix.translate(-translateX, cloudHeight - (float) cameraY, -translateZ);

        // PASS 1: Set up depth buffer
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        this.vertexBuffer.draw(modelViewMatrix, projectionMatrix, RenderSystem.getShader());

        // PASS 2: Render geometry
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);

        this.vertexBuffer.draw(modelViewMatrix, projectionMatrix, RenderSystem.getShader());

        matrices.pop();

        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void rebuildGeometry(BufferBuilder bufferBuilder, int cloudDistance, int centerCellX, int centerCellZ) {
        var sink = VertexDrain.of(bufferBuilder)
                .createSink(VanillaVertexTypes.BASIC_SCREEN_QUADS);

        for (int offsetX = -cloudDistance; offsetX < cloudDistance; offsetX++) {
            for (int offsetZ = -cloudDistance; offsetZ < cloudDistance; offsetZ++) {
                int connectedEdges = this.edges.getEdges(centerCellX + offsetX, centerCellZ + offsetZ);

                if (connectedEdges == 0) {
                    continue;
                }

                int baseColor = this.edges.getColor(centerCellX + offsetX, centerCellZ + offsetZ);

                float x = offsetX * 8.0f;
                float z = offsetZ * 8.0f;

                // -Y
                if ((connectedEdges & DIR_NEG_Y) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_Y);
                    sink.ensureCapacity(4);
                    sink.writeQuad(x + 8.0f, 0.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 0.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 0.0f, z + 0.0f, mixedColor);
                }

                // +Y
                if ((connectedEdges & DIR_POS_Y) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_Y);
                    sink.ensureCapacity(4);
                    sink.writeQuad(x + 0.0f, 4.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 4.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 4.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 4.0f, z + 0.0f, mixedColor);
                }

                // -X
                if ((connectedEdges & DIR_NEG_X) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_X);
                    sink.ensureCapacity(4);
                    sink.writeQuad(x + 0.0f, 0.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 4.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 4.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                }

                // +X
                if ((connectedEdges & DIR_POS_X) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_X);
                    sink.ensureCapacity(4);
                    sink.writeQuad(x + 8.0f, 4.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 0.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 0.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 4.0f, z + 0.0f, mixedColor);
                }

                // -Z
                if ((connectedEdges & DIR_NEG_Z) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_Z);
                    sink.ensureCapacity(4);
                    sink.writeQuad(x + 8.0f, 4.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 0.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 4.0f, z + 0.0f, mixedColor);
                }

                // +Z
                if ((connectedEdges & DIR_POS_Z) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_Z);
                    sink.ensureCapacity(4);
                    sink.writeQuad(x + 8.0f, 0.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 8.0f, 4.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 4.0f, z + 8.0f, mixedColor);
                    sink.writeQuad(x + 0.0f, 0.0f, z + 8.0f, mixedColor);
                }
            }
        }

        sink.flush();
    }

    public void reloadTextures() {
        this.edges = createCloudEdges();

        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    public void destroy() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    private static CloudEdges createCloudEdges() {
        NativeImage nativeImage;

        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID)
                .orElseThrow();

        try (InputStream inputStream = resource.getInputStream()){
            nativeImage = NativeImage.read(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load texture data", ex);
        }

        return new CloudEdges(nativeImage);
    }

    private static class CloudEdges {
        private final byte[] edges;
        private final int[] colors;
        private final int width, height;

        public CloudEdges(NativeImage texture) {
            int width = texture.getWidth();
            int height = texture.getHeight();

            Validate.isTrue(MathUtil.isPowerOfTwo(width), "Texture width must be power-of-two");
            Validate.isTrue(MathUtil.isPowerOfTwo(height), "Texture height must be power-of-two");

            this.edges = new byte[width * height];
            this.colors = new int[width * height];

            this.width = width;
            this.height = height;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    int index = index(x, z, width, height);
                    int cell = texture.getColor(x, z);

                    this.colors[index] = cell;

                    int edges = 0;

                    if (isOpaqueCell(cell)) {
                        edges |= DIR_NEG_Y | DIR_POS_Y;

                        int negX = texture.getColor(wrap(x - 1, width), wrap(z, height));

                        if (cell != negX) {
                            edges |= DIR_NEG_X;
                        }

                        int posX = texture.getColor(wrap(x + 1, width), wrap(z, height));

                        if (!isOpaqueCell(posX) && cell != posX) {
                            edges |= DIR_POS_X;
                        }

                        int negZ = texture.getColor(wrap(x, width), wrap(z - 1, height));

                        if (cell != negZ) {
                            edges |= DIR_NEG_Z;
                        }

                        int posZ = texture.getColor(wrap(x, width), wrap(z + 1, height));

                        if (!isOpaqueCell(posZ) && cell != posZ) {
                            edges |= DIR_POS_Z;
                        }
                    }

                    this.edges[index] = (byte) edges;
                }
            }
        }

        private static boolean isOpaqueCell(int color) {
            return ColorARGB.unpackAlpha(color) > 1;
        }

        public int getEdges(int x, int z) {
            return this.edges[index(x, z, this.width, this.height)];
        }

        public int getColor(int x, int z) {
            return this.colors[index(x, z, this.width, this.height)];
        }

        private static int index(int posX, int posZ, int width, int height) {
            return (wrap(posX, width) * width) + wrap(posZ, height);
        }

        private static int wrap(int pos, int dim) {
            return (pos & (dim - 1));
        }
    }
}
