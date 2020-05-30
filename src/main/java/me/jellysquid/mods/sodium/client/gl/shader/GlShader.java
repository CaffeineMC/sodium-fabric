package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * A compiled OpenGL shader object.
 */
public class GlShader extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

    private final Identifier name;
    private final List<String> defines;

    public GlShader(ShaderType type, Identifier name, String src, List<String> defines) {
        this.name = name;
        this.defines = defines;

        src = processShader(src, defines);

        int handle = GL20.glCreateShader(type.id);
        GL20.glShaderSource(handle, src);
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
    private static String processShader(String src, List<String> defines) {
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
                    writeDefines(builder, defines);

                    // We did our work, don't add them again
                    patched = true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not process shader source", e);
        }

        return builder.toString();
    }

    private static void writeDefines(StringBuilder builder, List<String> defines) {
        for (String define : defines) {
            builder.append("#define ").append(define).append("\n");
        }
    }

    public Identifier getName() {
        return this.name;
    }

    public void delete() {
        GL20.glDeleteShader(this.handle());

        this.invalidateHandle();
    }
}
