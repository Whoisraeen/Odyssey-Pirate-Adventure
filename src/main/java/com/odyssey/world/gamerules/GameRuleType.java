package com.odyssey.world.gamerules;

/**
 * Enumeration of game rule value types.
 * Defines the valid types for game rule values and provides validation and parsing.
 */
public enum GameRuleType {
    BOOLEAN {
        @Override
        public boolean isValidValue(Object value) {
            return value instanceof Boolean;
        }

        @Override
        public Object parseValue(String stringValue) {
            if ("true".equalsIgnoreCase(stringValue)) {
                return true;
            } else if ("false".equalsIgnoreCase(stringValue)) {
                return false;
            }
            return null;
        }

        @Override
        public String toString(Object value) {
            return value.toString();
        }
    },

    INTEGER {
        @Override
        public boolean isValidValue(Object value) {
            return value instanceof Integer;
        }

        @Override
        public Object parseValue(String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String toString(Object value) {
            return value.toString();
        }
    },

    FLOAT {
        @Override
        public boolean isValidValue(Object value) {
            return value instanceof Float || value instanceof Double;
        }

        @Override
        public Object parseValue(String stringValue) {
            try {
                return Float.parseFloat(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String toString(Object value) {
            return value.toString();
        }
    },

    STRING {
        @Override
        public boolean isValidValue(Object value) {
            return value instanceof String;
        }

        @Override
        public Object parseValue(String stringValue) {
            return stringValue;
        }

        @Override
        public String toString(Object value) {
            return (String) value;
        }
    };

    /**
     * Checks if the given value is valid for this type.
     * @param value The value to validate
     * @return true if the value is valid for this type
     */
    public abstract boolean isValidValue(Object value);

    /**
     * Parses a string value into the appropriate type.
     * @param stringValue The string representation of the value
     * @return The parsed value, or null if parsing failed
     */
    public abstract Object parseValue(String stringValue);

    /**
     * Converts a value to its string representation.
     * @param value The value to convert
     * @return The string representation of the value
     */
    public abstract String toString(Object value);
}