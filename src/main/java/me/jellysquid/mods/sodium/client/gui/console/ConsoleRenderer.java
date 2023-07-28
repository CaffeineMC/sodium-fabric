package me.jellysquid.mods.sodium.client.gui.console;

import me.jellysquid.mods.sodium.client.gui.console.message.Message;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.LinkedList;

public class ConsoleRenderer {
    static final ConsoleRenderer INSTANCE = new ConsoleRenderer();

    private final LinkedList<ActiveMessage> activeMessages = new LinkedList<>();

    public void update(Console console, double currentTime) {
        this.purgeMessages(currentTime);
        this.pollMessages(console, currentTime);
    }

    private void purgeMessages(double currentTime) {
        this.activeMessages.removeIf(message ->
                currentTime > message.timestamp() + message.duration());
    }

    private void pollMessages(Console console, double currentTime) {
        var log = console.getMessageDrain();

        while (!log.isEmpty()) {
            this.activeMessages.add(ActiveMessage.create(log.poll(), currentTime));
        }
    }

    public void draw(DrawContext context) {
        var currentTime = GLFW.glfwGetTime();

        MinecraftClient client = MinecraftClient.getInstance();

        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0.0f, 0.0f, 1000.0f);

        int x = 4;
        int y = 4;

        var width = 270;
        var height = 10;

        var paddingWidth = 3;
        var paddingHeight = 2;

        for (ActiveMessage message : this.activeMessages) {
            double opacity = getMessageOpacity(message, currentTime);

            if (opacity < 0.025D) {
                continue;
            }

            var colors = COLORS.get(message.level());

            // message background
            context.fill(x, y, x + width + paddingWidth, y + height + paddingHeight,
                    ColorARGB.withAlpha(colors.background(), weightAlpha(0.9D, opacity)));

            // message colored stripe
            context.fill(x, y, x + 1, y + height + paddingHeight,
                    ColorARGB.withAlpha(colors.foreground(), weightAlpha(1.0D, opacity)));

            // message text
            context.drawText(client.textRenderer, message.text(), x + paddingWidth + 3, y + (paddingHeight / 2),
                    ColorARGB.withAlpha(colors.text(), weightAlpha(1.0D, opacity)), false);

            y += height + paddingHeight;
        }

        matrices.pop();
    }

    private static double getMessageOpacity(ActiveMessage message, double time) {
        double midpoint = message.timestamp() + (message.duration() / 2.0D);

        if (time > midpoint) {
            return getFadeOutOpacity(message, time);
        } else if (time < midpoint) {
            return getFadeInOpacity(message, time);
        } else {
            return 1.0D;
        }
    }

    private static double getFadeInOpacity(ActiveMessage message, double time) {
        var animationDuration = 0.25D;

        var animationStart = message.timestamp();
        var animationEnd = message.timestamp() + animationDuration;

        return getAnimationProgress(time, animationStart, animationEnd);
    }

    private static double getFadeOutOpacity(ActiveMessage message, double time) {
        // animation duration is 1/5th the message's duration, or 0.5 seconds, whichever is smaller
        var animationDuration = Math.min(0.5D, message.duration() * 0.20D);

        var animationStart = message.timestamp() + message.duration() - animationDuration;
        var animationEnd = message.timestamp() + message.duration();

        return 1.0D - getAnimationProgress(time, animationStart, animationEnd);
    }

    private static double getAnimationProgress(double currentTime, double startTime, double endTime) {
        return MathHelper.clamp(MathHelper.getLerpProgress(currentTime, startTime, endTime), 0.0D, 1.0D);
    }

    private static int weightAlpha(double value, double scale) {
        var alpha = (float) MathHelper.clamp(value * scale, 0.0D, 1.0D);
        return ColorU8.normalizedFloatToByte(alpha);
    }

    private record ActiveMessage(MessageLevel level, Text text, double duration, double timestamp) {

        public static ActiveMessage create(Message message, double timestamp) {
            var text = message.text()
                    .copy()
                    .styled((style) -> style.withFont(MinecraftClient.UNICODE_FONT_ID));

            return new ActiveMessage(message.level(), text, message.duration(), timestamp);
        }
    }

    private static final EnumMap<MessageLevel, ColorPalette> COLORS = new EnumMap<>(MessageLevel.class);

    static {
        COLORS.put(MessageLevel.INFO, new ColorPalette(
                ColorARGB.pack(255, 255, 255),
                ColorARGB.pack( 15,  15,  15),
                ColorARGB.pack( 15,  15,  15)
        ));

        COLORS.put(MessageLevel.WARN, new ColorPalette(
                ColorARGB.pack(224, 187,   0),
                ColorARGB.pack( 42,  35,   0),
                ColorARGB.pack(180, 150,   0)
        ));

        COLORS.put(MessageLevel.ERROR, new ColorPalette(
                ColorARGB.pack(200,   0,   0),
                ColorARGB.pack( 42,   0,   0),
                ColorARGB.pack(160,   0,   0)
        ));
    }

    private record ColorPalette(int text, int background, int foreground) {

    }
}
