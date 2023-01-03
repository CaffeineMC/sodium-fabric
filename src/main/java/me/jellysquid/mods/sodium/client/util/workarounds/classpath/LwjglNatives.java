package me.jellysquid.mods.sodium.client.util.workarounds.classpath;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Library;

import java.util.concurrent.atomic.AtomicReference;

public class LwjglNatives {
    public static String findSystemNative(String module, String name) {
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

    public static String findBundledNative(String module, String name) {
        try (var lib = Library.loadNative(GLFW.class, module, name, true)) {
            return lib.getPath();
        }
    }

}
