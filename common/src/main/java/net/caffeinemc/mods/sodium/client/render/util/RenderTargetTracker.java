package net.caffeinemc.mods.sodium.client.render.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

public class RenderTargetTracker {
    private static final ReferenceSet<RenderTarget> DIRTY_FRAMEBUFFERS = new ReferenceOpenHashSet<>();

    private static RenderTarget ACTIVE_WRITE_TARGET;

    public static void setActiveWriteTarget(RenderTarget rt) {
        ACTIVE_WRITE_TARGET = rt;
    }

    public static void notifyActiveWriteTargetModified() {
        RenderTarget rt = ACTIVE_WRITE_TARGET;

        if (rt != null) {
            markDirty(rt);
        }
    }

    public static void markDirty(RenderTarget rt) {
        DIRTY_FRAMEBUFFERS.add(rt);
    }

    public static boolean isDirty(RenderTarget rt) {
        return DIRTY_FRAMEBUFFERS.contains(rt);
    }

    public static void markClean(RenderTarget rt) {
        DIRTY_FRAMEBUFFERS.remove(rt);
    }
}
