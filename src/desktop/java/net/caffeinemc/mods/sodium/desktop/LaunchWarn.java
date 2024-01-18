package net.caffeinemc.mods.sodium.desktop;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
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
    private static final String html_message = "<html><body><p style='width:600px;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. You must install Fabric Loader for Minecraft, and place this file in your mods directory instead." +
    "\nIf this is your first time installing mods for Fabric Loader, click \"Help\" for a guide on how to do this.</p></body></html>";
    private static final String html_fallback = "<html><body><p style='width:600px;'>You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. You must install Fabric Loader for Minecraft, and place this file in your mods directory instead." +
    "\nIf this is your first time installing mods for Fabric Loader, open \"https://github.com/CaffeineMC/sodium-fabric/wiki/Installation\" for a guide on how to do this.</p></body></html>";
    private static final String fallback = "You have tried to launch Sodium (a Minecraft mod) directly, but it is not an executable program or mod installer. You must install Fabric Loader for Minecraft, and place this file in your mods directory instead." +
    "\nIf this is your first time installing mods for Fabric Loader, open \"https://github.com/CaffeineMC/sodium-fabric/wiki/Installation\" for a guide on how to do this.";

    public static void main(String[] args) {

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(LaunchWarn.fallback);
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignored) {
                // Ignored
            }

            ImageIcon icon = new ImageIcon(LaunchWarn.class.getResource("/assets/sodium/icon.png"), "Sodium");

            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                int option = LaunchWarn.createOptionPane(LaunchWarn.html_message, "Sodium", JOptionPane.YES_NO_OPTION,
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
                LaunchWarn.createOptionPane(LaunchWarn.html_fallback, "Sodium", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, icon, null, null);
            }
        }

        System.exit(0);
    }

    private static int createOptionPane(String msg,
                                        String title,
                                        int messageType,
                                        int optionType,
                                        ImageIcon icon,
                                        Object[] options,
                                        Object initialValue) {
        JOptionPane pane = new JOptionPane(msg, messageType, optionType, icon, options, initialValue);
        JDialog dialog = pane.createDialog(title);
        dialog.setIconImage(icon.getImage());
        dialog.setVisible(true);
        Object selectedValue = pane.getValue();
        if(selectedValue == null)
            return JOptionPane.CLOSED_OPTION;
        //If there is not an array of option buttons:
        if(options == null) {
            if(selectedValue instanceof Integer)
                return ((Integer)selectedValue).intValue();
            return JOptionPane.CLOSED_OPTION;
        }
        //If there is an array of option buttons:
        for(int counter = 0, maxCounter = options.length;
            counter < maxCounter; counter++) {
            if(options[counter].equals(selectedValue))
            return counter;
        }
        return JOptionPane.CLOSED_OPTION;
    }
}
