package com.odyssey.world.save;

public class SaveData {
    private long timestamp;
    private String gameVersion;
    private Object playerData;
    private Object worldData;
    private Object shipData;
    private Object inventoryData;
    private Object questData;
    private Object gameSettings;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public Object getPlayerData() {
        return playerData;
    }

    public void setPlayerData(Object playerData) {
        this.playerData = playerData;
    }

    public Object getWorldData() {
        return worldData;
    }

    public void setWorldData(Object worldData) {
        this.worldData = worldData;
    }

    public Object getShipData() {
        return shipData;
    }

    public void setShipData(Object shipData) {
        this.shipData = shipData;
    }

    public Object getInventoryData() {
        return inventoryData;
    }

    public void setInventoryData(Object inventoryData) {
        this.inventoryData = inventoryData;
    }

    public Object getQuestData() {
        return questData;
    }

    public void setQuestData(Object questData) {
        this.questData = questData;
    }

    public Object getGameSettings() {
        return gameSettings;
    }

    public void setGameSettings(Object gameSettings) {
        this.gameSettings = gameSettings;
    }
}
