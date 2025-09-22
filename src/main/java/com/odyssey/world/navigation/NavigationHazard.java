package com.odyssey.world.navigation;

import com.odyssey.world.Hazard;
import com.odyssey.world.HazardType;
import com.odyssey.world.HazardSeverity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.ArrayList;

/**
 * Specialized hazard class for navigation-specific dangers that affect ship movement and trade routes
 */
public class NavigationHazard extends Hazard {
    
    // Navigation-specific properties
    private float navigationPenalty; // Additional penalty for navigation difficulty
    private boolean blocksPassage; // Whether this hazard completely blocks passage
    private float minimumSkillLevel; // Minimum navigation skill to safely traverse
    private float recommendedDetourDistance; // Suggested detour distance
    
    // Route impact
    private List<String> affectedRouteIds; // Trade routes affected by this hazard
    private float routeDelayMultiplier; // How much this hazard delays travel
    private float routeCostMultiplier; // Additional cost for traversing this hazard
    
    // Detection and avoidance
    private float detectionRange; // Range at which this hazard can be detected
    private boolean requiresSpecialEquipment; // Whether special equipment is needed to detect
    private String recommendedAvoidanceStrategy; // Suggested way to avoid or mitigate
    
    public NavigationHazard(String hazardId, String name, HazardType type, HazardSeverity severity,
                           Vector2f position, float radius) {
        super(hazardId, name, type, severity, position, radius);
        
        this.affectedRouteIds = new ArrayList<>();
        initializeNavigationProperties();
    }
    
    private void initializeNavigationProperties() {
        // Calculate navigation-specific properties based on hazard type and severity
        this.navigationPenalty = getType().getBaseNavigationDifficulty() * getSeverity().getDifficultyMultiplier();
        this.blocksPassage = calculatePassageBlocking();
        this.minimumSkillLevel = calculateMinimumSkillLevel();
        this.recommendedDetourDistance = getRadius() * 2.0f + (getSeverity().ordinal() * 50.0f);
        this.routeDelayMultiplier = 1.0f + (navigationPenalty * 0.5f);
        this.routeCostMultiplier = 1.0f + (navigationPenalty * 0.3f);
        this.detectionRange = calculateDetectionRange();
        this.requiresSpecialEquipment = determineSpecialEquipmentRequirement();
        this.recommendedAvoidanceStrategy = generateAvoidanceStrategy();
    }
    
    private boolean calculatePassageBlocking() {
        // Certain hazard types completely block passage
        switch (getType()) {
            case SHALLOW_WATERS:
            case ICE_FIELD:
            case VOLCANIC_ACTIVITY:
                return getSeverity().ordinal() >= HazardSeverity.SEVERE.ordinal();
            case WHIRLPOOL:
            case STORM_SYSTEM:
                return getSeverity() == HazardSeverity.CATASTROPHIC;
            default:
                return false;
        }
    }
    
    private float calculateMinimumSkillLevel() {
        float baseSkill = 0.0f;
        
        switch (getType()) {
            case SHALLOW_WATERS:
                baseSkill = 20.0f;
                break;
            case REEF:
                baseSkill = 30.0f;
                break;
            case WHIRLPOOL:
                baseSkill = 60.0f;
                break;
            case STORM_SYSTEM:
                baseSkill = 50.0f;
                break;
            case ICE_FIELD:
                baseSkill = 40.0f;
                break;
            case FOG_BANK:
                baseSkill = 25.0f;
                break;
            case SIREN_WATERS:
            case CURSED_WATERS:
                baseSkill = 70.0f;
                break;
            case GHOST_SHIP:
                baseSkill = 80.0f;
                break;
            case PIRATE_AMBUSH:
                baseSkill = 35.0f;
                break;
            default:
                baseSkill = 10.0f;
        }
        
        return baseSkill + (getSeverity().ordinal() * 15.0f);
    }
    
    private float calculateDetectionRange() {
        float baseRange = getRadius() * 1.5f;
        
        // Adjust based on hazard type
        switch (getType()) {
            case FOG_BANK:
                return baseRange * 0.3f; // Hard to see in fog
            case STORM_SYSTEM:
                return baseRange * 2.0f; // Storms are visible from far away
            case SHALLOW_WATERS:
            case REEF:
                return baseRange * 0.8f; // Need to be closer to detect
            case GHOST_SHIP:
            case SIREN_WATERS:
                return baseRange * 0.5f; // Supernatural hazards are harder to detect
            default:
                return baseRange;
        }
    }
    
    private boolean determineSpecialEquipmentRequirement() {
        switch (getType()) {
            case SHALLOW_WATERS:
            case REEF:
                return true; // Requires depth sounder
            case GHOST_SHIP:
            case SIREN_WATERS:
            case CURSED_WATERS:
                return true; // Requires magical detection
            default:
                return false;
        }
    }
    
    private String generateAvoidanceStrategy() {
        switch (getType()) {
            case SHALLOW_WATERS:
                return "Use depth charts and maintain safe distance from shore";
            case REEF:
                return "Navigate around reef using marked channels";
            case WHIRLPOOL:
                return "Maintain distance and use engine power to avoid being drawn in";
            case STORM_SYSTEM:
                return "Wait for storm to pass or take wide detour";
            case ICE_FIELD:
                return "Find ice-free passage or wait for seasonal melting";
            case FOG_BANK:
                return "Reduce speed, use fog signals, and navigate by compass";
            case SIREN_WATERS:
                return "Block ears, avoid eye contact, and maintain course discipline";
            case CURSED_WATERS:
                return "Carry protective charms and avoid lingering in the area";
            case GHOST_SHIP:
                return "Do not engage, change course immediately";
            case PIRATE_AMBUSH:
                return "Travel in convoy, maintain vigilance, and be prepared to flee";
            default:
                return "Exercise caution and maintain safe distance";
        }
    }
    
    /**
     * Calculates the navigation difficulty for a ship with given skill level
     */
    public float getNavigationDifficultyForSkill(float skillLevel) {
        if (skillLevel >= minimumSkillLevel) {
            float skillBonus = (skillLevel - minimumSkillLevel) / 100.0f;
            return Math.max(0.1f, navigationPenalty - skillBonus);
        } else {
            float skillPenalty = (minimumSkillLevel - skillLevel) / 100.0f;
            return Math.min(1.0f, navigationPenalty + skillPenalty);
        }
    }
    
    /**
     * Checks if a ship can safely traverse this hazard
     */
    public boolean canSafelyTraverse(float skillLevel, boolean hasSpecialEquipment) {
        if (blocksPassage) {
            return false;
        }
        
        if (requiresSpecialEquipment && !hasSpecialEquipment) {
            return false;
        }
        
        return skillLevel >= minimumSkillLevel * 0.8f; // Allow some leeway
    }
    
    /**
     * Gets the recommended course adjustment to avoid this hazard
     */
    public Vector2f getRecommendedCourseAdjustment(Vector2f currentPosition, Vector2f targetPosition) {
        Vector2f toHazard = new Vector2f(getPosition().x, getPosition().y).sub(currentPosition);
        Vector2f toTarget = new Vector2f(targetPosition).sub(currentPosition);
        
        // Calculate perpendicular vector for detour
        Vector2f perpendicular = new Vector2f(-toHazard.y, toHazard.x).normalize();
        
        // Determine which side to go around based on target direction
        if (perpendicular.dot(toTarget) < 0) {
            perpendicular.mul(-1);
        }
        
        return perpendicular.mul(recommendedDetourDistance);
    }
    
    /**
     * Updates the hazard's effect on trade routes
     */
    public void updateRouteImpacts() {
        // This would be called by the trade route system to update impacts
        // Implementation depends on how trade routes are managed
    }
    
    // Getters and setters
    public float getNavigationPenalty() { return navigationPenalty; }
    public boolean blocksPassage() { return blocksPassage; }
    public float getMinimumSkillLevel() { return minimumSkillLevel; }
    public float getRecommendedDetourDistance() { return recommendedDetourDistance; }
    public List<String> getAffectedRouteIds() { return new ArrayList<>(affectedRouteIds); }
    public float getRouteDelayMultiplier() { return routeDelayMultiplier; }
    public float getRouteCostMultiplier() { return routeCostMultiplier; }
    public float getDetectionRange() { return detectionRange; }
    public boolean requiresSpecialEquipment() { return requiresSpecialEquipment; }
    public String getRecommendedAvoidanceStrategy() { return recommendedAvoidanceStrategy; }
    
    public void addAffectedRoute(String routeId) {
        if (!affectedRouteIds.contains(routeId)) {
            affectedRouteIds.add(routeId);
        }
    }
    
    public void removeAffectedRoute(String routeId) {
        affectedRouteIds.remove(routeId);
    }
    
    /**
     * Gets the 3D position for compatibility with existing code
     */
    public Vector3f getPosition3D() {
        Vector2f pos2d = super.getPosition();
        return new Vector3f(pos2d.x, 0.0f, pos2d.y);
    }
    
    @Override
    public String toString() {
        return String.format("NavigationHazard[%s: %s at (%.1f, %.1f), skill_req=%.1f, blocks=%s]",
                           getHazardId(), getName(), getPosition().x, getPosition().y, 
                           minimumSkillLevel, blocksPassage);
    }
}