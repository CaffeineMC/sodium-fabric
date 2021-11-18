package me.jellysquid.mods.sodium.render.entity.buffer;

import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

public class SectionedSyncObjects implements AutoCloseable {
    private final long[] syncObjects;

    private int currentSection = 0;

    public SectionedSyncObjects(int sectionCount) {
        this.syncObjects = new long[sectionCount]; // these are 0 (null) by default
    }

    public long getCurrentSyncObject() {
        return syncObjects[currentSection];
    }

    public void setCurrentSyncObject(long pSyncObject) {
        syncObjects[currentSection] = pSyncObject;
    }

    public void nextSection() {
        currentSection++;
        currentSection %= syncObjects.length;
    }

    @Override
    public void close() {
        for (long syncObject : syncObjects) {
            if (syncObject != MemoryUtil.NULL) {
                GL32C.glDeleteSync(syncObject);
            }
        }
    }
}
