package net.caffeinemc.mods.sodium.client.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements the "Flawless Frames" FREX feature using which third-party mods can instruct Sodium to sacrifice
 * performance (even beyond the point where it can no longer achieve interactive frame rates) in exchange for
 * a noticeable boost to quality.
 * <p>
 * In Sodium's case, this means waiting for all chunks to be fully updated and ready for rendering before each frame.
 * <p>
 * See https://github.com/grondag/frex/pull/9
 */
public class FlawlessFrames {
    private static final Set<Object> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Function<String, Consumer<Boolean>> PROVIDER = name -> {
        Object token = new Object();
        return active -> {
            if (active) {
                ACTIVE.add(token);
            } else {
                ACTIVE.remove(token);
            }
        };
    };

    public static Function<String, Consumer<Boolean>> getProvider() {
        return PROVIDER;
    }

    public static boolean isActive() {
        return !ACTIVE.isEmpty();
    }
}
