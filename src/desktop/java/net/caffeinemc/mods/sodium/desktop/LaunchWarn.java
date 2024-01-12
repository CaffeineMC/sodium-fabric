package net.caffeinemc.mods.sodium.desktop;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URI;

/**
 * Taken from
 * https://github.com/IrisShaders/Iris/blob/6c880cd377d97ffd5de648ba4dfac7ea88897b4f/src/main/java/net/coderbot/iris/LaunchWarn.java
 * and modified to fit Sodium. See Iris' license for more information.
 */
public class LaunchWarn {
    public static void main(String[] args) {
        String message = "You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. You must install Fabric Loader for Minecraft, and place this file in your mods directory instead.\nIf this is your first time installing mods for Fabric Loader, click \"Help\" for a guide on how to do this.";
        String fallback = "You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. You must install Fabric Loader for Minecraft, and place this file in your mods directory instead.\nIf this is your first time installing mods for Fabric Loader, open \"https://github.com/CaffeineMC/sodium-fabric/wiki/Installation\" for a guide on how to do this.";
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(fallback);
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // Ignored
            }

			ImageIcon icon = new ImageIcon(LaunchWarn.class.getResource("/assets/sodium/icon.png"), "Sodium");

            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                int option = JOptionPane.showOptionDialog(null, message, "Sodium", JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, icon, new Object[] { "Help", "Cancel" }, JOptionPane.YES_OPTION);

                if (option == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://github.com/CaffeineMC/sodium-fabric/wiki/Installation"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Fallback for Linux, etc users with no "default" browser
                JOptionPane.showMessageDialog(null, fallback, "Sodium", JOptionPane.INFORMATION_MESSAGE, icon);
            }
        }

        System.exit(0);
    }
}
