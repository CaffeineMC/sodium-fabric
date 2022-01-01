package me.jellysquid.mods.sodium.client.render.immediate;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.sync.GlFence;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.render.immediate.stream.BufferHandle;
import me.jellysquid.mods.sodium.client.render.immediate.stream.FallbackStreamingBuffer;
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
import java.util.Map;
import java.util.function.Supplier;

public class RenderImmediate {
    private static RenderImmediate INSTANCE;

    private final StreamingBuffer vertexBuffer;
    private final StreamingBuffer elementBuffer;

    private final Map<Pair<VertexFormat, IndexSequenceType>, GlTessellation> defaultTessellations;
    private final Map<IndexSequenceType, SequenceIndexBuffer> defaultElementBuffers;

    private final Map<VertexFormat, GlTessellation> customTessellations;

    public RenderImmediate(RenderDevice device) {
        this.defaultTessellations = new Object2ObjectOpenHashMap<>();
        this.defaultElementBuffers = new Reference2ObjectArrayMap<>();

        this.customTessellations = new Reference2ReferenceOpenHashMap<>();

        try (CommandList commandList = device.createCommandList()) {
            if (SodiumClientMod.options().advanced.allowPersistentMemoryMapping && MappedStreamingBuffer.isSupported(device)) {
                this.vertexBuffer = new MappedStreamingBuffer(commandList, 64 * 1024 * 1024);
                this.elementBuffer = new MappedStreamingBuffer(commandList, 8 * 1024 * 1024);
            } else {
                this.vertexBuffer = new FallbackStreamingBuffer(commandList);
                this.elementBuffer = new FallbackStreamingBuffer(commandList);
            }

            for (IndexSequenceType sequenceType : IndexSequenceType.values()) {
                this.defaultElementBuffers.put(sequenceType, new SequenceIndexBuffer(commandList, sequenceType.builder));
            }
        }
    }

    public void draw(ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int vertexCount,
                     VertexFormat.IntType elementFormat, int elementCount, boolean useDefaultElementBuffer) {
        if (vertexCount <= 0) {
            return;
        }

        buffer.clear();

        CommandList commandList = RenderDevice.INSTANCE.createCommandList();
        GlTessellation tessellation;

        int vertexBytes = vertexCount * vertexFormat.getVertexSize();

        var vertexData = buffer.slice(0, vertexBytes);
        var vertexStride = vertexFormat.getVertexSize();

        BufferHandle vertices = this.vertexBuffer.write(commandList, vertexData, vertexStride);
        int baseVertex = vertices.getElementOffset();

        BufferHandle elements;
        int baseElement;

        VertexFormat.IntType usedElementFormat;

        if (useDefaultElementBuffer) {
            usedElementFormat = VertexFormat.IntType.INT;
            tessellation = this.prepareDefaultTessellation(commandList, vertexFormat, IndexSequenceType.map(drawMode), elementCount);

            elements = null;
            baseElement = 0;
        } else {
            var elementData = buffer.slice(vertexBytes, elementCount * elementFormat.size);

            usedElementFormat = elementFormat;
            tessellation = this.prepareCustomTessellation(commandList, vertexFormat);

            elements = this.elementBuffer.write(commandList, elementData, 1);
            baseElement = elements.getElementOffset();
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

        try (var drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.drawElementsBaseVertex(getPrimitiveType(drawMode), getElementType(usedElementFormat), baseElement, baseVertex, elementCount);
        }

        shader.unbind();

        Supplier<GlFence> fence = Suppliers.memoize(commandList::createFence);

        if (elements != null) {
            elements.finish(fence);
        }

        vertices.finish(fence);
    }

    private static GlIndexType getElementType(VertexFormat.IntType format) {
        return GlIndexType.BY_FORMAT.get(format.count);
    }

    private static GlPrimitiveType getPrimitiveType(VertexFormat.DrawMode drawMode) {
        return GlPrimitiveType.BY_FORMAT.get(drawMode.mode);
    }

    private GlTessellation prepareDefaultTessellation(CommandList commandList, VertexFormat vertexFormat, IndexSequenceType sequenceType, int sequenceLength) {
        var sequenceBufferBuilder = this.defaultElementBuffers.get(sequenceType);
        sequenceBufferBuilder.ensureCapacity(commandList, sequenceLength);

        var key = Pair.of(vertexFormat, sequenceType);
        var tessellation = this.defaultTessellations.get(key);

        if (tessellation == null) {
            tessellation = commandList.createTessellation(new TessellationBinding[] {
                    TessellationBinding.forVertexBuffer(this.vertexBuffer.getBuffer(), createVanillaVertexBindings(vertexFormat)),
                    TessellationBinding.forElementBuffer(sequenceBufferBuilder.getBuffer())
            });

            this.defaultTessellations.put(key, tessellation);
        }

        return tessellation;
    }

    private GlTessellation prepareCustomTessellation(CommandList commandList, VertexFormat vertexFormat) {
        var tessellation = this.customTessellations.get(vertexFormat);

        if (tessellation == null) {
            tessellation = commandList.createTessellation(new TessellationBinding[] {
                    TessellationBinding.forVertexBuffer(this.vertexBuffer.getBuffer(), createVanillaVertexBindings(vertexFormat)),
                    TessellationBinding.forElementBuffer(this.elementBuffer.getBuffer())
            });

            this.customTessellations.put(vertexFormat, tessellation);
        }

        return tessellation;
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

        RenderDevice.enterManagedCode();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            INSTANCE.delete(commandList);
        } finally {
            RenderDevice.exitManagedCode();
        }

        INSTANCE = null;
    }

    private void delete(CommandList commandList) {
        for (GlTessellation tessellation : this.defaultTessellations.values()) {
            tessellation.delete(commandList);
        }

        for (GlTessellation tessellation : this.customTessellations.values()) {
            tessellation.delete(commandList);
        }

        this.defaultTessellations.clear();
        this.customTessellations.clear();

        for (var buffer : this.defaultElementBuffers.values()) {
            buffer.delete(commandList);
        }

        this.vertexBuffer.delete(commandList);
        this.elementBuffer.delete(commandList);
    }
}
