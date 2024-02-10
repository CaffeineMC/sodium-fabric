package net.caffeinemc.mods.sodium.client.platform;

import net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox.MsgBoxParamSw;
import net.minecraft.Util;
import net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox.MsgBoxCallback;
import net.caffeinemc.mods.sodium.client.platform.windows.api.User32;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.platform.Window;
import java.nio.ByteBuffer;
import java.util.Objects;

public class MessageBox {
    private static final @Nullable MessageBoxImpl IMPL = MessageBoxImpl.chooseImpl();

    /**
     * <p>Opens a message box using the appropriate platform backend. If no backend is available, this function does
     * nothing and immediately returns. The message block will prevent interaction with the parent window, if
     * specified.</p>
     *
     * <p>If {@param helpUrl} is specified, the message box will include a "Help" button. Clicking the button will
     * open the user's default web browser and close the message box.</p>
     *
     * <p>NOTE: This function does not return until the message box has been closed by the user.</p>
     *
     * @param window The parent window of the message box
     * @param icon The icon type to be used for the message box
     * @param title The title of the message box
     * @param contents The contents of the message box
     * @param helpUrl The URL to be opened when clicking the "Help" button
     */
    public static void showMessageBox(@Nullable Window window,
                                      IconType icon, String title,
                                      String contents,
                                      @Nullable String helpUrl)
    {
        if (IMPL != null) {
            IMPL.showMessageBox(window, icon, title, contents, helpUrl);
        }
    }

    private interface MessageBoxImpl {
        static @Nullable MessageBoxImpl chooseImpl() {
            if (Util.getPlatform() == Util.OS.WINDOWS) {
                return new WindowsMessageBoxImpl();
            }

            // TODO: Provide an implementation on other platforms
            return null;
        }

        void showMessageBox(@Nullable Window window,
                            IconType icon, String title,
                            String contents,
                            @Nullable String helpUrl);
    }

    private static class WindowsMessageBoxImpl implements MessageBoxImpl {
        @Override
        public void showMessageBox(@Nullable Window window,
                                   IconType icon, String title,
                                   String contents,
                                   @Nullable String helpUrl) {
            Objects.requireNonNull(title);
            Objects.requireNonNull(contents);
            Objects.requireNonNull(icon);

            final MsgBoxCallback msgBoxCallback;

            if (helpUrl != null) {
                msgBoxCallback = MsgBoxCallback.create(lpHelpInfo -> {
                    Util.getPlatform()
                            .openUri(helpUrl);
                });
            } else {
                msgBoxCallback = null;
            }

            final long hWndOwner;

            if (window != null) {
                hWndOwner = GLFWNativeWin32.glfwGetWin32Window(window.getWindow());
            } else {
                hWndOwner = MemoryUtil.NULL;
            }

            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer lpText = stack.malloc(MemoryUtil.memLengthUTF16(contents, true));
                MemoryUtil.memUTF16(contents, true, lpText);

                ByteBuffer lpCaption = stack.malloc(MemoryUtil.memLengthUTF16(title, true));
                MemoryUtil.memUTF16(title, true, lpCaption);

                var params = MsgBoxParamSw.allocate(stack);
                params.setCbSize(MsgBoxParamSw.SIZEOF);
                params.setHWndOwner(hWndOwner);
                params.setText(lpText);
                params.setCaption(lpCaption);
                params.setStyle(getStyle(icon, msgBoxCallback != null));
                params.setCallback(msgBoxCallback);

                User32.callMessageBoxIndirectW(params);
            } finally {
                if (msgBoxCallback != null) {
                    msgBoxCallback.free();
                }
            }
        }

        private static int getStyle(IconType icon, boolean showHelp) {
            int style = switch (icon) {
                case INFO -> 0x00000040; /* MB_ICONINFORMATION */
                case WARNING -> 0x00000030; /* MB_ICONWARNING */
                case ERROR -> 0x00000010; /* MB_ICONERROR */
            };

            if (showHelp) {
                style |= 0x00004000 /* MB_HELP */;
            }

            return style;
        }

    }

    public enum IconType {
        INFO,
        WARNING,
        ERROR
    }
}
