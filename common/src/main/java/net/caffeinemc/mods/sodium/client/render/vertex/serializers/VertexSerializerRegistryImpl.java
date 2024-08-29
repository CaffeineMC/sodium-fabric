package net.caffeinemc.mods.sodium.client.render.vertex.serializers;

import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatExtensions;
import net.caffeinemc.mods.sodium.client.render.vertex.serializers.generated.VertexSerializerFactory;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.StampedLock;

public class VertexSerializerRegistryImpl implements VertexSerializerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertexSerializerRegistryImpl.class);

    private static final Path CLASS_DUMP_PATH;

    static {
        var classDumpPath = System.getProperty("sodium.codegen.dump", null);

        if (classDumpPath != null) {
            CLASS_DUMP_PATH = Path.of(classDumpPath);
        } else {
            CLASS_DUMP_PATH = null;
        }
    }

    private final Long2ReferenceMap<VertexSerializer> cache = new Long2ReferenceOpenHashMap<>();
    private final StampedLock lock = new StampedLock();

    @Override
    public VertexSerializer get(VertexFormat srcFormat, VertexFormat dstFormat) {
        var identifier = createKey(srcFormat, dstFormat);
        var serializer = this.find(identifier);

        if (serializer == null) {
            serializer = this.create(identifier, srcFormat, dstFormat);
        }

        return serializer;
    }

    @Override
    public void registerSerializer(VertexFormat srcFormat, VertexFormat dstFormat, VertexSerializer serializer) {
        this.cache.put(createKey(srcFormat, dstFormat), serializer);
    }

    private VertexSerializer create(long identifier, VertexFormat srcFormat, VertexFormat dstFormat) {
        var stamp = this.lock.writeLock();

        try {
            // Additional lookup to avoid calling createSerializer twice
            // Necessary because 'find' only acquires a non-exclusive lock
            var cached = this.cache.get(identifier);
            if (cached != null) {
                return cached;
            }

            // Create serializer
            var serializer = createSerializer(srcFormat, dstFormat);
            this.cache.put(identifier, serializer);
            return serializer;
        } finally {
            this.lock.unlockWrite(stamp);
        }
    }

    private VertexSerializer find(long identifier) {
        var stamp = this.lock.readLock();

        try {
            return this.cache.get(identifier);
        } finally {
            this.lock.unlockRead(stamp);
        }
    }

    private static VertexSerializer createSerializer(VertexFormat srcVertexFormat, VertexFormat dstVertexFormat) {
        var identifier = String.format("%04X$%04X", getGlobalId(srcVertexFormat), getGlobalId(dstVertexFormat));

        var bytecode = VertexSerializerFactory.generate(srcVertexFormat, dstVertexFormat, identifier);

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

    private static void dumpClass(String id, VertexSerializerFactory.Bytecode bytecode) {
        var path = CLASS_DUMP_PATH.resolve("VertexSerializer$Impl$%s.class".formatted(id));

        try {
            Files.write(path, bytecode.copy());
        } catch (IOException e) {
            LOGGER.warn("Could not dump bytecode to location: {}", path, e);
        }
    }

    private static long createKey(VertexFormat a, VertexFormat b) {
        return (long) getGlobalId(a) & 0xffffffffL | ((long) getGlobalId(b) & 0xffffffffL) << 32;
    }

    private static int getGlobalId(VertexFormat format) {
        return ((VertexFormatExtensions) format).sodium$getGlobalId();
    }
}
