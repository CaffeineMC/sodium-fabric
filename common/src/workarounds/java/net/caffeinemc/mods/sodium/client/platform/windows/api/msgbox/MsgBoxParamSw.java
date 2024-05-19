package net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class MsgBoxParamSw extends Struct<MsgBoxParamSw> {
    public static final int SIZEOF;
    public static final int ALIGNOF;
    public static final int
            OFFSET_CB_SIZE,
            OFFSET_HWND_OWNER,
            OFFSET_HINSTANCE,
            OFFSET_LPSZ_TEXT,
            OFFSET_LPSZ_CAPTION,
            OFFSET_DW_STYLE,
            OFFSET_LPSZ_ICON,
            OFFSET_DW_CONTEXT_HELP_ID,
            OFFSET_LPFN_MSG_BOX_CALLBACK,
            OFFSET_DW_LANGUAGE_ID;

    static {
        Layout layout = __struct(
                __member(Integer.BYTES),         /* cbSize */
                __member(Pointer.POINTER_SIZE),  /* hwndOwner */
                __member(Pointer.POINTER_SIZE),  /* hInstance */
                __member(Pointer.POINTER_SIZE),  /* lpszText */
                __member(Pointer.POINTER_SIZE),  /* lpszCaption */
                __member(Integer.BYTES),         /* dwStyle */
                __member(Pointer.POINTER_SIZE),  /* lpszIcon */
                __member(Pointer.POINTER_SIZE),  /* dwContextHelpId */
                __member(Pointer.POINTER_SIZE),  /* lpfnMsgBoxCallback */
                __member(Integer.BYTES)          /* dwLangaugeId */
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_CB_SIZE = layout.offsetof(0);
        OFFSET_HWND_OWNER = layout.offsetof(1);
        OFFSET_HINSTANCE = layout.offsetof(2);
        OFFSET_LPSZ_TEXT = layout.offsetof(3);
        OFFSET_LPSZ_CAPTION = layout.offsetof(4);
        OFFSET_DW_STYLE = layout.offsetof(5);
        OFFSET_LPSZ_ICON = layout.offsetof(6);
        OFFSET_DW_CONTEXT_HELP_ID = layout.offsetof(7);
        OFFSET_LPFN_MSG_BOX_CALLBACK = layout.offsetof(8);
        OFFSET_DW_LANGUAGE_ID = layout.offsetof(9);
    }

    public static MsgBoxParamSw allocate(MemoryStack stack) {
        return new MsgBoxParamSw(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    private MsgBoxParamSw(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull MsgBoxParamSw create(long address, ByteBuffer container) {
        return new MsgBoxParamSw(address, container);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public void setCbSize(int size) {
        MemoryUtil.memPutInt(this.address + OFFSET_CB_SIZE, size);
    }

    public void setHWndOwner(long hWnd) {
        MemoryUtil.memPutAddress(this.address + OFFSET_HWND_OWNER, hWnd);
    }

    public void setText(ByteBuffer buffer) {
        MemoryUtil.memPutAddress(this.address + OFFSET_LPSZ_TEXT, MemoryUtil.memAddress(buffer));
    }

    public void setCaption(ByteBuffer buffer) {
        MemoryUtil.memPutAddress(this.address + OFFSET_LPSZ_CAPTION, MemoryUtil.memAddress(buffer));
    }

    public void setStyle(int style) {
        MemoryUtil.memPutInt(this.address + OFFSET_DW_STYLE, style);
    }

    public void setCallback(@Nullable MsgBoxCallbackI callback) {
        MemoryUtil.memPutAddress(this.address + OFFSET_LPFN_MSG_BOX_CALLBACK, callback == null ? MemoryUtil.NULL : callback.address());
    }
}
