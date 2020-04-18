package me.jellysquid.mods.sodium.client.gl.shader;

import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class ShaderLoader {
    public static GlShader loadShader(ShaderType type, Identifier name) {
        return loadShader(type, name, Collections.emptyList());
    }

    public static GlShader loadShader(ShaderType type, Identifier name, List<String> defines) {
        return new GlShader(type, name, getShaderSource(getShaderPath(name)), defines);
    }

    private static String getShaderPath(Identifier name) {
        return String.format("/assets/%s/shaders/%s", name.getNamespace(), name.getPath());
    }

    private static String getShaderSource(String path) {
        try (InputStream in = ShaderLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Shader not found: " + path);
            }

            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read shader sources", e);
        }
    }
}
