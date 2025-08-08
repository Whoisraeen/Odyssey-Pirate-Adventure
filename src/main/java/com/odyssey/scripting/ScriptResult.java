package com.odyssey.scripting;

import org.luaj.vm2.LuaValue;

/**
 * Represents the result of a script execution or function call.
 */
public class ScriptResult {
    private final boolean success;
    private final String errorMessage;
    private final LuaValue result;
    
    private ScriptResult(boolean success, String errorMessage, LuaValue result) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.result = result;
    }
    
    /**
     * Creates a successful script result.
     * @param result The Lua value result
     * @return Success result
     */
    public static ScriptResult success(LuaValue result) {
        return new ScriptResult(true, null, result);
    }
    
    /**
     * Creates an error script result.
     * @param errorMessage The error message
     * @return Error result
     */
    public static ScriptResult error(String errorMessage) {
        return new ScriptResult(false, errorMessage, LuaValue.NIL);
    }
    
    /**
     * Checks if the script execution was successful.
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Gets the error message if execution failed.
     * @return Error message or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Gets the Lua result value.
     * @return Lua value result
     */
    public LuaValue getResult() {
        return result;
    }
    
    /**
     * Gets the result as a string.
     * @return String representation of result
     */
    public String getResultAsString() {
        if (!success) return errorMessage;
        return result.isnil() ? "nil" : result.tojstring();
    }
    
    /**
     * Gets the result as an integer.
     * @return Integer value or 0 if not a number
     */
    public int getResultAsInt() {
        if (!success || result.isnil()) return 0;
        return result.toint();
    }
    
    /**
     * Gets the result as a double.
     * @return Double value or 0.0 if not a number
     */
    public double getResultAsDouble() {
        if (!success || result.isnil()) return 0.0;
        return result.todouble();
    }
    
    /**
     * Gets the result as a boolean.
     * @return Boolean value
     */
    public boolean getResultAsBoolean() {
        if (!success || result.isnil()) return false;
        return result.toboolean();
    }
    
    @Override
    public String toString() {
        if (success) {
            return "ScriptResult{success=true, result=" + getResultAsString() + "}";
        } else {
            return "ScriptResult{success=false, error='" + errorMessage + "'}";
        }
    }
}