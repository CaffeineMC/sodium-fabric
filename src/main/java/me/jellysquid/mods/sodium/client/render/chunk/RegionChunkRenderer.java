package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.format.StructBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType.CubeBufferTarget;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType.ModelBufferTarget;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionStorage;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public abstract class RegionChunkRenderer<T extends ChunkShaderInterface, E extends Enum<E> & ChunkMeshType.StorageBufferTarget> extends ShaderChunkRenderer<T> {
    protected final ChunkMeshType<E> meshType;

    protected final SequenceIndexBuffer sequenceIndexBuffer;
    protected GlTessellation tessellation;

    protected int maxPrimitiveCount = 0;

    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    protected final StructBufferBuilder instanceUniformBufferBuilder = new StructBufferBuilder(RenderRegion.REGION_SIZE, 32);
    protected final GlMutableBuffer instanceUniformBuffer;

    protected final MultiDrawBatch batch = MultiDrawBatch.create(RenderRegion.REGION_SIZE * 7);

    protected RegionChunkRenderer(RenderDevice device, ChunkMeshType<E> meshType, String shaderName) {
        super(device, shaderName);

        this.meshType = meshType;

        try (CommandList commandList = device.createCommandList()) {
            this.instanceUniformBuffer = commandList.createMutableBuffer();
        }

        this.sequenceIndexBuffer = new SequenceIndexBuffer(device, 16384);
    }

    protected GlTessellation prepareTessellation(CommandList commandList) {
        if (this.sequenceIndexBuffer.ensureCapacity(commandList, this.maxPrimitiveCount) || this.tessellation == null) {
            this.tessellation = commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                    TessellationBinding.forElementBuffer(this.sequenceIndexBuffer.getBufferObject())
            });
        }

        return this.tessellation;
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList,
                       ChunkRenderList list,
                       BlockRenderLayer renderLayer,
                       ChunkCameraContext camera) {
        super.begin(renderLayer);

        T shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setModelViewMatrix(matrices.modelView());

        this.prepareShader(shader);

        for (Map.Entry<RenderRegion, List<RenderSection>> entry : sortedRegions(list, renderLayer.translucent())) {
            var region = entry.getKey();
            var sections = entry.getValue();

            var storage = region.getMeshStorage(this.meshType);

            if (storage == null) {
                continue;
            }

            if (this.buildDrawBatches(storage, sections, renderLayer, camera)) {
                this.renderRegion(commandList, shader, storage);
            }
        }
        
        super.end();
    }

    protected abstract void prepareShader(T shader);

    protected abstract void renderRegion(CommandList commandList, T shader, RenderRegionStorage<E> storage);

    protected boolean buildDrawBatches(RenderRegionStorage<E> storage, List<RenderSection> sections, BlockRenderLayer layer, ChunkCameraContext camera) {
        this.batch.reset();
        this.instanceUniformBufferBuilder.reset();

        this.maxPrimitiveCount = 0;

        int instanceCount = 0;

        for (RenderSection section : sortedChunks(sections, layer.translucent())) {
            ChunkGraphicsState<E> state = storage.getGraphicsState(layer, section);

            if (state == null) {
                continue;
            }

            boolean added = this.addDrawCall(state, ModelQuadFacing.UNASSIGNED, instanceCount);

            if (this.isBlockFaceCullingEnabled) {
                ChunkRenderBounds bounds = section.getBounds();

                if (camera.posY > bounds.y1) {
                    added |= this.addDrawCall(state, ModelQuadFacing.UP, instanceCount);
                }

                if (camera.posY < bounds.y2) {
                    added |= this.addDrawCall(state, ModelQuadFacing.DOWN, instanceCount);
                }

                if (camera.posX > bounds.x1) {
                    added |= this.addDrawCall(state, ModelQuadFacing.EAST, instanceCount);
                }

                if (camera.posX < bounds.x2) {
                    added |= this.addDrawCall(state, ModelQuadFacing.WEST, instanceCount);
                }

                if (camera.posZ > bounds.z1) {
                    added |= this.addDrawCall(state, ModelQuadFacing.SOUTH, instanceCount);
                }

                if (camera.posZ < bounds.z2) {
                    added |= this.addDrawCall(state, ModelQuadFacing.NORTH, instanceCount);
                }
            } else {
                for (ModelQuadFacing facing : ModelQuadFacing.DIRECTIONS) {
                    added |= this.addDrawCall(state, facing, instanceCount);
                }
            }

            if (added) {
                this.addDrawInstanceState(storage, state, instanceCount, section, camera);
                instanceCount++;
            }
        }

        return instanceCount > 0;
    }

    protected abstract void addDrawInstanceState(RenderRegionStorage<E> storage, ChunkGraphicsState<E> state, int baseInstance, RenderSection section, ChunkCameraContext camera);

    protected boolean addDrawCall(ChunkGraphicsState<?> state, ModelQuadFacing facing, int baseInstance) {
        var range = state.getRange(facing);

        if (range == null) {
            return false;
        }

        this.batch.add(0, range.length() * 6, (baseInstance << 24) | (range.offset() * 4));
        this.maxPrimitiveCount = Math.max(this.maxPrimitiveCount, range.length());

        return true;
    }

    @Override
    public void delete() {
        super.delete();
        // TODO: clean up resources
    }

    private static Iterable<Map.Entry<RenderRegion, List<RenderSection>>> sortedRegions(ChunkRenderList list, boolean translucent) {
        return list.sorted(translucent);
    }

    private static Iterable<RenderSection> sortedChunks(List<RenderSection> chunks, boolean translucent) {
        return translucent ? Lists.reverse(chunks) : chunks;
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    public static class CubeRenderer extends RegionChunkRenderer<ChunkShaderInterface.Cube, CubeBufferTarget> {
        public CubeRenderer(RenderDevice device) {
            super(device, ChunkMeshType.CUBE, "blocks/opaque_cube");
        }

        @Override
        protected void prepareShader(ChunkShaderInterface.Cube shader) {
            shader.setInstanceUniforms(this.instanceUniformBuffer);
        }

        @Override
        protected void renderRegion(CommandList commandList, ChunkShaderInterface.Cube shader, RenderRegionStorage<CubeBufferTarget> storage) {
            commandList.uploadData(this.instanceUniformBuffer, this.instanceUniformBufferBuilder.window(), GlBufferUsage.STATIC_DRAW);

            shader.setStorageQuads(storage.getBuffer(CubeBufferTarget.QUADS));
            shader.setStorageVertices(storage.getBuffer(CubeBufferTarget.VERTICES));

            try (DrawCommandList drawCommandList = commandList.beginTessellating(this.prepareTessellation(commandList))) {
                drawCommandList.multiDrawElementsBaseVertex(this.batch.getPointerBuffer(), this.batch.getCountBuffer(), this.batch.getBaseVertexBuffer(),
                        GlIndexType.UNSIGNED_INT);
            }
        }

        @Override
        protected void addDrawInstanceState(RenderRegionStorage<CubeBufferTarget> storage, ChunkGraphicsState<CubeBufferTarget> state, int baseInstance, RenderSection section, ChunkCameraContext camera) {
            var quads = state.getBuffer(CubeBufferTarget.QUADS);
            var vertices = state.getBuffer(CubeBufferTarget.VERTICES);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer buf = stack.malloc(32);
                buf.putFloat(16, getCameraTranslation(section.getOriginX(), camera.blockX, camera.deltaX));
                buf.putFloat(20, getCameraTranslation(section.getOriginY(), camera.blockY, camera.deltaY));
                buf.putFloat(24, getCameraTranslation(section.getOriginZ(), camera.blockZ, camera.deltaZ));
                buf.putInt(0, quads.getOffset() / 20);
                buf.putInt(4, vertices.getOffset() / 4);

                this.instanceUniformBufferBuilder.add(buf);
            }
        }

        @Override
        protected ChunkShaderInterface.Cube createShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
            return new ChunkShaderInterface.Cube(context, options);
        }
    }

    public static class ModelRenderer extends RegionChunkRenderer<ChunkShaderInterface.Model, ModelBufferTarget> {
        public ModelRenderer(RenderDevice device) {
            super(device, ChunkMeshType.MODEL, "blocks/opaque_model");
        }

        @Override
        protected void prepareShader(ChunkShaderInterface.Model shader) {
            shader.setInstanceUniforms(this.instanceUniformBuffer);
        }

        @Override
        protected void renderRegion(CommandList commandList, ChunkShaderInterface.Model shader, RenderRegionStorage<ModelBufferTarget> storage) {
            commandList.uploadData(this.instanceUniformBuffer, this.instanceUniformBufferBuilder.window(), GlBufferUsage.STATIC_DRAW);

            shader.setVertexStorage(storage.getBuffer(ModelBufferTarget.VERTICES));

            try (DrawCommandList drawCommandList = commandList.beginTessellating(this.prepareTessellation(commandList))) {
                drawCommandList.multiDrawElementsBaseVertex(this.batch.getPointerBuffer(), this.batch.getCountBuffer(), this.batch.getBaseVertexBuffer(),
                        GlIndexType.UNSIGNED_INT);
            }
        }

        @Override
        protected void addDrawInstanceState(RenderRegionStorage<ModelBufferTarget> storage, ChunkGraphicsState<ModelBufferTarget> state, int baseInstance, RenderSection section, ChunkCameraContext camera) {
            var vertices = state.getBuffer(ModelBufferTarget.VERTICES);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer buf = stack.malloc(32);
                buf.putFloat(16, getCameraTranslation(section.getOriginX(), camera.blockX, camera.deltaX));
                buf.putFloat(20, getCameraTranslation(section.getOriginY(), camera.blockY, camera.deltaY));
                buf.putFloat(24, getCameraTranslation(section.getOriginZ(), camera.blockZ, camera.deltaZ));
                buf.putInt(0, vertices.getOffset() / 24);

                this.instanceUniformBufferBuilder.add(buf);
            }
        }

        @Override
        protected ChunkShaderInterface.Model createShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
            return new ChunkShaderInterface.Model(context, options);
        }
    }
}
