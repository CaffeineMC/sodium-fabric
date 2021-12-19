package me.jellysquid.mods.sodium.render.entity.part;

import org.jetbrains.annotations.Nullable;

public interface BakeablePartBuilder {
    void setId(int id);
    int getId();

    void setParent(BakeablePartBuilder parent);
    @Nullable BakeablePartBuilder getParent();

    int getNextAvailableModelId();
}
