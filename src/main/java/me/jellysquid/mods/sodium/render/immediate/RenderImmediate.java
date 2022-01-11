package me.jellysquid.mods.sodium.render.immediate;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.ShaderTexture;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.ShaderTextureParameters;
import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.pipeline.Pipeline;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineState;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import me.jellysquid.mods.sodium.render.sequence.IndexSequenceType;
import me.jellysquid.mods.sodium.render.sequence.SequenceIndexBuffer;
import me.jellysquid.mods.sodium.render.stream.MappedStreamingBuffer;
import me.jellysquid.mods.sodium.render.stream.StreamingBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.Window;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;
import java.util.Map;

public class RenderImmediate {
    private static RenderImmediate INSTANCE;

    private final StreamingBuffer vertexBuffer;
    private final StreamingBuffer elementBuffer;

    private final Map<IndexSequenceType, SequenceIndexBuffer> defaultElementBuffers;
    private final RenderDevice device;

    private final Map<ShaderTextureParameters, Sampler> samplers = new Object2ObjectOpenHashMap<>();

    public RenderImmediate(RenderDevice device) {
        this.device = device;

        this.defaultElementBuffers = new Reference2ObjectArrayMap<>();

        this.vertexBuffer = new MappedStreamingBuffer(device, 64 * 1024 * 1024);
        this.elementBuffer = new MappedStreamingBuffer(device, 8 * 1024 * 1024);

        for (IndexSequenceType sequenceType : IndexSequenceType.values()) {
            this.defaultElementBuffers.put(sequenceType, new SequenceIndexBuffer(device, sequenceType.builder));
        }
    }

    public void draw(Pipeline<VanillaShaderInterface, VanillaShaderInterface.BufferTarget> pipeline, ShaderTexture[] shaderTextures,
                     ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int vertexCount,
                     VertexFormat.IntType elementFormat, int elementCount, boolean useDefaultElementBuffer) {
        if (vertexCount <= 0) {
            return;
        }

        int vertexBytes = vertexCount * vertexFormat.getVertexSize();

        var vertexData = buffer.slice(0, vertexBytes);
        var vertexBufferHandle = this.vertexBuffer.write(vertexData);

        if (useDefaultElementBuffer) {
            var sequenceBufferBuilder = this.defaultElementBuffers.get(IndexSequenceType.map(drawMode));
            sequenceBufferBuilder.ensureCapacity(vertexCount);

            this.draw(pipeline, shaderTextures, drawMode, vertexFormat,
                    vertexBufferHandle.getBuffer(), vertexBufferHandle.getOffset(),
                    sequenceBufferBuilder.getElementDataType(), sequenceBufferBuilder.getBuffer(), 0, elementCount);

        } else {
            var intType = getElementType(elementFormat);

            var elementData = buffer.slice(vertexBytes, elementCount * intType.getSize());
            var elementBufferHandle = this.elementBuffer.write(elementData);

            this.draw(pipeline, shaderTextures, drawMode, vertexFormat,
                    vertexBufferHandle.getBuffer(), vertexBufferHandle.getOffset(),
                    intType, elementBufferHandle.getBuffer(), elementBufferHandle.getOffset(), elementCount);

            elementBufferHandle.free();
        }

        vertexBufferHandle.free();
    }

    private void draw(Pipeline<VanillaShaderInterface, VanillaShaderInterface.BufferTarget> pipeline, ShaderTexture[] shaderTextures,
                      VertexFormat.DrawMode drawMode,
                      VertexFormat vertexFormat, Buffer vertexBuffer, int vertexPointer,
                      IntType elementFormat, Buffer elementBuffer, int elementPointer, int elementCount) {
        this.device.usePipeline(pipeline, (drawCommands, programInterface, pipelineState) -> {
            this.updateSamplers(programInterface, shaderTextures, pipelineState);
            this.updateUniforms(programInterface, drawMode);

            drawCommands.bindVertexBuffer(VanillaShaderInterface.BufferTarget.VERTICES, vertexBuffer, vertexPointer, vertexFormat.getVertexSize());
            drawCommands.bindElementBuffer(elementBuffer);

            drawCommands.drawElements(getPrimitiveType(drawMode), elementFormat, elementPointer, elementCount);
        });
    }

    private void updateSamplers(VanillaShaderInterface programInterface, ShaderTexture[] shaderTextures, PipelineState pipelineState) {
        for (var uniform : programInterface.getSamplerUniforms()) {
            var shaderTexture = shaderTextures[uniform.target()];

            if (shaderTexture == null) {
                throw new RuntimeException("Shader texture %s not provided by render phase".formatted(uniform.target()));
            }

            var texture = shaderTexture.texture();
            var sampler = this.getSampler(shaderTexture.params());

            pipelineState.bindTexture(uniform.unit(), texture.getAsInt(), sampler);
        }
    }

    private void updateUniforms(VanillaShaderInterface programInterface, VertexFormat.DrawMode drawMode) {
        if (programInterface.modelViewMat != null) {
            programInterface.modelViewMat.set(RenderSystem.getModelViewMatrix());
        }

        if (programInterface.projectionMat != null) {
            programInterface.projectionMat.set(RenderSystem.getProjectionMatrix());
        }

        if (programInterface.inverseViewRotationMat != null) {
            programInterface.inverseViewRotationMat.set(RenderSystem.getInverseViewRotationMatrix());
        }

        if (programInterface.colorModulator != null) {
            programInterface.colorModulator.setFloats(RenderSystem.getShaderColor());
        }

        if (programInterface.fogStart != null) {
            programInterface.fogStart.setFloat(RenderSystem.getShaderFogStart());
        }

        if (programInterface.fogEnd != null) {
            programInterface.fogEnd.setFloat(RenderSystem.getShaderFogEnd());
        }

        if (programInterface.fogColor != null) {
            programInterface.fogColor.setFloats(RenderSystem.getShaderFogColor());
        }

        if (programInterface.textureMat != null) {
            programInterface.textureMat.set(RenderSystem.getTextureMatrix());
        }

        if (programInterface.gameTime != null) {
            programInterface.gameTime.setFloat(RenderSystem.getShaderGameTime());
        }

        if (programInterface.screenSize != null) {
            Window window = MinecraftClient.getInstance().getWindow();
            programInterface.screenSize.setFloats(window.getFramebufferWidth(), window.getFramebufferHeight());
        }

        if (programInterface.lineWidth != null && (drawMode == VertexFormat.DrawMode.LINES || drawMode == VertexFormat.DrawMode.LINE_STRIP)) {
            programInterface.lineWidth.setFloat(RenderSystem.getShaderLineWidth());
        }

        // We can't use RenderSystem.setupLightDirections because it expects to be able to modify
        // the dirty table of the Minecraft shader, which we aren't using. We must extract these values
        // manually.
        if (programInterface.light0Direction != null) {
            programInterface.light0Direction.setFloats(RenderSystem.shaderLightDirections[0]);
        }

        if (programInterface.light1Direction != null) {
            programInterface.light1Direction.setFloats(RenderSystem.shaderLightDirections[1]);
        }
    }

    private Sampler getSampler(ShaderTextureParameters params) {
        return this.samplers.computeIfAbsent(params, RenderImmediate::createSampler);
    }

    private static Sampler createSampler(ShaderTextureParameters params) {
        var sampler = RenderDevice.INSTANCE.createSampler();

        int minFilter;
        int maxFilter;

        if (params.bilinear()) {
            minFilter = params.mipmap() ? GL30C.GL_LINEAR_MIPMAP_LINEAR : GL30C.GL_LINEAR;
            maxFilter = GL30C.GL_LINEAR;
        } else {
            minFilter = params.mipmap() ? GL30C.GL_NEAREST_MIPMAP_LINEAR : GL30C.GL_NEAREST;
            maxFilter = GL30C.GL_NEAREST;
        }

        sampler.setParameter(GL30C.GL_TEXTURE_MIN_FILTER, minFilter);
        sampler.setParameter(GL30C.GL_TEXTURE_MAG_FILTER, maxFilter);

        return sampler;
    }

    private static IntType getElementType(VertexFormat.IntType format) {
        return IntType.BY_FORMAT.get(format.count);
    }

    private static PrimitiveType getPrimitiveType(VertexFormat.DrawMode drawMode) {
        return PrimitiveType.BY_FORMAT.get(drawMode.mode);
    }

    public static RenderImmediate getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RenderImmediate(RenderDevice.INSTANCE);
        }

        return INSTANCE;
    }

    public static void delete() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.delete0();
        INSTANCE = null;
    }

    private void delete0() {
        for (var buffer : this.defaultElementBuffers.values()) {
            buffer.delete();
        }

        this.defaultElementBuffers.clear();

        for (var sampler : this.samplers.values()) {
            this.device.deleteSampler(sampler);
        }

        this.samplers.clear();

        this.vertexBuffer.delete();
        this.elementBuffer.delete();
    }

    public static void tryFlush() {
        if (INSTANCE != null) {
            INSTANCE.flush();
        }
    }

    private void flush() {
        this.vertexBuffer.flush();
        this.elementBuffer.flush();
    }

}
