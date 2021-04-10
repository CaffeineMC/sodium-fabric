package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.GlMultiDrawBatch;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsStateArray;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterator;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import net.minecraft.util.Identifier;

import net.coderbot.iris.gl.program.ProgramUniforms;
import net.coderbot.iris.pipeline.SodiumTerrainPipeline;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public abstract class ChunkRenderBackendOneshot<T extends ChunkOneshotGraphicsState> extends ChunkRenderShaderBackend<T, ChunkProgramOneshot> {
    private final GlMultiDrawBatch batch = new GlMultiDrawBatch(ModelQuadFacing.COUNT);
    @Nullable
    private final SodiumTerrainPipeline pipeline = SodiumTerrainPipeline.create().orElse(null);

    private final MemoryTracker memoryTracker = new MemoryTracker();

    public ChunkRenderBackendOneshot(Class<T> graphicsType, ChunkVertexType vertexType) {
        super(graphicsType, vertexType);
    }

    @Override
    protected ChunkProgramOneshot createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode, BlockRenderPass pass) {
        ProgramUniforms uniforms = null;

        if (pipeline != null) {
            uniforms = pipeline.initUniforms(handle);
        }

        return new ChunkProgramOneshot(name, handle, fogMode.getFactory(), uniforms);
    }

    @Override
    protected GlShader createVertexShader(ChunkFogMode fogMode, BlockRenderPass pass) {
        if (pipeline != null) {
            Optional<String> irisVertexShader = pass.isTranslucent() ? pipeline.getTranslucentVertexShaderSource() : pipeline.getTerrainVertexShaderSource();

            if (irisVertexShader.isPresent()) {
                return new GlShader(ShaderType.VERTEX, new Identifier("iris", "sodium-terrain.vsh"), irisVertexShader.get(), ShaderConstants.builder().build());
            }
        }

        return ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_gl20.v.glsl"), fogMode.getDefines());
    }

    @Override
    protected GlShader createFragmentShader(ChunkFogMode fogMode, BlockRenderPass pass) {
        if (pipeline != null) {
            Optional<String> irisFragmentShader = pass.isTranslucent() ? pipeline.getTranslucentFragmentShaderSource() : pipeline.getTerrainFragmentShaderSource();

            if (irisFragmentShader.isPresent()) {
                return new GlShader(ShaderType.FRAGMENT, new Identifier("iris", "sodium-terrain.fsh"), irisFragmentShader.get(), ShaderConstants.builder().build());
            }
        }

        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_gl20.f.glsl"), fogMode.getDefines());
    }

    @Override
    public void upload(Iterator<ChunkBuildResult> queue) {
        while (queue.hasNext()) {
            ChunkBuildResult result = queue.next();

            ChunkRenderContainer render = result.render;
            ChunkRenderData data = result.data;

            ChunkGraphicsStateArray states = render.getGraphicsStates();

            for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                int stateId = states.remove(pass);

                if (stateId != -1) {
                    this.deleteGraphicsState(stateId);
                }

                ChunkMeshData mesh = data.getMesh(pass);

                if (mesh.hasVertexData()) {
                    T state = this.createGraphicsState(this.memoryTracker, render, this.stateIds.allocateId());
                    state.upload(mesh);

                    this.stateStorage.add(state);

                    states.set(pass, state.getId());
                }
            }

            render.setData(data);
        }
    }

    @Override
    public void render(ChunkRenderListIterator it, ChunkCameraContext camera) {
        while (it.hasNext()) {
            T state = this.stateStorage.get(it.getGraphicsStateId());

            int visibleFaces = it.getVisibleFaces();

            this.buildBatch(state, visibleFaces);

            if (this.batch.isBuilding()) {
                this.prepareDrawBatch(camera, state);
                this.drawBatch(state);
            }

            it.advance();
        }
    }

    protected void prepareDrawBatch(ChunkCameraContext camera, T state) {
        float modelX = camera.getChunkModelOffset(state.getX(), camera.blockOriginX, camera.originX);
        float modelY = camera.getChunkModelOffset(state.getY(), camera.blockOriginY, camera.originY);
        float modelZ = camera.getChunkModelOffset(state.getZ(), camera.blockOriginZ, camera.originZ);

        this.activeProgram.setModelOffset(modelX, modelY, modelZ);
    }

    protected void buildBatch(T state, int visibleFaces) {
        GlMultiDrawBatch batch = this.batch;
        batch.begin();

        for (int i = 0; i < ModelQuadFacing.COUNT; i++) {
            if ((visibleFaces & (1 << i)) == 0) {
                continue;
            }

            long part = state.getModelPart(i);
            batch.addChunkRender(BufferSlice.unpackStart(part), BufferSlice.unpackLength(part));
        }
    }

    protected void drawBatch(T state) {
        this.batch.end();

        state.bind();

        GL20.glMultiDrawArrays(GL11.GL_QUADS, this.batch.getIndicesBuffer(), this.batch.getLengthBuffer());
    }

    protected abstract T createGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer container, int id);

    @Override
    public void delete() {
        super.delete();

        this.batch.delete();
    }

    @Override
    public List<String> getDebugStrings() {
        List<String> list = new ArrayList<>();
        list.add(String.format("VRAM Usage: %s MB", MemoryTracker.toMiB(this.memoryTracker.getUsedMemory())));

        return list;
    }
}
