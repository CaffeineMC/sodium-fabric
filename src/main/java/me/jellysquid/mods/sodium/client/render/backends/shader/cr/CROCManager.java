package me.jellysquid.mods.sodium.client.render.backends.shader.cr;

import me.jellysquid.mods.sodium.client.render.backends.shader.lcb.ChunkRegion;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class CROCManager {

    private ArrayList<ChunkRegion> regionsToRender = new ArrayList<>();

    private static Vec3d getCameraPos() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
    }

    public static void onBlockRenderingStarted(
            ArrayList<ChunkRender<CRRenderState>> chunksToRender
    ) {

    }
}
