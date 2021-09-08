package me.jellysquid.mods.thingl.array;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL30C;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public GlVertexArray(RenderDeviceImpl device) {
        super(device);
        this.setHandle(GL30C.glGenVertexArrays());
    }

    public void bind() {
        var tracker = this.device.getStateTracker();
        var handle = this.handle();

        if (tracker.makeVertexArrayActive(handle)) {
            GL30C.glBindVertexArray(handle);
        }
    }
}
