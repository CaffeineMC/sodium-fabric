package me.jellysquid.mods.sodium.client.gl.device;

public interface RenderDevice {
    RenderDevice INSTANCE = new GLRenderDevice();

    CommandList createCommandList();
}
