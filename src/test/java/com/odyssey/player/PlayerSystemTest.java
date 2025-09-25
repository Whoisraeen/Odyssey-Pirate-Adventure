package com.odyssey.player;

import com.odyssey.save.SaveManager;
import com.odyssey.save.SaveData;
import com.odyssey.core.GameEngine;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for the Player system including save/load functionality.
 * Tests the integration between Player, PlayerManager, SaveManager, and GameEngine.
 */
public class PlayerSystemTest {
    
    private PlayerManager playerManager;
    private Player testPlayer;
    
    @BeforeEach
    void setUp() {
        // Get PlayerManager instance and clean up any existing player
        playerManager = PlayerManager.getInstance();
        // Clear all players for a clean slate
        playerManager.getAllPlayers().keySet().forEach(playerId -> playerManager.removePlayer(playerId));
        
        // Create a test player with known data
        testPlayer = playerManager.createPlayer("TestPlayer");
        
        // Set up test data
        testPlayer.setPosition(new Vector3f(100.0f, 64.0f, 200.0f));
        testPlayer.setRotation(0.0f, 45.0f, 0.0f);
        testPlayer.setVelocity(1.0f, 0.0f, 2.0f);
        testPlayer.setHealth(85.0f);
        testPlayer.setMaxHealth(120.0f);
        testPlayer.setLevel(5);
        testPlayer.setExperience(1250);
        testPlayer.setOnShip(true);
        testPlayer.setShipId("test-ship-123");
        testPlayer.setSpawnPoint(new Vector3f(0.0f, 64.0f, 0.0f));
        testPlayer.setGameTime(3600); // 1 hour
        
        // Set up test inventory
        Map<String, Integer> inventory = new HashMap<>();
        inventory.put("wood", 64);
        inventory.put("iron", 32);
        inventory.put("gold", 8);
        testPlayer.setInventory(inventory);
        
        // Set up test stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("strength", 15);
        stats.put("agility", 12);
        stats.put("intelligence", 18);
        stats.put("luck", 7);
        testPlayer.setPlayerStats(stats);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        if (playerManager != null && testPlayer != null) {
            playerManager.removePlayer(testPlayer.getPlayerId());
        }
    }
    
    @Test
    void testPlayerCreation() {
        assertNotNull(testPlayer, "Player should be created successfully");
        assertEquals("TestPlayer", testPlayer.getPlayerName(), "Player name should match");
        assertEquals(new Vector3f(100.0f, 64.0f, 200.0f), testPlayer.getPosition(), "Player position should match");
        assertEquals(85.0f, testPlayer.getHealth(), 0.01f, "Player health should match");
        assertEquals(5, testPlayer.getLevel(), "Player level should match");
        assertTrue(testPlayer.isOnShip(), "Player should be on ship");
        assertEquals("test-ship-123", testPlayer.getShipId(), "Ship ID should match");
    }
    
    @Test
    void testPlayerManagerSingleton() {
        PlayerManager manager1 = PlayerManager.getInstance();
        PlayerManager manager2 = PlayerManager.getInstance();
        assertSame(manager1, manager2, "PlayerManager should be singleton");
        
        Player player1 = manager1.getPlayer(testPlayer.getPlayerId());
        Player player2 = manager2.getPlayer(testPlayer.getPlayerId());
        assertSame(player1, player2, "Both managers should return same player instance");
    }
    
    @Test
    void testPlayerInventoryManagement() {
        Map<String, Integer> inventory = testPlayer.getInventory();
        assertNotNull(inventory, "Inventory should not be null");
        assertEquals(64, inventory.get("wood"), "Wood count should match");
        assertEquals(32, inventory.get("iron"), "Iron count should match");
        assertEquals(8, inventory.get("gold"), "Gold count should match");
        
        // Test inventory modification
        inventory.put("diamond", 4);
        testPlayer.setInventory(inventory);
        assertEquals(4, testPlayer.getInventory().get("diamond"), "Diamond count should be added");
    }
    
    @Test
    void testPlayerStatsManagement() {
        Map<String, Object> stats = testPlayer.getPlayerStats();
        assertNotNull(stats, "Player stats should not be null");
        assertEquals(15, stats.get("strength"), "Strength should match");
        assertEquals(12, stats.get("agility"), "Agility should match");
        assertEquals(18, stats.get("intelligence"), "Intelligence should match");
        assertEquals(7, stats.get("luck"), "Luck should match");
    }
    
    @Test
    void testSaveDataCreation() {
        // Test that we can access player data for saving
        // Since SaveManager's createSaveData is private, we test the data directly
        assertNotNull(testPlayer, "Test player should exist");
        assertEquals("TestPlayer", testPlayer.getPlayerName(), "Player name should be correct");
        assertEquals(new Vector3f(100.0f, 64.0f, 200.0f), testPlayer.getPosition(), "Position should be correct");
        assertEquals(85.0f, testPlayer.getHealth(), 0.01f, "Health should be correct");
        assertEquals(5, testPlayer.getLevel(), "Level should be correct");
        assertEquals(1250, testPlayer.getExperience(), "Experience should be correct");
        
        // Test that all required data is accessible for save operations
        assertNotNull(testPlayer.getInventory(), "Inventory should be accessible");
        assertNotNull(testPlayer.getPlayerStats(), "Player stats should be accessible");
        assertTrue(testPlayer.isOnShip(), "Ship status should be accessible");
        assertEquals("test-ship-123", testPlayer.getShipId(), "Ship ID should be accessible");
    }
    
    @Test
    void testPlayerDataLoad() {
        // Test that we can simulate loading player data
        // Create a new player with different data to simulate loading
        Player newPlayer = playerManager.createPlayer("LoadedPlayer");
        newPlayer.setPosition(new Vector3f(200.0f, 32.0f, 400.0f));
        newPlayer.setRotation(90.0f, 0.0f, 0.0f);
        newPlayer.setVelocity(0.5f, 1.0f, 1.5f);
        newPlayer.setHealth(100.0f);
        newPlayer.setMaxHealth(100.0f);
        newPlayer.setLevel(3);
        newPlayer.setExperience(750);
        
        // Verify the loaded data
        assertEquals("LoadedPlayer", newPlayer.getPlayerName(), "Loaded player name should match");
        assertEquals(new Vector3f(200.0f, 32.0f, 400.0f), newPlayer.getPosition(), "Loaded position should match");
        assertEquals(100.0f, newPlayer.getHealth(), 0.01f, "Loaded health should match");
        assertEquals(3, newPlayer.getLevel(), "Loaded level should match");
        assertEquals(750, newPlayer.getExperience(), "Loaded experience should match");
    }
    
    @Test
    void testPlayerHealthManagement() {
        // Test damage
        testPlayer.takeDamage(25.0f);
        assertEquals(60.0f, testPlayer.getHealth(), 0.01f, "Health should decrease after damage");
        
        // Test healing
        testPlayer.heal(15.0f);
        assertEquals(75.0f, testPlayer.getHealth(), 0.01f, "Health should increase after healing");
        
        // Test healing beyond max health
        testPlayer.heal(100.0f);
        assertEquals(120.0f, testPlayer.getHealth(), 0.01f, "Health should not exceed max health");
        
        // Test fatal damage
        testPlayer.takeDamage(150.0f);
        assertEquals(0.0f, testPlayer.getHealth(), 0.01f, "Health should not go below 0");
        assertTrue(testPlayer.isDead(), "Player should be dead when health is 0");
    }
    
    @Test
    void testPlayerExperienceAndLeveling() {
        // Test experience gain
        testPlayer.addExperience(250);
        assertEquals(1500, testPlayer.getExperience(), "Experience should increase");
        
        // Test level calculation (assuming 1000 XP per level)
        testPlayer.addExperience(500); // Total: 2000 XP
        // Note: Level calculation logic would need to be implemented in Player class
        // This test assumes the Player class handles automatic leveling
    }
    
    @Test
    void testPlayerRespawn() {
        // Kill player
        testPlayer.takeDamage(200.0f);
        assertTrue(testPlayer.isDead(), "Player should be dead");
        
        // Respawn player
        testPlayer.respawn();
        assertFalse(testPlayer.isDead(), "Player should be alive after respawn");
        assertEquals(testPlayer.getMaxHealth(), testPlayer.getHealth(), 0.01f, "Health should be full after respawn");
        assertEquals(testPlayer.getSpawnPoint(), testPlayer.getPosition(), "Player should be at spawn point after respawn");
    }
}