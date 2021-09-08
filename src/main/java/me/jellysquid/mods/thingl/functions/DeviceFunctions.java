package me.jellysquid.mods.thingl.functions;

import me.jellysquid.mods.thingl.device.RenderDeviceImpl;

public class DeviceFunctions {
    private final BufferStorageFunctions bufferStorageFunctions;
    private final DirectStateAccessFunctions directStateAccessFunctions;

    public DeviceFunctions(RenderDeviceImpl device) {
        this.bufferStorageFunctions = BufferStorageFunctions.pickBest(device);
        this.directStateAccessFunctions = DirectStateAccessFunctions.pickBest(device);
    }

    public BufferStorageFunctions getBufferStorageFunctions() {
        return this.bufferStorageFunctions;
    }

    public DirectStateAccessFunctions getDirectStateAccessFunctions() {
        return this.directStateAccessFunctions;
    }
}
