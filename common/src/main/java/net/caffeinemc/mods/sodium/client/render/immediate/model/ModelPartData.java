package net.caffeinemc.mods.sodium.client.render.immediate.model;

import net.minecraft.client.model.geom.ModelPart;

public interface ModelPartData {
    static ModelPartData from(ModelPart part) {
        return (ModelPartData) (Object) part;
    }

    ModelCuboid[] getCuboids();
    ModelPart[] getChildren();

    boolean isVisible();
    boolean isHidden();
}
