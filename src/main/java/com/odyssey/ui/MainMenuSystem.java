package com.odyssey.ui;

import com.odyssey.events.EventBus;
import com.odyssey.events.MenuEvent;
import com.odyssey.data.BinaryTagLibrary;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages the main menu system and navigation.
 * Provides a flexible, data-driven menu framework.
 */
public class MainMenuSystem {
    private static final MainMenuSystem INSTANCE = new MainMenuSystem();
    
    private final Map<String, Menu> menus = new ConcurrentHashMap<>();
    private final Stack<String> menuStack = new Stack<>();
    private final EventBus eventBus = EventBus.getInstance();
    
    private String currentMenuId;
    private boolean menuVisible = false;
    private final Map<String, Object> menuContext = new HashMap<>();
    
    private MainMenuSystem() {
        registerDefaultMenus();
    }
    
    /**
     * Gets the singleton instance
     */
    public static MainMenuSystem getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers a menu
     */
    public void registerMenu(Menu menu) {
        if (menu == null || menu.getId() == null) {
            throw new IllegalArgumentException("Menu and ID cannot be null");
        }
        
        menus.put(menu.getId(), menu);
        eventBus.post(new MenuEvent.MenuRegistered(menu.getId()));
    }
    
    /**
     * Unregisters a menu
     */
    public void unregisterMenu(String menuId) {
        Menu removed = menus.remove(menuId);
        if (removed != null) {
            eventBus.post(new MenuEvent.MenuUnregistered(menuId));
        }
    }
    
    /**
     * Gets a menu by ID
     */
    public Menu getMenu(String menuId) {
        return menus.get(menuId);
    }
    
    /**
     * Opens a menu
     */
    public boolean openMenu(String menuId) {
        return openMenu(menuId, new HashMap<>());
    }
    
    /**
     * Opens a menu with context
     */
    public boolean openMenu(String menuId, Map<String, Object> context) {
        Menu menu = menus.get(menuId);
        if (menu == null) {
            return false;
        }
        
        // Fire pre-open event
        MenuEvent.PreOpen preOpenEvent = new MenuEvent.PreOpen(menuId, context);
        eventBus.post(preOpenEvent);
        
        if (preOpenEvent.isCancelled()) {
            return false;
        }
        
        // Close current menu if open
        if (currentMenuId != null) {
            closeCurrentMenu(false);
        }
        
        // Set new menu
        currentMenuId = menuId;
        menuVisible = true;
        menuContext.clear();
        menuContext.putAll(context);
        
        // Add to stack for navigation
        menuStack.push(menuId);
        
        // Initialize menu
        menu.onOpen(context);
        
        // Fire open event
        eventBus.post(new MenuEvent.Opened(menuId, context));
        
        return true;
    }
    
    /**
     * Closes the current menu
     */
    public void closeCurrentMenu() {
        closeCurrentMenu(true);
    }
    
    /**
     * Closes the current menu
     */
    private void closeCurrentMenu(boolean fireEvent) {
        if (currentMenuId == null) {
            return;
        }
        
        String closingMenuId = currentMenuId;
        Menu menu = menus.get(currentMenuId);
        
        if (fireEvent) {
            // Fire pre-close event
            MenuEvent.PreClose preCloseEvent = new MenuEvent.PreClose(closingMenuId);
            eventBus.post(preCloseEvent);
            
            if (preCloseEvent.isCancelled()) {
                return;
            }
        }
        
        // Close menu
        if (menu != null) {
            menu.onClose();
        }
        
        currentMenuId = null;
        menuVisible = false;
        menuContext.clear();
        
        // Remove from stack
        if (!menuStack.isEmpty() && menuStack.peek().equals(closingMenuId)) {
            menuStack.pop();
        }
        
        if (fireEvent) {
            eventBus.post(new MenuEvent.Closed(closingMenuId));
        }
    }
    
    /**
     * Goes back to the previous menu
     */
    public boolean goBack() {
        if (menuStack.size() <= 1) {
            return false;
        }
        
        // Remove current menu from stack
        if (!menuStack.isEmpty()) {
            menuStack.pop();
        }
        
        // Open previous menu
        if (!menuStack.isEmpty()) {
            String previousMenuId = menuStack.pop(); // Remove it so openMenu can add it back
            return openMenu(previousMenuId);
        }
        
        return false;
    }
    
    /**
     * Navigates to a menu (adds to navigation stack)
     */
    public boolean navigateTo(String menuId) {
        return navigateTo(menuId, new HashMap<>());
    }
    
    /**
     * Navigates to a menu with context
     */
    public boolean navigateTo(String menuId, Map<String, Object> context) {
        return openMenu(menuId, context);
    }
    
    /**
     * Replaces the current menu (doesn't add to stack)
     */
    public boolean replaceMenu(String menuId) {
        return replaceMenu(menuId, new HashMap<>());
    }
    
    /**
     * Replaces the current menu with context
     */
    public boolean replaceMenu(String menuId, Map<String, Object> context) {
        if (currentMenuId != null) {
            closeCurrentMenu(false);
            // Remove from stack since we're replacing
            if (!menuStack.isEmpty()) {
                menuStack.pop();
            }
        }
        
        return openMenu(menuId, context);
    }
    
    /**
     * Clears the navigation stack and opens a menu
     */
    public boolean resetToMenu(String menuId) {
        return resetToMenu(menuId, new HashMap<>());
    }
    
    /**
     * Clears the navigation stack and opens a menu with context
     */
    public boolean resetToMenu(String menuId, Map<String, Object> context) {
        menuStack.clear();
        if (currentMenuId != null) {
            closeCurrentMenu(false);
        }
        
        return openMenu(menuId, context);
    }
    
    /**
     * Toggles menu visibility
     */
    public void toggleMenu() {
        if (menuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }
    
    /**
     * Shows the current menu
     */
    public void showMenu() {
        if (currentMenuId != null) {
            menuVisible = true;
            eventBus.post(new MenuEvent.Shown(currentMenuId));
        }
    }
    
    /**
     * Hides the current menu
     */
    public void hideMenu() {
        if (currentMenuId != null) {
            menuVisible = false;
            eventBus.post(new MenuEvent.Hidden(currentMenuId));
        }
    }
    
    /**
     * Checks if a menu is currently open
     */
    public boolean isMenuOpen() {
        return currentMenuId != null && menuVisible;
    }
    
    /**
     * Gets the current menu ID
     */
    public String getCurrentMenuId() {
        return currentMenuId;
    }
    
    /**
     * Gets the current menu
     */
    public Menu getCurrentMenu() {
        return currentMenuId != null ? menus.get(currentMenuId) : null;
    }
    
    /**
     * Gets the menu context
     */
    public Map<String, Object> getMenuContext() {
        return new HashMap<>(menuContext);
    }
    
    /**
     * Sets a context value
     */
    public void setContextValue(String key, Object value) {
        menuContext.put(key, value);
    }
    
    /**
     * Gets a context value
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, T defaultValue) {
        return (T) menuContext.getOrDefault(key, defaultValue);
    }
    
    /**
     * Gets the navigation stack
     */
    public List<String> getNavigationStack() {
        return new ArrayList<>(menuStack);
    }
    
    /**
     * Gets all registered menus
     */
    public Collection<Menu> getAllMenus() {
        return new ArrayList<>(menus.values());
    }
    
    /**
     * Gets menu IDs by category
     */
    public List<String> getMenusByCategory(String category) {
        return menus.values().stream()
            .filter(menu -> category.equals(menu.getCategory()))
            .map(Menu::getId)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Updates the current menu
     */
    public void update() {
        if (currentMenuId != null && menuVisible) {
            Menu menu = menus.get(currentMenuId);
            if (menu != null) {
                menu.update();
            }
        }
    }
    
    /**
     * Handles input for the current menu
     */
    public boolean handleInput(String input) {
        if (currentMenuId != null && menuVisible) {
            Menu menu = menus.get(currentMenuId);
            if (menu != null) {
                return menu.handleInput(input);
            }
        }
        return false;
    }
    
    /**
     * Registers default menus
     */
    private void registerDefaultMenus() {
        // Main menu
        registerMenu(Menu.builder("main")
            .title("Odyssey Pirate Adventure")
            .category("main")
            .item(MenuItem.builder("new_game")
                .title("New Game")
                .description("Start a new pirate adventure")
                .action(ctx -> navigateTo("new_game"))
                .build())
            .item(MenuItem.builder("load_game")
                .title("Load Game")
                .description("Continue your adventure")
                .action(ctx -> navigateTo("load_game"))
                .build())
            .item(MenuItem.builder("multiplayer")
                .title("Multiplayer")
                .description("Join or host a multiplayer game")
                .action(ctx -> navigateTo("multiplayer"))
                .build())
            .item(MenuItem.builder("settings")
                .title("Settings")
                .description("Configure game settings")
                .action(ctx -> navigateTo("settings"))
                .build())
            .item(MenuItem.builder("credits")
                .title("Credits")
                .description("View game credits")
                .action(ctx -> navigateTo("credits"))
                .build())
            .item(MenuItem.builder("quit")
                .title("Quit Game")
                .description("Exit the game")
                .action(ctx -> System.exit(0))
                .build())
            .build());
        
        // New game menu
        registerMenu(Menu.builder("new_game")
            .title("New Game")
            .category("game")
            .item(MenuItem.builder("create_character")
                .title("Create Character")
                .description("Create your pirate character")
                .action(ctx -> navigateTo("character_creation"))
                .build())
            .item(MenuItem.builder("quick_start")
                .title("Quick Start")
                .description("Start with a default character")
                .action(ctx -> {
                    // Start game with default character
                    eventBus.post(new MenuEvent.GameStarted("quick_start"));
                })
                .build())
            .item(MenuItem.builder("back")
                .title("Back")
                .description("Return to main menu")
                .action(ctx -> goBack())
                .build())
            .build());
        
        // Settings menu
        registerMenu(Menu.builder("settings")
            .title("Settings")
            .category("settings")
            .item(MenuItem.builder("graphics")
                .title("Graphics")
                .description("Graphics and display settings")
                .action(ctx -> navigateTo("graphics_settings"))
                .build())
            .item(MenuItem.builder("audio")
                .title("Audio")
                .description("Sound and music settings")
                .action(ctx -> navigateTo("audio_settings"))
                .build())
            .item(MenuItem.builder("controls")
                .title("Controls")
                .description("Input and control settings")
                .action(ctx -> navigateTo("control_settings"))
                .build())
            .item(MenuItem.builder("gameplay")
                .title("Gameplay")
                .description("Gameplay and difficulty settings")
                .action(ctx -> navigateTo("gameplay_settings"))
                .build())
            .item(MenuItem.builder("back")
                .title("Back")
                .description("Return to main menu")
                .action(ctx -> goBack())
                .build())
            .build());
        
        // In-game menu
        registerMenu(Menu.builder("ingame")
            .title("Game Menu")
            .category("ingame")
            .item(MenuItem.builder("resume")
                .title("Resume Game")
                .description("Continue playing")
                .action(ctx -> closeCurrentMenu())
                .build())
            .item(MenuItem.builder("inventory")
                .title("Inventory")
                .description("Manage your items")
                .action(ctx -> navigateTo("inventory"))
                .build())
            .item(MenuItem.builder("map")
                .title("Map")
                .description("View the world map")
                .action(ctx -> navigateTo("map"))
                .build())
            .item(MenuItem.builder("ship")
                .title("Ship Management")
                .description("Manage your ship")
                .action(ctx -> navigateTo("ship_management"))
                .build())
            .item(MenuItem.builder("crew")
                .title("Crew")
                .description("Manage your crew")
                .action(ctx -> navigateTo("crew_management"))
                .build())
            .item(MenuItem.builder("quests")
                .title("Quests")
                .description("View active quests")
                .action(ctx -> navigateTo("quest_log"))
                .build())
            .item(MenuItem.builder("settings")
                .title("Settings")
                .description("Game settings")
                .action(ctx -> navigateTo("settings"))
                .build())
            .item(MenuItem.builder("save_quit")
                .title("Save & Quit")
                .description("Save and return to main menu")
                .action(ctx -> {
                    eventBus.post(new MenuEvent.GameSaved());
                    resetToMenu("main");
                })
                .build())
            .build());
    }
    
    /**
     * Represents a menu in the system
     */
    public static class Menu {
        private final String id;
        private final String title;
        private final String category;
        private final List<MenuItem> items;
        private final Map<String, Object> properties;
        private final Consumer<Map<String, Object>> onOpenCallback;
        private final Runnable onCloseCallback;
        private final Runnable updateCallback;
        
        private Menu(Builder builder) {
            this.id = builder.id;
            this.title = builder.title;
            this.category = builder.category;
            this.items = new ArrayList<>(builder.items);
            this.properties = new HashMap<>(builder.properties);
            this.onOpenCallback = builder.onOpenCallback;
            this.onCloseCallback = builder.onCloseCallback;
            this.updateCallback = builder.updateCallback;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public List<MenuItem> getItems() { return new ArrayList<>(items); }
        public Map<String, Object> getProperties() { return new HashMap<>(properties); }
        
        public void onOpen(Map<String, Object> context) {
            if (onOpenCallback != null) {
                onOpenCallback.accept(context);
            }
        }
        
        public void onClose() {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        }
        
        public void update() {
            if (updateCallback != null) {
                updateCallback.run();
            }
        }
        
        public boolean handleInput(String input) {
            // Try to find menu item by input
            for (MenuItem item : items) {
                if (item.getId().equals(input) || item.getTitle().toLowerCase().contains(input.toLowerCase())) {
                    item.execute(new HashMap<>());
                    return true;
                }
            }
            return false;
        }
        
        public static Builder builder(String id) {
            return new Builder(id);
        }
        
        public static class Builder {
            private final String id;
            private String title = "";
            private String category = "misc";
            private final List<MenuItem> items = new ArrayList<>();
            private final Map<String, Object> properties = new HashMap<>();
            private Consumer<Map<String, Object>> onOpenCallback;
            private Runnable onCloseCallback;
            private Runnable updateCallback;
            
            private Builder(String id) {
                this.id = id;
            }
            
            public Builder title(String title) {
                this.title = title;
                return this;
            }
            
            public Builder category(String category) {
                this.category = category;
                return this;
            }
            
            public Builder item(MenuItem item) {
                items.add(item);
                return this;
            }
            
            public Builder property(String key, Object value) {
                properties.put(key, value);
                return this;
            }
            
            public Builder onOpen(Consumer<Map<String, Object>> callback) {
                this.onOpenCallback = callback;
                return this;
            }
            
            public Builder onClose(Runnable callback) {
                this.onCloseCallback = callback;
                return this;
            }
            
            public Builder onUpdate(Runnable callback) {
                this.updateCallback = callback;
                return this;
            }
            
            public Menu build() {
                return new Menu(this);
            }
        }
    }
    
    /**
     * Represents a menu item
     */
    public static class MenuItem {
        private final String id;
        private final String title;
        private final String description;
        private final Consumer<Map<String, Object>> action;
        private final Map<String, Object> properties;
        private boolean enabled;
        private boolean visible;
        
        private MenuItem(Builder builder) {
            this.id = builder.id;
            this.title = builder.title;
            this.description = builder.description;
            this.action = builder.action;
            this.properties = new HashMap<>(builder.properties);
            this.enabled = builder.enabled;
            this.visible = builder.visible;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Map<String, Object> getProperties() { return new HashMap<>(properties); }
        public boolean isEnabled() { return enabled; }
        public boolean isVisible() { return visible; }
        
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setVisible(boolean visible) { this.visible = visible; }
        
        public void execute(Map<String, Object> context) {
            if (enabled && action != null) {
                action.accept(context);
            }
        }
        
        public static Builder builder(String id) {
            return new Builder(id);
        }
        
        public static class Builder {
            private final String id;
            private String title = "";
            private String description = "";
            private Consumer<Map<String, Object>> action;
            private final Map<String, Object> properties = new HashMap<>();
            private boolean enabled = true;
            private boolean visible = true;
            
            private Builder(String id) {
                this.id = id;
            }
            
            public Builder title(String title) {
                this.title = title;
                return this;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder action(Consumer<Map<String, Object>> action) {
                this.action = action;
                return this;
            }
            
            public Builder property(String key, Object value) {
                properties.put(key, value);
                return this;
            }
            
            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            
            public Builder visible(boolean visible) {
                this.visible = visible;
                return this;
            }
            
            public MenuItem build() {
                return new MenuItem(this);
            }
        }
    }
}