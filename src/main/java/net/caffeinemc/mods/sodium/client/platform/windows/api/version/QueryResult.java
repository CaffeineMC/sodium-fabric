package net.caffeinemc.mods.sodium.client.platform.windows.api.version;

public record QueryResult(
        /* The memory address which points to the start of the response payload */
        long address,

        /* The length of the response, in bytes. This is undefined when the query's result is a string,
        * as the value is null-terminated. */
        int length) {
}
