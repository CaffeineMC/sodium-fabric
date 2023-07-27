package me.jellysquid.mods.sodium.client.render.chunk;

public enum ChunkUpdateType {
    TRANSLUCENT_SORT(false), // the other types include a sort
    INITIAL_BUILD(false),
    REBUILD(false),
    IMPORTANT_REBUILD(true);

    private final boolean important;

    ChunkUpdateType(boolean important) {
        this.important = important;
    }

    public boolean isImportant() {
        return this.important;
    }
}
