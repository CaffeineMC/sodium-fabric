package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import org.lwjgl.opengl.GL20;
import net.minecraft.util.Identifier;

/**
 * A compositing program for flattening weighted, blended OIT
 */
public class TranslucencyProgram extends GlProgram {
    private final int uAccumTex;
    private final int uRevealTex;

    public TranslucencyProgram(Identifier name, int handle) {
        super(name, handle);
        this.uAccumTex = this.getUniformLocation("u_AccumTex");
        this.uRevealTex = this.getUniformLocation("u_RevealTex");
    }

    public void setup() {
        GL20.glUniform1i(this.uAccumTex, 3);
        GL20.glUniform1i(this.uRevealTex, 4);
    }
}
