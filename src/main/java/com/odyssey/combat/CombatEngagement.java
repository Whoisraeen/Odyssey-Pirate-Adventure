package com.odyssey.combat;

import com.odyssey.ship.Ship;
import com.odyssey.util.Logger;

/**
 * CombatEngagement represents an active combat encounter between two ships.
 * It tracks the duration of combat, participants, and combat statistics.
 * 
 * This class helps manage combat state and determine when engagements
 * should end due to distance or time constraints.
 */
public class CombatEngagement {
    private static final Logger logger = Logger.getLogger(CombatEngagement.class);
    
    private final Ship attacker;
    private final Ship defender;
    private final long startTime;
    private long lastCombatActivity;
    
    // Combat statistics
    private int attackerShotsFired;
    private int defenderShotsFired;
    private int attackerHits;
    private int defenderHits;
    private float totalDamageDealt;
    private float totalDamageReceived;
    
    /**
     * Creates a new combat engagement between two ships.
     * 
     * @param attacker The ship that initiated combat
     * @param defender The ship being attacked
     */
    public CombatEngagement(Ship attacker, Ship defender) {
        this.attacker = attacker;
        this.defender = defender;
        this.startTime = System.currentTimeMillis();
        this.lastCombatActivity = this.startTime;
        
        // Initialize statistics
        this.attackerShotsFired = 0;
        this.defenderShotsFired = 0;
        this.attackerHits = 0;
        this.defenderHits = 0;
        this.totalDamageDealt = 0.0f;
        this.totalDamageReceived = 0.0f;
        
        logger.debug("Combat engagement created between '{}' and '{}'", 
                    attacker.getName(), defender.getName());
    }
    
    /**
     * Updates the engagement state.
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(float deltaTime) {
        // Update engagement logic here if needed
        // For now, this is primarily a data holder
    }
    
    /**
     * Records that combat activity occurred, updating the last activity timestamp.
     */
    public void recordCombatActivity() {
        this.lastCombatActivity = System.currentTimeMillis();
    }
    
    /**
     * Records a shot fired by one of the participants.
     * 
     * @param shooter The ship that fired the shot
     */
    public void recordShotFired(Ship shooter) {
        if (shooter.equals(attacker)) {
            attackerShotsFired++;
        } else if (shooter.equals(defender)) {
            defenderShotsFired++;
        }
        recordCombatActivity();
    }
    
    /**
     * Records a successful hit by one of the participants.
     * 
     * @param shooter The ship that scored the hit
     * @param damage The amount of damage dealt
     */
    public void recordHit(Ship shooter, float damage) {
        if (shooter.equals(attacker)) {
            attackerHits++;
            totalDamageDealt += damage;
        } else if (shooter.equals(defender)) {
            defenderHits++;
            totalDamageReceived += damage;
        }
        recordCombatActivity();
    }
    
    /**
     * Checks if this engagement involves the specified ships.
     * 
     * @param ship1 First ship to check
     * @param ship2 Second ship to check
     * @return true if both ships are participants in this engagement
     */
    public boolean involvesShips(Ship ship1, Ship ship2) {
        return (ship1.equals(attacker) && ship2.equals(defender)) ||
               (ship1.equals(defender) && ship2.equals(attacker));
    }
    
    /**
     * Gets the time since the last combat activity in seconds.
     * 
     * @return Time since last combat activity in seconds
     */
    public float getTimeSinceLastCombat() {
        return (System.currentTimeMillis() - lastCombatActivity) / 1000.0f;
    }
    
    /**
     * Gets the total duration of this engagement in seconds.
     * 
     * @return Total engagement duration in seconds
     */
    public float getTotalDuration() {
        return (System.currentTimeMillis() - startTime) / 1000.0f;
    }
    
    /**
     * Gets the attacking ship.
     * 
     * @return The attacking ship
     */
    public Ship getAttacker() {
        return attacker;
    }
    
    /**
     * Gets the defending ship.
     * 
     * @return The defending ship
     */
    public Ship getDefender() {
        return defender;
    }
    
    /**
     * Gets the number of shots fired by the attacker.
     * 
     * @return Number of shots fired by attacker
     */
    public int getAttackerShotsFired() {
        return attackerShotsFired;
    }
    
    /**
     * Gets the number of shots fired by the defender.
     * 
     * @return Number of shots fired by defender
     */
    public int getDefenderShotsFired() {
        return defenderShotsFired;
    }
    
    /**
     * Gets the number of hits scored by the attacker.
     * 
     * @return Number of hits by attacker
     */
    public int getAttackerHits() {
        return attackerHits;
    }
    
    /**
     * Gets the number of hits scored by the defender.
     * 
     * @return Number of hits by defender
     */
    public int getDefenderHits() {
        return defenderHits;
    }
    
    /**
     * Gets the total damage dealt by the attacker.
     * 
     * @return Total damage dealt by attacker
     */
    public float getTotalDamageDealt() {
        return totalDamageDealt;
    }
    
    /**
     * Gets the total damage received by the attacker.
     * 
     * @return Total damage received by attacker
     */
    public float getTotalDamageReceived() {
        return totalDamageReceived;
    }
    
    /**
     * Calculates the attacker's accuracy percentage.
     * 
     * @return Accuracy as a percentage (0.0 to 100.0)
     */
    public float getAttackerAccuracy() {
        if (attackerShotsFired == 0) return 0.0f;
        return (float) attackerHits / attackerShotsFired * 100.0f;
    }
    
    /**
     * Calculates the defender's accuracy percentage.
     * 
     * @return Accuracy as a percentage (0.0 to 100.0)
     */
    public float getDefenderAccuracy() {
        if (defenderShotsFired == 0) return 0.0f;
        return (float) defenderHits / defenderShotsFired * 100.0f;
    }
    
    /**
     * Gets a summary of the engagement statistics.
     * 
     * @return String summary of engagement stats
     */
    public String getEngagementSummary() {
        return String.format(
            "Engagement: %s vs %s | Duration: %.1fs | " +
            "Attacker: %d shots, %d hits (%.1f%%) | " +
            "Defender: %d shots, %d hits (%.1f%%) | " +
            "Damage: %.1f dealt, %.1f received",
            attacker.getName(), defender.getName(), getTotalDuration(),
            attackerShotsFired, attackerHits, getAttackerAccuracy(),
            defenderShotsFired, defenderHits, getDefenderAccuracy(),
            totalDamageDealt, totalDamageReceived
        );
    }
    
    @Override
    public String toString() {
        return String.format("CombatEngagement[%s vs %s, duration=%.1fs]", 
                           attacker.getName(), defender.getName(), getTotalDuration());
    }
}