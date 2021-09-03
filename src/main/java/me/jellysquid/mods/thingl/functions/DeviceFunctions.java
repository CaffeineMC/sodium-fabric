package me.jellysquid.mods.thingl.functions;

import me.jellysquid.mods.thingl.device.RenderDevice;

public class DeviceFunctions {
    private final BufferStorageFunctions bufferStorageFunctions;

    public DeviceFunctions(RenderDevice device) {
        this.bufferStorageFunctions = BufferStorageFunctions.pickBest(device);
    }

    public BufferStorageFunctions getBufferStorageFunctions() {
        return bufferStorageFunctions;
    }
}
