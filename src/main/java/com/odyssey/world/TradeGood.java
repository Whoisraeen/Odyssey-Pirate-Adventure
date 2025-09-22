package com.odyssey.world;

/**
 * Represents a tradeable good with economic properties
 */
public class TradeGood {
    
    private String name;
    private String category;
    private float basePrice;
    private float currentPrice;
    private float weight; // Per unit
    private float volume; // Per unit
    private int stackSize; // Maximum units per stack
    
    // Economic properties
    private float volatility; // Price fluctuation tendency (0.0 to 1.0)
    private float demand; // Current demand level (0.0 to 2.0)
    private float supply; // Current supply level (0.0 to 2.0)
    private float perishability; // How quickly it spoils (0.0 = never, 1.0 = very fast)
    
    // Trade properties
    private boolean isLegal;
    private boolean isLuxury;
    private boolean isRare;
    private boolean isHazardous;
    private String origin; // Where it's typically produced
    private String[] preferredDestinations; // Where it's in high demand
    
    // Quality and condition
    private float quality; // 0.0 to 1.0
    private float condition; // 0.0 to 1.0 (affects price)
    private long lastUpdated;
    
    public TradeGood(String name, float basePrice, float supply, float demand) {
        this.name = name;
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.supply = supply;
        this.demand = demand;
        this.quality = 1.0f;
        this.condition = 1.0f;
        this.lastUpdated = System.currentTimeMillis();
        
        // Set defaults based on good type
        initializeDefaults();
    }
    
    public TradeGood(String name, String category, float basePrice, float weight, float volume, 
                     int stackSize, float volatility, boolean isLegal, boolean isLuxury, 
                     boolean isRare, boolean isHazardous, String origin) {
        this.name = name;
        this.category = category;
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.weight = weight;
        this.volume = volume;
        this.stackSize = stackSize;
        this.volatility = volatility;
        this.isLegal = isLegal;
        this.isLuxury = isLuxury;
        this.isRare = isRare;
        this.isHazardous = isHazardous;
        this.origin = origin;
        this.quality = 1.0f;
        this.condition = 1.0f;
        this.demand = 1.0f;
        this.supply = 1.0f;
        this.perishability = 0.0f;
        this.lastUpdated = System.currentTimeMillis();
        
        // Set category-specific defaults
        setCategoryDefaults();
    }
    
    private void initializeDefaults() {
        // Determine category and properties based on name
        String lowerName = name.toLowerCase();
        
        if (lowerName.contains("rum") || lowerName.contains("wine") || lowerName.contains("ale")) {
            this.category = "Beverages";
            this.weight = 1.0f;
            this.volume = 1.0f;
            this.stackSize = 50;
            this.volatility = 0.2f;
            this.isLegal = true;
            this.perishability = 0.1f;
        } else if (lowerName.contains("spice") || lowerName.contains("pepper") || lowerName.contains("cinnamon")) {
            this.category = "Spices";
            this.weight = 0.1f;
            this.volume = 0.1f;
            this.stackSize = 100;
            this.volatility = 0.4f;
            this.isLegal = true;
            this.isLuxury = true;
            this.perishability = 0.05f;
        } else if (lowerName.contains("gold") || lowerName.contains("silver") || lowerName.contains("gem")) {
            this.category = "Precious Metals";
            this.weight = 2.0f;
            this.volume = 0.1f;
            this.stackSize = 20;
            this.volatility = 0.3f;
            this.isLegal = true;
            this.isLuxury = true;
            this.isRare = true;
        } else if (lowerName.contains("weapon") || lowerName.contains("sword") || lowerName.contains("gun")) {
            this.category = "Weapons";
            this.weight = 3.0f;
            this.volume = 2.0f;
            this.stackSize = 10;
            this.volatility = 0.5f;
            this.isLegal = false; // Often restricted
            this.isHazardous = true;
        } else if (lowerName.contains("food") || lowerName.contains("bread") || lowerName.contains("meat")) {
            this.category = "Food";
            this.weight = 0.5f;
            this.volume = 0.5f;
            this.stackSize = 100;
            this.volatility = 0.6f;
            this.isLegal = true;
            this.perishability = 0.8f;
        } else {
            // Default values
            this.category = "General Goods";
            this.weight = 1.0f;
            this.volume = 1.0f;
            this.stackSize = 50;
            this.volatility = 0.3f;
            this.isLegal = true;
            this.perishability = 0.1f;
        }
        
        setCategoryDefaults();
    }
    
    private void setCategoryDefaults() {
        switch (category.toLowerCase()) {
            case "beverages":
                this.preferredDestinations = new String[]{"Pirate Haven", "Port Town", "Naval Base"};
                break;
            case "spices":
                this.preferredDestinations = new String[]{"Colonial Settlement", "Trading Post", "Luxury Market"};
                break;
            case "precious metals":
                this.preferredDestinations = new String[]{"Banking Center", "Jewelry Market", "Royal Court"};
                break;
            case "weapons":
                this.preferredDestinations = new String[]{"Military Outpost", "Pirate Haven", "Frontier Settlement"};
                break;
            case "food":
                this.preferredDestinations = new String[]{"Any Settlement", "Naval Base", "Remote Outpost"};
                break;
            default:
                this.preferredDestinations = new String[]{"Trading Post", "Port Town"};
                break;
        }
    }
    
    /**
     * Updates the current price based on supply and demand
     */
    public void updatePrice() {
        // Base price calculation
        float priceMultiplier = demand / supply;
        
        // Apply volatility
        float volatilityFactor = 1.0f + (float)(Math.random() - 0.5) * volatility;
        priceMultiplier *= volatilityFactor;
        
        // Apply quality and condition modifiers
        priceMultiplier *= quality * condition;
        
        // Apply perishability (reduces price over time)
        long timeSinceUpdate = System.currentTimeMillis() - lastUpdated;
        float perishabilityFactor = 1.0f - (perishability * timeSinceUpdate / (24 * 60 * 60 * 1000)); // Per day
        perishabilityFactor = Math.max(0.1f, perishabilityFactor);
        
        this.currentPrice = basePrice * priceMultiplier * perishabilityFactor;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Gets the profit margin when selling at current market conditions
     */
    public float getProfitMargin() {
        return (currentPrice - basePrice) / basePrice;
    }
    
    /**
     * Calculates the total weight for a given quantity
     */
    public float getTotalWeight(int quantity) {
        return weight * quantity;
    }
    
    /**
     * Calculates the total volume for a given quantity
     */
    public float getTotalVolume(int quantity) {
        return volume * quantity;
    }
    
    /**
     * Gets the maximum quantity that can fit in given cargo space
     */
    public int getMaxQuantityForSpace(float availableWeight, float availableVolume) {
        int maxByWeight = (int)(availableWeight / weight);
        int maxByVolume = (int)(availableVolume / volume);
        return Math.min(maxByWeight, maxByVolume);
    }
    
    /**
     * Checks if this good is in high demand at the destination
     */
    public boolean isInDemandAt(String destination) {
        for (String preferred : preferredDestinations) {
            if (preferred.equalsIgnoreCase(destination) || preferred.equals("Any Settlement")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the risk factor for carrying this good
     */
    public float getRiskFactor() {
        float risk = 0.0f;
        
        if (!isLegal) risk += 0.5f;
        if (isHazardous) risk += 0.3f;
        if (isRare) risk += 0.2f; // Attracts thieves
        if (isLuxury) risk += 0.1f; // Valuable target
        
        return Math.min(1.0f, risk);
    }
    
    /**
     * Gets the storage requirements description
     */
    public String getStorageRequirements() {
        StringBuilder requirements = new StringBuilder();
        
        if (perishability > 0.5f) {
            requirements.append("Refrigerated storage required. ");
        }
        if (isHazardous) {
            requirements.append("Hazardous materials protocols. ");
        }
        if (isLuxury) {
            requirements.append("Secure storage recommended. ");
        }
        if (volatility > 0.7f) {
            requirements.append("Price monitoring essential. ");
        }
        
        if (requirements.length() == 0) {
            return "Standard storage conditions.";
        }
        
        return requirements.toString().trim();
    }
    
    /**
     * Degrades the condition over time
     */
    public void degradeCondition(float deltaTime) {
        if (perishability > 0) {
            float degradation = perishability * deltaTime * 0.001f; // Slow degradation
            condition = Math.max(0.0f, condition - degradation);
        }
    }
    
    /**
     * Creates a copy of this trade good
     */
    public TradeGood copy() {
        TradeGood copy = new TradeGood(name, category, basePrice, weight, volume, stackSize, 
                                      volatility, isLegal, isLuxury, isRare, isHazardous, origin);
        copy.currentPrice = this.currentPrice;
        copy.demand = this.demand;
        copy.supply = this.supply;
        copy.quality = this.quality;
        copy.condition = this.condition;
        copy.perishability = this.perishability;
        copy.preferredDestinations = this.preferredDestinations.clone();
        return copy;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public float getBasePrice() { return basePrice; }
    public void setBasePrice(float basePrice) { this.basePrice = basePrice; }
    
    public float getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(float currentPrice) { this.currentPrice = currentPrice; }
    
    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }
    
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }
    
    public int getStackSize() { return stackSize; }
    public void setStackSize(int stackSize) { this.stackSize = stackSize; }
    
    public float getVolatility() { return volatility; }
    public void setVolatility(float volatility) { this.volatility = volatility; }
    
    public float getDemand() { return demand; }
    public void setDemand(float demand) { this.demand = demand; }
    
    public float getSupply() { return supply; }
    public void setSupply(float supply) { this.supply = supply; }
    
    public float getPerishability() { return perishability; }
    public void setPerishability(float perishability) { this.perishability = perishability; }
    
    public boolean isLegal() { return isLegal; }
    public void setLegal(boolean legal) { isLegal = legal; }
    
    public boolean isLuxury() { return isLuxury; }
    public void setLuxury(boolean luxury) { isLuxury = luxury; }
    
    public boolean isRare() { return isRare; }
    public void setRare(boolean rare) { isRare = rare; }
    
    public boolean isHazardous() { return isHazardous; }
    public void setHazardous(boolean hazardous) { isHazardous = hazardous; }
    
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    
    public String[] getPreferredDestinations() { return preferredDestinations; }
    public void setPreferredDestinations(String[] preferredDestinations) { 
        this.preferredDestinations = preferredDestinations; 
    }
    
    public float getQuality() { return quality; }
    public void setQuality(float quality) { this.quality = Math.max(0.0f, Math.min(1.0f, quality)); }
    
    public float getCondition() { return condition; }
    public void setCondition(float condition) { this.condition = Math.max(0.0f, Math.min(1.0f, condition)); }
    
    @Override
    public String toString() {
        return String.format("%s (%.2f gold, %s, Q:%.1f, C:%.1f)", 
                           name, currentPrice, category, quality, condition);
    }
}