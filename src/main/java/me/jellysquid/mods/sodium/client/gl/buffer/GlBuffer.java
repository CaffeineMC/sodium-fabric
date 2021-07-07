package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import org.lwjgl.opengl.GL20C;

public abstract class GlBuffer extends GlObject {
    public static final int NULL_BUFFER_ID = 0;

    protected GlBuffer() {
        this.setHandle(GL20C.glGenBuffers());
    }

}
