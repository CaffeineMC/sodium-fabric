package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

public abstract class RenderBatch {
    private boolean building;

    public final void begin() {
        if (this.building) {
            throw new IllegalStateException("Already building");
        }

        this.building = true;

        this.startBatches();
    }

    public final void draw() {
        if (!this.building) {
            throw new IllegalStateException("Not building");
        }

        this.building = false;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        this.drawBatches();
    }

    protected abstract void startBatches();

    protected abstract void drawBatches();

    public boolean isBuilding() {
        return this.building;
    }
}
