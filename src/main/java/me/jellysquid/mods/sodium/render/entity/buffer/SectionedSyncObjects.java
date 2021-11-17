package me.jellysquid.mods.sodium.render.entity.buffer;

public class SectionedSyncObjects {
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
}
