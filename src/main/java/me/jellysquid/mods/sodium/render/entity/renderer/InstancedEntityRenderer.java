package me.jellysquid.mods.sodium.render.entity.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.interop.vanilla.buffer.VertexBufferAccessor;
import me.jellysquid.mods.sodium.interop.vanilla.model.VboBackedModel;
import me.jellysquid.mods.sodium.render.entity.DebugInfo;
import me.jellysquid.mods.sodium.render.entity.buffer.SectionedPersistentBuffer;
import me.jellysquid.mods.sodium.render.entity.buffer.SectionedSyncObjects;
import me.jellysquid.mods.sodium.render.entity.data.ModelBakingData;
import me.jellysquid.mods.sodium.render.entity.data.InstanceBatch;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.tessellation.TessellationImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.util.Map;

public class InstancedEntityRenderer implements EntityRenderer {

    public static final int BUFFER_CREATION_FLAGS = GL30C.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT;
    public static final int BUFFER_MAP_FLAGS = GL30C.GL_MAP_WRITE_BIT | GL30C.GL_MAP_FLUSH_EXPLICIT_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT;
    public static final int BUFFER_SECTIONS = 3;
    public static final long PART_PBO_SIZE = 9175040L; // 8.75 MiB
    public static final long MODEL_PBO_SIZE = 524288L; // 512 KiB
    public static final long TRANSLUCENT_EBO_SIZE = 1048576L; // 1 MiB

    public final SectionedPersistentBuffer partPersistentSsbo;
    public final SectionedPersistentBuffer modelPersistentSsbo;
    public final SectionedPersistentBuffer translucencyPersistentEbo;
    public final SectionedSyncObjects syncObjects;

    public InstancedEntityRenderer() {
        partPersistentSsbo = createPersistentBuffer(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, PART_PBO_SIZE);
        modelPersistentSsbo = createPersistentBuffer(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, MODEL_PBO_SIZE);
        translucencyPersistentEbo = createPersistentBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, TRANSLUCENT_EBO_SIZE);
        syncObjects = new SectionedSyncObjects(BUFFER_SECTIONS);
    }

    private static SectionedPersistentBuffer createPersistentBuffer(int bufferType, long ssboSize) {
        int name = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer(bufferType, name);
        long fullSize = ssboSize * BUFFER_SECTIONS;
        ARBBufferStorage.nglBufferStorage(bufferType, fullSize, MemoryUtil.NULL, BUFFER_CREATION_FLAGS);
        return new SectionedPersistentBuffer(
                GL30C.nglMapBufferRange(bufferType, 0, fullSize, BUFFER_MAP_FLAGS),
                name,
                BUFFER_SECTIONS,
                ssboSize
        );
    }

    public void close() {
        partPersistentSsbo.close();
        modelPersistentSsbo.close();
        translucencyPersistentEbo.close();
        syncObjects.close();
    }

    public static boolean isSupported(RenderDevice renderDevice) {
        if (renderDevice instanceof RenderDeviceImpl renderDeviceImpl) {
            GLCapabilities capabilities = renderDeviceImpl.getCapabilities();
            return capabilities.OpenGL44 ||
                    capabilities.GL_ARB_buffer_storage &&
                            capabilities.GL_ARB_shader_storage_buffer_object &&
                            capabilities.GL_ARB_shading_language_packing;
        }

        return false;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void render(RenderDevice device, ModelBakingData modelBakingData) {
        if (modelBakingData.isEmptyShallow()) return;

        long currentPartSyncObject = syncObjects.getCurrentSyncObject();

        if (currentPartSyncObject != MemoryUtil.NULL) {
            int waitResult = GL32C.glClientWaitSync(currentPartSyncObject, GL32C.GL_SYNC_FLUSH_COMMANDS_BIT, 10000000); // 10 seconds
            if (waitResult == GL32C.GL_WAIT_FAILED || waitResult == GL32C.GL_TIMEOUT_EXPIRED) {
                SodiumClient.logger().error("OpenGL sync failed, err code: " + waitResult);
            }
        }

        modelBakingData.writeData();

        long partSectionStartPos = partPersistentSsbo.getCurrentSection() * partPersistentSsbo.getSectionSize();
        long modelSectionStartPos = modelPersistentSsbo.getCurrentSection() * modelPersistentSsbo.getSectionSize();
        long translucencySectionStartPos = translucencyPersistentEbo.getCurrentSection() * translucencyPersistentEbo.getSectionSize();
        long partLength = partPersistentSsbo.getPositionOffset().getAcquire();
        long modelLength = modelPersistentSsbo.getPositionOffset().getAcquire();
        long translucencyLength = translucencyPersistentEbo.getPositionOffset().getAcquire();

        GlStateManager._glBindBuffer(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, partPersistentSsbo.getName());
        GL30C.glFlushMappedBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, partSectionStartPos, partLength);

        GlStateManager._glBindBuffer(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, modelPersistentSsbo.getName());
        GL30C.glFlushMappedBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, modelSectionStartPos, modelLength);

        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, translucencyPersistentEbo.getName());
        GL30C.glFlushMappedBufferRange(GL15.GL_ELEMENT_ARRAY_BUFFER, translucencySectionStartPos, translucencyLength);

        GL30C.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, 1, partPersistentSsbo.getName(), partSectionStartPos, partLength);
        GL30C.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, 2, modelPersistentSsbo.getName(), modelSectionStartPos, modelLength);

        DebugInfo.currentPartBufferSize = partLength;
        DebugInfo.currentModelBufferSize = modelLength;
        DebugInfo.currentTranslucencyEboSize = translucencyLength;
        partPersistentSsbo.nextSection();
        modelPersistentSsbo.nextSection();
        translucencyPersistentEbo.nextSection();

        int instanceOffset = 0;

        RenderLayer currentRenderLayer = null;
        TessellationImpl currentTesselation = null;
        BufferRenderer.unbindAll();

        for (Map<RenderLayer, Map<VboBackedModel, InstanceBatch>> perOrderedSectionData : modelBakingData) {

            for (Map.Entry<RenderLayer, Map<VboBackedModel, InstanceBatch>> perRenderLayerData : perOrderedSectionData.entrySet()) {
                RenderLayer nextRenderLayer = perRenderLayerData.getKey();
                boolean firstLayer = currentRenderLayer == null;
                if (firstLayer || !currentRenderLayer.equals(nextRenderLayer)) {
                    if (!firstLayer) {
                        currentRenderLayer.endDrawing();
                    }
                    currentRenderLayer = nextRenderLayer;
                    currentRenderLayer.startDrawing();
                }

                Shader shader = RenderSystem.getShader();

                for (Map.Entry<VboBackedModel, InstanceBatch> perModelData : perRenderLayerData.getValue().entrySet()) {
                    VboBackedModel model = perModelData.getKey();
                    TessellationImpl nextTesselation = (TessellationImpl) model.getBakedVertices();
                    int vertexCount = nextTesselation.getIndexCount();
                    if (vertexCount <= 0) continue;

                    InstanceBatch instanceBatch = perModelData.getValue();
                    boolean isIndexed = instanceBatch.isIndexed();

                    boolean firstVbo = currentTesselation == null;
                    if (firstVbo || !currentTesselation.equals(nextTesselation)) {
                        if (!firstVbo) {
                            currentTesselation.getElementFormat().endDrawing();
                        }
                        currentTesselation = nextTesselation;
                        vertexBufferAccessor.invokeBindVertexArray();
                        if (isIndexed) {
                            GL30C.glBindBufferBase(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, 3, vertexBufferAccessor.getVertexBufferId());
                        } else {
                            vertexBufferAccessor.invokeBind();
                            currentTesselation.getElementFormat().startDrawing();
                        }
                    }

                    VertexFormat.DrawMode drawMode = vertexBufferAccessor.getDrawMode();
                    int instanceCount = instanceBatch.size();
                    if (instanceCount <= 0) continue;

                    for (int i = 0; i < 12; ++i) {
                        int j = RenderSystem.getShaderTexture(i);
                        shader.addSampler("Sampler" + i, j);
                    }

                    if (shader.projectionMat != null) {
                        shader.projectionMat.set(RenderSystem.getProjectionMatrix());
                    }

                    if (shader.colorModulator != null) {
                        shader.colorModulator.set(RenderSystem.getShaderColor());
                    }

                    if (shader.fogStart != null) {
                        shader.fogStart.set(RenderSystem.getShaderFogStart());
                    }

                    if (shader.fogEnd != null) {
                        shader.fogEnd.set(RenderSystem.getShaderFogEnd());
                    }

                    if (shader.fogColor != null) {
                        shader.fogColor.set(RenderSystem.getShaderFogColor());
                    }

                    if (shader.textureMat != null) {
                        shader.textureMat.set(RenderSystem.getTextureMatrix());
                    }

                    if (shader.gameTime != null) {
                        shader.gameTime.set(RenderSystem.getShaderGameTime());
                    }

                    if (shader.screenSize != null) {
                        Window window = MinecraftClient.getInstance().getWindow();
                        shader.screenSize.set((float) window.getFramebufferWidth(), (float) window.getFramebufferHeight());
                    }

                    if (shader.lineWidth != null && (drawMode == VertexFormat.DrawMode.LINES || drawMode == VertexFormat.DrawMode.LINE_STRIP)) {
                        shader.lineWidth.set(RenderSystem.getShaderLineWidth());
                    }

                    // we have to manually get it from the shader every time because different shaders have different uniform objects for the same uniform.
                    GlUniform instanceOffsetUniform = shader.getUniform("InstanceOffset");
                    if (instanceOffsetUniform != null) {
                        instanceOffsetUniform.set(instanceOffset);
                    }

                    if (isIndexed) {
                        drawSortedFakeInstanced(instanceBatch, shader, vertexBufferAccessor, model.getVertexCount(), translucencySectionStartPos);
                    } else {
                        RenderSystem.setupShaderLights(shader);
                        shader.upload(); // should be bind
                        if (instanceCount > 1) {
                            GL31C.glDrawElementsInstanced(drawMode.mode, vertexCount, vertexBufferAccessor.getIndexType().count, MemoryUtil.NULL, instanceCount);
                        } else {
                            GL11.glDrawElements(drawMode.mode, vertexCount, vertexBufferAccessor.getIndexType().count, MemoryUtil.NULL);
                        }
                        shader.bind(); // should be unbind
                    }

                    instanceOffset += instanceCount;

                    DebugInfo.ModelDebugInfo currentDebugInfo = DebugInfo.modelToDebugInfoMap.computeIfAbsent(model.getClass().getSimpleName(), ignored -> new DebugInfo.ModelDebugInfo());
                    currentDebugInfo.instances += instanceCount;
                    currentDebugInfo.sets++;

                    modelBakingData.recycleInstanceBatch(instanceBatch);
                }
            }
        }

        if (currentTesselation != null) {
            currentTesselation.getElementFormat().endDrawing();
        }

        if (currentRenderLayer != null) {
            currentRenderLayer.endDrawing();
        }

        if (currentPartSyncObject != MemoryUtil.NULL) {
            GL32C.glDeleteSync(currentPartSyncObject);
        }
        syncObjects.setCurrentSyncObject(GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
        syncObjects.nextSection();

        modelBakingData.reset();
    }

    /**
     * Do a 'fake' glDrawElementsInstanced call, with vertex sorting.
     * <p>
     * This builds a EBO containing all the vertices for all the elements to draw. The notable
     * thing here is that we have control of the draw order - we can sort the elements by depth
     * from the camera, and use this to batch the rendering of transparent objects.
     */
    private void drawSortedFakeInstanced(InstanceBatch batch, Shader shader, VertexBufferAccessor vba, int vertexCount, long sectionStartPos) {
        GlUniform countUniform = shader.getUniform("InstanceVertCount");
        if (countUniform != null) {
            countUniform.set(vertexCount);
        }

        // this needs to be re-bound because normal instanced stuff will probably be rendered before this
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, translucencyPersistentEbo.getName());

        // TODO do we need to disable the VAO here? It's not bound in the shader, can it still
        //  cause issues?
        // (from burger) probably not

        RenderSystem.setupShaderLights(shader);
        shader.upload(); // should be bind
        VertexFormat.DrawMode drawMode = vba.getDrawMode();
        VertexFormat.IntType indexType = batch.getIndexType();
        GL11.glDrawElements(drawMode.mode, batch.getIndexCount(), indexType.count, sectionStartPos + batch.getIndexStartingPos());
        shader.bind(); // should be unbind

        // TODO Unbind EBO?
        // (from burger) the next thing that comes across will replace the binding, so it's probably not needed
    }

}
