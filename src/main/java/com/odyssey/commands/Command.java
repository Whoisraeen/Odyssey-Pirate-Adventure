package com.odyssey.commands;

import java.util.List;

/**
 * Base interface for all console commands.
 * Commands provide functionality for server administration and debugging.
 */
public interface Command {
    
    /**
     * Gets the name of this command.
     * @return The command name (without the leading slash)
     */
    String getName();
    
    /**
     * Gets the description of this command.
     * @return A brief description of what this command does
     */
    String getDescription();
    
    /**
     * Gets the usage string for this command.
     * @return Usage string showing syntax and parameters
     */
    String getUsage();
    
    /**
     * Gets the permission level required to execute this command.
     * @return Permission level (0 = all players, 1 = moderator, 2 = admin, etc.)
     */
    int getPermissionLevel();
    
    /**
     * Gets aliases for this command.
     * @return List of alternative names for this command
     */
    default List<String> getAliases() {
        return List.of();
    }
    
    /**
     * Executes the command.
     * @param sender The command sender (player, console, etc.)
     * @param args Command arguments
     * @return CommandResult indicating success/failure and any output
     */
    CommandResult execute(CommandSender sender, String[] args);
    
    /**
     * Provides tab completion suggestions for this command.
     * @param sender The command sender
     * @param args Current arguments being typed
     * @return List of completion suggestions
     */
    default List<String> getTabCompletions(CommandSender sender, String[] args) {
        return List.of();
    }
    
    /**
     * Validates command arguments before execution.
     * @param sender The command sender
     * @param args Command arguments
     * @return true if arguments are valid, false otherwise
     */
    default boolean validateArgs(CommandSender sender, String[] args) {
        return true;
    }
}