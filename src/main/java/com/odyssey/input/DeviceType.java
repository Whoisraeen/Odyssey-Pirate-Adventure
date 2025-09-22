package com.odyssey.input;

/**
 * Enumeration of supported input device types in the Odyssey Pirate Adventure.
 * 
 * This enum is used to track which type of input device was last used,
 * enabling the game to provide appropriate UI hints and handle device-specific
 * behavior such as hot-swapping between keyboard/mouse and gamepad controls.
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since Java 25
 */
public enum DeviceType {
    
    /**
     * Keyboard and mouse input devices.
     * This includes all keyboard keys and mouse buttons/movement.
     */
    KEYBOARD("keyboard", "Keyboard & Mouse", true),
    
    /**
     * Gamepad/controller input devices.
     * This includes Xbox controllers, PlayStation controllers, Switch Pro controllers,
     * and other SDL2-compatible game controllers.
     */
    GAMEPAD("gamepad", "Gamepad", false);
    
    private final String id;
    private final String displayName;
    private final boolean requiresCursor;
    
    /**
     * Constructs a DeviceType with the specified properties.
     * 
     * @param id The unique identifier for this device type
     * @param displayName The human-readable name for this device type
     * @param requiresCursor Whether this device type requires a visible cursor
     */
    DeviceType(String id, String displayName, boolean requiresCursor) {
        this.id = id;
        this.displayName = displayName;
        this.requiresCursor = requiresCursor;
    }
    
    /**
     * Gets the unique identifier for this device type.
     * 
     * @return The device type identifier
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the human-readable display name for this device type.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Determines if this device type requires a visible cursor.
     * 
     * @return true if a cursor should be visible, false otherwise
     */
    public boolean requiresCursor() {
        return requiresCursor;
    }
    
    /**
     * Finds a DeviceType by its identifier.
     * 
     * @param id The identifier to search for
     * @return The matching DeviceType, or null if not found
     */
    public static DeviceType fromId(String id) {
        for (DeviceType deviceType : values()) {
            if (deviceType.id.equals(id)) {
                return deviceType;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}