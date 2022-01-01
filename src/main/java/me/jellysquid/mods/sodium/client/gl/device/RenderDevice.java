package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.functions.DeviceFunctions;
import me.jellysquid.mods.sodium.client.gl.state.GlStateTracker;
import org.lwjgl.opengl.GLCapabilities;

public interface RenderDevice {
    RenderDevice INSTANCE = new GLRenderDevice();

    CommandList createCommandList();

    static void enterManagedCode() {
        RenderDevice.INSTANCE.makeActive();
    }

    static void exitManagedCode() {
        RenderDevice.INSTANCE.makeInactive();
    }

    void makeActive();
    void makeInactive();

    GlStateTracker getStateTracker();

    GLCapabilities getCapabilities();

    DeviceFunctions getDeviceFunctions();
}
