package net.caffeinemc.mods.sodium.client.platform.windows.api.version;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Locale;

public class VersionInfo implements Closeable {
    private final ByteBuffer pBlock;

    VersionInfo(ByteBuffer buffer) {
        this.pBlock = buffer;
    }

    /**
     * Allocates the storage for the data returned by <a href="https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-getfileversioninfow">GetFileVersionInfoW</a>.
     * The {@param len} parameter should be determined from <a href="https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-getfileversioninfosizew">GetFileVersionInfoSizeW</a>
     * @param len The size of the structure, in bytes
     * @return A wrapper object around the off-heap memory
     */
    public static VersionInfo allocate(int len) {
        return new VersionInfo(MemoryUtil.memAlignedAlloc(16, len));
    }

    /**
     * Queries the value for the given key using the provided translation.
     * @param key The name of the key to query
     * @param translation The translation to query the key under
     * @return The string value associated with the key, or null if it doesn't exist
     */
    public @Nullable String queryValue(String key, LanguageCodePage translation) {
        var result = Version.query(this.pBlock, getStringFileInfoPath(key, translation));

        if (result == null) {
            return null;
        }

        return MemoryUtil.memUTF16(result.address());
    }

    /**
     * Queries the supported translations for this module, and returns the best match for an "English" code-page. If the
     * exact match is not found, the first code page is returned (if it exists.)
     */
    public @Nullable LanguageCodePage queryEnglishTranslation() {
        var result = Version.query(this.pBlock, "\\VarFileInfo\\Translation");

        if (result == null) {
            return null;
        }

        return findEnglishTranslationEntry(result);
    }

    /**
     * Searches the array of provided translations for an "English" code-page. If the exact match is not found,
     * the first code page is returned (if it exists.)
     * @param result The result of a query for <pre>\VarFileInfo\Translation</pre>
     * @return The code page which best matches, or null if no code pages exist
     */
    private static @Nullable LanguageCodePage findEnglishTranslationEntry(final QueryResult result) {
        LanguageCodePage translation = null;

        int offset = 0;

        while (offset < result.length()) {
            translation = LanguageCodePage.decode(result.address() + offset);
            offset += LanguageCodePage.STRIDE;

            if (translation.codePage() == 1200 /* UTF-16LE */ && translation.languageId() == 1033 /* English, United States */) {
                return translation;
            }
        }

        return translation;
    }

    /**
     * Creates a query string for the given key under the given code page. This can then be used
     * @param key The name of the key
     * @param translation The code page to access the key from
     * @return A fully-formed query string
     */
    private static String getStringFileInfoPath(String key, LanguageCodePage translation) {
        return String.format(Locale.ROOT, "\\StringFileInfo\\%04x%04x\\%s", translation.languageId(), translation.codePage(), key);
    }

    /**
     * Frees the memory allocated by this structure.
     *
     * <p>SAFETY: It is not safe to call this function multiple times, as it will cause a double-free.</p>
     */
    @Override
    public void close() {
        MemoryUtil.memAlignedFree(this.pBlock);
    }

    /**
     * @return A memory address which points to the data held by this wrapper
     */
    long address() {
        return MemoryUtil.memAddress(this.pBlock);
    }
}
