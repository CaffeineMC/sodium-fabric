package net.caffeinemc.mods.sodium.desktop;

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
        String message = "This file is Sodium, meant to be installed as a mod. Do you want to download the Modrinth App to use Sodium?";
        String fallback = "This file is Sodium, meant to be installed as a mod. You can also get the Modrinth App from https://modrinth.com/app to use Sodium.";
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(fallback);
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // Ignored
            }

            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                int option = JOptionPane.showOptionDialog(null, message, "Sodium", JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, null, null, null);

                if (option == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().browse(URI.create("https://modrinth.com/app"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Fallback for Linux, etc users with no "default" browser
                JOptionPane.showMessageDialog(null, fallback);
            }
        }

        System.exit(0);
    }
}
