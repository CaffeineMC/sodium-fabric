package me.jellysquid.mods.sodium.client.util.workarounds;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DriverProbe {
    public static void main(String[] args) {
        GLFW.glfwInit();
        GLFW.glfwDefaultWindowHints();;
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        var window = GLFW.glfwCreateWindow(854, 480, "Minecraft", MemoryUtil.NULL, MemoryUtil.NULL);

        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        var fields = new HashMap<String, String>();
        fields.put("vendor", GL30C.glGetString(GL30C.GL_VENDOR));
        fields.put("version", GL30C.glGetString(GL30C.GL_VERSION));
        fields.put("renderer", GL30C.glGetString(GL30C.GL_RENDERER));

        System.out.print(encodeResponse(fields));

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static DriverProbeResult launchProbe() {
        var lwjglCoreJar = Validate.notNull(getClasspathEntry(Library.class),
                "Could not find LWJGL Core library path");
        var lwjglOpenGlJar = Validate.notNull(getClasspathEntry(GL30C.class),
                "Could not find LWJGL OpenGL library path");
        var lwjglGlfwJar = Validate.notNull(getClasspathEntry(GLFW.class),
                "Could not find LWJGL GLFW library path");
        var probeJar = Validate.notNull(getClasspathEntry(DriverProbe.class),
                "Could not find driver prober library path");

        var lwjglCoreNativeName = Validate.notNull(Configuration.LIBRARY_NAME.get(Platform.mapLibraryNameBundled("lwjgl")),
                "Couldn't determine LWJGL Core native name");
        var lwjglCoreNative = Validate.notNull(findSystemNative("org.lwjgl", lwjglCoreNativeName),
                "Couldn't find LWJGL Core native path");

        var lwjglOpenGlNativeName = Validate.notNull(Platform.mapLibraryNameBundled("lwjgl_opengl"),
                "Couldn't determine LWJGL OpenGL native name");
        var lwjglOpenGlNative = Validate.notNull(findSystemNative("org.lwjgl.opengl", lwjglOpenGlNativeName),
                "Couldn't find LWJGL OpenGL native path");

        var lwjglGlfwNativeName = Validate.notNull(Configuration.GLFW_LIBRARY_NAME.get(Platform.mapLibraryNameBundled("glfw")),
                "Couldn't determine LWJGL GLFW native name");
        var lwjglGlfwNative = Validate.notNull(findBundledNative("org.lwjgl.glfw", lwjglGlfwNativeName),
                "Couldn't find LWJGL GLFW native path");

        var jvmExecutable = Validate.notNull(findJvmExecutable(),
                "Couldn't find currently running JVM executable");

        var classpath = new HashSet<String>();
        classpath.add(lwjglCoreJar);
        classpath.add(lwjglOpenGlJar);
        classpath.add(lwjglGlfwJar);
        classpath.add(probeJar);

        var natives = new HashSet<String>();
        natives.add(getDirectory(lwjglCoreNative));
        natives.add(getDirectory(lwjglOpenGlNative));
        natives.add(getDirectory(lwjglGlfwNative));

        var pb = new ProcessBuilder();
        pb.command(jvmExecutable,
                "-classpath", String.join(File.pathSeparator, classpath),
                "-Djava.library.path=" + String.join(File.pathSeparator, natives),
                DriverProbe.class.getName());

        try {
            var process = pb.start();
            process.waitFor(5000, TimeUnit.MILLISECONDS);

            if (process.exitValue() == 0) {
                var response = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                var fields = decodeResponse(response);

                return new DriverProbeResult(fields);
            } else {
                throw new RuntimeException("Driver probe returned exit code " + process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getClasspathEntry(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        var resource = clazz.getResource(clazz.getSimpleName() + ".class");

        if (resource == null) {
            return null;
        }

        return switch (resource.getProtocol()) {
            case "jar" -> getFileForJarUrl(resource);
            case "file" -> getBaseDirectoryForFileUrl(resource, clazz);
            default -> null;
        };
    }

    private static String getBaseDirectoryForFileUrl(URL resource, Class<?> clazz) {
        var pathFragment = clazz.getName()
                .replace('.', File.separatorChar) + ".class";
        var path = resource.getPath();

        var idx = path.lastIndexOf(pathFragment);

        if (idx == -1) {
            return null;
        }

        return path.substring(0, idx);
    }

    private static String getFileForJarUrl(URL url) {
        try {
            final JarURLConnection connection =
                    (JarURLConnection) url.openConnection();
            final URL resolvedUrl = connection.getJarFileURL();

            return resolvedUrl.getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findJvmExecutable() {
        var command = ProcessHandle.current()
                .info()
                .command();

        if (command.isEmpty()) {
            throw new RuntimeException("Currently running process does not have an accessible command structure");
        }

        return command.get();
    }

    private static String findSystemNative(String module, String name) {
        AtomicReference<String> load = new AtomicReference<>();
        AtomicReference<String> loadSystem = new AtomicReference<>();

        Library.loadSystem(load::set, loadSystem::set, Library.class, module, name);

        if (load.get() != null) {
            return load.get();
        }

        if (loadSystem.get() != null) {
            return loadSystem.get();
        }

        throw new RuntimeException("Couldn't find library");
    }

    private static String findBundledNative(String module, String name) {
        try (var lib = Library.loadNative(GLFW.class, module, name, true)) {
            return lib.getPath();
        }
    }

    private static String getDirectory(String file) {
        return FilenameUtils.getFullPath(file);
    }

    private static String encodeResponse(Map<String, String> fields) {
        var response = new StringBuilder();

        for (var entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String encodedValue;

            if (value == null) {
                encodedValue = "null";
            } else {
                encodedValue = Base64.getEncoder()
                        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
            }

            response.append(key).append(':').append(encodedValue).append(';');
        }

        return response.toString();
    }

    private static Map<String, String> decodeResponse(String response) {
        var fields = new HashMap<String, String>();
        var lines = response.split(";");

        for (var line : lines) {
            var parts = line.split(":");
            var key = parts[0];
            var encodedValue = parts[1];

            String value;

            if (encodedValue.equals("null")) {
                value = null;
            } else {
                value = new String(Base64.getDecoder()
                        .decode(encodedValue), StandardCharsets.UTF_8);
            }

            fields.put(key, value);
        }

        return fields;
    }
}