package me.jellysquid.mods.sodium.client.gl.functions;

import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

public class DeviceFunctions {
    private final BufferStorageFunctions bufferStorageFunctions;

    public DeviceFunctions(RenderDevice device) {
        this.bufferStorageFunctions = BufferStorageFunctions.pickBest(device);
    }

    public BufferStorageFunctions getBufferStorageFunctions() {
        return bufferStorageFunctions;
    }
}
