package net.caffeinemc.gfx.opengl;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.function.Consumer;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeFormat;
import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.shader.BufferBlockType;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.texture.parameters.AddressMode;
import net.caffeinemc.gfx.api.texture.parameters.FilterMode;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import org.lwjgl.opengl.GL45C;

public class GlEnum {
    private static final int[] PRIMITIVE_TYPES = build(PrimitiveType.class, (map) -> {
        map.put(PrimitiveType.TRIANGLES,        GL45C.GL_TRIANGLES);
        map.put(PrimitiveType.TRIANGLE_STRIP,   GL45C.GL_TRIANGLE_STRIP);
        map.put(PrimitiveType.TRIANGLE_FAN,     GL45C.GL_TRIANGLE_FAN);
        map.put(PrimitiveType.LINES,            GL45C.GL_LINES);
        map.put(PrimitiveType.LINE_STRIP,       GL45C.GL_LINE_STRIP);
    });

    private static final int[] BLEND_SRC_FACTORS = build(BlendFunc.SrcFactor.class, (map) -> {
        map.put(BlendFunc.SrcFactor.ZERO,                       GL45C.GL_ZERO);
        map.put(BlendFunc.SrcFactor.ONE,                        GL45C.GL_ONE);
        map.put(BlendFunc.SrcFactor.SRC_COLOR,                  GL45C.GL_SRC_COLOR);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_SRC_COLOR,        GL45C.GL_ONE_MINUS_SRC_COLOR);
        map.put(BlendFunc.SrcFactor.DST_COLOR,                  GL45C.GL_DST_COLOR);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_DST_COLOR,        GL45C.GL_ONE_MINUS_DST_COLOR);
        map.put(BlendFunc.SrcFactor.SRC_ALPHA,                  GL45C.GL_SRC_ALPHA);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_SRC_ALPHA,        GL45C.GL_ONE_MINUS_SRC_ALPHA);
        map.put(BlendFunc.SrcFactor.DST_ALPHA,                  GL45C.GL_DST_ALPHA);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_DST_ALPHA,        GL45C.GL_ONE_MINUS_DST_ALPHA);
        map.put(BlendFunc.SrcFactor.CONSTANT_COLOR,             GL45C.GL_CONSTANT_COLOR);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_CONSTANT_COLOR,   GL45C.GL_ONE_MINUS_CONSTANT_COLOR);
        map.put(BlendFunc.SrcFactor.CONSTANT_ALPHA,             GL45C.GL_CONSTANT_ALPHA);
        map.put(BlendFunc.SrcFactor.ONE_MINUS_CONSTANT_ALPHA,   GL45C.GL_ONE_MINUS_CONSTANT_ALPHA);
        map.put(BlendFunc.SrcFactor.SRC_ALPHA_SATURATE,         GL45C.GL_SRC_ALPHA_SATURATE);
    });

    private static final int[] BLEND_DST_FACTORS = build(BlendFunc.DstFactor.class, (map) -> {
        map.put(BlendFunc.DstFactor.ZERO,                       GL45C.GL_ZERO);
        map.put(BlendFunc.DstFactor.ONE,                        GL45C.GL_ONE);
        map.put(BlendFunc.DstFactor.SRC_COLOR,                  GL45C.GL_SRC_COLOR);
        map.put(BlendFunc.DstFactor.ONE_MINUS_SRC_COLOR,        GL45C.GL_ONE_MINUS_SRC_COLOR);
        map.put(BlendFunc.DstFactor.DST_COLOR,                  GL45C.GL_DST_COLOR);
        map.put(BlendFunc.DstFactor.ONE_MINUS_DST_COLOR,        GL45C.GL_ONE_MINUS_DST_COLOR);
        map.put(BlendFunc.DstFactor.SRC_ALPHA,                  GL45C.GL_SRC_ALPHA);
        map.put(BlendFunc.DstFactor.ONE_MINUS_SRC_ALPHA,        GL45C.GL_ONE_MINUS_SRC_ALPHA);
        map.put(BlendFunc.DstFactor.DST_ALPHA,                  GL45C.GL_DST_ALPHA);
        map.put(BlendFunc.DstFactor.ONE_MINUS_DST_ALPHA,        GL45C.GL_ONE_MINUS_DST_ALPHA);
        map.put(BlendFunc.DstFactor.CONSTANT_COLOR,             GL45C.GL_CONSTANT_COLOR);
        map.put(BlendFunc.DstFactor.ONE_MINUS_CONSTANT_COLOR,   GL45C.GL_ONE_MINUS_CONSTANT_COLOR);
        map.put(BlendFunc.DstFactor.CONSTANT_ALPHA,             GL45C.GL_CONSTANT_ALPHA);
        map.put(BlendFunc.DstFactor.ONE_MINUS_CONSTANT_ALPHA,   GL45C.GL_ONE_MINUS_CONSTANT_ALPHA);
    });

    private static final int[] INT_TYPES = build(ElementFormat.class, (map) -> {
        map.put(ElementFormat.UNSIGNED_BYTE,  GL45C.GL_UNSIGNED_BYTE);
        map.put(ElementFormat.UNSIGNED_SHORT, GL45C.GL_UNSIGNED_SHORT);
        map.put(ElementFormat.UNSIGNED_INT,   GL45C.GL_UNSIGNED_INT);
    });

    private static final int[] DEPTH_FUNCS = build(DepthFunc.class, (map) -> {
        map.put(DepthFunc.NEVER,                    GL45C.GL_NEVER);
        map.put(DepthFunc.LESS,                     GL45C.GL_LESS);
        map.put(DepthFunc.LESS_THAN_OR_EQUAL,       GL45C.GL_LEQUAL);
        map.put(DepthFunc.EQUAL,                    GL45C.GL_EQUAL);
        map.put(DepthFunc.NOT_EQUAL,                GL45C.GL_NOTEQUAL);
        map.put(DepthFunc.GREATER,                  GL45C.GL_GREATER);
        map.put(DepthFunc.GREATER_THAN_OR_EQUAL,    GL45C.GL_GEQUAL);
        map.put(DepthFunc.ALWAYS,                   GL45C.GL_ALWAYS);
    });

    private static final int[] ATTRIBUTE_FORMATS = build(VertexAttributeFormat.class, (map) -> {
        map.put(VertexAttributeFormat.FLOAT,            GL45C.GL_FLOAT);
        map.put(VertexAttributeFormat.BYTE,             GL45C.GL_BYTE);
        map.put(VertexAttributeFormat.UNSIGNED_BYTE,    GL45C.GL_UNSIGNED_BYTE);
        map.put(VertexAttributeFormat.SHORT,            GL45C.GL_SHORT);
        map.put(VertexAttributeFormat.UNSIGNED_SHORT,   GL45C.GL_UNSIGNED_SHORT);
        map.put(VertexAttributeFormat.INT,              GL45C.GL_INT);
        map.put(VertexAttributeFormat.UNSIGNED_INT,     GL45C.GL_UNSIGNED_INT);
    });

    private static final int[] SHADER_TYPES = build(ShaderType.class, (map) -> {
        map.put(ShaderType.VERTEX,                  GL45C.GL_VERTEX_SHADER);
        map.put(ShaderType.FRAGMENT,                GL45C.GL_FRAGMENT_SHADER);
        map.put(ShaderType.GEOMETRY,                GL45C.GL_GEOMETRY_SHADER);
        map.put(ShaderType.COMPUTE,                 GL45C.GL_COMPUTE_SHADER);
        map.put(ShaderType.TESSELLATION_CONTROL,    GL45C.GL_TESS_CONTROL_SHADER);
        map.put(ShaderType.TESSELLATION_EVALUATION, GL45C.GL_TESS_EVALUATION_SHADER);
    });

    private static final int[] BUFFER_BLOCK_TYPES = build(BufferBlockType.class, (map) -> {
        map.put(BufferBlockType.STORAGE, GL45C.GL_SHADER_STORAGE_BUFFER);
        map.put(BufferBlockType.UNIFORM, GL45C.GL_UNIFORM_BUFFER);
    });

    private static final int[] ADDRESS_MODES = build(AddressMode.class, (map) -> {
        map.put(AddressMode.CLAMP_TO_EDGE, GL45C.GL_CLAMP_TO_EDGE);
        map.put(AddressMode.CLAMP_TO_BORDER, GL45C.GL_CLAMP_TO_BORDER);
        map.put(AddressMode.MIRRORED_REPEAT, GL45C.GL_MIRRORED_REPEAT);
        map.put(AddressMode.REPEAT, GL45C.GL_REPEAT);
        map.put(AddressMode.MIRROR_CLAMP_TO_EDGE, GL45C.GL_MIRROR_CLAMP_TO_EDGE);
    });
    
    private static final int[] FILTER_MODES = build(FilterMode.class, (map) -> {
        map.put(FilterMode.NEAREST, GL45C.GL_NEAREST);
        map.put(FilterMode.LINEAR, GL45C.GL_LINEAR);
    });

    public static int from(PrimitiveType value) {
        return PRIMITIVE_TYPES[value.ordinal()];
    }

    public static int from(BlendFunc.SrcFactor value) {
        return BLEND_SRC_FACTORS[value.ordinal()];
    }

    public static int from(BlendFunc.DstFactor value) {
        return BLEND_DST_FACTORS[value.ordinal()];
    }

    public static int from(ElementFormat value) {
        return INT_TYPES[value.ordinal()];
    }

    public static int from(DepthFunc value) {
        return DEPTH_FUNCS[value.ordinal()];
    }

    public static int from(VertexAttributeFormat value) {
        return ATTRIBUTE_FORMATS[value.ordinal()];
    }

    public static int from(ShaderType value) {
        return SHADER_TYPES[value.ordinal()];
    }

    public static int from(BufferBlockType value) {
        return BUFFER_BLOCK_TYPES[value.ordinal()];
    }

    public static int from(AddressMode value) {
        return ADDRESS_MODES[value.ordinal()];
    }

    /**
     * Warning: does not cover all the mipmap values.
     */
    public static int from(FilterMode value) {
        return FILTER_MODES[value.ordinal()];
    }

    private static <T extends Enum<T>> int[] build(Class<T> type, Consumer<Reference2IntMap<T>> consumer) {
        Enum<T>[] universe = type.getEnumConstants();

        Reference2IntMap<T> map = new Reference2IntOpenHashMap<>(universe.length);
        map.defaultReturnValue(-1);

        consumer.accept(map);

        int[] values = new int[universe.length];

        for (Enum<T> e : universe) {
            int value = map.getInt(e);

            if (value == -1) {
                throw new RuntimeException("No mapping defined for " + e.name());
            }

            values[e.ordinal()] = value;
        }

        return values;
    }
}
