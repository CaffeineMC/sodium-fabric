package net.caffeinemc.sodium.render.arena;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.MappedBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class UploadBatch {
    public final Buffer buffer;
    public final List<Entry> queue;

    public final int bytes;

    public UploadBatch(RenderDevice device, List<PendingUpload> uploads) {
        // A linked list is used as we'll be randomly removing elements and want O(1) performance
        var entries = new LinkedList<Entry>();
        var bytes = 0;

        for (var upload : uploads) {
            var entry = new Entry(upload.holder, bytes, upload.data.getLength());
            entries.add(entry);

            bytes += upload.data.getLength();
        }

        var buffer = device.createMappedBuffer(bytes, EnumSet.of(MappedBufferFlags.WRITE, MappedBufferFlags.CLIENT_STORAGE));
        var view = buffer.view();

        for (var upload : uploads) {
            view.put(upload.data.getDirectBuffer());
        }

        this.buffer = buffer;
        this.queue = entries;
        this.bytes = bytes;
    }

    protected record Entry(AtomicReference<BufferSegment> holder, int offset, int length) {

    }
}
