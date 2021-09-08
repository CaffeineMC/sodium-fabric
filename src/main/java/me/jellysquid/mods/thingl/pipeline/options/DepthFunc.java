package me.jellysquid.mods.thingl.pipeline.options;

import org.lwjgl.opengl.GL15C;

public enum DepthFunc {
    NEVER(GL15C.GL_NEVER),
    LESS(GL15C.GL_LESS),
    LESS_THAN_OR_EQUAL(GL15C.GL_LEQUAL),
    EQUAL(GL15C.GL_EQUAL),
    NOT_EQUAL(GL15C.GL_NOTEQUAL),
    GREATER(GL15C.GL_GREATER),
    GREATER_THAN_OR_EQUAL(GL15C.GL_GEQUAL),
    ALWAYS(GL15C.GL_ALWAYS);

    public final int id;

    DepthFunc(int id) {
        this.id = id;
    }
}
