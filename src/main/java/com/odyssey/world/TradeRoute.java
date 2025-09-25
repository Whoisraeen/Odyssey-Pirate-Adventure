package com.odyssey.world;

import com.odyssey.economy.Currency;
import org.joml.Vector2f;
import java.util.*;

/**
 * Represents a trade route between islands with economic and navigation data
 */
public class TradeRoute {
    
    private String routeId;
    private Island startIsland;
    private Island endIsland;
    private List<Vector2f> waypoints;
    private float distance;
    private TradeRouteType type;
    private TradeRouteStatus status;
    
    // Economic properties
    private Map<String, TradeGood> availableGoods;
    private Map<String, Float> demandMultipliers;
    private Map<String, Float> supplyMultipliers;
    private float baseProfitMargin;
    private Currency preferredCurrency;
    
    // Navigation properties
    private float difficulty; // 0.0 to 1.0
    private Set<Hazard> hazards;
    private float averageTravelTime; // In game hours
    private float safetyRating; // 0.0 to 1.0
    
    // Dynamic properties
    private float currentTraffic; // 0.0 to 1.0
    private long lastUpdated; // Timestamp of last update for data cleanup
    private Map<String, Float> recentPrices;
    private List<TradeEvent> recentEvents;
    
    public TradeRoute(String routeId, Island startIsland, Island endIsland, TradeRouteType type) {
        this.routeId = routeId;
        this.startIsland = startIsland;
        this.endIsland = endIsland;
        this.type = type;
        this.status = TradeRouteStatus.ACTIVE;
        this.waypoints = new ArrayList<>();
        this.availableGoods = new HashMap<>();
        this.demandMultipliers = new HashMap<>();
        this.supplyMultipliers = new HashMap<>();
        this.hazards = new HashSet<>();
        this.recentPrices = new HashMap<>();
        this.recentEvents = new ArrayList<>();
        this.lastUpdated = System.currentTimeMillis();
        this.preferredCurrency = Currency.GOLD;
        
        calculateRouteProperties();
        generateTradeData();
    }
    
    /**
     * Helper method to get island position as Vector2f
     */
    private Vector2f getIslandPosition(Island island) {
        return new Vector2f(island.getCenterX(), island.getCenterZ());
    }
    
    private void calculateRouteProperties() {
        // Calculate direct distance
        Vector2f startPos = getIslandPosition(startIsland);
        Vector2f endPos = getIslandPosition(endIsland);
        this.distance = startPos.distance(endPos);
        
        // Generate waypoints for navigation
        generateWaypoints();
        
        // Calculate difficulty based on distance and island types
        this.difficulty = calculateDifficulty();
        
        // Calculate average travel time
        this.averageTravelTime = calculateTravelTime();
        
        // Calculate safety rating
        this.safetyRating = calculateSafetyRating();
        
        // Set base profit margin
        this.baseProfitMargin = type.getBaseProfitMargin() * (1.0f + difficulty * 0.5f);
    }
    
    private void generateWaypoints() {
        waypoints.clear();
        Vector2f startPos = getIslandPosition(startIsland);
        Vector2f endPos = getIslandPosition(endIsland);
        waypoints.add(startPos);
        
        // Add intermediate waypoints for longer routes
        if (distance > 1000.0f) {
            int numWaypoints = (int)(distance / 500.0f);
            Vector2f direction = new Vector2f(
                endPos.x - startPos.x,
                endPos.y - startPos.y
            );
            direction.normalize();
            
            for (int i = 1; i <= numWaypoints; i++) {
                float progress = (float)i / (numWaypoints + 1);
                Vector2f waypoint = new Vector2f(
                    startPos.x + direction.x * distance * progress,
                    startPos.y + direction.y * distance * progress
                );
                
                // Add some variation to avoid straight lines
                waypoint.x += (float)((Math.random() - 0.5) * 100.0);
                waypoint.y += (float)((Math.random() - 0.5) * 100.0);
                
                waypoints.add(waypoint);
            }
        }
        
        waypoints.add(endPos);
    }
    
    private float calculateDifficulty() {
        float diff = 0.0f;
        
        // Distance factor
        diff += Math.min(0.4f, distance / 5000.0f);
        
        // Island type factor
        if (startIsland.getType() == IslandType.VOLCANIC_SMALL || startIsland.getType() == IslandType.VOLCANIC_LARGE ||
            endIsland.getType() == IslandType.VOLCANIC_SMALL || endIsland.getType() == IslandType.VOLCANIC_LARGE) {
            diff += 0.2f;
        }
        if (startIsland.getType() == IslandType.SWAMP_MEDIUM || endIsland.getType() == IslandType.SWAMP_MEDIUM) {
            diff += 0.3f;
        }
        
        // Route type factor
        diff += type.getDifficultyModifier();
        
        return Math.min(1.0f, diff);
    }
    
    private float calculateTravelTime() {
        // Base time calculation (assuming average ship speed)
        float baseTime = distance / 50.0f; // 50 units per hour average speed
        
        // Modify based on difficulty and waypoints
        baseTime *= (1.0f + difficulty * 0.5f);
        baseTime *= (1.0f + waypoints.size() * 0.1f);
        
        return baseTime;
    }
    
    private float calculateSafetyRating() {
        float safety = 1.0f;
        
        // Reduce safety based on difficulty
        safety -= difficulty * 0.4f;
        
        // Reduce safety based on hazards
        safety -= hazards.size() * 0.1f;
        
        // Route type affects safety
        safety *= type.getSafetyModifier();
        
        return Math.max(0.0f, Math.min(1.0f, safety));
    }
    
    private void generateTradeData() {
        // Generate available goods based on island types and route type
        generateAvailableGoods();
        
        // Generate demand and supply multipliers
        generateMarketMultipliers();
        
        // Set preferred currency based on route
        determinePreferredCurrency();
    }
    
    private void generateAvailableGoods() {
        availableGoods.clear();
        
        // Add goods based on start island production
        for (String good : startIsland.getProducedGoods()) {
            TradeGood tradeGood = new TradeGood(good, 
                getBasePrice(good), 
                startIsland.getProductionRate(good),
                endIsland.getDemandRate(good));
            availableGoods.put(good, tradeGood);
        }
        
        // Add route-specific goods
        for (String good : type.getSpecialGoods()) {
            if (!availableGoods.containsKey(good)) {
                TradeGood tradeGood = new TradeGood(good, 
                    getBasePrice(good), 
                    0.5f, 0.7f);
                availableGoods.put(good, tradeGood);
            }
        }
    }
    
    private void generateMarketMultipliers() {
        demandMultipliers.clear();
        supplyMultipliers.clear();
        
        for (String good : availableGoods.keySet()) {
            // Demand is higher at destination
            float demand = 0.8f + (float)Math.random() * 0.4f;
            if (endIsland.getNeededGoods().contains(good)) {
                demand += 0.3f;
            }
            demandMultipliers.put(good, Math.min(2.0f, demand));
            
            // Supply is higher at source
            float supply = 0.6f + (float)Math.random() * 0.3f;
            if (startIsland.getProducedGoods().contains(good)) {
                supply += 0.4f;
            }
            supplyMultipliers.put(good, Math.min(2.0f, supply));
        }
    }
    
    private void determinePreferredCurrency() {
        // Determine preferred currency based on islands and route type
        if (type == TradeRouteType.LUXURY || type == TradeRouteType.EXOTIC) {
            preferredCurrency = Currency.DOUBLOONS;
        } else if (startIsland.getType() == IslandType.ROCKY_SMALL || 
                   endIsland.getType() == IslandType.ROCKY_SMALL) {
            preferredCurrency = Currency.PIECES_OF_EIGHT;
        } else {
            preferredCurrency = Currency.GOLD;
        }
    }
    
    /**
     * Updates the trade route's dynamic properties
     */
    public void update(float deltaTime) {
        long currentTime = System.currentTimeMillis();
        
        // Update traffic based on time and events
        updateTraffic(deltaTime);
        
        // Update prices based on supply and demand
        updatePrices();
        
        // Process recent events
        processEvents();
        
        // Clean up old data
        cleanupOldData(currentTime);
        
        lastUpdated = currentTime;
    }
    
    private void updateTraffic(float deltaTime) {
        // Traffic fluctuates based on various factors
        float targetTraffic = type.getBaseTraffic();
        
        // Modify based on safety and profitability
        targetTraffic *= safetyRating;
        targetTraffic *= (1.0f + baseProfitMargin);
        
        // Add some randomness
        targetTraffic += (float)(Math.random() - 0.5) * 0.2f;
        targetTraffic = Math.max(0.0f, Math.min(1.0f, targetTraffic));
        
        // Smooth transition
        float diff = targetTraffic - currentTraffic;
        currentTraffic += diff * deltaTime * 0.1f;
    }
    
    private void updatePrices() {
        for (Map.Entry<String, TradeGood> entry : availableGoods.entrySet()) {
            String good = entry.getKey();
            TradeGood tradeGood = entry.getValue();
            
            // Calculate current price based on supply, demand, and traffic
            float basePrice = tradeGood.getBasePrice();
            float supply = supplyMultipliers.getOrDefault(good, 1.0f);
            float demand = demandMultipliers.getOrDefault(good, 1.0f);
            
            float currentPrice = basePrice * (demand / supply) * (1.0f + currentTraffic * 0.2f);
            recentPrices.put(good, currentPrice);
        }
    }
    
    private void processEvents() {
        // Remove expired events and apply their effects
        recentEvents.removeIf(event -> event.isExpired());
        
        // Apply event effects to multipliers
        for (TradeEvent event : recentEvents) {
            event.applyEffects(this);
        }
    }
    
    private void cleanupOldData(long currentTime) {
        // Remove price data older than 1 hour
        // In a real implementation, you'd track timestamps for each price entry
    }
    
    /**
     * Adds a trade event to this route
     */
    public void addTradeEvent(TradeEvent event) {
        recentEvents.add(event);
    }
    
    /**
     * Gets the current price for a good on this route
     */
    public float getCurrentPrice(String good) {
        return recentPrices.getOrDefault(good, getBasePrice(good));
    }
    
    /**
     * Gets the profit potential for trading a specific good
     */
    public float getProfitPotential(String good, float quantity) {
        if (!availableGoods.containsKey(good)) return 0.0f;
        
        float buyPrice = getCurrentPrice(good) * supplyMultipliers.getOrDefault(good, 1.0f);
        float sellPrice = getCurrentPrice(good) * demandMultipliers.getOrDefault(good, 1.0f);
        
        float profit = (sellPrice - buyPrice) * quantity;
        
        // Apply route profit margin
        profit *= (1.0f + baseProfitMargin);
        
        // Reduce profit based on risk
        profit *= safetyRating;
        
        return profit;
    }
    
    /**
     * Checks if the route is currently safe for travel
     */
    public boolean isSafeForTravel() {
        return status == TradeRouteStatus.ACTIVE && safetyRating > 0.3f;
    }
    
    /**
     * Gets the recommended ship type for this route
     */
    public String getRecommendedShipType() {
        if (difficulty > 0.7f) {
            return "Warship";
        } else if (type == TradeRouteType.LUXURY) {
            return "Fast Merchant";
        } else if (distance > 2000.0f) {
            return "Large Merchant";
        } else {
            return "Standard Merchant";
        }
    }
    
    private float getBasePrice(String good) {
        // Base prices for different goods
        switch (good.toLowerCase()) {
            case "rum": return 50.0f;
            case "sugar": return 30.0f;
            case "tobacco": return 80.0f;
            case "cotton": return 40.0f;
            case "spices": return 120.0f;
            case "silk": return 200.0f;
            case "gold": return 500.0f;
            case "silver": return 300.0f;
            case "gems": return 800.0f;
            case "weapons": return 150.0f;
            case "gunpowder": return 100.0f;
            case "food": return 20.0f;
            case "water": return 10.0f;
            case "wood": return 25.0f;
            case "iron": return 60.0f;
            default: return 50.0f;
        }
    }
    
    // Getters and setters
    public String getRouteId() { return routeId; }
    public String getName() { return routeId; } // Alias for routeId
    public Island getStartIsland() { return startIsland; }
    public Island getEndIsland() { return endIsland; }
    public List<Vector2f> getWaypoints() { return new ArrayList<>(waypoints); }
    public float getDistance() { return distance; }
    public TradeRouteType getType() { return type; }
    public TradeRouteStatus getStatus() { return status; }
    public void setStatus(TradeRouteStatus status) { this.status = status; }
    public Map<String, TradeGood> getAvailableGoods() { return new HashMap<>(availableGoods); }
    public float getDifficulty() { return difficulty; }
    public Set<Hazard> getHazards() { return new HashSet<>(hazards); }
    public float getAverageTravelTime() { return averageTravelTime; }
    public float getSafetyRating() { return safetyRating; }
    public float getCurrentTraffic() { return currentTraffic; }
    public Currency getPreferredCurrency() { return preferredCurrency; }
    public float getBaseProfitMargin() { return baseProfitMargin; }
    public long getLastUpdated() { return lastUpdated; }
    
    public void addHazard(Hazard hazard) { 
        hazards.add(hazard);
        // Recalculate safety rating
        this.safetyRating = calculateSafetyRating();
    }
    
    public void removeHazard(Hazard hazard) { 
        hazards.remove(hazard);
        // Recalculate safety rating
        this.safetyRating = calculateSafetyRating();
    }
    
    @Override
    public String toString() {
        return String.format("TradeRoute[%s: %s -> %s, %.1fkm, %s, safety=%.2f]",
            routeId, startIsland.getName(), endIsland.getName(), 
            distance / 1000.0f, type, safetyRating);
    }
}