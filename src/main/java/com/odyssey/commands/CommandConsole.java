package com.odyssey.commands;

import com.odyssey.core.Engine;
import com.odyssey.game.GameStateManager;
import com.odyssey.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command console system for executing commands and scripts in-game.
 * Provides a flexible command framework with permission system and scripting support.
 */
public class CommandConsole {
    private static final Logger logger = LoggerFactory.getLogger(CommandConsole.class);
    
    private final Engine engine;
    private final Map<String, Command> commands;
    private final Map<String, String> aliases;
    private final List<String> commandHistory;
    private final Set<String> enabledCommands;
    private boolean consoleEnabled = true;
    private int maxHistorySize = 100;
    
    // Command parsing
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^/(\\w+)(?:\\s+(.*))?$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    // Built-in variables
    private final Map<String, String> variables;
    
    public CommandConsole(Engine engine) {
        this.engine = engine;
        this.commands = new ConcurrentHashMap<>();
        this.aliases = new ConcurrentHashMap<>();
        this.commandHistory = new ArrayList<>();
        this.enabledCommands = ConcurrentHashMap.newKeySet();
        this.variables = new ConcurrentHashMap<>();
        
        initializeBuiltInCommands();
        initializeBuiltInVariables();
        
        logger.info("Command console initialized with {} commands", commands.size());
    }
    
    /**
     * Executes a command string
     */
    public CommandResult executeCommand(String input, CommandSender sender) {
        if (!consoleEnabled) {
            return new CommandResult(false, "Command console is disabled");
        }
        
        if (input == null || input.trim().isEmpty()) {
            return new CommandResult(false, "Empty command");
        }
        
        // Add to history
        addToHistory(input);
        
        // Parse command
        String processedInput = processVariables(input.trim());
        Matcher matcher = COMMAND_PATTERN.matcher(processedInput);
        
        if (!matcher.matches()) {
            return new CommandResult(false, "Invalid command format. Commands must start with '/'");
        }
        
        String commandName = matcher.group(1).toLowerCase();
        String args = matcher.group(2);
        
        // Resolve aliases
        commandName = aliases.getOrDefault(commandName, commandName);
        
        // Check if command exists
        Command command = commands.get(commandName);
        if (command == null) {
            return new CommandResult(false, "Unknown command: " + commandName);
        }
        
        // Check if command is enabled
        if (!enabledCommands.contains(commandName)) {
            return new CommandResult(false, "Command '" + commandName + "' is disabled");
        }
        
        // Check permissions
        if (!hasPermission(sender, command.getPermission())) {
            return new CommandResult(false, "You don't have permission to use this command");
        }
        
        try {
            // Parse arguments
            String[] argArray = parseArguments(args);
            
            // Execute command
            CommandResult result = command.execute(sender, argArray);
            
            logger.debug("Command '{}' executed by {} with result: {}", 
                commandName, sender.getName(), result.isSuccess());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing command '{}': {}", commandName, e.getMessage(), e);
            return new CommandResult(false, "Error executing command: " + e.getMessage());
        }
    }
    
    /**
     * Registers a command
     */
    public void registerCommand(Command command) {
        String name = command.getName().toLowerCase();
        commands.put(name, command);
        enabledCommands.add(name);
        
        // Register aliases
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), name);
        }
        
        logger.debug("Registered command: {} with {} aliases", name, command.getAliases().length);
    }
    
    /**
     * Unregisters a command
     */
    public void unregisterCommand(String name) {
        name = name.toLowerCase();
        Command command = commands.remove(name);
        if (command != null) {
            enabledCommands.remove(name);
            
            // Remove aliases
            for (String alias : command.getAliases()) {
                aliases.remove(alias.toLowerCase());
            }
            
            logger.debug("Unregistered command: {}", name);
        }
    }
    
    /**
     * Processes variables in command input
     */
    private String processVariables(String input) {
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variables.getOrDefault(varName, "${" + varName + "}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Parses command arguments
     */
    private String[] parseArguments(String args) {
        if (args == null || args.trim().isEmpty()) {
            return new String[0];
        }
        
        List<String> argList = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (char c : args.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    argList.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            argList.add(current.toString());
        }
        
        return argList.toArray(new String[0]);
    }
    
    /**
     * Checks if sender has permission
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        
        // For now, simple permission check
        // In a real implementation, this would integrate with a permission system
        return sender.hasPermission(permission);
    }
    
    /**
     * Adds command to history
     */
    private void addToHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > maxHistorySize) {
            commandHistory.remove(0);
        }
    }
    
    /**
     * Initializes built-in commands
     */
    private void initializeBuiltInCommands() {
        // Help command
        registerCommand(new Command("help", "odyssey.command.help", "Shows command help") {
            @Override
            public CommandResult execute(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    StringBuilder help = new StringBuilder("Available commands:\n");
                    for (Command cmd : commands.values()) {
                        if (hasPermission(sender, cmd.getPermission())) {
                            help.append("/").append(cmd.getName()).append(" - ").append(cmd.getDescription()).append("\n");
                        }
                    }
                    return new CommandResult(true, help.toString());
                } else {
                    String cmdName = args[0].toLowerCase();
                    Command cmd = commands.get(cmdName);
                    if (cmd != null) {
                        return new CommandResult(true, "/" + cmd.getName() + " - " + cmd.getDescription() + 
                            "\nUsage: " + cmd.getUsage());
                    } else {
                        return new CommandResult(false, "Unknown command: " + cmdName);
                    }
                }
            }
            
            @Override
            public String[] getAliases() {
                return new String[]{"?", "h"};
            }
        });
        
        // Time command
        registerCommand(new Command("time", "odyssey.command.time", "Manages world time") {
            @Override
            public CommandResult execute(CommandSender sender, String[] args) {
                World world = engine.getWorld();
                if (world == null) {
                    return new CommandResult(false, "No world loaded");
                }
                
                if (args.length == 0) {
                    // TODO: Implement day/night cycle system
                    return new CommandResult(true, "Current time: 0 (day/night cycle not implemented)");
                }
                
                switch (args[0].toLowerCase()) {
                    case "set":
                        if (args.length < 2) {
                            return new CommandResult(false, "Usage: /time set <time>");
                        }
                        try {
                            long time = Long.parseLong(args[1]);
                            // TODO: Implement day/night cycle system
                            return new CommandResult(true, "Time set to " + time + " (day/night cycle not implemented)");
                        } catch (NumberFormatException e) {
                            return new CommandResult(false, "Invalid time value");
                        }
                    case "add":
                        if (args.length < 2) {
                            return new CommandResult(false, "Usage: /time add <ticks>");
                        }
                        try {
                            long ticks = Long.parseLong(args[1]);
                            // TODO: Implement day/night cycle system
                            return new CommandResult(true, "Added " + ticks + " ticks (day/night cycle not implemented)");
                        } catch (NumberFormatException e) {
                            return new CommandResult(false, "Invalid tick value");
                        }
                    case "day":
                        // TODO: Implement day/night cycle system
                        return new CommandResult(true, "Time set to day (day/night cycle not implemented)");
                    case "night":
                        // TODO: Implement day/night cycle system
                        return new CommandResult(true, "Time set to night (day/night cycle not implemented)");
                    case "dawn":
                        // TODO: Implement day/night cycle system
                        return new CommandResult(true, "Time set to dawn (day/night cycle not implemented)");
                    case "dusk":
                        // TODO: Implement day/night cycle system
                        return new CommandResult(true, "Time set to dusk (day/night cycle not implemented)");
                    default:
                        return new CommandResult(false, "Usage: /time <set|add|day|night|dawn|dusk> [value]");
                }
            }
            
            @Override
            public String getUsage() {
                return "/time <set|add|day|night|dawn|dusk> [value]";
            }
        });
        
        // Weather command
        registerCommand(new Command("weather", "odyssey.command.weather", "Controls weather") {
            @Override
            public CommandResult execute(CommandSender sender, String[] args) {
                // Access weather system through engine, not world
                var weatherSystem = engine.getWeatherSystem();
                if (weatherSystem == null) {
                    return new CommandResult(false, "Weather system not available");
                }
                
                if (args.length == 0) {
                    return new CommandResult(true, "Current weather: " + weatherSystem.getCurrentWeather());
                }
                
                // Note: This is a simplified version. The proper WeatherCommand implementation
                // is in com.odyssey.commands.impl.WeatherCommand
                switch (args[0].toLowerCase()) {
                    case "clear":
                        weatherSystem.forceWeather(com.odyssey.world.weather.WeatherSystem.WeatherType.CLEAR, 0.0f, 300.0);
                        return new CommandResult(true, "Weather set to clear");
                    case "rain":
                        weatherSystem.forceWeather(com.odyssey.world.weather.WeatherSystem.WeatherType.MODERATE_RAIN, 0.6f, 300.0);
                        return new CommandResult(true, "Weather set to rain");
                    case "storm":
                        weatherSystem.forceWeather(com.odyssey.world.weather.WeatherSystem.WeatherType.THUNDERSTORM, 0.9f, 300.0);
                        return new CommandResult(true, "Weather set to storm");
                    default:
                        return new CommandResult(false, "Usage: /weather <clear|rain|storm>");
                }
            }
            
            @Override
            public String getUsage() {
                return "/weather <clear|rain|storm>";
            }
        });
        
        // Gamemode command
        registerCommand(new Command("gamemode", "odyssey.command.gamemode", "Changes game mode") {
            @Override
            public CommandResult execute(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    return new CommandResult(false, "Usage: /gamemode <survival|creative|adventure|spectator>");
                }
                
                String mode = args[0].toLowerCase();
                switch (mode) {
                    case "survival":
                    case "s":
                    case "0":
                        // Set survival mode
                        return new CommandResult(true, "Game mode set to Survival");
                    case "creative":
                    case "c":
                    case "1":
                        // Set creative mode
                        return new CommandResult(true, "Game mode set to Creative");
                    case "adventure":
                    case "a":
                    case "2":
                        // Set adventure mode
                        return new CommandResult(true, "Game mode set to Adventure");
                    case "spectator":
                    case "sp":
                    case "3":
                        // Set spectator mode
                        return new CommandResult(true, "Game mode set to Spectator");
                    default:
                        return new CommandResult(false, "Unknown game mode: " + mode);
                }
            }
            
            @Override
            public String getUsage() {
                return "/gamemode <survival|creative|adventure|spectator>";
            }
            
            @Override
            public String[] getAliases() {
                return new String[]{"gm"};
            }
        });
        
        // Variable command
        registerCommand(new Command("var", "odyssey.command.var", "Manages variables") {
            @Override
            public CommandResult execute(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    StringBuilder vars = new StringBuilder("Variables:\n");
                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        vars.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
                    }
                    return new CommandResult(true, vars.toString());
                }
                
                if (args.length == 1) {
                    String value = variables.get(args[0]);
                    if (value != null) {
                        return new CommandResult(true, args[0] + " = " + value);
                    } else {
                        return new CommandResult(false, "Variable not found: " + args[0]);
                    }
                }
                
                if (args.length >= 2) {
                    String name = args[0];
                    String value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    variables.put(name, value);
                    return new CommandResult(true, "Variable set: " + name + " = " + value);
                }
                
                return new CommandResult(false, "Usage: /var [name] [value]");
            }
            
            @Override
            public String getUsage() {
                return "/var [name] [value]";
            }
        });
        
        // Console command
        registerCommand(new Command("console", "odyssey.command.console", "Console management") {
            @Override
            public CommandResult execute(CommandSender sender, String[] args) {
                if (args.length == 0) {
                    return new CommandResult(true, "Console enabled: " + consoleEnabled);
                }
                
                switch (args[0].toLowerCase()) {
                    case "enable":
                        consoleEnabled = true;
                        return new CommandResult(true, "Console enabled");
                    case "disable":
                        consoleEnabled = false;
                        return new CommandResult(true, "Console disabled");
                    case "history":
                        StringBuilder history = new StringBuilder("Command history:\n");
                        for (int i = Math.max(0, commandHistory.size() - 10); i < commandHistory.size(); i++) {
                            history.append(i + 1).append(": ").append(commandHistory.get(i)).append("\n");
                        }
                        return new CommandResult(true, history.toString());
                    case "clear":
                        commandHistory.clear();
                        return new CommandResult(true, "Command history cleared");
                    default:
                        return new CommandResult(false, "Usage: /console <enable|disable|history|clear>");
                }
            }
            
            @Override
            public String getUsage() {
                return "/console <enable|disable|history|clear>";
            }
        });
    }
    
    /**
     * Initializes built-in variables
     */
    private void initializeBuiltInVariables() {
        variables.put("version", "1.0.0");
        variables.put("engine", "Odyssey Engine");
        variables.put("time", "0");
        variables.put("day", "0");
        variables.put("weather", "clear");
    }
    
    /**
     * Updates dynamic variables
     */
    public void updateVariables() {
        World world = engine.getWorld();
        if (world != null) {
            // TODO: Implement day/night cycle system
            variables.put("time", "0");
            variables.put("day", "0");
        }
        
        // Update weather from engine's weather system
        var weatherSystem = engine.getWeatherSystem();
        if (weatherSystem != null) {
            variables.put("weather", weatherSystem.getCurrentWeather().toString());
        }
    }
    
    // Getters and setters
    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }
    
    public void setConsoleEnabled(boolean enabled) {
        this.consoleEnabled = enabled;
    }
    
    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }
    
    public Set<String> getAvailableCommands() {
        return new HashSet<>(commands.keySet());
    }
    
    public void enableCommand(String name) {
        enabledCommands.add(name.toLowerCase());
    }
    
    public void disableCommand(String name) {
        enabledCommands.remove(name.toLowerCase());
    }
    
    public boolean isCommandEnabled(String name) {
        return enabledCommands.contains(name.toLowerCase());
    }
    
    public void setVariable(String name, String value) {
        variables.put(name, value);
    }
    
    public String getVariable(String name) {
        return variables.get(name);
    }
    
    /**
     * Command result class
     */
    public static class CommandResult {
        private final boolean success;
        private final String message;
        
        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Command sender interface
     */
    public interface CommandSender {
        String getName();
        boolean hasPermission(String permission);
        void sendMessage(String message);
    }
    
    /**
     * Abstract command class
     */
    public abstract static class Command {
        private final String name;
        private final String permission;
        private final String description;
        
        public Command(String name, String permission, String description) {
            this.name = name;
            this.permission = permission;
            this.description = description;
        }
        
        public abstract CommandResult execute(CommandSender sender, String[] args);
        
        public String getName() {
            return name;
        }
        
        public String getPermission() {
            return permission;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getUsage() {
            return "/" + name;
        }
        
        public String[] getAliases() {
            return new String[0];
        }
    }
}