package me.jellysquid.mods.sodium.render.sequence;

import com.mojang.blaze3d.vertex.VertexFormat;

public enum IndexSequenceType {
    QUADS(SequenceBuilder.QUADS),
    LINES(SequenceBuilder.LINES),
    NONE(SequenceBuilder.NONE);

    public final SequenceBuilder builder;

    IndexSequenceType(SequenceBuilder builder) {
        this.builder = builder;
    }

    public static IndexSequenceType map(VertexFormat.Mode drawMode) {
        if (drawMode == VertexFormat.Mode.QUADS) {
            return QUADS;
        } else if (drawMode == VertexFormat.Mode.LINES) {
            return LINES;
        } else {
            return NONE;
        }
    }
}
