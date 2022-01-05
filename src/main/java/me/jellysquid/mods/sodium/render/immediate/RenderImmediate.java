package me.jellysquid.mods.sodium.render.immediate;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.ShaderExtended;
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

        this.device.usePipeline(null, (pipelineCommands, pipelineState) -> {
            pipelineCommands.useProgram(getProgram(shader), (programCommands, programInterface) -> {
                setup(shader);

                for (var sampler : programInterface.getSamplers()) {
                    var samplerLocation = sampler.location();
                    var samplerTexture = sampler.textureSupplier()
                                    .getAsInt();

                    pipelineState.bindTexture(samplerLocation, samplerTexture, null);
                }

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

                programCommands.useVertexArray(vertexArray, (drawCommands) -> {
                    drawCommands.bindVertexBuffers(vertexArray.createResourceSet(
                            Map.of(BufferTarget.VERTICES, new VertexArrayBuffer(this.vertexBuffer.getBuffer(), vertexFormat.getVertexSize()))
                    ));
                    drawCommands.bindElementBuffer(elementBuffer);

                    drawCommands.drawElementsBaseVertex(getPrimitiveType(drawMode), usedElementFormat,
                            (long) elementPointer * elementFormat.size, baseVertex, elementCount);
                });
            });
        });
    }

    @Deprecated(forRemoval = true)
    private static void setup(Shader shader) {
        ((ShaderExtended) shader).setup();
    }

    private static Program<VanillaShaderInterface> getProgram(Shader shader) {
        return ((ShaderExtended) shader).sodium$getProgram();
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

    public enum BufferTarget {
        VERTICES
    }
}
