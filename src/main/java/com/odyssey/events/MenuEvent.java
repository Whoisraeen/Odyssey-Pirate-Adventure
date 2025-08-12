package com.odyssey.events;

import java.util.Map;

/**
 * Events related to menu operations and navigation.
 */
public abstract class MenuEvent extends Event {
    
    /**
     * Fired when a menu is registered
     */
    public static class MenuRegistered extends MenuEvent {
        private final String menuId;
        
        public MenuRegistered(String menuId) {
            this.menuId = menuId;
        }
        
        public String getMenuId() { return menuId; }
    }
    
    /**
     * Fired when a menu is unregistered
     */
    public static class MenuUnregistered extends MenuEvent {
        private final String menuId;
        
        public MenuUnregistered(String menuId) {
            this.menuId = menuId;
        }
        
        public String getMenuId() { return menuId; }
    }
    
    /**
     * Fired before a menu opens (cancellable)
     */
    public static class PreOpen extends CancellableEvent {
        private final String menuId;
        private final Map<String, Object> context;
        
        public PreOpen(String menuId, Map<String, Object> context) {
            this.menuId = menuId;
            this.context = context;
        }
        
        public String getMenuId() { return menuId; }
        public Map<String, Object> getContext() { return context; }
    }
    
    /**
     * Fired when a menu opens
     */
    public static class Opened extends MenuEvent {
        private final String menuId;
        private final Map<String, Object> context;
        
        public Opened(String menuId, Map<String, Object> context) {
            this.menuId = menuId;
            this.context = context;
        }
        
        public String getMenuId() { return menuId; }
        public Map<String, Object> getContext() { return context; }
    }
    
    /**
     * Fired before a menu closes (cancellable)
     */
    public static class PreClose extends CancellableEvent {
        private final String menuId;
        
        public PreClose(String menuId) {
            this.menuId = menuId;
        }
        
        public String getMenuId() { return menuId; }
    }
    
    /**
     * Fired when a menu closes
     */
    public static class Closed extends MenuEvent {
        private final String menuId;
        
        public Closed(String menuId) {
            this.menuId = menuId;
        }
        
        public String getMenuId() { return menuId; }
    }
    
    /**
     * Fired when a menu is shown
     */
    public static class Shown extends MenuEvent {
        private final String menuId;
        
        public Shown(String menuId) {
            this.menuId = menuId;
        }
        
        public String getMenuId() { return menuId; }
    }
    
    /**
     * Fired when a menu is hidden
     */
    public static class Hidden extends MenuEvent {
        private final String menuId;
        
        public Hidden(String menuId) {
            this.menuId = menuId;
        }
        
        public String getMenuId() { return menuId; }
    }
    
    /**
     * Fired when a menu item is selected
     */
    public static class ItemSelected extends MenuEvent {
        private final String menuId;
        private final String itemId;
        private final Map<String, Object> context;
        
        public ItemSelected(String menuId, String itemId, Map<String, Object> context) {
            this.menuId = menuId;
            this.itemId = itemId;
            this.context = context;
        }
        
        public String getMenuId() { return menuId; }
        public String getItemId() { return itemId; }
        public Map<String, Object> getContext() { return context; }
    }
    
    /**
     * Fired when navigation occurs
     */
    public static class Navigation extends MenuEvent {
        private final String fromMenuId;
        private final String toMenuId;
        private final NavigationType type;
        
        public Navigation(String fromMenuId, String toMenuId, NavigationType type) {
            this.fromMenuId = fromMenuId;
            this.toMenuId = toMenuId;
            this.type = type;
        }
        
        public String getFromMenuId() { return fromMenuId; }
        public String getToMenuId() { return toMenuId; }
        public NavigationType getType() { return type; }
        
        public enum NavigationType {
            NAVIGATE, REPLACE, RESET, BACK
        }
    }
    
    /**
     * Fired when a game is started from the menu
     */
    public static class GameStarted extends MenuEvent {
        private final String startType;
        private final Map<String, Object> gameSettings;
        
        public GameStarted(String startType) {
            this(startType, Map.of());
        }
        
        public GameStarted(String startType, Map<String, Object> gameSettings) {
            this.startType = startType;
            this.gameSettings = gameSettings;
        }
        
        public String getStartType() { return startType; }
        public Map<String, Object> getGameSettings() { return gameSettings; }
    }
    
    /**
     * Fired when a game is saved from the menu
     */
    public static class GameSaved extends MenuEvent {
        private final String saveSlot;
        private final Map<String, Object> saveData;
        
        public GameSaved() {
            this(null, Map.of());
        }
        
        public GameSaved(String saveSlot, Map<String, Object> saveData) {
            this.saveSlot = saveSlot;
            this.saveData = saveData;
        }
        
        public String getSaveSlot() { return saveSlot; }
        public Map<String, Object> getSaveData() { return saveData; }
    }
    
    /**
     * Fired when a game is loaded from the menu
     */
    public static class GameLoaded extends MenuEvent {
        private final String saveSlot;
        private final Map<String, Object> loadedData;
        
        public GameLoaded(String saveSlot, Map<String, Object> loadedData) {
            this.saveSlot = saveSlot;
            this.loadedData = loadedData;
        }
        
        public String getSaveSlot() { return saveSlot; }
        public Map<String, Object> getLoadedData() { return loadedData; }
    }
    
    /**
     * Fired when settings are changed
     */
    public static class SettingsChanged extends MenuEvent {
        private final String category;
        private final String setting;
        private final Object oldValue;
        private final Object newValue;
        
        public SettingsChanged(String category, String setting, Object oldValue, Object newValue) {
            this.category = category;
            this.setting = setting;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
        
        public String getCategory() { return category; }
        public String getSetting() { return setting; }
        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
    }
    
    /**
     * Fired when a menu error occurs
     */
    public static class MenuError extends MenuEvent {
        private final String menuId;
        private final String error;
        private final Exception exception;
        
        public MenuError(String menuId, String error) {
            this(menuId, error, null);
        }
        
        public MenuError(String menuId, String error, Exception exception) {
            this.menuId = menuId;
            this.error = error;
            this.exception = exception;
        }
        
        public String getMenuId() { return menuId; }
        public String getError() { return error; }
        public Exception getException() { return exception; }
    }
}