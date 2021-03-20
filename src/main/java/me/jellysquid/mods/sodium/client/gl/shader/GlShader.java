package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * A compiled OpenGL shader object.
 */
public class GlShader extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

    private final Identifier name;

    public GlShader(ShaderType type, Identifier name, String src, ShaderConstants constants) {
        this.name = name;

        src = processShader(src, constants);

        int handle = GL20.glCreateShader(type.id);
        ShaderWorkarounds.safeShaderSource(handle, src);
        GL20.glCompileShader(handle);

        String log = GL20.glGetShaderInfoLog(handle);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log for " + this.name + ": " + log);
        }

        int result = GL20.glGetShaderi(handle, GL20.GL_COMPILE_STATUS);

        if (result != GL20.GL_TRUE) {
            throw new RuntimeException("Shader compilation failed, see log for details");
        }

        this.setHandle(handle);
    }

    /**
     * Adds an additional list of defines to the top of a GLSL shader file just after the version declaration. This
     * allows for ghetto shader specialization.
     */
    private static String processShader(String src, ShaderConstants constants) {
        StringBuilder builder = new StringBuilder(src.length());
        boolean patched = false;

        try (BufferedReader reader = new BufferedReader(new StringReader(src))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Write the line out to the patched GLSL code string
                builder.append(line).append("\n");

                // Now, see if the line we just wrote declares the version
                // If we haven't already added our define declarations, add them just after the version declaration
                if (!patched && line.startsWith("#version")) {
                    for (String macro : constants.getDefineStrings()) {
                        builder.append(macro).append('\n');
                    }

                    // We did our work, don't add them again
                    patched = true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not process shader source", e);
        }

        return builder.toString();
    }

    public Identifier getName() {
        return this.name;
    }

    public void delete() {
        GL20.glDeleteShader(this.handle());

        this.invalidateHandle();
    }
}
