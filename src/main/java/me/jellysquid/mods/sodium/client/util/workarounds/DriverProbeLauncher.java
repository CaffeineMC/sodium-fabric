package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.util.workarounds.classpath.LibraryClasspaths;
import me.jellysquid.mods.sodium.client.util.workarounds.classpath.LwjglNatives;
import me.jellysquid.mods.sodium.client.util.workarounds.classpath.ModClasspaths;
import net.minecraft.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DriverProbeLauncher {
    public static DriverProbeResult launchProbe() {
        var lwjglCoreJar = Validate.notNull(LibraryClasspaths.getClasspathEntry(Library.class),
                "Could not find LWJGL Core library path");
        var lwjglOpenGlJar = Validate.notNull(LibraryClasspaths.getClasspathEntry(GL30C.class),
                "Could not find LWJGL OpenGL library path");
        var lwjglGlfwJar = Validate.notNull(LibraryClasspaths.getClasspathEntry(GLFW.class),
                "Could not find LWJGL GLFW library path");
        var modJarPaths = Validate.notNull(ModClasspaths.getClasspathEntriesForMod(DriverProbeEntrypoint.class, "sodium"),
                "Could not find driver prober library path");

        var lwjglCoreNativeName = Validate.notNull(Configuration.LIBRARY_NAME.get(Platform.mapLibraryNameBundled("lwjgl")),
                "Couldn't determine LWJGL Core native name");
        var lwjglCoreNative = Validate.notNull(LwjglNatives.findSystemNative("org.lwjgl", lwjglCoreNativeName),
                "Couldn't find LWJGL Core native path");

        var lwjglOpenGlNativeName = Validate.notNull(Platform.mapLibraryNameBundled("lwjgl_opengl"),
                "Couldn't determine LWJGL OpenGL native name");
        var lwjglOpenGlNative = Validate.notNull(LwjglNatives.findSystemNative("org.lwjgl.opengl", lwjglOpenGlNativeName),
                "Couldn't find LWJGL OpenGL native path");

        var lwjglGlfwNativeName = Validate.notNull(Configuration.GLFW_LIBRARY_NAME.get(Platform.mapLibraryNameBundled("glfw")),
                "Couldn't determine LWJGL GLFW native name");
        var lwjglGlfwNative = Validate.notNull(LwjglNatives.findBundledNative("org.lwjgl.glfw", lwjglGlfwNativeName),
                "Couldn't find LWJGL GLFW native path");

        var jvmExecutable = Validate.notNull(findJvmExecutable(),
                "Couldn't find currently running JVM executable");

        var classpath = new HashSet<Path>();
        classpath.add(lwjglCoreJar);
        classpath.add(lwjglOpenGlJar);
        classpath.add(lwjglGlfwJar);
        classpath.addAll(modJarPaths);

        var natives = new HashSet<String>();
        natives.add(getDirectory(lwjglCoreNative));
        natives.add(getDirectory(lwjglOpenGlNative));
        natives.add(getDirectory(lwjglGlfwNative));

        var commands = new ArrayList<String>();
        commands.add(jvmExecutable);
        commands.add("-classpath");
        commands.add(classpath.stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator)));

        if (Util.getOperatingSystem() == Util.OperatingSystem.OSX) {
            commands.add("-XstartOnFirstThread");
        }

        commands.add("-Djava.library.path=" + String.join(File.pathSeparator, natives));
        commands.add(DriverProbeEntrypoint.class.getName());

        var pb = new ProcessBuilder();
        pb.command(commands);

        try {
            var process = pb.start();
            process.waitFor(5000, TimeUnit.MILLISECONDS);

            if (process.exitValue() == 0) {
                var response = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                var fields = DriverProbe.decodeResponse(response);

                return new DriverProbeResult(fields);
            } else {
                var error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);

                System.out.println("COMMAND LINE: " + String.join(",", commands));

                for (var line : error.split("\n")) {
                    System.out.println("SUBPROCESS ERROR: " + line);
                }

                throw new RuntimeException("Driver probe returned exit code " + process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
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

    private static String getDirectory(String file) {
        return FilenameUtils.getFullPath(file);
    }
}