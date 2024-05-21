package net.caffeinemc.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class CloudRenderer {
    private static final ResourceLocation CLOUDS_TEXTURE_ID = new ResourceLocation("textures/environment/clouds.png");

    private CloudTextureData textureData;
    private ShaderInstance shaderProgram;

    private @Nullable CloudRenderer.CloudGeometry cachedGeometry;

    public CloudRenderer(ResourceProvider resourceProvider) {
        this.reloadTextures(resourceProvider);
    }

    public void render(Camera camera,
                       ClientLevel level,
                       Matrix4f projectionMatrix,
                       PoseStack poseStack,
                       float ticks,
                       float tickDelta)
    {
        float cloudHeight = level.effects().getCloudHeight();

        // Vanilla uses NaN height as a way to disable cloud rendering
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        Vec3 pos = camera.getPosition();

        double cloudTime = (ticks + tickDelta) * 0.03F;
        double cloudCenterX = (pos.x() + cloudTime);
        double cloudCenterZ = (pos.z()) + 0.33D;

        int cloudDistance = getCloudRenderDistance();

        int centerCellX = (int) (Math.floor(cloudCenterX / 12.0));
        int centerCellZ = (int) (Math.floor(cloudCenterZ / 12.0));

        var parameters = new CloudGeometryParameters(centerCellX, centerCellZ, cloudDistance, Minecraft.getInstance().options.getCloudsType());

        CloudGeometry geometry = this.cachedGeometry;

        if (geometry == null || !Objects.equals(geometry.params(), parameters)) {
            this.cachedGeometry = (geometry = rebuildGeometry(geometry, parameters, this.textureData));
        }

        final float translateX = (float) (cloudCenterX - (centerCellX * 12));
        final float translateZ = (float) (cloudCenterZ - (centerCellZ * 12));

        final var prevShaderFogShape = RenderSystem.getShaderFogShape();
        final var prevShaderFogEnd = RenderSystem.getShaderFogEnd();
        final var prevShaderFogStart = RenderSystem.getShaderFogStart();

        FogRenderer.setupFog(camera, FogRenderer.FogMode.FOG_TERRAIN, cloudDistance * 8, shouldUseWorldFog(level, pos), tickDelta);

        RenderSystem.enableDepthTest();

        boolean insideClouds = pos.y() < cloudHeight + 4.5f && pos.y() > cloudHeight - 0.5f;
        boolean fastClouds = geometry.params().renderMode() == CloudStatus.FAST;

        if (insideClouds || fastClouds) {
            RenderSystem.disableCull();
        } else {
            RenderSystem.enableCull();
        }

        Vec3 colorModulator = level.getCloudColor(tickDelta);
        RenderSystem.setShaderColor((float) colorModulator.x, (float) colorModulator.y, (float) colorModulator.z, 0.8f);

        poseStack.pushPose();

        var poseEntry = poseStack.last();

        Matrix4f modelViewMatrix = poseEntry.pose();
        modelViewMatrix.translate(-translateX, cloudHeight - (float) pos.y() + 0.33F, -translateZ);

        VertexBuffer vertexBuffer = geometry.vertexBuffer();
        vertexBuffer.bind();

        // Depth pre-pass
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);

        vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, this.shaderProgram);

        // Color pass
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL30C.GL_EQUAL);
        RenderSystem.colorMask(true, true, true, true);

        vertexBuffer.drawWithShader(modelViewMatrix, projectionMatrix, this.shaderProgram);

        poseStack.popPose();

        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.depthFunc(GL30C.GL_LEQUAL);

        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.setShaderFogShape(prevShaderFogShape);
        RenderSystem.setShaderFogEnd(prevShaderFogEnd);
        RenderSystem.setShaderFogStart(prevShaderFogStart);
    }


    private static @NotNull CloudGeometry rebuildGeometry(@Nullable CloudGeometry existingGeometry,
                                                          CloudGeometryParameters parameters,
                                                          CloudTextureData textureData)
    {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var writer = VertexBufferWriter.of(bufferBuilder);

        var originCellX = parameters.originX();
        var originCellZ = parameters.originZ();
        var radius = parameters.radius();
        var useFastGraphics = parameters.renderMode() == CloudStatus.FAST;

        addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, 0, 0, useFastGraphics);

        for (int layer = 1; layer <= radius; layer++) {
            for (int z = -layer; z < layer; z++) {
                int x = Math.abs(z) - layer;
                addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, x, z, useFastGraphics);
            }

            for (int z = layer; z > -layer; z--) {
                int x = layer - Math.abs(z);
                addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, x, z, useFastGraphics);
            }
        }

        for (int layer = radius + 1; layer <= 2 * radius; layer++) {
            int l = layer - radius;

            for (int z = -radius; z <= -l; z++) {
                int x = -z - layer;
                addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, x, z, useFastGraphics);
            }

            for (int z = l; z <= radius; z++) {
                int x = z - layer;
                addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, x, z, useFastGraphics);
            }

            for (int z = radius; z >= l; z--) {
                int x = layer - z;
                addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, x, z, useFastGraphics);
            }

            for (int z = -l; z >= -radius; z--) {
                int x = layer + z;
                addCellGeometryToBuffer(writer, textureData, originCellX, originCellZ, x, z, useFastGraphics);
            }
        }

        BufferBuilder.RenderedBuffer builtBuffer = bufferBuilder.end();

        VertexBuffer vertexBuffer;

        if (existingGeometry != null) {
            vertexBuffer = existingGeometry.vertexBuffer();
        } else {
            vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }

        uploadToVertexBuffer(vertexBuffer, builtBuffer);

        return new CloudGeometry(vertexBuffer, parameters);
    }

    private static void addCellGeometryToBuffer(VertexBufferWriter writer,
                                                CloudTextureData textureData,
                                                int originX, int originZ,
                                                int offsetX, int offsetZ,
                                                boolean useFastGraphics) {
        int cellX = originX + offsetX;
        int cellZ = originZ + offsetZ;

        int cellIndex = textureData.getCellIndexWrapping(cellX, cellZ);
        int cellFaces = textureData.getCellFaces(cellIndex);

        if (cellFaces == 0) {
            return;
        }

        int cellColor = textureData.getCellColor(cellIndex);

        float x = offsetX * 12;
        float z = offsetZ * 12;

        if (useFastGraphics) {
            emitCellGeometry2D(writer, cellFaces, cellColor, x, z);
        } else {
            emitCellGeometry3D(writer, cellFaces, cellColor, x, z);
        }
    }

    private static void emitCellGeometry2D(VertexBufferWriter writer, int faces, int color, float x, float z) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final long buffer = stack.nmalloc(4 * ColorVertex.STRIDE);

            long ptr = buffer;
            int count = 0;

            // -Y
            if (CloudFaceSet.contains(faces, CloudFace.NEG_Y)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.POS_Y.getColor());

                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);

                count += 4;
            }

            if (count > 0) {
                writer.push(stack, buffer, count, ColorVertex.FORMAT);
            }
        }
    }

    private static void emitCellGeometry3D(VertexBufferWriter writer, int faces, int color, float x, float z) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final long buffer = stack.nmalloc(6 * 4 * ColorVertex.STRIDE);

            long ptr = buffer;
            int count = 0;

            // -Y
            if (CloudFaceSet.contains(faces, CloudFace.NEG_Y)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.NEG_Y.getColor());

                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);

                count += 4;
            }

            // +Y
            if (CloudFaceSet.contains(faces, CloudFace.POS_Y)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.POS_Y.getColor());

                ptr = writeVertex(ptr, x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 4.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 4.0f, z +  0.0f, mixedColor);

                count += 4;
            }

            // -X
            if (CloudFaceSet.contains(faces, CloudFace.NEG_X)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.NEG_X.getColor());

                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 4.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);

                count += 4;
            }

            // +X
            if (CloudFaceSet.contains(faces, CloudFace.POS_X)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.POS_X.getColor());

                ptr = writeVertex(ptr, x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 4.0f, z +  0.0f, mixedColor);

                count += 4;
            }

            // -Z
            if (CloudFaceSet.contains(faces, CloudFace.NEG_Z)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.NEG_Z.getColor());

                ptr = writeVertex(ptr, x + 12.0f, 4.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x + 12.0f, 0.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 0.0f, z +  0.0f, mixedColor);
                ptr = writeVertex(ptr, x +  0.0f, 4.0f, z +  0.0f, mixedColor);

                count += 4;
            }

            // +Z
            if (CloudFaceSet.contains(faces, CloudFace.POS_Z)) {
                int mixedColor = ColorMixer.mul(color, CloudFace.POS_Z.getColor());

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

    private static long writeVertex(long buffer, float x, float y, float z, int color) {
        ColorVertex.put(buffer, x, y, z, color);
        return buffer + ColorVertex.STRIDE;
    }

    private static void uploadToVertexBuffer(VertexBuffer vertexBuffer, BufferBuilder.RenderedBuffer builtBuffer) {
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);

        VertexBuffer.unbind();
    }

    public void reloadTextures(ResourceProvider resourceProvider) {
        this.destroy();

        this.textureData = loadTextureData();

        try {
            this.shaderProgram = new ShaderInstance(resourceProvider, "clouds", DefaultVertexFormat.POSITION_COLOR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        if (this.shaderProgram != null) {
            this.shaderProgram.close();
            this.shaderProgram = null;
        }

        if (this.cachedGeometry != null) {
            var vertexBuffer = this.cachedGeometry.vertexBuffer();
            vertexBuffer.close();

            this.cachedGeometry = null;
        }
    }

    private static CloudTextureData loadTextureData() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID)
                .orElseThrow();

        try (InputStream inputStream = resource.open()){
            try (NativeImage nativeImage = NativeImage.read(inputStream)) {
                return new CloudTextureData(nativeImage);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load texture data", ex);
        }
    }

    private static boolean shouldUseWorldFog(ClientLevel level, Vec3 pos) {
        return level.effects().isFoggyAt(Mth.floor(pos.x()), Mth.floor(pos.z())) ||
                Minecraft.getInstance().gui.getBossOverlay().shouldCreateWorldFog();
    }

    private static int getCloudRenderDistance() {
        return Math.max(32, (Minecraft.getInstance().options.getEffectiveRenderDistance() * 2) + 9);
    }

    private enum CloudFace {
        NEG_Y(ColorABGR.pack(0.7F, 0.7F, 0.7F, 1.0f)),
        POS_Y(ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f)),
        NEG_X(ColorABGR.pack(0.9F, 0.9F, 0.9F, 1.0f)),
        POS_X(ColorABGR.pack(0.9F, 0.9F, 0.9F, 1.0f)),
        NEG_Z(ColorABGR.pack(0.8F, 0.8F, 0.8F, 1.0f)),
        POS_Z(ColorABGR.pack(0.8F, 0.8F, 0.8F, 1.0f));

        private final int color;

        CloudFace(int color) {
            this.color = color;
        }

        public int getColor() {
            return this.color;
        }
    }

    private static class CloudFaceSet {
        public static int empty() {
            return 0;
        }

        public static boolean contains(int set, CloudFace face) {
            return (set & (1 << face.ordinal())) != 0;
        }

        public static int add(int set, CloudFace face) {
            return set | (1 << face.ordinal());
        }
    }

    private static class CloudTextureData {
        private final byte[] faces;
        private final int[] colors;

        private final int width, height;

        public CloudTextureData(NativeImage texture) {
            int width = texture.getWidth();
            int height = texture.getHeight();

            this.faces = new byte[width * height];
            this.colors = new int[width * height];

            this.width = width;
            this.height = height;

            this.loadTextureData(texture, width, height);
        }

        private void loadTextureData(NativeImage texture, int width, int height) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < height; z++) {
                    int index = this.getCellIndex(x, z);
                    int color = texture.getPixelRGBA(x, z);

                    this.colors[index] = color;

                    if (!isTransparentCell(color)) {
                        this.faces[index] = (byte) getVisibleFaces(texture, color, x, z);
                    }
                }
            }
        }

        private static int getVisibleFaces(NativeImage image, int color, int x, int z) {
            // Since the cloud texture is only 2D, nothing can hide the top or bottom faces
            int faces = CloudFaceSet.empty();
            faces = CloudFaceSet.add(faces, CloudFace.NEG_Y);
            faces = CloudFaceSet.add(faces, CloudFace.POS_Y);

            // Generate faces where the neighbor cell is a different color
            // Do not generate duplicate faces between two cells
            {
                // -X face
                int neighbor = getNeighborTexel(image, x - 1, z);

                if (color != neighbor) {
                    faces = CloudFaceSet.add(faces, CloudFace.NEG_X);
                }
            }

            {
                // +X face
                int neighbor = getNeighborTexel(image, x + 1, z);

                if (isTransparentCell(neighbor) && color != neighbor) {
                    faces = CloudFaceSet.add(faces, CloudFace.POS_X);
                }
            }

            {
                // -Z face
                int neighbor = getNeighborTexel(image, x, z - 1);

                if (color != neighbor) {
                    faces = CloudFaceSet.add(faces, CloudFace.NEG_Z);
                }
            }

            {
                // +Z face
                int neighbor = getNeighborTexel(image, x, z + 1);

                if (isTransparentCell(neighbor) && color != neighbor) {
                    faces = CloudFaceSet.add(faces, CloudFace.POS_Z);
                }
            }

            return faces;
        }

        private static int getNeighborTexel(NativeImage image, int x, int z) {
            x = wrapTexelCoord(x, 0, image.getWidth() - 1);
            z = wrapTexelCoord(z, 0, image.getHeight() - 1);

            return image.getPixelRGBA(x, z);
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

    private record CloudGeometry(VertexBuffer vertexBuffer, CloudGeometryParameters params) {

    }

    private record CloudGeometryParameters(int originX, int originZ, int radius, CloudStatus renderMode) {

    }
}
