package me.jellysquid.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.array.*;
import me.jellysquid.mods.sodium.client.render.immediate.stream.MappedStreamingBuffer;
import me.jellysquid.mods.sodium.client.render.immediate.stream.StreamingBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.util.Window;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderImmediate {
    private static RenderImmediate INSTANCE;

    private final StreamingBuffer vertexBuffer;
    private final StreamingBuffer elementBuffer;

    private final Map<VertexFormat, VertexArray<BufferTarget>> vertexArrays;
    private final Map<IndexSequenceType, SequenceIndexBuffer> defaultElementBuffers;

    public RenderImmediate(RenderDevice device) {
        this.vertexArrays = new Reference2ReferenceOpenHashMap<>();
        this.defaultElementBuffers = new Reference2ObjectArrayMap<>();

        try (CommandList commandList = device.createCommandList()) {
            this.vertexBuffer = new MappedStreamingBuffer(commandList, 64 * 1024 * 1024);
            this.elementBuffer = new MappedStreamingBuffer(commandList, 8 * 1024 * 1024);
        }

        for (IndexSequenceType sequenceType : IndexSequenceType.values()) {
            this.defaultElementBuffers.put(sequenceType, new SequenceIndexBuffer(sequenceType.builder));
        }
    }

    public void draw(ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int vertexCount,
                     VertexFormat.IntType elementFormat, int elementCount, boolean useDefaultElementBuffer) {
        if (vertexCount <= 0) {
            return;
        }

        buffer.clear();

        CommandList commandList = RenderDevice.INSTANCE.createCommandList();
        ;

        int vertexBytes = vertexCount * vertexFormat.getVertexSize();

        var vertexData = buffer.slice(0, vertexBytes);
        var vertexStride = vertexFormat.getVertexSize();

        VertexArray<BufferTarget> vertexArray = this.createVertexArray(commandList, vertexFormat);
        int baseVertex = this.vertexBuffer.write(commandList, vertexData, vertexStride);

        GlBuffer elementBuffer;
        int elementPointer;

        GlIndexType usedElementFormat;

        if (useDefaultElementBuffer) {
            var sequenceBufferBuilder = this.defaultElementBuffers.get(IndexSequenceType.map(drawMode));
            sequenceBufferBuilder.ensureCapacity(commandList, vertexCount);

            usedElementFormat = getElementType(VertexFormat.IntType.INT);
            elementBuffer = sequenceBufferBuilder.getBuffer();
            elementPointer = 0;
        } else {
            var elementData = buffer.slice(vertexBytes, elementCount * elementFormat.size);

            usedElementFormat = getElementType(elementFormat);
            elementBuffer = this.elementBuffer.getBuffer();
            elementPointer = this.elementBuffer.write(commandList, elementData, elementFormat.size);
        }

        Shader shader = RenderSystem.getShader();
        Validate.notNull(shader, "No active shader");

        for (int samplerIndex = 0; samplerIndex < 8; samplerIndex++) {
            int textureHandle = RenderSystem.getShaderTexture(samplerIndex);
            shader.addSampler("Sampler" + samplerIndex, textureHandle);
        }

        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(RenderSystem.getModelViewMatrix());
        }

        if (shader.projectionMat != null) {
            shader.projectionMat.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.field_36323 != null) {
            shader.field_36323.method_39978(RenderSystem.getInverseViewRotationMatrix());
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
            // GlUniform.set is ambiguous without the cast
            shader.screenSize.set((float) window.getFramebufferWidth(), (float) window.getFramebufferHeight());
        }

        if (shader.lineWidth != null && (drawMode == VertexFormat.DrawMode.LINES || drawMode == VertexFormat.DrawMode.LINE_STRIP)) {
            shader.lineWidth.set(RenderSystem.getShaderLineWidth());
        }

        RenderSystem.setupShaderLights(shader);

        // Uniforms must be updated before binding shader
        shader.bind();

        commandList.useVertexArray(vertexArray, (drawCommandList) -> {
            drawCommandList.bindVertexBuffers(vertexArray.createBindings(
                    Map.of(BufferTarget.VERTICES, new VertexArrayBuffer(this.vertexBuffer.getBuffer(), vertexFormat.getVertexSize()))
            ));
            drawCommandList.bindElementBuffer(elementBuffer);

            drawCommandList.drawElementsBaseVertex(getPrimitiveType(drawMode), usedElementFormat,
                    (long) elementPointer * elementFormat.size, baseVertex, elementCount);
        });

        shader.unbind();
    }

    private static GlIndexType getElementType(VertexFormat.IntType format) {
        return GlIndexType.BY_FORMAT.get(format.count);
    }

    private static GlPrimitiveType getPrimitiveType(VertexFormat.DrawMode drawMode) {
        return GlPrimitiveType.BY_FORMAT.get(drawMode.mode);
    }

    private VertexArray<BufferTarget> createVertexArray(CommandList commandList, VertexFormat vertexFormat) {
        var vertexArray = this.vertexArrays.get(vertexFormat);

        if (vertexArray == null) {
            vertexArray = commandList.createVertexArray(new VertexArrayDescription<>(BufferTarget.class,
                    List.of(new VertexBufferBinding<>(BufferTarget.VERTICES, createVanillaVertexBindings(vertexFormat)))));

            this.vertexArrays.put(vertexFormat, vertexArray);
        }

        return vertexArray;
    }

    private static GlVertexAttributeBinding[] createVanillaVertexBindings(VertexFormat vertexFormat) {
        var elements = vertexFormat.getElements();
        var bindings = new ArrayList<GlVertexAttributeBinding>();

        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);

            if (element.getType() == VertexFormatElement.Type.PADDING) {
                continue;
            }

            var format = element.getDataType().getId();
            var count = element.getLength();
            var size = element.getByteLength();
            var normalized = isVanillaAttributeNormalized(element.getType());
            var intType = isVanillaIntType(element.getType(), element.getDataType());

            var attribute = new GlVertexAttribute(format, size, count, normalized, vertexFormat.offsets.getInt(i), vertexFormat.getVertexSize(), intType);

            bindings.add(new GlVertexAttributeBinding(i, attribute));
        }

        return bindings.toArray(GlVertexAttributeBinding[]::new);
    }

    private static boolean isVanillaIntType(VertexFormatElement.Type type, VertexFormatElement.DataType dataType) {
        return type == VertexFormatElement.Type.UV && dataType != VertexFormatElement.DataType.FLOAT;
    }

    private static boolean isVanillaAttributeNormalized(VertexFormatElement.Type type) {
        return type == VertexFormatElement.Type.NORMAL || type == VertexFormatElement.Type.COLOR;
    }

    public static RenderImmediate getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RenderImmediate(RenderDevice.INSTANCE);
        }

        return INSTANCE;
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            INSTANCE.delete(commandList);
        }

        INSTANCE = null;
    }

    public static void flush() {
        if (INSTANCE == null) {
            return;
        }

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            INSTANCE.flush(commandList);
        }

        INSTANCE = null;
    }

    private void flush(CommandList commandList) {
        this.vertexBuffer.flush(commandList);
        this.elementBuffer.flush(commandList);
    }

    private void delete(CommandList commandList) {
        for (var array : this.vertexArrays.values()) {
            commandList.deleteVertexArray(array);
        }

        this.vertexArrays.clear();

        for (var buffer : this.defaultElementBuffers.values()) {
            buffer.delete(commandList);
        }

        this.vertexBuffer.delete(commandList);
        this.elementBuffer.delete(commandList);
    }
    
    private enum BufferTarget {
        VERTICES
    }
}
