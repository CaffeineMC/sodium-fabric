package net.caffeinemc.gfx.opengl.texture;

import net.caffeinemc.gfx.api.texture.Texture;
import net.caffeinemc.gfx.opengl.GlObject;
import org.lwjgl.opengl.GL45C;

public class GlTexture extends GlObject implements Texture {
    // TODO: add constructor which creates a texture and sets its parameters

    public GlTexture(int name) {
        this.setHandle(name);
    }

    public static GlTexture wrap(int name) {
        if (!GL45C.glIsTexture(name)) {
            throw new IllegalArgumentException("Handle provided is not an OpenGL texture name.");
        }
        return new GlTexture(name);
    }

    public static int getHandle(Texture texture) {
        return ((GlTexture) texture).getHandle();
    }
}
