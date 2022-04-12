package net.caffeinemc.sodium.render.shader;

import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public interface ShaderLoader<T> {
    ShaderLoader<Identifier> MINECRAFT_ASSETS = name -> {
        String path = String.format("/assets/%s/shaders/%s", name.getNamespace(), name.getPath());

        try (InputStream in = ShaderLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Shader not found: " + path);
            }

            return IOUtils.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader source for " + path, e);
        }
    };

    byte[] getShaderSource(T name);
}
