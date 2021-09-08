package me.jellysquid.mods.thingl.array;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL30C;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class VertexArrayImpl extends GlObject implements VertexArray {
    private VertexArrayImpl(RenderDeviceImpl device, int handle) {
        super(device);

        this.setHandle(handle);
    }

    public static VertexArrayImpl create(RenderDeviceImpl device, boolean dsa) {
        int handle;

        if (dsa) {
            handle = device.getDeviceFunctions()
                    .getDirectStateAccessFunctions()
                    .createVertexArrays();
        } else {
            handle = GL30C.glGenVertexArrays();
        }

        return new VertexArrayImpl(device, handle);
    }

    public void bind() {
        var tracker = this.device.getStateTracker();
        var handle = this.handle();

        if (tracker.makeVertexArrayActive(handle)) {
            GL30C.glBindVertexArray(handle);
        }
    }
}
