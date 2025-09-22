package com.odyssey.world;

import org.joml.Vector2f;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents events that affect trade routes and economic conditions
 */
public class TradeEvent {
    
    private String eventId;
    private String name;
    private String description;
    private TradeEventType type;
    private TradeEventSeverity severity;
    private long startTime;
    private long duration; // In milliseconds
    private boolean isActive;
    
    // Affected area
    private Vector2f center;
    private float radius;
    private String affectedRegion;
    
    // Economic effects
    private Map<String, Float> goodsPriceModifiers; // Good name -> price multiplier
    private Map<String, Float> goodsSupplyModifiers; // Good name -> supply multiplier
    private Map<String, Float> goodsDemandModifiers; // Good name -> demand multiplier
    private float generalTrafficModifier;
    private float safetyModifier;
    private float speedModifier;
    
    // Faction effects
    private Map<String, Integer> factionStandingChanges;
    private String triggeringFaction;
    
    public TradeEvent(String eventId, String name, TradeEventType type, TradeEventSeverity severity,
                      Vector2f center, float radius, long duration) {
        this.eventId = eventId;
        this.name = name;
        this.type = type;
        this.severity = severity;
        this.center = center;
        this.radius = radius;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
        
        this.goodsPriceModifiers = new HashMap<>();
        this.goodsSupplyModifiers = new HashMap<>();
        this.goodsDemandModifiers = new HashMap<>();
        this.factionStandingChanges = new HashMap<>();
        
        // Generate description and effects based on type and severity
        generateDescription();
        generateEffects();
    }
    
    private void generateDescription() {
        StringBuilder desc = new StringBuilder();
        
        switch (type) {
            case PIRATE_RAID:
                desc.append("Pirates have attacked merchant vessels in the area");
                if (severity == TradeEventSeverity.MAJOR) {
                    desc.append(", causing widespread panic among traders");
                }
                break;
            case NAVAL_BLOCKADE:
                desc.append("Naval forces have established a blockade");
                if (severity == TradeEventSeverity.CRITICAL) {
                    desc.append(", completely restricting civilian traffic");
                }
                break;
            case STORM_DAMAGE:
                desc.append("Severe weather has damaged shipping infrastructure");
                break;
            case PLAGUE_OUTBREAK:
                desc.append("Disease outbreak has quarantined the region");
                break;
            case RESOURCE_SHORTAGE:
                desc.append("Critical shortage of essential goods");
                break;
            case MARKET_CRASH:
                desc.append("Economic collapse has destabilized trade");
                break;
            case DIPLOMATIC_CRISIS:
                desc.append("Political tensions have disrupted international trade");
                break;
            case DISCOVERY:
                desc.append("New trade opportunities have been discovered");
                break;
            case FESTIVAL:
                desc.append("Local celebration has increased demand for luxury goods");
                break;
            case WAR:
                desc.append("Military conflict has made the region extremely dangerous");
                break;
        }
        
        this.description = desc.toString();
    }
    
    private void generateEffects() {
        float severityMultiplier = severity.getEffectMultiplier();
        
        switch (type) {
            case PIRATE_RAID:
                safetyModifier = 0.3f * severityMultiplier;
                generalTrafficModifier = 0.7f * severityMultiplier;
                goodsPriceModifiers.put("weapons", 1.3f + severityMultiplier * 0.2f);
                goodsPriceModifiers.put("luxury", 0.8f - severityMultiplier * 0.1f);
                factionStandingChanges.put("Pirates", 5);
                factionStandingChanges.put("Navy", -3);
                break;
                
            case NAVAL_BLOCKADE:
                safetyModifier = 0.9f;
                generalTrafficModifier = 0.2f * severityMultiplier;
                speedModifier = 0.5f * severityMultiplier;
                goodsPriceModifiers.put("contraband", 2.0f + severityMultiplier);
                factionStandingChanges.put("Navy", 3);
                factionStandingChanges.put("Smugglers", -5);
                break;
                
            case STORM_DAMAGE:
                safetyModifier = 0.6f * severityMultiplier;
                speedModifier = 0.7f * severityMultiplier;
                goodsPriceModifiers.put("wood", 1.5f + severityMultiplier * 0.3f);
                goodsPriceModifiers.put("tools", 1.4f + severityMultiplier * 0.2f);
                goodsSupplyModifiers.put("food", 0.8f - severityMultiplier * 0.2f);
                break;
                
            case PLAGUE_OUTBREAK:
                safetyModifier = 0.4f * severityMultiplier;
                generalTrafficModifier = 0.3f * severityMultiplier;
                goodsPriceModifiers.put("medicine", 3.0f + severityMultiplier);
                goodsPriceModifiers.put("food", 1.8f + severityMultiplier * 0.5f);
                goodsSupplyModifiers.put("luxury", 0.2f);
                break;
                
            case RESOURCE_SHORTAGE:
                goodsPriceModifiers.put("food", 2.0f + severityMultiplier);
                goodsPriceModifiers.put("water", 1.8f + severityMultiplier * 0.5f);
                goodsSupplyModifiers.put("food", 0.3f * severityMultiplier);
                goodsDemandModifiers.put("food", 2.0f + severityMultiplier);
                break;
                
            case MARKET_CRASH:
                generalTrafficModifier = 0.5f * severityMultiplier;
                goodsPriceModifiers.put("luxury", 0.4f * severityMultiplier);
                goodsPriceModifiers.put("gold", 0.7f * severityMultiplier);
                goodsDemandModifiers.put("basic_goods", 1.5f + severityMultiplier * 0.3f);
                break;
                
            case DIPLOMATIC_CRISIS:
                safetyModifier = 0.7f * severityMultiplier;
                generalTrafficModifier = 0.6f * severityMultiplier;
                goodsPriceModifiers.put("weapons", 1.6f + severityMultiplier * 0.4f);
                goodsPriceModifiers.put("diplomatic_goods", 0.3f);
                break;
                
            case DISCOVERY:
                generalTrafficModifier = 1.4f + severityMultiplier * 0.3f;
                goodsPriceModifiers.put("exploration_supplies", 1.3f + severityMultiplier * 0.2f);
                goodsDemandModifiers.put("maps", 2.0f + severityMultiplier);
                break;
                
            case FESTIVAL:
                generalTrafficModifier = 1.2f + severityMultiplier * 0.2f;
                goodsPriceModifiers.put("luxury", 1.5f + severityMultiplier * 0.3f);
                goodsPriceModifiers.put("alcohol", 1.4f + severityMultiplier * 0.2f);
                goodsDemandModifiers.put("entertainment", 2.0f + severityMultiplier);
                break;
                
            case WAR:
                safetyModifier = 0.2f * severityMultiplier;
                generalTrafficModifier = 0.4f * severityMultiplier;
                speedModifier = 0.6f * severityMultiplier;
                goodsPriceModifiers.put("weapons", 2.0f + severityMultiplier);
                goodsPriceModifiers.put("medicine", 1.8f + severityMultiplier * 0.4f);
                goodsPriceModifiers.put("food", 1.6f + severityMultiplier * 0.3f);
                goodsSupplyModifiers.put("luxury", 0.3f);
                break;
        }
        
        // Ensure modifiers are within reasonable bounds
        clampModifiers();
    }
    
    private void clampModifiers() {
        safetyModifier = Math.max(0.1f, Math.min(2.0f, safetyModifier));
        generalTrafficModifier = Math.max(0.1f, Math.min(3.0f, generalTrafficModifier));
        speedModifier = Math.max(0.1f, Math.min(2.0f, speedModifier));
        
        // Clamp price modifiers
        for (Map.Entry<String, Float> entry : goodsPriceModifiers.entrySet()) {
            entry.setValue(Math.max(0.1f, Math.min(10.0f, entry.getValue())));
        }
    }
    
    /**
     * Updates the event state
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        
        if (isActive && (currentTime - startTime) >= duration) {
            isActive = false;
        }
        
        // Some events may have evolving effects over time
        if (isActive) {
            updateEvolvingEffects();
        }
    }
    
    private void updateEvolvingEffects() {
        float progress = (float)(System.currentTimeMillis() - startTime) / duration;
        
        switch (type) {
            case PLAGUE_OUTBREAK:
                // Plague effects worsen over time, then improve
                if (progress < 0.7f) {
                    float worsening = 1.0f + progress * 0.5f;
                    goodsPriceModifiers.replaceAll((k, v) -> v * worsening);
                } else {
                    float improvement = 1.0f - (progress - 0.7f) * 0.3f;
                    goodsPriceModifiers.replaceAll((k, v) -> v * improvement);
                }
                break;
                
            case STORM_DAMAGE:
                // Recovery improves conditions over time
                float recovery = 1.0f - progress * 0.3f;
                safetyModifier = Math.min(1.0f, safetyModifier + recovery * 0.1f);
                speedModifier = Math.min(1.0f, speedModifier + recovery * 0.1f);
                break;
        }
    }
    
    /**
     * Checks if this event affects the given position
     */
    public boolean affectsPosition(Vector2f position) {
        if (center == null) return false;
        return center.distance(position) <= radius;
    }
    
    /**
     * Gets the effect intensity at a given position (0.0 to 1.0)
     */
    public float getEffectIntensity(Vector2f position) {
        if (!affectsPosition(position)) return 0.0f;
        
        float distance = center.distance(position);
        float intensity = 1.0f - (distance / radius);
        
        // Apply severity modifier
        intensity *= severity.getEffectMultiplier();
        
        return Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    /**
     * Applies this event's effects to a trade route
     */
    public void applyEffects(TradeRoute route) {
        if (!isActive) return;
        
        // Check if route is affected
        boolean routeAffected = false;
        for (Vector2f waypoint : route.getWaypoints()) {
            if (affectsPosition(waypoint)) {
                routeAffected = true;
                break;
            }
        }
        
        if (!routeAffected) return;
        
        // Apply effects based on how much of the route is affected
        float averageIntensity = calculateAverageIntensity(route);
        
        // Apply traffic modifier
        // Note: This would need to be implemented in TradeRoute class
        // route.modifyTraffic(generalTrafficModifier * averageIntensity);
        
        // Apply safety modifier
        // route.modifySafety(safetyModifier * averageIntensity);
        
        // Apply good price modifiers
        for (Map.Entry<String, Float> entry : goodsPriceModifiers.entrySet()) {
            String good = entry.getKey();
            float modifier = 1.0f + (entry.getValue() - 1.0f) * averageIntensity;
            // route.modifyGoodPrice(good, modifier);
        }
    }
    
    private float calculateAverageIntensity(TradeRoute route) {
        float totalIntensity = 0.0f;
        int affectedWaypoints = 0;
        
        for (Vector2f waypoint : route.getWaypoints()) {
            float intensity = getEffectIntensity(waypoint);
            if (intensity > 0) {
                totalIntensity += intensity;
                affectedWaypoints++;
            }
        }
        
        return affectedWaypoints > 0 ? totalIntensity / affectedWaypoints : 0.0f;
    }
    
    /**
     * Checks if the event has expired
     */
    public boolean isExpired() {
        return !isActive && (System.currentTimeMillis() - startTime) >= duration;
    }
    
    /**
     * Gets the remaining duration in milliseconds
     */
    public long getRemainingDuration() {
        if (!isActive) return 0;
        return Math.max(0, duration - (System.currentTimeMillis() - startTime));
    }
    
    /**
     * Gets the progress of the event (0.0 to 1.0)
     */
    public float getProgress() {
        return Math.min(1.0f, (float)(System.currentTimeMillis() - startTime) / duration);
    }
    
    /**
     * Creates a news report about this event
     */
    public String generateNewsReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("TRADE ALERT: ").append(name).append("\n");
        report.append(description).append("\n\n");
        
        report.append("Severity: ").append(severity.getDisplayName()).append("\n");
        report.append("Affected Region: ").append(affectedRegion != null ? affectedRegion : "Unknown").append("\n");
        
        if (getRemainingDuration() > 0) {
            long hours = getRemainingDuration() / (60 * 60 * 1000);
            report.append("Estimated Duration: ").append(hours).append(" hours\n");
        }
        
        if (!goodsPriceModifiers.isEmpty()) {
            report.append("\nPrice Effects:\n");
            for (Map.Entry<String, Float> entry : goodsPriceModifiers.entrySet()) {
                String effect = entry.getValue() > 1.0f ? "increased" : "decreased";
                int percentage = (int)((Math.abs(entry.getValue() - 1.0f)) * 100);
                report.append("- ").append(entry.getKey()).append(" prices ").append(effect)
                      .append(" by ").append(percentage).append("%\n");
            }
        }
        
        return report.toString();
    }
    
    // Getters and setters
    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public TradeEventType getType() { return type; }
    public TradeEventSeverity getSeverity() { return severity; }
    public long getStartTime() { return startTime; }
    public long getDuration() { return duration; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Vector2f getCenter() { return center; }
    public float getRadius() { return radius; }
    public String getAffectedRegion() { return affectedRegion; }
    public void setAffectedRegion(String affectedRegion) { this.affectedRegion = affectedRegion; }
    public Map<String, Float> getGoodsPriceModifiers() { return new HashMap<>(goodsPriceModifiers); }
    public float getGeneralTrafficModifier() { return generalTrafficModifier; }
    public float getSafetyModifier() { return safetyModifier; }
    public float getSpeedModifier() { return speedModifier; }
    public String getTriggeringFaction() { return triggeringFaction; }
    public void setTriggeringFaction(String triggeringFaction) { this.triggeringFaction = triggeringFaction; }
    
    @Override
    public String toString() {
        return String.format("TradeEvent[%s: %s, %s, %.1f%% complete]", 
                           eventId, name, severity, getProgress() * 100);
    }
}