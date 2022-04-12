package net.caffeinemc.gradle.shaderc.compiler;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jni.JNINativeInterface;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IncludeResolver {
    private static final Cleaner CLEANER = Cleaner.create();

    public static final ShadercIncludeResolve RESOLVE_CALLBACK = new ShadercIncludeResolve() {
        @Override
        public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
            var resolver = MemoryUtil.<IncludeResolver>memGlobalRefToObject(user_data);
            var result = resolver.create(MemoryUtil.memUTF8(requested_source), type, MemoryUtil.memUTF8(requesting_source), include_depth);

            return result.address();
        }
    };

    public static final ShadercIncludeResultRelease RELEASE_CALLBACK = new ShadercIncludeResultRelease() {
        @Override
        public void invoke(long user_data, long include_result) {
            var resolver = MemoryUtil.<IncludeResolver>memGlobalRefToObject(user_data);
            var result = ShadercIncludeResult.create(include_result);

            resolver.release(result);
        }
    };

    private final long pointer;
    private final Set<ResultData> results;

    private final Set<Path> searchDirectories;

    public IncludeResolver(Collection<Path> paths) {
        var pointer = JNINativeInterface.NewGlobalRef(this);
        CLEANER.register(this, () -> JNINativeInterface.DeleteGlobalRef(pointer));

        var results = Collections.synchronizedSet(new LinkedHashSet<ResultData>());
        CLEANER.register(this, () -> {
            for (var item : results) {
                item.free();
            }

            results.clear();
        });

        this.pointer = pointer;
        this.results = results;
        this.searchDirectories = new LinkedHashSet<>(paths);
    }

    public ShadercIncludeResult create(String requestedSource, int type, String requestingSource, long depth) {
        ResultData data;

        try {
            Path resolved = this.resolve(requestedSource);

            if (resolved == null) {
                return ShadercIncludeResult.create();
            }

            data = this.open(resolved);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader source", e);
        }

        var buffer = ShadercIncludeResult.create();
        buffer.content(data.content());
        buffer.source_name(data.name());
        buffer.user_data(data.pointer());

        return buffer;
    }

    private Path resolve(String requestedSource) throws IOException {
        for (var directory : this.searchDirectories) {
            var path = directory.resolve(requestedSource);

            if (Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    private ResultData open(Path path) throws IOException {
        var source = Files.readString(path, StandardCharsets.UTF_8);
        var data = new ResultData(path.toString(), source);

        this.results.add(data);

        return data;
    }

    public void release(ShadercIncludeResult result) {
        var data = MemoryUtil.<ResultData>memGlobalRefToObject(result.user_data());
        data.free();

        this.results.remove(data);
    }

    public long pointer() {
        return this.pointer;
    }

    static class ResultData {
        private final long pointer;

        private final ByteBuffer name;
        private final ByteBuffer content;

        ResultData(String name, String content) {
            this.name = MemoryUtil.memUTF8(name, false);
            this.content = MemoryUtil.memUTF8(content, false);

            this.pointer = JNINativeInterface.NewGlobalRef(this);
        }

        public ByteBuffer name() {
            return this.name;
        }

        public ByteBuffer content() {
            return this.content;
        }

        public long pointer() {
            return this.pointer;
        }

        private void free() {
            MemoryUtil.memFree(this.name);
            MemoryUtil.memFree(this.content);

            JNINativeInterface.DeleteGlobalRef(this.pointer);
        }
    }
}
