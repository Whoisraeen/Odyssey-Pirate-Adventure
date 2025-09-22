package com.odyssey.economy;

/**
 * Currency types used in the pirate world economy
 */
public enum Currency {
    GOLD("Gold", "g", 1.0f, true),
    SILVER("Silver", "s", 0.1f, true),
    COPPER("Copper", "c", 0.01f, true),
    DOUBLOONS("Doubloons", "db", 10.0f, true),
    PIECES_OF_EIGHT("Pieces of Eight", "p8", 8.0f, true),
    REPUTATION("Reputation", "rep", 0.0f, false),
    INFLUENCE("Influence", "inf", 0.0f, false);
    
    private final String displayName;
    private final String symbol;
    private final float goldValue; // Value relative to gold
    private final boolean tradeable;
    
    Currency(String displayName, String symbol, float goldValue, boolean tradeable) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.goldValue = goldValue;
        this.tradeable = tradeable;
    }
    
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public float getGoldValue() { return goldValue; }
    public boolean isTradeable() { return tradeable; }
    
    /**
     * Converts an amount of this currency to gold value
     */
    public float toGold(float amount) {
        return amount * goldValue;
    }
    
    /**
     * Converts a gold amount to this currency
     */
    public float fromGold(float goldAmount) {
        if (goldValue == 0.0f) return 0.0f;
        return goldAmount / goldValue;
    }
}