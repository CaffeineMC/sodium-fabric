package me.jellysquid.mods.sodium.render.shader;

import me.jellysquid.mods.thingl.device.CommandList;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.shader.GlShader;
import me.jellysquid.mods.thingl.shader.ShaderConstants;
import me.jellysquid.mods.thingl.shader.ShaderType;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ShaderLoader {
    private final RenderDevice device;

    public ShaderLoader(RenderDevice device) {
        this.device = device;
    }

    /**
     * Creates an OpenGL shader from GLSL sources. The GLSL source file should be made available on the classpath at the
     * path of `/assets/{namespace}/shaders/{path}`. User defines can be used to declare variables in the shader source
     * after the version header, allowing for conditional compilation with macro code.
     *
     * @param type The type of shader to create
     * @param name The identifier used to locate the shader source file
     * @param constants A list of constants for shader specialization
     * @return An OpenGL shader object compiled with the given user defines
     */
    public GlShader loadShader(ShaderType type, Identifier name, ShaderConstants constants) {
        try (CommandList commandList = this.device.createCommandList()) {
            return commandList.createShader(type, this.parseShader(name, constants));
        }
    }

    public String parseShader(Identifier name, ShaderConstants constants) {
        return ShaderParser.parseShader(this, getShaderSource(name), constants);
    }

    private static String getShaderSource(Identifier name) {
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
}
