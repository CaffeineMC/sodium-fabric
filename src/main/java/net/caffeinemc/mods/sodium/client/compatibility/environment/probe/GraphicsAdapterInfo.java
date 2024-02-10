package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

public record GraphicsAdapterInfo(
        /* The identified vendor of the graphics adapter. */
        GraphicsAdapterVendor vendor,
        /* The name of the graphics adapter, if it is known. */
        String name,
        /* The driver version for the graphics adapter, if it is known. */
        String version
) {

}
