package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;

import java.util.ServiceLoader;

public class Services {
    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform.
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        SodiumClientMod.logger().debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
