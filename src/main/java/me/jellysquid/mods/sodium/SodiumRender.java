package me.jellysquid.mods.sodium;

import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.state.RecordingStateTracker;

public class SodiumRender {
    private static final RecordingStateTracker STATE_TRACKER = new RecordingStateTracker();

    public static final RenderDevice DEVICE = RenderDevice.create(STATE_TRACKER);

    public static boolean isDirectMemoryAccessEnabled() {
        return SodiumClient.options().advanced.allowDirectMemoryAccess;
    }

    public static void enterManagedCode() {
        STATE_TRACKER.push();
    }

    public static void exitManagedCode() {
        STATE_TRACKER.pop();
    }
}