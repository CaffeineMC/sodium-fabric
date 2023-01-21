package me.jellysquid.mods.sodium.client.render.vertex.serializers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.generated.VertexSerializerFactory;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VertexSerializerCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertexSerializerCache.class);

    private static final boolean ALLOW_RUNTIME_CODE_GENERATION;

    private static final Path CLASS_DUMP_PATH;

    static {
        ALLOW_RUNTIME_CODE_GENERATION = System.getProperty("sodium.codegen.enabled", "true").equals("true");

        var classDumpPath = System.getProperty("sodium.codegen.dump", null);

        if (classDumpPath != null) {
            CLASS_DUMP_PATH = Path.of(classDumpPath);
        } else {
            CLASS_DUMP_PATH = null;
        }
    }

    private static final Long2ReferenceMap<VertexSerializer> CACHE = new Long2ReferenceOpenHashMap<>();

    public static VertexSerializer get(VertexFormatDescription srcFormat, VertexFormatDescription dstFormat) {
        Validate.isTrue(srcFormat != dstFormat, "Source and destination formats must not be the same");

        var identifier = getSerializerKey(srcFormat, dstFormat);
        var serializer = CACHE.get(identifier);

        if (serializer == null) {
            CACHE.put(identifier, serializer = createSerializer(srcFormat, dstFormat));
        }

        return serializer;
    }

    private static long getSerializerKey(VertexFormatDescription a, VertexFormatDescription b) {
        return (long) a.id & 0xffffffffL | ((long) b.id & 0xffffffffL) << 32;
    }

    private static VertexSerializer createSerializer(VertexFormatDescription srcVertexFormat, VertexFormatDescription dstVertexFormat) {
        var identifier = String.format("%04X$%04X", srcVertexFormat.stride, dstVertexFormat.stride);
        var ops = createMemoryTransferList(srcVertexFormat, dstVertexFormat);

        if (ALLOW_RUNTIME_CODE_GENERATION) {
            return generateImpl(ops, srcVertexFormat, dstVertexFormat, identifier);
        } else {
            return generateFallbackImpl(ops, srcVertexFormat, dstVertexFormat);
        }
    }

    private static List<MemoryTransfer> createMemoryTransferList(VertexFormatDescription srcVertexFormat, VertexFormatDescription dstVertexFormat) {
        if (srcVertexFormat.elements.length < dstVertexFormat.elements.length) {
            throw new IllegalArgumentException("Source format has fewer elements than destination format");
        }

        var ops = new ArrayList<MemoryTransfer>();

        var srcElements = srcVertexFormat.elements;
        var srcOffsets = srcVertexFormat.offsets;

        var dstElements = dstVertexFormat.elements;
        var dstOffsets = dstVertexFormat.offsets;

        for (int dstIndex = 0; dstIndex < dstElements.length; dstIndex++) {
            var dstElement = dstElements[dstIndex];

            var srcIndex = ArrayUtils.indexOf(srcElements, dstElement);

            if (srcIndex == ArrayUtils.INDEX_NOT_FOUND) {
                throw new RuntimeException("Source vertex format does not contain element: " + dstElement);
            }

            var srcOffset = srcOffsets[srcIndex];
            var dstOffset = dstOffsets[dstIndex];

            ops.add(new MemoryTransfer(srcOffset, dstOffset, dstElement.getByteLength()));
        }

        return mergeAdjacentMemoryTransfers(ops);
    }

    private static List<MemoryTransfer> mergeAdjacentMemoryTransfers(ArrayList<MemoryTransfer> src) {
        var dst = new ArrayList<MemoryTransfer>(src.size());

        var srcOffset = 0;
        var dstOffset = 0;

        var length = 0;

        for (var op : src) {
            if (srcOffset + length == op.src() && dstOffset + length == op.dst()) {
                length += op.length();
                continue;
            }

            if (length > 0) {
                dst.add(new MemoryTransfer(srcOffset, dstOffset, length));
            }

            srcOffset = op.src();
            dstOffset = op.dst();
            length = op.length();
        }

        if (length > 0) {
            dst.add(new MemoryTransfer(srcOffset, dstOffset, length));
        }

        return dst;
    }

    private static VertexSerializer generateFallbackImpl(List<MemoryTransfer> ops, VertexFormatDescription srcVertexFormat, VertexFormatDescription dstVertexFormat) {
        return new VertexSerializerFallback(ops, srcVertexFormat, dstVertexFormat);
    }

    private static VertexSerializer generateImpl(List<MemoryTransfer> ops, VertexFormatDescription srcVertexFormat, VertexFormatDescription dstVertexFormat, String identifier) {
        var generated = VertexSerializerFactory.generate(ops, srcVertexFormat, dstVertexFormat, identifier);

        if (CLASS_DUMP_PATH != null) {
            dumpClass(identifier, generated);
        }

        Class<?> clazz = VertexSerializerFactory.define(generated);
        Constructor<?> constructor;

        try {
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find constructor of generated class", e);
        }

        Object instance;

        try {
            instance = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate generated class", e);
        }

        VertexSerializer serializer;

        try {
            serializer = (VertexSerializer) instance;
        } catch (ClassCastException e) {
            throw new RuntimeException("Failed to cast generated class to interface type", e);
        }

        return serializer;
    }

    private static void dumpClass(String id, VertexSerializerFactory.Bytecode bytecode) {
        var path = CLASS_DUMP_PATH.resolve("VertexSerializer$Impl$%s.class".formatted(id));

        try {
            Files.write(path, bytecode.copy());
        } catch (IOException e) {
            LOGGER.warn("Could not dump bytecode to location: {}", path, e);
        }
    }
}
