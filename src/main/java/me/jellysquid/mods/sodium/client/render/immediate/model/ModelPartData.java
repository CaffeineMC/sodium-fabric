package me.jellysquid.mods.sodium.client.render.immediate.model;

import net.minecraft.client.model.geom.ModelPart;

public interface ModelPartData {
    static ModelPartData from(ModelPart child) {
        return (ModelPartData) (Object) child;
    }

    ModelCuboid[] getCuboids();
    ModelPart[] getChildren();

    boolean isVisible();
    boolean isHidden();
}
