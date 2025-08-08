package com.odyssey.scripting;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lua scripting engine for server-side scripting hooks.
 * Provides a secure sandbox for executing Lua scripts with game API access.
 */
public class ScriptEngine {
    private static final Logger logger = LoggerFactory.getLogger(ScriptEngine.class);
    
    private final Globals globals;
    private final Map<String, LuaValue> loadedScripts = new ConcurrentHashMap<>();
    private final ScriptAPI scriptAPI;
    private final Path scriptsDirectory;
    
    public ScriptEngine() {
        this.globals = JsePlatform.standardGlobals();
        this.scriptAPI = new ScriptAPI();
        this.scriptsDirectory = Paths.get("scripts");
        
        initializeScriptEnvironment();
    }
    
    /**
     * Initializes the script environment with game API bindings.
     */
    private void initializeScriptEnvironment() {
        logger.info("Initializing Lua script environment");
        
        // Create scripts directory if it doesn't exist
        try {
            Files.createDirectories(scriptsDirectory);
        } catch (IOException e) {
            logger.error("Failed to create scripts directory", e);
        }
        
        // Bind game API to Lua environment
        globals.set("game", scriptAPI.createLuaAPI(globals));
        
        // Remove potentially dangerous functions for security
        globals.set("io", LuaValue.NIL);
        globals.set("os", LuaValue.NIL);
        globals.set("debug", LuaValue.NIL);
        globals.set("package", LuaValue.NIL);
        
        logger.info("Script environment initialized");
    }
    
    /**
     * Loads a Lua script from file.
     * @param scriptName The name of the script (without .lua extension)
     * @return true if script was loaded successfully
     */
    public boolean loadScript(String scriptName) {
        Path scriptPath = scriptsDirectory.resolve(scriptName + ".lua");
        
        if (!Files.exists(scriptPath)) {
            logger.warn("Script file not found: {}", scriptPath);
            return false;
        }
        
        try {
            String scriptContent = Files.readString(scriptPath);
            LuaValue chunk = globals.load(scriptContent, scriptName);
            
            // Execute the script to load functions
            chunk.call();
            
            loadedScripts.put(scriptName, chunk);
            logger.info("Loaded script: {}", scriptName);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to load script '{}': {}", scriptName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Executes a Lua script from string.
     * @param scriptCode The Lua code to execute
     * @param scriptName Name for error reporting
     * @return Script execution result
     */
    public ScriptResult executeScript(String scriptCode, String scriptName) {
        try {
            LuaValue chunk = globals.load(scriptCode, scriptName);
            LuaValue result = chunk.call();
            
            return ScriptResult.success(result);
            
        } catch (Exception e) {
            logger.error("Script execution error in '{}': {}", scriptName, e.getMessage());
            return ScriptResult.error(e.getMessage());
        }
    }
    
    /**
     * Calls a function in a loaded script.
     * @param scriptName The script containing the function
     * @param functionName The function to call
     * @param args Function arguments
     * @return Function call result
     */
    public ScriptResult callFunction(String scriptName, String functionName, Object... args) {
        if (!loadedScripts.containsKey(scriptName)) {
            return ScriptResult.error("Script not loaded: " + scriptName);
        }
        
        try {
            LuaValue function = globals.get(functionName);
            if (function.isnil()) {
                return ScriptResult.error("Function not found: " + functionName);
            }
            
            // Convert Java arguments to Lua values
            LuaValue[] luaArgs = new LuaValue[args.length];
            for (int i = 0; i < args.length; i++) {
                luaArgs[i] = toLuaValue(args[i]);
            }
            
            LuaValue result = function.invoke(luaArgs).arg1();
            return ScriptResult.success(result);
            
        } catch (Exception e) {
            logger.error("Error calling function '{}' in script '{}': {}", 
                        functionName, scriptName, e.getMessage());
            return ScriptResult.error(e.getMessage());
        }
    }
    
    /**
     * Reloads a script from file.
     * @param scriptName The script to reload
     * @return true if reload was successful
     */
    public boolean reloadScript(String scriptName) {
        loadedScripts.remove(scriptName);
        return loadScript(scriptName);
    }
    
    /**
     * Unloads a script.
     * @param scriptName The script to unload
     */
    public void unloadScript(String scriptName) {
        loadedScripts.remove(scriptName);
        logger.info("Unloaded script: {}", scriptName);
    }
    
    /**
     * Gets a list of loaded scripts.
     * @return Array of loaded script names
     */
    public String[] getLoadedScripts() {
        return loadedScripts.keySet().toArray(new String[0]);
    }
    
    /**
     * Loads all scripts from the scripts directory.
     */
    public void loadAllScripts() {
        try {
            Files.list(scriptsDirectory)
                    .filter(path -> path.toString().endsWith(".lua"))
                    .forEach(path -> {
                        String scriptName = path.getFileName().toString();
                        scriptName = scriptName.substring(0, scriptName.length() - 4); // Remove .lua
                        loadScript(scriptName);
                    });
        } catch (IOException e) {
            logger.error("Failed to list scripts directory", e);
        }
    }
    
    /**
     * Converts a Java object to a Lua value.
     */
    private LuaValue toLuaValue(Object obj) {
        if (obj == null) return LuaValue.NIL;
        if (obj instanceof String) return LuaValue.valueOf((String) obj);
        if (obj instanceof Integer) return LuaValue.valueOf((Integer) obj);
        if (obj instanceof Double) return LuaValue.valueOf((Double) obj);
        if (obj instanceof Float) return LuaValue.valueOf((Float) obj);
        if (obj instanceof Boolean) return LuaValue.valueOf((Boolean) obj);
        
        // For complex objects, convert to string representation
        return LuaValue.valueOf(obj.toString());
    }
    
    /**
     * Shuts down the script engine and cleans up resources.
     */
    public void shutdown() {
        logger.info("Shutting down script engine");
        loadedScripts.clear();
    }
}