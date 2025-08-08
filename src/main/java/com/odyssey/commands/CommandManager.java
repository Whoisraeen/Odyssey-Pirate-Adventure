package com.odyssey.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages registration and execution of console commands.
 * Provides command parsing, permission checking, and tab completion.
 */
public class CommandManager {
    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
    
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();
    private final List<String> commandHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;
    
    /**
     * Registers a command with the manager.
     * @param command The command to register
     */
    public void registerCommand(Command command) {
        String name = command.getName().toLowerCase();
        commands.put(name, command);
        
        // Register aliases
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), name);
        }
        
        logger.debug("Registered command: {} with {} aliases", name, command.getAliases().size());
    }
    
    /**
     * Unregisters a command from the manager.
     * @param commandName The name of the command to unregister
     */
    public void unregisterCommand(String commandName) {
        String name = commandName.toLowerCase();
        Command command = commands.remove(name);
        
        if (command != null) {
            // Remove aliases
            for (String alias : command.getAliases()) {
                aliases.remove(alias.toLowerCase());
            }
            logger.debug("Unregistered command: {}", name);
        }
    }
    
    /**
     * Executes a command string.
     * @param sender The command sender
     * @param commandLine The full command line (including command name)
     * @return CommandResult indicating success/failure
     */
    public CommandResult executeCommand(CommandSender sender, String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return CommandResult.invalidArgs("Empty command");
        }
        
        // Remove leading slash if present
        String line = commandLine.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }
        
        // Parse command and arguments
        String[] parts = parseCommandLine(line);
        if (parts.length == 0) {
            return CommandResult.invalidArgs("Invalid command");
        }
        
        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        
        // Add to history
        addToHistory(commandLine);
        
        // Resolve alias
        String resolvedName = aliases.getOrDefault(commandName, commandName);
        Command command = commands.get(resolvedName);
        
        if (command == null) {
            return CommandResult.failure("Unknown command: " + commandName);
        }
        
        // Check permissions
        if (!sender.hasPermission(command.getPermissionLevel())) {
            return CommandResult.noPermission();
        }
        
        // Validate arguments
        if (!command.validateArgs(sender, args)) {
            return CommandResult.invalidArgs("Usage: " + command.getUsage());
        }
        
        try {
            logger.debug("Executing command '{}' with {} args for sender '{}'", 
                        commandName, args.length, sender.getName());
            return command.execute(sender, args);
        } catch (Exception e) {
            logger.error("Error executing command '{}': {}", commandName, e.getMessage(), e);
            return CommandResult.failure("Internal error executing command");
        }
    }
    
    /**
     * Gets tab completion suggestions for a command line.
     * @param sender The command sender
     * @param commandLine The partial command line
     * @return List of completion suggestions
     */
    public List<String> getTabCompletions(CommandSender sender, String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return getAvailableCommands(sender);
        }
        
        String line = commandLine.trim();
        if (line.startsWith("/")) {
            line = line.substring(1);
        }
        
        String[] parts = parseCommandLine(line);
        if (parts.length == 0) {
            return getAvailableCommands(sender);
        }
        
        String commandName = parts[0].toLowerCase();
        
        // If we're still typing the command name
        if (parts.length == 1 && !commandLine.endsWith(" ")) {
            return getAvailableCommands(sender).stream()
                    .filter(cmd -> cmd.startsWith(commandName))
                    .collect(Collectors.toList());
        }
        
        // Get command-specific completions
        String resolvedName = aliases.getOrDefault(commandName, commandName);
        Command command = commands.get(resolvedName);
        
        if (command != null && sender.hasPermission(command.getPermissionLevel())) {
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            return command.getTabCompletions(sender, args);
        }
        
        return List.of();
    }
    
    /**
     * Gets a list of available commands for the sender.
     * @param sender The command sender
     * @return List of command names the sender can use
     */
    public List<String> getAvailableCommands(CommandSender sender) {
        return commands.values().stream()
                .filter(cmd -> sender.hasPermission(cmd.getPermissionLevel()))
                .map(Command::getName)
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * Gets information about a specific command.
     * @param commandName The command name
     * @return Command object, or null if not found
     */
    public Command getCommand(String commandName) {
        String name = commandName.toLowerCase();
        String resolvedName = aliases.getOrDefault(name, name);
        return commands.get(resolvedName);
    }
    
    /**
     * Gets the command history.
     * @return List of previously executed commands
     */
    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }
    
    /**
     * Clears the command history.
     */
    public void clearHistory() {
        commandHistory.clear();
    }
    
    /**
     * Parses a command line into command and arguments.
     * Handles quoted arguments properly.
     */
    private String[] parseCommandLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (char c : line.toCharArray()) {
            if (escapeNext) {
                current.append(c);
                escapeNext = false;
            } else if (c == '\\') {
                escapeNext = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Adds a command to the history.
     */
    private void addToHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.remove(0);
        }
    }
}