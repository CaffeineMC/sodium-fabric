package me.jellysquid.mods.sodium.render.immediate;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayBuffer;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayDescription;
import me.jellysquid.mods.sodium.opengl.array.VertexArrayResourceBinding;
import me.jellysquid.mods.sodium.opengl.attribute.VertexAttribute;
import me.jellysquid.mods.sodium.opengl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import me.jellysquid.mods.sodium.render.sequence.IndexSequenceType;
import me.jellysquid.mods.sodium.render.sequence.SequenceIndexBuffer;
import me.jellysquid.mods.sodium.render.stream.MappedStreamingBuffer;
import me.jellysquid.mods.sodium.render.stream.StreamingBuffer;
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
    private final RenderDevice device;

    public RenderImmediate(RenderDevice device) {
        this.device = device;

        this.vertexArrays = new Reference2ReferenceOpenHashMap<>();
        this.defaultElementBuffers = new Reference2ObjectArrayMap<>();

        this.vertexBuffer = new MappedStreamingBuffer(device, 64 * 1024 * 1024);
        this.elementBuffer = new MappedStreamingBuffer(device, 8 * 1024 * 1024);

        for (IndexSequenceType sequenceType : IndexSequenceType.values()) {
            this.defaultElementBuffers.put(sequenceType, new SequenceIndexBuffer(device, sequenceType.builder));
        }
    }

    public void draw(ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int vertexCount,
                     VertexFormat.IntType elementFormat, int elementCount, boolean useDefaultElementBuffer) {
        if (vertexCount <= 0) {
            return;
        }

        buffer.clear();

        int vertexBytes = vertexCount * vertexFormat.getVertexSize();

        var vertexData = buffer.slice(0, vertexBytes);
        var vertexStride = vertexFormat.getVertexSize();

        VertexArray<BufferTarget> vertexArray = this.createVertexArray(vertexFormat);
        int baseVertex = this.vertexBuffer.write(vertexData, vertexStride);

        Buffer elementBuffer;
        int elementPointer;

        IntType usedElementFormat;

        if (useDefaultElementBuffer) {
            var sequenceBufferBuilder = this.defaultElementBuffers.get(IndexSequenceType.map(drawMode));
            sequenceBufferBuilder.ensureCapacity(vertexCount);

            usedElementFormat = getElementType(VertexFormat.IntType.INT);
            elementBuffer = sequenceBufferBuilder.getBuffer();
            elementPointer = 0;
        } else {
            var elementData = buffer.slice(vertexBytes, elementCount * elementFormat.size);

            usedElementFormat = getElementType(elementFormat);
            elementBuffer = this.elementBuffer.getBuffer();
            elementPointer = this.elementBuffer.write(elementData, elementFormat.size);
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

        this.device.useProgram(new VanillaProgram(shader), (programCommandList, programInterface) -> {
            programCommandList.useVertexArray(vertexArray, (drawCommandList) -> {
                drawCommandList.bindVertexBuffers(vertexArray.createResourceSet(
                        Map.of(BufferTarget.VERTICES, new VertexArrayBuffer(this.vertexBuffer.getBuffer(), vertexFormat.getVertexSize()))
                ));
                drawCommandList.bindElementBuffer(elementBuffer);

                drawCommandList.drawElementsBaseVertex(getPrimitiveType(drawMode), usedElementFormat,
                        (long) elementPointer * elementFormat.size, baseVertex, elementCount);
            });
        });

        shader.unbind();
    }

    private static IntType getElementType(VertexFormat.IntType format) {
        return IntType.BY_FORMAT.get(format.count);
    }

    private static PrimitiveType getPrimitiveType(VertexFormat.DrawMode drawMode) {
        return PrimitiveType.BY_FORMAT.get(drawMode.mode);
    }

    private VertexArray<BufferTarget> createVertexArray(VertexFormat vertexFormat) {
        var vertexArray = this.vertexArrays.get(vertexFormat);

        if (vertexArray == null) {
            vertexArray = this.device.createVertexArray(new VertexArrayDescription<>(BufferTarget.class,
                    List.of(new VertexArrayResourceBinding<>(BufferTarget.VERTICES, createVanillaVertexBindings(vertexFormat)))));

            this.vertexArrays.put(vertexFormat, vertexArray);
        }

        return vertexArray;
    }

    private static VertexAttributeBinding[] createVanillaVertexBindings(VertexFormat vertexFormat) {
        var elements = vertexFormat.getElements();
        var bindings = new ArrayList<VertexAttributeBinding>();

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

            var attribute = new VertexAttribute(format, size, count, normalized, vertexFormat.offsets.getInt(i), vertexFormat.getVertexSize(), intType);

            bindings.add(new VertexAttributeBinding(i, attribute));
        }

        return bindings.toArray(VertexAttributeBinding[]::new);
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

    public static void delete() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.delete0();
        INSTANCE = null;
    }

    private void delete0() {
        for (var array : this.vertexArrays.values()) {
            this.device.deleteVertexArray(array);
        }

        this.vertexArrays.clear();

        for (var buffer : this.defaultElementBuffers.values()) {
            buffer.delete();
        }

        this.vertexBuffer.delete();
        this.elementBuffer.delete();
    }

    public static void flush() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.flush0();
        INSTANCE = null;
    }

    private void flush0() {
        this.vertexBuffer.flush();
        this.elementBuffer.flush();
    }

    private enum BufferTarget {
        VERTICES
    }

    private record VanillaProgram(Shader shader) implements Program<Shader> {
        @Override
        public Shader getInterface() {
            return this.shader;
        }

        @Override
        public int handle() {
            return this.shader.getProgramRef();
        }

        @Override
        public void bindResources() {
            this.shader.bind();
        }

        @Override
        public void unbindResources() {
            this.shader.unbind();
        }
    }
}
