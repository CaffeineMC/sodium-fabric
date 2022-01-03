package me.jellysquid.mods.sodium.opengl.device;

public interface RenderDevice {
    RenderDevice INSTANCE = new GLRenderDevice();

    CommandList createCommandList();
}
