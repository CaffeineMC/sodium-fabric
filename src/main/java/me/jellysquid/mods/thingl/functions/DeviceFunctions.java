package me.jellysquid.mods.thingl.functions;

import me.jellysquid.mods.thingl.device.RenderDeviceImpl;

public class DeviceFunctions {
    private final BufferStorageFunctions bufferStorageFunctions;

    public DeviceFunctions(RenderDeviceImpl device) {
        this.bufferStorageFunctions = BufferStorageFunctions.pickBest(device);
    }

    public BufferStorageFunctions getBufferStorageFunctions() {
        return this.bufferStorageFunctions;
    }
}
