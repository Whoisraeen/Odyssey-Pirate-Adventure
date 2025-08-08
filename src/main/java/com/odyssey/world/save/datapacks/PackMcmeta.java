package com.odyssey.world.save.datapacks;

/**
 * Represents the pack.mcmeta file structure for datapacks.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PackMcmeta {
    private Pack pack;
    
    public Pack getPack() { return pack; }
    public void setPack(Pack pack) { this.pack = pack; }
    
    public static class Pack {
        private int pack_format;
        private String description;
        
        public int getPackFormat() { return pack_format; }
        public void setPackFormat(int packFormat) { this.pack_format = packFormat; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}