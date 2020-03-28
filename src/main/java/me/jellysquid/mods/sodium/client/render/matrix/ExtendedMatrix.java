package me.jellysquid.mods.sodium.client.render.matrix;

import net.minecraft.util.math.Quaternion;

public interface ExtendedMatrix {
    void rotate(Quaternion quaternion);

    void translate(float x, float y, float z);
}
