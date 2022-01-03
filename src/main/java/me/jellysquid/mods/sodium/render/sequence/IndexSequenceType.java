package me.jellysquid.mods.sodium.render.sequence;

import net.minecraft.client.render.VertexFormat;

public enum IndexSequenceType {
    QUADS(SequenceBuilder.QUADS),
    LINES(SequenceBuilder.LINES),
    NONE(SequenceBuilder.NONE);

    public final SequenceBuilder builder;

    IndexSequenceType(SequenceBuilder builder) {
        this.builder = builder;
    }

    public static IndexSequenceType map(VertexFormat.DrawMode drawMode) {
        if (drawMode == VertexFormat.DrawMode.QUADS) {
            return QUADS;
        } else if (drawMode == VertexFormat.DrawMode.LINES) {
            return LINES;
        } else {
            return NONE;
        }
    }
}
