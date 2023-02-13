package me.jellysquid.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormat;

import java.util.Map;
import java.util.concurrent.locks.StampedLock;

public class VertexFormatRegistryImpl implements VertexFormatRegistry {
    private final Map<VertexFormat, VertexFormatDescriptionImpl> descriptions = new Reference2ReferenceOpenHashMap<>();

    private final StampedLock lock = new StampedLock();

    @Override
    public VertexFormatDescription get(VertexFormat format) {
        VertexFormatDescription desc = this.findExisting(format);

        if (desc == null) {
            desc = this.create(format);
        }

        return desc;
    }

    private VertexFormatDescription findExisting(VertexFormat format) {
        var stamp = this.lock.readLock();

        try {
            return this.descriptions.get(format);
        } finally {
            this.lock.unlockRead(stamp);
        }
    }

    private VertexFormatDescription create(VertexFormat format) {
        var stamp = this.lock.writeLock();

        var id = this.descriptions.size();
        var desc = new VertexFormatDescriptionImpl(format, id);

        try {
            this.descriptions.put(format, desc);
        } finally {
            this.lock.unlockWrite(stamp);
        }

        return desc;
    }
}
