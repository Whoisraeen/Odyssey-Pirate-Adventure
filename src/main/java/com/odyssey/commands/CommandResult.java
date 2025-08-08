package com.odyssey.commands;

/**
 * Represents the result of command execution.
 */
public class CommandResult {
    
    public enum Status {
        SUCCESS,
        FAILURE,
        INVALID_ARGS,
        NO_PERMISSION,
        PLAYER_ONLY,
        CONSOLE_ONLY
    }
    
    private final Status status;
    private final String message;
    private final Object data;
    
    private CommandResult(Status status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
    
    /**
     * Creates a successful command result.
     * @return Success result
     */
    public static CommandResult success() {
        return new CommandResult(Status.SUCCESS, null, null);
    }
    
    /**
     * Creates a successful command result with a message.
     * @param message Success message
     * @return Success result with message
     */
    public static CommandResult success(String message) {
        return new CommandResult(Status.SUCCESS, message, null);
    }
    
    /**
     * Creates a successful command result with data.
     * @param message Success message
     * @param data Result data
     * @return Success result with data
     */
    public static CommandResult success(String message, Object data) {
        return new CommandResult(Status.SUCCESS, message, data);
    }
    
    /**
     * Creates a failure command result.
     * @param message Error message
     * @return Failure result
     */
    public static CommandResult failure(String message) {
        return new CommandResult(Status.FAILURE, message, null);
    }
    
    /**
     * Creates an invalid arguments result.
     * @param message Error message
     * @return Invalid arguments result
     */
    public static CommandResult invalidArgs(String message) {
        return new CommandResult(Status.INVALID_ARGS, message, null);
    }
    
    /**
     * Creates a no permission result.
     * @return No permission result
     */
    public static CommandResult noPermission() {
        return new CommandResult(Status.NO_PERMISSION, "You don't have permission to use this command.", null);
    }
    
    /**
     * Creates a player-only result.
     * @return Player-only result
     */
    public static CommandResult playerOnly() {
        return new CommandResult(Status.PLAYER_ONLY, "This command can only be used by players.", null);
    }
    
    /**
     * Creates a console-only result.
     * @return Console-only result
     */
    public static CommandResult consoleOnly() {
        return new CommandResult(Status.CONSOLE_ONLY, "This command can only be used from the console.", null);
    }
    
    // Getters
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailure() { return status != Status.SUCCESS; }
}