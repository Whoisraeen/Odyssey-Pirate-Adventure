package com.odyssey.commands.impl;

import com.odyssey.commands.Command;
import com.odyssey.commands.CommandResult;
import com.odyssey.commands.CommandSender;
import com.odyssey.scripting.ScriptEngine;
import com.odyssey.scripting.ScriptResult;

import java.util.Arrays;
import java.util.List;

/**
 * Command for managing Lua scripts.
 * Allows loading, unloading, reloading, and executing scripts.
 */
public class ScriptCommand implements Command {
    private final ScriptEngine scriptEngine;
    
    public ScriptCommand(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }
    
    @Override
    public String getName() {
        return "script";
    }
    
    @Override
    public String getDescription() {
        return "Manage Lua scripts";
    }
    
    @Override
    public String getUsage() {
        return "/script <load|unload|reload|list|exec> [script_name] [code]";
    }
    
    @Override
    public int getPermissionLevel() {
        return 100; // High permission level for script management
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("lua", "scripts");
    }
    
    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return CommandResult.invalidArgs("Usage: " + getUsage());
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "load":
                return handleLoad(sender, args);
            case "unload":
                return handleUnload(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "list":
                return handleList(sender);
            case "exec":
                return handleExec(sender, args);
            case "loadall":
                return handleLoadAll(sender);
            default:
                return CommandResult.invalidArgs("Unknown action: " + action + ". Use load, unload, reload, list, exec, or loadall");
        }
    }
    
    private CommandResult handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return CommandResult.invalidArgs("Usage: /script load <script_name>");
        }
        
        String scriptName = args[1];
        boolean success = scriptEngine.loadScript(scriptName);
        
        if (success) {
            return CommandResult.success("Successfully loaded script: " + scriptName);
        } else {
            return CommandResult.failure("Failed to load script: " + scriptName);
        }
    }
    
    private CommandResult handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return CommandResult.invalidArgs("Usage: /script unload <script_name>");
        }
        
        String scriptName = args[1];
        scriptEngine.unloadScript(scriptName);
        return CommandResult.success("Unloaded script: " + scriptName);
    }
    
    private CommandResult handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return CommandResult.invalidArgs("Usage: /script reload <script_name>");
        }
        
        String scriptName = args[1];
        boolean success = scriptEngine.reloadScript(scriptName);
        
        if (success) {
            return CommandResult.success("Successfully reloaded script: " + scriptName);
        } else {
            return CommandResult.failure("Failed to reload script: " + scriptName);
        }
    }
    
    private CommandResult handleList(CommandSender sender) {
        String[] loadedScripts = scriptEngine.getLoadedScripts();
        
        if (loadedScripts.length == 0) {
            return CommandResult.success("No scripts currently loaded");
        }
        
        StringBuilder message = new StringBuilder("Loaded scripts (").append(loadedScripts.length).append("):\n");
        for (String script : loadedScripts) {
            message.append("  - ").append(script).append("\n");
        }
        
        return CommandResult.success(message.toString().trim());
    }
    
    private CommandResult handleExec(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return CommandResult.invalidArgs("Usage: /script exec <lua_code>");
        }
        
        // Join all arguments after "exec" as the Lua code
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) codeBuilder.append(" ");
            codeBuilder.append(args[i]);
        }
        
        String luaCode = codeBuilder.toString();
        ScriptResult result = scriptEngine.executeScript(luaCode, "console_exec");
        
        if (result.isSuccess()) {
            String resultStr = result.getResultAsString();
            return CommandResult.success("Script executed successfully. Result: " + resultStr);
        } else {
            return CommandResult.failure("Script execution failed: " + result.getErrorMessage());
        }
    }
    
    private CommandResult handleLoadAll(CommandSender sender) {
        scriptEngine.loadAllScripts();
        String[] loadedScripts = scriptEngine.getLoadedScripts();
        return CommandResult.success("Loaded " + loadedScripts.length + " scripts from scripts directory");
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("load", "unload", "reload", "list", "exec", "loadall");
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("unload") || args[0].equalsIgnoreCase("reload"))) {
            return Arrays.asList(scriptEngine.getLoadedScripts());
        }
        
        return Arrays.asList();
    }
    
    @Override
    public boolean validateArgs(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        
        String action = args[0].toLowerCase();
        switch (action) {
            case "load":
            case "unload":
            case "reload":
                return args.length >= 2;
            case "list":
            case "loadall":
                return true;
            case "exec":
                return args.length >= 2;
            default:
                return false;
        }
    }
}