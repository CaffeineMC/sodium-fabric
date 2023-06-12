package me.jellysquid.mods.sodium.client.render.chunk.graph;

import net.minecraft.util.math.Direction;

public enum GraphDirection {
    NegX,
    NegY,
    NegZ,
    PosX,
    PosY,
    PosZ;

    public static GraphDirection from(Direction direction) {
        return switch (direction) {
            case DOWN -> NegY;
            case UP -> PosY;
            case NORTH -> NegZ;
            case SOUTH -> PosZ;
            case WEST -> NegX;
            case EAST -> PosX;
        };
    }
}
