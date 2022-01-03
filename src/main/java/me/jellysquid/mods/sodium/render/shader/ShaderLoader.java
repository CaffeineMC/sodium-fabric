package me.jellysquid.mods.sodium.render.shader;

import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public interface ShaderLoader<T> {
    ShaderLoader<Identifier> MINECRAFT_ASSETS = new ShaderLoader<>() {
        @Override
        public String getShaderSource(Identifier name) {
            String path = String.format("/assets/%s/shaders/%s", name.getNamespace(), name.getPath());

            try (InputStream in = ShaderLoader.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new RuntimeException("Shader not found: " + path);
                }

                return IOUtils.toString(in, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read shader source for " + path, e);
            }
        }

        @Override
        public String getShaderSource(String name) {
            return this.getShaderSource(new Identifier(name));
        }
    };

    String getShaderSource(T name);

    String getShaderSource(String name);
}
