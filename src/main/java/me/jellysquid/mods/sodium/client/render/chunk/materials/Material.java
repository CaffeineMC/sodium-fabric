package me.jellysquid.mods.sodium.client.render.chunk.materials;

import me.jellysquid.mods.sodium.client.render.chunk.passes.RenderPass;

public class Material {
    public final RenderPass pass;
    public final byte packed;

    public final AlphaCutoffParameter alphaCutoff;
    public final boolean mipped;

    public Material(RenderPass pass, AlphaCutoffParameter alphaCutoff, boolean mipped) {
        this.pass = pass;
        this.packed = MaterialParameters.pack(alphaCutoff, mipped);

        this.alphaCutoff = alphaCutoff;
        this.mipped = mipped;
    }

    public byte bits() {
        return this.packed;
    }
}
