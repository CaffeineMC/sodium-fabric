package net.caffeinemc.gfx.opengl;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.caffeinemc.gfx.api.array.attribute.VertexAttributeFormat;
import net.caffeinemc.gfx.api.buffer.BufferMapFlags;
import net.caffeinemc.gfx.api.buffer.BufferStorageFlags;
import net.caffeinemc.gfx.api.shader.ShaderType;
import net.caffeinemc.gfx.api.pipeline.state.BlendFunc;
import net.caffeinemc.gfx.api.pipeline.state.DepthFunc;
import net.caffeinemc.gfx.api.types.ElementFormat;
import net.caffeinemc.gfx.api.types.PrimitiveType;
import org.lwjgl.opengl.GL45C;

import java.util.function.Consumer;

public class GlEnum {
    private static final int[] PRIMITIVE_TYPES = build(PrimitiveType.class, (map) -> {
        map.put(PrimitiveType.TRIANGLES,        GL45C.GL_TRIANGLES);
        map.put(PrimitiveType.TRIANGLE_STRIP,   GL45C.GL_TRIANGLE_STRIP);
        map.put(PrimitiveType.TRIANGLE_FAN,     GL45C.GL_TRIANGLE_FAN);
        map.put(PrimitiveType.LINES,            GL45C.GL_LINES);
        map.put(PrimitiveType.LINE_STRIP,       GL45C.GL_LINE_STRIP);
    });

    private static final int[] BLEND_SRC_FACTOR = build(BlendFunc.SrcFactor.class, (map) -> {
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

    private static final int[] BLEND_DST_FACTOR = build(BlendFunc.DstFactor.class, (map) -> {
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

    private static final int[] INT_TYPE = build(ElementFormat.class, (map) -> {
        map.put(ElementFormat.UNSIGNED_BYTE,  GL45C.GL_UNSIGNED_BYTE);
        map.put(ElementFormat.UNSIGNED_SHORT, GL45C.GL_UNSIGNED_SHORT);
        map.put(ElementFormat.UNSIGNED_INT,   GL45C.GL_UNSIGNED_INT);
    });

    private static final int[] BUFFER_MAP_FLAGS = build(BufferMapFlags.class, (map) -> {
        map.put(BufferMapFlags.READ,                GL45C.GL_MAP_READ_BIT);
        map.put(BufferMapFlags.WRITE,               GL45C.GL_MAP_WRITE_BIT);
        map.put(BufferMapFlags.PERSISTENT,          GL45C.GL_MAP_PERSISTENT_BIT);
        map.put(BufferMapFlags.INVALIDATE_BUFFER,   GL45C.GL_MAP_INVALIDATE_BUFFER_BIT);
        map.put(BufferMapFlags.INVALIDATE_RANGE,    GL45C.GL_MAP_INVALIDATE_RANGE_BIT);
        map.put(BufferMapFlags.EXPLICIT_FLUSH,      GL45C.GL_MAP_FLUSH_EXPLICIT_BIT);
        map.put(BufferMapFlags.UNSYNCHRONIZED,      GL45C.GL_MAP_UNSYNCHRONIZED_BIT);
        map.put(BufferMapFlags.COHERENT,            GL45C.GL_MAP_COHERENT_BIT);
    });

    private static final int[] BUFFER_STORAGE_FLAGS = build(BufferStorageFlags.class, (map) -> {
        map.put(BufferStorageFlags.PERSISTENT,      GL45C.GL_MAP_PERSISTENT_BIT);
        map.put(BufferStorageFlags.READABLE,        GL45C.GL_MAP_READ_BIT);
        map.put(BufferStorageFlags.WRITABLE,       GL45C.GL_MAP_WRITE_BIT);
        map.put(BufferStorageFlags.CLIENT_STORAGE,  GL45C.GL_CLIENT_STORAGE_BIT);
        map.put(BufferStorageFlags.COHERENT,        GL45C.GL_MAP_COHERENT_BIT);
    });

    private static final int[] DEPTH_FUNC = build(DepthFunc.class, (map) -> {
        map.put(DepthFunc.NEVER,                    GL45C.GL_NEVER);
        map.put(DepthFunc.LESS,                     GL45C.GL_LESS);
        map.put(DepthFunc.LESS_THAN_OR_EQUAL,       GL45C.GL_LEQUAL);
        map.put(DepthFunc.EQUAL,                    GL45C.GL_EQUAL);
        map.put(DepthFunc.NOT_EQUAL,                GL45C.GL_NOTEQUAL);
        map.put(DepthFunc.GREATER,                  GL45C.GL_GREATER);
        map.put(DepthFunc.GREATER_THAN_OR_EQUAL,    GL45C.GL_GEQUAL);
        map.put(DepthFunc.ALWAYS,                   GL45C.GL_ALWAYS);
    });

    private static final int[] ATTRIBUTE_FORMAT = build(VertexAttributeFormat.class, (map) -> {
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

    public static int from(PrimitiveType value) {
        return PRIMITIVE_TYPES[value.ordinal()];
    }

    public static int from(BlendFunc.SrcFactor value) {
        return BLEND_SRC_FACTOR[value.ordinal()];
    }

    public static int from(BlendFunc.DstFactor value) {
        return BLEND_DST_FACTOR[value.ordinal()];
    }

    public static int from(ElementFormat value) {
        return INT_TYPE[value.ordinal()];
    }

    public static int from(DepthFunc value) {
        return DEPTH_FUNC[value.ordinal()];
    }

    public static int from(VertexAttributeFormat value) {
        return ATTRIBUTE_FORMAT[value.ordinal()];
    }

    public static int from(ShaderType value) {
        return SHADER_TYPES[value.ordinal()];
    }

    public static int mapFlags(Iterable<BufferMapFlags> values) {
        int result = 0;

        for (BufferMapFlags value : values) {
            result |= BUFFER_MAP_FLAGS[value.ordinal()];
        }

        return result;
    }

    public static int storageFlags(Iterable<BufferStorageFlags> values) {
        int result = 0;

        for (BufferStorageFlags value : values) {
            result |= BUFFER_STORAGE_FLAGS[value.ordinal()];
        }

        return result;
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
