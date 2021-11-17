package me.jellysquid.mods.sodium.render.entity.buffer;

import java.util.concurrent.atomic.AtomicLong;

// TODO: abstract this but still use long for getPointer
public class SectionedPersistentBuffer {
    private final long pointer;
    private final int name;
    private final int sectionCount;
    private final long sectionSize;

    private int currentSection = 0;
    private long sectionOffset = 0;
    private final AtomicLong positionOffset = new AtomicLong();

    public SectionedPersistentBuffer(long pointer, int name, int sectionCount, long sectionSize) {
        this.pointer = pointer;
        this.name = name;
        this.sectionCount = sectionCount;
        this.sectionSize = sectionSize;
    }

    public long getSectionedPointer() {
        return pointer + sectionOffset;
    }

    public int getName() {
        return name;
    }

    public long getSectionSize() {
        return sectionSize;
    }

    public int getCurrentSection() {
        return currentSection;
    }

    public void nextSection() {
        currentSection++;
        currentSection %= sectionCount;
        sectionOffset = getCurrentSection() * getSectionSize();
        positionOffset.setRelease(0);
    }

    public AtomicLong getPositionOffset() {
        return positionOffset;
    }

}
