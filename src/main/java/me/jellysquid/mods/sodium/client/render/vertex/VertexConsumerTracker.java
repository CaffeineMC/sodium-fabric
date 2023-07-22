package me.jellysquid.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.render.VertexConsumer;

public class VertexConsumerTracker {
    private static final ReferenceSet<Class<? extends VertexConsumer>> BAD_CONSUMERS = new ReferenceOpenHashSet<>();

    public static void logBadConsumer(VertexConsumer consumer) {
        if(BAD_CONSUMERS.add(consumer.getClass())) {
            SodiumClientMod.logger().warn("Some Sodium optimizations are being bypassed to prevent crashes with a mod that is implementing " + consumer.getClass().getName());
        }
    }
}
