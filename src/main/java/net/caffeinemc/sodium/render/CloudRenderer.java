package net.caffeinemc.sodium.render;

import net.caffeinemc.gfx.api.array.VertexArrayDescription;
import net.caffeinemc.gfx.api.array.VertexArrayResourceBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeBinding;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeFormat;
import net.caffeinemc.gfx.api.array.attribute.VertexFormat;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.DynamicBuffer;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.device.commands.RenderCommandList;
import net.caffeinemc.gfx.api.pipeline.PipelineState;
import net.caffeinemc.gfx.api.pipeline.RenderPipeline;
import net.caffeinemc.gfx.api.pipeline.RenderPipelineDescription;
import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.pipeline.state.WriteMask;
import net.caffeinemc.gfx.api.shader.*;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import net.caffeinemc.gfx.util.buffer.streaming.SequenceBuilder;
import net.caffeinemc.gfx.util.buffer.streaming.SequenceIndexBuffer;
import net.caffeinemc.gfx.util.misc.MathUtil;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.shader.ShaderLoader;
import net.caffeinemc.sodium.render.shader.ShaderParser;
import net.caffeinemc.sodium.util.color.ColorMixer;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.caffeinemc.sodium.util.packed.ColorARGB;
import net.minecraft.client.MinecraftClient;
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
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

public class CloudRenderer {
    private static final Identifier CLOUDS_TEXTURE_ID = new Identifier("textures/environment/clouds.png");

    private static final VertexFormat<CloudMeshAttribute> VERTEX_FORMAT = VertexFormat.builder(CloudMeshAttribute.class, 16)
            .addElement(CloudMeshAttribute.POSITION, 0, VertexAttributeFormat.FLOAT, 3, false, false)
            .addElement(CloudMeshAttribute.COLOR, 12, VertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .build();

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

    private final RenderDevice device;

    private final DynamicBuffer uniformBuffer;

    private final SequenceIndexBuffer indexBuffer;

    private Buffer vertexBuffer;
    private int vertexCount;

    private CloudEdges edges;

    private int prevCenterCellX, prevCenterCellY, cachedRenderDistance;

    private Program<CloudShaderInterface> shaderColor, shaderDepth;

    private RenderPipeline<CloudShaderInterface, CloudBufferTarget> pipelineColor, pipelineDepth;

    public CloudRenderer(RenderDevice device) {
        this.device = device;
        this.indexBuffer = new SequenceIndexBuffer(device, SequenceBuilder.QUADS_INT);
        this.uniformBuffer = device.createDynamicBuffer(192, Set.of());

        this.reloadTextures();
    }

    public void render(@Nullable ClientWorld world, MatrixStack matrices, Matrix4f projectionMatrix, float ticks, float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (world == null) {
            return;
        }

        float cloudHeight = world.getDimensionEffects().getCloudsHeight();

        double cloudTime = (ticks + tickDelta) * 0.03F;
        double cloudCenterX = (cameraX + cloudTime);
        double cloudCenterZ = (cameraZ) + 0.33D;

        int renderDistance = MinecraftClient.getInstance().options.getClampedViewDistance();
        int cloudDistance = Math.max(32, (renderDistance * 2) + 9);

        int centerCellX = (int) (Math.floor(cloudCenterX / 12));
        int centerCellZ = (int) (Math.floor(cloudCenterZ / 12));

        if (this.vertexBuffer == null || this.prevCenterCellX != centerCellX || this.prevCenterCellY != centerCellZ || this.cachedRenderDistance != renderDistance) {
            this.rebuildGeometry(cloudDistance, centerCellX, centerCellZ);

            this.prevCenterCellX = centerCellX;
            this.prevCenterCellY = centerCellZ;
            this.cachedRenderDistance = renderDistance;
        }

        Vec3d color = world.getCloudsColor(tickDelta);

        float fogEnd = cloudDistance * 8;
        float fogStart = fogEnd - 16;

        float translateX = (float) (cloudCenterX - (centerCellX * 12));
        float translateZ = (float) (cloudCenterZ - (centerCellZ * 12));

        Matrix4f modelViewMatrix = new Matrix4f(matrices.peek().getPositionMatrix());
        modelViewMatrix.translate(-translateX, cloudHeight - (float) cameraY + 0.33F, -translateZ);

        updateUniforms(this.device, this.uniformBuffer, projectionMatrix, modelViewMatrix, fogStart, fogEnd,
                new Vector4f((float) color.x, (float) color.y, (float) color.z, 0.8f));

        this.device.useRenderPipeline(this.pipelineDepth, this::renderClouds);
        this.device.useRenderPipeline(this.pipelineColor, this::renderClouds);
    }

    private void renderClouds(RenderCommandList<CloudBufferTarget> commandList, CloudShaderInterface programInterface, PipelineState pipelineState) {
        commandList.bindVertexBuffer(CloudBufferTarget.VERTICES, CloudRenderer.this.vertexBuffer, 0, 16);
        commandList.bindElementBuffer(CloudRenderer.this.indexBuffer.getBuffer());

        pipelineState.bindBufferBlock(programInterface.skyParametersBlock, CloudRenderer.this.uniformBuffer);

        commandList.drawElements(PrimitiveType.TRIANGLES, ElementFormat.UNSIGNED_INT, 0, (CloudRenderer.this.vertexCount / 4) * 6);
    }

    private void rebuildGeometry(int cloudDistance, int centerCellX, int centerCellZ) {
        var vertexData = MemoryUtil.memAlloc(4096 * 16);
        var vertexCount = 0;

        for (int offsetX = -cloudDistance; offsetX < cloudDistance; offsetX++) {
            for (int offsetZ = -cloudDistance; offsetZ < cloudDistance; offsetZ++) {
                int connectedEdges = this.edges.getEdges(centerCellX + offsetX, centerCellZ + offsetZ);

                if (connectedEdges == 0) {
                    continue;
                }

                int baseColor = this.edges.getColor(centerCellX + offsetX, centerCellZ + offsetZ);

                float x = offsetX * 12;
                float z = offsetZ * 12;

                if ((vertexCount + 24) * 16 > vertexData.capacity()) {
                    vertexData = MemoryUtil.memRealloc(vertexData, vertexData.capacity() * 2);
                }

                // -Y
                if ((connectedEdges & DIR_NEG_Y) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_Y);

                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 0.0f, z +  0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 0.0f, z +  0.0f, mixedColor);
                }

                // +Y
                if ((connectedEdges & DIR_POS_Y) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_Y);

                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 4.0f, z +  0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 4.0f, z +  0.0f, mixedColor);
                }

                // -X
                if ((connectedEdges & DIR_NEG_X) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_X);

                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 0.0f, 0.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 0.0f, 4.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 0.0f, 4.0f, z +  0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 0.0f, 0.0f, z +  0.0f, mixedColor);
                }

                // +X
                if ((connectedEdges & DIR_POS_X) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_X);

                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 0.0f, z +  0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 4.0f, z +  0.0f, mixedColor);
                }

                // -Z
                if ((connectedEdges & DIR_NEG_Z) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_NEG_Z);

                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 4.0f, z + 0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 0.0f, z + 0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 0.0f, z + 0.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 4.0f, z + 0.0f, mixedColor);
                }

                // +Z
                if ((connectedEdges & DIR_POS_Z) != 0) {
                    int mixedColor = ColorMixer.mulARGB(baseColor, CLOUD_COLOR_POS_Z);

                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 0.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x + 12.0f, 4.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 4.0f, z + 12.0f, mixedColor);
                    putVertex(addrOfVertex(vertexData, vertexCount++), x +  0.0f, 0.0f, z + 12.0f, mixedColor);
                }
            }
        }

        if (this.vertexBuffer != null) {
            this.device.deleteBuffer(this.vertexBuffer);
        }

        this.vertexBuffer = this.device.createBuffer(
                MemoryUtil.memSlice(vertexData, 0, vertexCount * 16),
                Set.of());
        this.vertexCount = vertexCount;

        this.indexBuffer.ensureCapacity(this.vertexCount);

        MemoryUtil.memFree(vertexData);
    }

    private static void updateUniforms(RenderDevice device, DynamicBuffer buffer,
                                       Matrix4f projMatrix, Matrix4f modelViewMatrix,
                                       float fogStart, float fogEnd,
                                       Vector4f colorModulator) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = stack.malloc(192);

            projMatrix.get(0, data);
            modelViewMatrix.get(64, data);

            colorModulator.get(128, data);

            data.putFloat(144, fogStart);
            data.putFloat(148, fogEnd);

            device.updateBuffer(buffer, 0, data);
        }
    }

    private static long addrOfVertex(ByteBuffer buffer, int index) {
        return MemoryUtil.memAddress(buffer, (index * 16));
    }

    private static void putVertex(long ptr, float x, float y, float z, int color) {
        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);
        MemoryUtil.memPutInt(ptr + 12, color);
    }

    public void reloadTextures() {
        this.destroy();

        this.edges = createCloudEdges();

        this.shaderColor = SodiumClientMod.DEVICE.createProgram(ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, ShaderParser.parseSodiumShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "cloud.vert")))
                .addShaderSource(ShaderType.FRAGMENT, ShaderParser.parseSodiumShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "cloud.frag")))
                .build(), CloudShaderInterface::new);

        this.shaderDepth = SodiumClientMod.DEVICE.createProgram(ShaderDescription.builder()
                .addShaderSource(ShaderType.VERTEX, ShaderParser.parseSodiumShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "cloud_zprepass.vert")))
                .addShaderSource(ShaderType.FRAGMENT, ShaderParser.parseSodiumShader(ShaderLoader.MINECRAFT_ASSETS, new Identifier("sodium", "cloud_zprepass.frag")))
                .build(), CloudShaderInterface::new);

        var vertexArrayDescription = new VertexArrayDescription<>(CloudBufferTarget.values(),
                List.of(
                        new VertexArrayResourceBinding<>(CloudBufferTarget.VERTICES, new VertexAttributeBinding[] {
                                new VertexAttributeBinding(0, VERTEX_FORMAT.getAttribute(CloudMeshAttribute.POSITION)),
                                new VertexAttributeBinding(1, VERTEX_FORMAT.getAttribute(CloudMeshAttribute.COLOR))
                        })
                ));

        this.pipelineColor = this.device.createRenderPipeline(RenderPipelineDescription.builder()
                .setBlendFunction(BlendFunc.separate(BlendFunc.SrcFactor.SRC_ALPHA, BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA,
                        BlendFunc.SrcFactor.ONE, BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA))
                .setDepthFunc(DepthFunc.EQUAL)
                .setWriteMask(new WriteMask(true, false))
                .build(), this.shaderColor, vertexArrayDescription);

        this.pipelineDepth = this.device.createRenderPipeline(RenderPipelineDescription.builder()
                .setDepthFunc(DepthFunc.LESS_THAN_OR_EQUAL)
                .setWriteMask(new WriteMask(false, true))
                .build(), this.shaderDepth, vertexArrayDescription);
    }

    public void destroy() {
        if (this.pipelineColor != null) {
            this.device.deleteRenderPipeline(this.pipelineColor);
        }

        if (this.pipelineDepth != null) {
            this.device.deleteRenderPipeline(this.pipelineDepth);
        }

        if (this.shaderColor != null) {
            this.device.deleteProgram(this.shaderColor);
        }

        if (this.shaderDepth != null) {
            this.device.deleteProgram(this.shaderDepth);
        }

        if (this.vertexBuffer != null) {
            this.device.deleteBuffer(this.vertexBuffer);
        }
    }

    private static CloudEdges createCloudEdges() {
        NativeImage nativeImage;

        ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();
        Resource resource = resourceManager.getResource(CLOUDS_TEXTURE_ID)
                .orElseThrow();

        try (InputStream inputStream = resource.getInputStream()) {
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

    private enum CloudBufferTarget {
        VERTICES
    }


    private enum CloudMeshAttribute {
        POSITION,
        COLOR
    }

    private static class CloudShaderInterface {
        public final BufferBlock skyParametersBlock;

        public CloudShaderInterface(ShaderBindingContext ctx) {
            this.skyParametersBlock = ctx.bindBufferBlock(BufferBlockType.UNIFORM, 0);
        }
    }
}
