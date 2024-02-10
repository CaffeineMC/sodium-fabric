package net.caffeinemc.mods.sodium.client.gui.console.message;

/**
 * Used for indicating how important a message is. The level can affect the rendering of messages, with higher
 * levels indicating greater severity.
 */
public enum MessageLevel {
    /**
     * The message is purely informational and does not convey a need for action. Messages of this type are rendered
     * in neutral colors as to not be distracting.
     */
    INFO,

    /**
     * A problem has occurred, and the user should know about it. This is useful for indicating that something needs
     * attention, but isn't critical to the functioning of the game. Messages of this type will be rendered with
     * additional colors to signal their importance.
     */
    WARN,

    /**
     * A severe problem has occurred, and the user needs to be alerted to it. This should only be used in situations
     * where the immediate functioning of the game is at risk. Messages of this type will be rendered much
     * more intensely.
     */
    SEVERE
}
