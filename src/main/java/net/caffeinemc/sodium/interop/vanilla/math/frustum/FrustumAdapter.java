package net.caffeinemc.sodium.interop.vanilla.math.frustum;

public interface FrustumAdapter {
    Frustum sodium$createFrustum();

    static Frustum adapt(net.minecraft.client.render.Frustum frustum) {
        return ((FrustumAdapter) frustum).sodium$createFrustum();
    }
}
