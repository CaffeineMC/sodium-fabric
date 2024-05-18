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

    public static VersionInfo allocate(int len) {
        return new VersionInfo(MemoryUtil.memAlignedAlloc(16, len));
    }

    public @Nullable String queryValue(String key, LanguageCodePage translation) {
        var result = Version.query(this.pBlock, getStringFileInfoPath(key, translation));

        if (result == null) {
            return null;
        }

        return MemoryUtil.memUTF16(result.address());
    }

    public @Nullable VersionFixedFileInfoStruct queryFixedFileInfo() {
        var result = Version.query(this.pBlock, "\\");

        if (result == null) {
            return null;
        }

        return VersionFixedFileInfoStruct.from(result.address());
    }

    public @Nullable LanguageCodePage queryEnglishTranslation() {
        var result = Version.query(this.pBlock, "\\VarFileInfo\\Translation");

        if (result == null) {
            return null;
        }

        return findEnglishTranslationEntry(result);
    }

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

    private static String getStringFileInfoPath(String key, LanguageCodePage translation) {
        return String.format(Locale.ROOT, "\\StringFileInfo\\%04x%04x\\%s", translation.languageId(), translation.codePage(), key);
    }

    @Override
    public void close() {
        MemoryUtil.memAlignedFree(this.pBlock);
    }

    long address() {
        return MemoryUtil.memAddress(this.pBlock);
    }
}
