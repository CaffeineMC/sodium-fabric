package me.jellysquid.mods.sodium.client.render.vertex.serializers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.generated.VertexSerializerFactory;
import me.jellysquid.mods.sodium.client.render.vertex.transform.CommonVertexElement;
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

    private static final Path CLASS_DUMP_PATH;

    static {
        var classDumpPath = System.getProperty("sodium.codegen.dump", null);

        if (classDumpPath != null) {
            CLASS_DUMP_PATH = Path.of(classDumpPath);
        } else {
            CLASS_DUMP_PATH = null;
        }
    }

    private static final Long2ReferenceMap<VertexSerializer> CACHE = new Long2ReferenceOpenHashMap<>();

    public static VertexSerializer get(VertexFormatDescription srcFormat, VertexFormatDescription dstFormat) {
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
        var identifier = String.format("%04X$%04X", srcVertexFormat.id, dstVertexFormat.id);
        var desc = createMemoryTransferList(srcVertexFormat, dstVertexFormat);

        var bytecode = VertexSerializerFactory.generate(desc, srcVertexFormat, dstVertexFormat, identifier);

        if (CLASS_DUMP_PATH != null) {
            dumpClass(identifier, bytecode);
        }

        Class<?> clazz = VertexSerializerFactory.define(bytecode);
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

    private static List<MemoryTransfer> createMemoryTransferList(VertexFormatDescription srcVertexFormat, VertexFormatDescription dstVertexFormat) {
        var ops = new ArrayList<MemoryTransfer>();

        for (var elementType : CommonVertexElement.values()) {
            // Check if we need to transfer the element into the destination format
            if (!dstVertexFormat.hasElement(elementType)) {
                continue;
            }

            // If the destination format has the element, then the source format needs to have it as well
            if (!srcVertexFormat.hasElement(elementType)) {
                throw new RuntimeException("Source format is missing element %s as required by destination format".formatted(elementType));
            }

            var srcOffset = srcVertexFormat.getElementOffset(elementType);
            var dstOffset = dstVertexFormat.getElementOffset(elementType);

            ops.add(new MemoryTransfer(srcOffset, dstOffset, elementType.getByteLength()));
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

    private static void dumpClass(String id, VertexSerializerFactory.Bytecode bytecode) {
        var path = CLASS_DUMP_PATH.resolve("VertexSerializer$Impl$%s.class".formatted(id));

        try {
            Files.write(path, bytecode.copy());
        } catch (IOException e) {
            LOGGER.warn("Could not dump bytecode to location: {}", path, e);
        }
    }
}
