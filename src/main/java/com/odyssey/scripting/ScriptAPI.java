package com.odyssey.scripting;

import com.odyssey.core.Engine;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides game API bindings for Lua scripts.
 * Exposes safe game functionality to the scripting environment.
 */
public class ScriptAPI {
    private static final Logger logger = LoggerFactory.getLogger(ScriptAPI.class);
    
    /**
     * Creates the Lua API table with game functions.
     * @param globals The Lua globals environment
     * @return Lua table containing game API
     */
    public LuaTable createLuaAPI(Globals globals) {
        LuaTable gameAPI = new LuaTable();
        
        // Logging functions
        gameAPI.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                logger.info("[Script] {}", message.tojstring());
                return LuaValue.NIL;
            }
        });
        
        gameAPI.set("warn", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                logger.warn("[Script] {}", message.tojstring());
                return LuaValue.NIL;
            }
        });
        
        gameAPI.set("error", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                logger.error("[Script] {}", message.tojstring());
                return LuaValue.NIL;
            }
        });
        
        // Time functions
        gameAPI.set("getTime", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return LuaValue.valueOf(System.currentTimeMillis());
            }
        });
        
        // Weather control (placeholder - would integrate with actual weather system)
        gameAPI.set("setWeather", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue weatherType) {
                logger.info("Script requested weather change to: {}", weatherType.tojstring());
                // TODO: Integrate with actual weather system
                return LuaValue.TRUE;
            }
        });
        
        // Player functions (placeholder - would integrate with actual player system)
        gameAPI.set("getPlayerCount", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                // TODO: Get actual player count from game state
                return LuaValue.valueOf(0);
            }
        });
        
        gameAPI.set("broadcastMessage", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue message) {
                logger.info("Script broadcast: {}", message.tojstring());
                // TODO: Integrate with actual chat/messaging system
                return LuaValue.TRUE;
            }
        });
        
        // World functions (placeholder - would integrate with actual world system)
        gameAPI.set("getWorldSeed", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                // TODO: Get actual world seed
                return LuaValue.valueOf(12345L);
            }
        });
        
        gameAPI.set("setBlock", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
                logger.info("Script requested block placement at ({}, {}, {})", 
                           x.toint(), y.toint(), z.toint());
                // TODO: Integrate with actual world modification system
                return LuaValue.TRUE;
            }
        });
        
        // Configuration access
        gameAPI.set("getConfig", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue key) {
                try {
                    // TODO: Integrate with actual configuration system
                    logger.info("Script requested config value for: {}", key.tojstring());
                    return LuaValue.valueOf("default_value");
                } catch (Exception e) {
                    logger.error("Error accessing config from script", e);
                    return LuaValue.NIL;
                }
            }
        });
        
        // Event system integration (placeholder)
        gameAPI.set("registerEvent", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue eventName, LuaValue callback) {
                logger.info("Script registered event handler for: {}", eventName.tojstring());
                // TODO: Integrate with actual event system
                return LuaValue.TRUE;
            }
        });
        
        // Command execution
        gameAPI.set("executeCommand", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue command) {
                logger.info("Script requested command execution: {}", command.tojstring());
                // TODO: Integrate with command system
                return LuaValue.TRUE;
            }
        });
        
        // Math utilities
        LuaTable mathAPI = new LuaTable();
        mathAPI.set("random", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue min, LuaValue max) {
                int minVal = min.toint();
                int maxVal = max.toint();
                int result = minVal + (int)(Math.random() * (maxVal - minVal + 1));
                return LuaValue.valueOf(result);
            }
        });
        
        mathAPI.set("distance", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue x1, LuaValue y1, LuaValue z1) {
                // Calculate distance from origin
                double x = x1.todouble();
                double y = y1.todouble();
                double z = z1.todouble();
                double distance = Math.sqrt(x*x + y*y + z*z);
                return LuaValue.valueOf(distance);
            }
        });
        
        gameAPI.set("math", mathAPI);
        
        return gameAPI;
    }
}