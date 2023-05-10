package me.jellysquid.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ColorVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

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
    private ShaderProgram clouds;
    private final BackgroundRenderer.FogData fogData = new BackgroundRenderer.FogData(BackgroundRenderer.FogType.FOG_TERRAIN);

    private int prevCenterCellX, prevCenterCellY, cachedRenderDistance;

    public CloudRenderer(ResourceFactory factory) {
        this.reloadTextures(factory);
    }

    public void render(@Nullable ClientWorld world, ClientPlayerEntity player, MatrixStack matrices, Matrix4f projectionMatrix, float ticks, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (world == null) {
            return;
        }

        Vec3d color = world.getCloudsColor(tickDelta);

        float cloudHeight = world.getDimensionEffects().getCloudsHeight();

        double cloudTime = (ticks + tickDelta) * 0.03F;
        double cloudCenterX = (cameraX + cloudTime);
        double cloudCenterZ = (cameraZ) + 0.33D;

        int renderDistance = MinecraftClient.getInstance().options.getClampedViewDistance();
        int cloudDistance = Math.max(32, (renderDistance * 2) + 9);

        int centerCellX = (int) (Math.floor(cloudCenterX / 12));
        int centerCellZ = (int) (Math.floor(cloudCenterZ / 12));

        if (this.vertexBuffer == null || this.prevCenterCellX != centerCellX || this.prevCenterCellY != centerCellZ || this.cachedRenderDistance != renderDistance) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            this.rebuildGeometry(bufferBuilder, cloudDistance, centerCellX, centerCellZ);

            if (this.vertexBuffer == null) {
                this.vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            }

            this.vertexBuffer.bind();
            this.vertexBuffer.upload(bufferBuilder.end());

            VertexBuffer.unbind();

            this.prevCenterCellX = centerCellX;
            this.prevCenterCellY = centerCellZ;
            this.cachedRenderDistance = renderDistance;
        }

        float previousEnd = RenderSystem.getShaderFogEnd();
        float previousStart = RenderSystem.getShaderFogStart();
        this.fogData.fogEnd = cloudDistance * 8;
        this.fogData.fogStart = (cloudDistance * 8) - 16;

        applyFogModifiers(world, this.fogData, player, cloudDistance * 8, tickDelta);


        RenderSystem.setShaderFogEnd(this.fogData.fogEnd);
        RenderSystem.setShaderFogStart(this.fogData.fogStart);

        float translateX = (float) (cloudCenterX - (centerCellX * 12));
        float translateZ = (float) (cloudCenterZ - (centerCellZ * 12));

        RenderSystem.enableDepthTest();

        this.vertexBuffer.bind();

        boolean insideClouds = cameraY < cloudHeight + 4.5f && cameraY > cloudHeight - 0.5f;

        if (insideClouds) {
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableCull();
        }

        RenderSystem.setShaderColor((float) color.x, (float) color.y, (float) color.z, 0.8f);

        matrices.push();

        Matrix4f modelViewMatrix = matrices.peek().getPositionMatrix();
        modelViewMatrix.translate(-translateX, cloudHeight - (float) cameraY + 0.33F, -translateZ);

        // PASS 1: Set up depth buffer
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        this.vertexBuffer.draw(modelViewMatrix, projectionMatrix, this.clouds);

        // PASS 2: Render geometry
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL30C.GL_EQUAL);
        RenderSystem.colorMask(true, true, true, true);

        this.vertexBuffer.draw(modelViewMatrix, projectionMatrix, this.clouds);

        matrices.pop();

        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.depthFunc(GL30C.GL_LEQUAL);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.enableCull();

        RenderSystem.setShaderFogEnd(previousEnd);
        RenderSystem.setShaderFogStart(previousStart);
    }

    private void applyFogModifiers(ClientWorld world, BackgroundRenderer.FogData fogData, ClientPlayerEntity player, int cloudDistance, float tickDelta) {
        if (MinecraftClient.getInstance().gameRenderer == null || MinecraftClient.getInstance().gameRenderer.getCamera() == null) {
            return;
        }

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
        if (cameraSubmersionType == CameraSubmersionType.LAVA) {
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
        } else if (cameraSubmersionType == CameraSubmersionType.POWDER_SNOW) {
            if (player.isSpectator()) {
                fogData.fogStart = -8.0f;
                fogData.fogEnd = (cloudDistance) * 0.5f;
            } else {
                fogData.fogStart = 0.0f;
                fogData.fogEnd = 2.0f;
            }
        } else if (cameraSubmersionType == CameraSubmersionType.WATER) {
            fogData.fogStart = -8.0f;
            fogData.fogEnd = 96.0f;
            fogData.fogEnd *= Math.max(0.25f, player.getUnderwaterVisibility());
            if (fogData.fogEnd > (cloudDistance)) {
                fogData.fogEnd = cloudDistance;
                fogData.fogShape = FogShape.CYLINDER;
            }
        } else if (world.getDimensionEffects().useThickFog(MathHelper.floor(camera.getPos().x), MathHelper.floor(camera.getPos().z)) || MinecraftClient.getInstance().inGameHud.getBossBarHud().shouldThickenFog()) {
            fogData.fogStart = (cloudDistance) * 0.05f;
            fogData.fogEnd = Math.min((cloudDistance), 192.0f) * 0.5f;
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

        for (int offsetX = -cloudDistance; offsetX < cloudDistance; offsetX++) {
            for (int offsetZ = -cloudDistance; offsetZ < cloudDistance; offsetZ++) {
                int connectedEdges = this.edges.getEdges(centerCellX + offsetX, centerCellZ + offsetZ);

                if (connectedEdges == 0) {
                    continue;
                }

                int baseColor = this.edges.getColor(centerCellX + offsetX, centerCellZ + offsetZ);

                float x = offsetX * 12;
                float z = offsetZ * 12;

                try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
                    final long buffer = stack.nmalloc(6 * 4 * ColorVertex.STRIDE);

                    long ptr = buffer;
                    int count = 0;

                    // -Y
                    if ((connectedEdges & DIR_NEG_Y) != 0) {
                        int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_Y);

                        ptr = writeVertex(ptr, x + 12, 0.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 0.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // +Y
                    if ((connectedEdges & DIR_POS_Y) != 0) {
                        int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_Y);

                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 4.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 4.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // -X
                    if ((connectedEdges & DIR_NEG_X) != 0) {
                        int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_X);

                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // +X
                    if ((connectedEdges & DIR_POS_X) != 0) {
                        int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_X);

                        ptr = writeVertex(ptr, x + 12, 4.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 0.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 4.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // -Z
                    if ((connectedEdges & DIR_NEG_Z) != 0) {
                        int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_Z);

                        ptr = writeVertex(ptr, x + 12, 4.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 0.0f, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 0.0f, mixedColor);

                        count += 4;
                    }

                    // +Z
                    if ((connectedEdges & DIR_POS_Z) != 0) {
                        int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_Z);

                        ptr = writeVertex(ptr, x + 12, 0.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 12, 4.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 4.0f, z + 12, mixedColor);
                        ptr = writeVertex(ptr, x + 0.0f, 0.0f, z + 12, mixedColor);

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
        ColorVertex.write(buffer, x, y, z, color);
        return buffer + ColorVertex.STRIDE;
    }

    public void reloadTextures(ResourceFactory factory) {
        this.edges = createCloudEdges();

        if (this.clouds != null) {
            this.clouds.close();
        }

        try {
            this.clouds = new ShaderProgram(factory, "clouds", VertexFormats.POSITION_COLOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }

    public void destroy() {
        this.clouds.close();

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
