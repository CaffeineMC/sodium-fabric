package net.caffeinemc.mods.sodium.client.platform.windows.api.version;

import org.lwjgl.system.MemoryUtil;

public record LanguageCodePage(int languageId, int codePage) {
    static final int STRIDE = Integer.BYTES;

    /**
     * <p>Decodes the <a href="https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-verqueryvaluea">
     *     LANGANDCODEPAGE struct</a> from the given address in memory and copies the value into a managed object.</p>
     *
     * <p>SAFETY: {@param address} must be a valid aligned pointer to the structure.</p>
     *
     * @param address The memory address to read from
     * @return The decoded structure
     */
    static LanguageCodePage decode(long address) {
        var value = MemoryUtil.memGetInt(address);

        int languageId = value & 0xFFFF;
        int codePage = (value & 0xFFFF0000) >> 16;

        return new LanguageCodePage(languageId, codePage);
    }
}