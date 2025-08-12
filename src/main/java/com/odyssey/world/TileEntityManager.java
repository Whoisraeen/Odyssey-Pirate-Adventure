package com.odyssey.world;

import com.odyssey.world.chunk.ChunkPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages tile entities (block entities) in the world.
 * Tile entities are blocks with additional data and behavior like chests, furnaces, signs, etc.
 */
public class TileEntityManager {
    private static final Logger logger = LoggerFactory.getLogger(TileEntityManager.class);
    
    private final World world;
    private final Map<BlockPosition, TileEntity> tileEntities;
    private final Map<ChunkPosition, Set<BlockPosition>> chunkTileEntities;
    private final Set<TileEntity> tickingTileEntities;
    
    public TileEntityManager(World world) {
        this.world = world;
        this.tileEntities = new ConcurrentHashMap<>();
        this.chunkTileEntities = new ConcurrentHashMap<>();
        this.tickingTileEntities = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Creates a tile entity at the specified position
     */
    public TileEntity createTileEntity(int x, int y, int z, String blockType) {
        BlockPosition pos = new BlockPosition(x, y, z);
        
        // Remove existing tile entity if present
        removeTileEntity(x, y, z);
        
        TileEntity tileEntity = createTileEntityForBlock(blockType, pos);
        if (tileEntity != null) {
            addTileEntity(tileEntity);
            logger.debug("Created tile entity {} at ({}, {}, {})", blockType, x, y, z);
        }
        
        return tileEntity;
    }
    
    /**
     * Adds a tile entity to the manager
     */
    public void addTileEntity(TileEntity tileEntity) {
        BlockPosition pos = tileEntity.getPosition();
        tileEntities.put(pos, tileEntity);
        
        // Add to chunk mapping
        ChunkPosition chunkPos = new ChunkPosition(pos.x >> 4, pos.z >> 4);
        chunkTileEntities.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet()).add(pos);
        
        // Add to ticking list if needed
        if (tileEntity.shouldTick()) {
            tickingTileEntities.add(tileEntity);
        }
        
        tileEntity.onAdded(world);
    }
    
    /**
     * Removes a tile entity at the specified position
     */
    public TileEntity removeTileEntity(int x, int y, int z) {
        BlockPosition pos = new BlockPosition(x, y, z);
        TileEntity tileEntity = tileEntities.remove(pos);
        
        if (tileEntity != null) {
            // Remove from chunk mapping
            ChunkPosition chunkPos = new ChunkPosition(x >> 4, z >> 4);
            Set<BlockPosition> chunkTEs = chunkTileEntities.get(chunkPos);
            if (chunkTEs != null) {
                chunkTEs.remove(pos);
                if (chunkTEs.isEmpty()) {
                    chunkTileEntities.remove(chunkPos);
                }
            }
            
            // Remove from ticking list
            tickingTileEntities.remove(tileEntity);
            
            tileEntity.onRemoved();
            logger.debug("Removed tile entity at ({}, {}, {})", x, y, z);
        }
        
        return tileEntity;
    }
    
    /**
     * Gets a tile entity at the specified position
     */
    public TileEntity getTileEntity(int x, int y, int z) {
        return tileEntities.get(new BlockPosition(x, y, z));
    }
    
    /**
     * Gets a tile entity of a specific type at the specified position
     */
    @SuppressWarnings("unchecked")
    public <T extends TileEntity> T getTileEntity(int x, int y, int z, Class<T> type) {
        TileEntity tileEntity = getTileEntity(x, y, z);
        if (type.isInstance(tileEntity)) {
            return (T) tileEntity;
        }
        return null;
    }
    
    /**
     * Gets all tile entities in a chunk
     */
    public Collection<TileEntity> getTileEntitiesInChunk(ChunkPosition chunkPos) {
        Set<BlockPosition> positions = chunkTileEntities.get(chunkPos);
        if (positions == null) {
            return Collections.emptyList();
        }
        
        List<TileEntity> result = new ArrayList<>();
        for (BlockPosition pos : positions) {
            TileEntity te = tileEntities.get(pos);
            if (te != null) {
                result.add(te);
            }
        }
        return result;
    }
    
    /**
     * Ticks all ticking tile entities
     */
    public void tick() {
        Iterator<TileEntity> iterator = tickingTileEntities.iterator();
        while (iterator.hasNext()) {
            TileEntity tileEntity = iterator.next();
            
            try {
                if (tileEntity.isValid()) {
                    tileEntity.tick();
                } else {
                    iterator.remove();
                    logger.debug("Removed invalid tile entity from ticking list: {}", tileEntity);
                }
            } catch (Exception e) {
                logger.error("Error ticking tile entity at {}: {}", tileEntity.getPosition(), e.getMessage(), e);
                iterator.remove();
            }
        }
    }
    
    /**
     * Unloads tile entities for a chunk
     */
    public void unloadChunk(ChunkPosition chunkPos) {
        Set<BlockPosition> positions = chunkTileEntities.remove(chunkPos);
        if (positions != null) {
            for (BlockPosition pos : positions) {
                TileEntity tileEntity = tileEntities.remove(pos);
                if (tileEntity != null) {
                    tickingTileEntities.remove(tileEntity);
                    tileEntity.onUnloaded();
                }
            }
            logger.debug("Unloaded {} tile entities from chunk {}", positions.size(), chunkPos);
        }
    }
    
    /**
     * Loads tile entities for a chunk
     */
    public void loadChunk(ChunkPosition chunkPos) {
        // This would typically load tile entity data from disk
        // For now, we'll just log the event
        logger.debug("Loading tile entities for chunk {}", chunkPos);
    }
    
    /**
     * Creates a tile entity instance for a given block type
     */
    private TileEntity createTileEntityForBlock(String blockType, BlockPosition position) {
        switch (blockType) {
            case "chest":
                return new ChestTileEntity(position);
            case "furnace":
                return new FurnaceTileEntity(position);
            case "sign":
            case "wall_sign":
                return new SignTileEntity(position);
            case "brewing_stand":
                return new BrewingStandTileEntity(position);
            case "enchanting_table":
                return new EnchantingTableTileEntity(position);
            case "beacon":
                return new BeaconTileEntity(position);
            case "hopper":
                return new HopperTileEntity(position);
            case "dispenser":
            case "dropper":
                return new DispenserTileEntity(position);
            case "note_block":
                return new NoteBlockTileEntity(position);
            case "jukebox":
                return new JukeboxTileEntity(position);
            case "flower_pot":
                return new FlowerPotTileEntity(position);
            case "skull":
            case "player_head":
                return new SkullTileEntity(position);
            case "banner":
            case "wall_banner":
                return new BannerTileEntity(position);
            case "shulker_box":
                return new ShulkerBoxTileEntity(position);
            case "bed":
                return new BedTileEntity(position);
            case "conduit":
                return new ConduitTileEntity(position);
            case "barrel":
                return new BarrelTileEntity(position);
            case "smoker":
                return new SmokerTileEntity(position);
            case "blast_furnace":
                return new BlastFurnaceTileEntity(position);
            case "campfire":
                return new CampfireTileEntity(position);
            case "lectern":
                return new LecternTileEntity(position);
            case "bell":
                return new BellTileEntity(position);
            case "beehive":
            case "bee_nest":
                return new BeehiveTileEntity(position);
            default:
                return null;
        }
    }
    
    /**
     * Gets all tile entities
     */
    public Collection<TileEntity> getAllTileEntities() {
        return new ArrayList<>(tileEntities.values());
    }
    
    /**
     * Gets the number of tile entities
     */
    public int getTileEntityCount() {
        return tileEntities.size();
    }
    
    /**
     * Gets the number of ticking tile entities
     */
    public int getTickingTileEntityCount() {
        return tickingTileEntities.size();
    }
    
    /**
     * Clears all tile entities
     */
    public void clear() {
        for (TileEntity tileEntity : tileEntities.values()) {
            tileEntity.onRemoved();
        }
        tileEntities.clear();
        chunkTileEntities.clear();
        tickingTileEntities.clear();
        logger.info("Cleared all tile entities");
    }
    
    /**
     * Block position class for internal use
     */
    public static class BlockPosition {
        public final int x, y, z;
        
        public BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockPosition)) return false;
            BlockPosition other = (BlockPosition) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
        
        @Override
        public String toString() {
            return String.format("(%d, %d, %d)", x, y, z);
        }
    }
    
    /**
     * Base tile entity class
     */
    public abstract static class TileEntity {
        protected final BlockPosition position;
        protected boolean valid = true;
        
        public TileEntity(BlockPosition position) {
            this.position = position;
        }
        
        public BlockPosition getPosition() {
            return position;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void invalidate() {
            this.valid = false;
        }
        
        public abstract boolean shouldTick();
        
        public void tick() {
            // Override in subclasses
        }
        
        public void onAdded(World world) {
            // Override in subclasses
        }
        
        public void onRemoved() {
            invalidate();
        }
        
        public void onUnloaded() {
            // Override in subclasses
        }
        
        public abstract String getType();
    }
    
    // Concrete tile entity implementations
    public static class ChestTileEntity extends TileEntity {
        private final Map<Integer, String> inventory = new HashMap<>();
        
        public ChestTileEntity(BlockPosition position) {
            super(position);
        }
        
        @Override
        public boolean shouldTick() {
            return false;
        }
        
        @Override
        public String getType() {
            return "chest";
        }
        
        public Map<Integer, String> getInventory() {
            return inventory;
        }
    }
    
    public static class FurnaceTileEntity extends TileEntity {
        private int burnTime = 0;
        private int cookTime = 0;
        private final Map<String, String> inventory = new HashMap<>();
        
        public FurnaceTileEntity(BlockPosition position) {
            super(position);
        }
        
        @Override
        public boolean shouldTick() {
            return burnTime > 0 || !inventory.isEmpty();
        }
        
        @Override
        public void tick() {
            if (burnTime > 0) {
                burnTime--;
                if (!inventory.isEmpty()) {
                    cookTime++;
                    if (cookTime >= 200) { // 10 seconds at 20 TPS
                        // Cook item
                        cookTime = 0;
                    }
                }
            }
        }
        
        @Override
        public String getType() {
            return "furnace";
        }
        
        public int getBurnTime() {
            return burnTime;
        }
        
        public void setBurnTime(int burnTime) {
            this.burnTime = burnTime;
        }
        
        public int getCookTime() {
            return cookTime;
        }
    }
    
    public static class SignTileEntity extends TileEntity {
        private final String[] lines = new String[4];
        
        public SignTileEntity(BlockPosition position) {
            super(position);
            Arrays.fill(lines, "");
        }
        
        @Override
        public boolean shouldTick() {
            return false;
        }
        
        @Override
        public String getType() {
            return "sign";
        }
        
        public String[] getLines() {
            return lines.clone();
        }
        
        public void setLine(int index, String text) {
            if (index >= 0 && index < lines.length) {
                lines[index] = text != null ? text : "";
            }
        }
    }
    
    // Additional tile entity classes would be implemented similarly
    public static class BrewingStandTileEntity extends TileEntity {
        public BrewingStandTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "brewing_stand"; }
    }
    
    public static class EnchantingTableTileEntity extends TileEntity {
        public EnchantingTableTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "enchanting_table"; }
    }
    
    public static class BeaconTileEntity extends TileEntity {
        public BeaconTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "beacon"; }
    }
    
    public static class HopperTileEntity extends TileEntity {
        public HopperTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "hopper"; }
    }
    
    public static class DispenserTileEntity extends TileEntity {
        public DispenserTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "dispenser"; }
    }
    
    public static class NoteBlockTileEntity extends TileEntity {
        public NoteBlockTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "note_block"; }
    }
    
    public static class JukeboxTileEntity extends TileEntity {
        public JukeboxTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "jukebox"; }
    }
    
    public static class FlowerPotTileEntity extends TileEntity {
        public FlowerPotTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "flower_pot"; }
    }
    
    public static class SkullTileEntity extends TileEntity {
        public SkullTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "skull"; }
    }
    
    public static class BannerTileEntity extends TileEntity {
        public BannerTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "banner"; }
    }
    
    public static class ShulkerBoxTileEntity extends TileEntity {
        public ShulkerBoxTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "shulker_box"; }
    }
    
    public static class BedTileEntity extends TileEntity {
        public BedTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "bed"; }
    }
    
    public static class ConduitTileEntity extends TileEntity {
        public ConduitTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "conduit"; }
    }
    
    public static class BarrelTileEntity extends TileEntity {
        public BarrelTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "barrel"; }
    }
    
    public static class SmokerTileEntity extends TileEntity {
        public SmokerTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "smoker"; }
    }
    
    public static class BlastFurnaceTileEntity extends TileEntity {
        public BlastFurnaceTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "blast_furnace"; }
    }
    
    public static class CampfireTileEntity extends TileEntity {
        public CampfireTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "campfire"; }
    }
    
    public static class LecternTileEntity extends TileEntity {
        public LecternTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "lectern"; }
    }
    
    public static class BellTileEntity extends TileEntity {
        public BellTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return false; }
        @Override public String getType() { return "bell"; }
    }
    
    public static class BeehiveTileEntity extends TileEntity {
        public BeehiveTileEntity(BlockPosition position) { super(position); }
        @Override public boolean shouldTick() { return true; }
        @Override public String getType() { return "beehive"; }
    }
}