package com.odyssey.world.save.icon;

import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Manages world selection screen icon system with 64x64 PNG format.
 * Provides functionality to create, update, and retrieve world icons.
 * 
 * <p>Features:
 * <ul>
 * <li>64x64 PNG icon generation</li>
 * <li>Default icon creation for new worlds</li>
 * <li>Custom icon support</li>
 * <li>Icon validation and conversion</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class WorldIconManager {
    private static final Logger logger = LoggerFactory.getLogger(WorldIconManager.class);
    
    public static final int ICON_SIZE = 64;
    public static final String ICON_FORMAT = "PNG";
    
    private final Path worldDirectory;
    private final Path iconFile;
    
    /**
     * Creates a new world icon manager.
     * 
     * @param worldDirectory the world directory
     */
    public WorldIconManager(Path worldDirectory) {
        this.worldDirectory = worldDirectory;
        this.iconFile = worldDirectory.resolve(WorldSaveFormat.WORLD_ICON_FILE);
    }
    
    /**
     * Creates a default icon for the world if none exists.
     * 
     * @param worldName the world name to display on the icon
     */
    public void createDefaultIconIfMissing(String worldName) {
        if (!Files.exists(iconFile)) {
            try {
                createDefaultIcon(worldName);
                logger.info("Created default icon for world: {}", worldName);
            } catch (IOException e) {
                logger.error("Failed to create default icon for world: {}", worldName, e);
            }
        }
    }
    
    /**
     * Creates a default icon with the world name and maritime theme.
     * 
     * @param worldName the world name
     * @throws IOException if icon creation fails
     */
    public void createDefaultIcon(String worldName) throws IOException {
        BufferedImage icon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = icon.createGraphics();
        
        try {
            // Enable antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            // Create ocean gradient background
            GradientPaint oceanGradient = new GradientPaint(
                0, 0, new Color(135, 206, 235),  // Sky blue
                0, ICON_SIZE, new Color(25, 25, 112)  // Midnight blue
            );
            g2d.setPaint(oceanGradient);
            g2d.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
            
            // Draw waves
            g2d.setColor(new Color(70, 130, 180, 128)); // Steel blue with transparency
            for (int y = ICON_SIZE / 2; y < ICON_SIZE; y += 8) {
                for (int x = 0; x < ICON_SIZE; x += 16) {
                    g2d.fillOval(x - 4, y - 2, 8, 4);
                }
            }
            
            // Draw a simple ship silhouette
            g2d.setColor(new Color(139, 69, 19)); // Saddle brown
            int shipY = ICON_SIZE / 2 - 8;
            g2d.fillRect(ICON_SIZE / 2 - 12, shipY, 24, 6); // Hull
            g2d.fillRect(ICON_SIZE / 2 - 2, shipY - 12, 4, 12); // Mast
            
            // Draw sail
            g2d.setColor(Color.WHITE);
            int[] sailX = {ICON_SIZE / 2 + 2, ICON_SIZE / 2 + 2, ICON_SIZE / 2 + 12};
            int[] sailY = {shipY - 12, shipY - 4, shipY - 8};
            g2d.fillPolygon(sailX, sailY, 3);
            
            // Draw world name (truncated if too long)
            String displayName = worldName.length() > 8 ? worldName.substring(0, 8) + "..." : worldName;
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(displayName);
            int textX = (ICON_SIZE - textWidth) / 2;
            int textY = ICON_SIZE - 8;
            
            // Draw text with shadow
            g2d.setColor(Color.BLACK);
            g2d.drawString(displayName, textX + 1, textY + 1);
            g2d.setColor(Color.WHITE);
            g2d.drawString(displayName, textX, textY);
            
        } finally {
            g2d.dispose();
        }
        
        // Save the icon
        ImageIO.write(icon, ICON_FORMAT, iconFile.toFile());
    }
    
    /**
     * Sets a custom icon from the provided image data.
     * 
     * @param imageData the image data
     * @throws IOException if icon setting fails
     */
    public void setCustomIcon(byte[] imageData) throws IOException {
        // Load and validate the image
        BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
        if (originalImage == null) {
            throw new IOException("Invalid image data");
        }
        
        // Resize to 64x64 if necessary
        BufferedImage resizedImage = resizeImage(originalImage, ICON_SIZE, ICON_SIZE);
        
        // Save as PNG
        ImageIO.write(resizedImage, ICON_FORMAT, iconFile.toFile());
        logger.info("Set custom icon for world");
    }
    
    /**
     * Gets the world icon as image data.
     * 
     * @return the icon image data, or null if no icon exists
     */
    public byte[] getIconData() {
        try {
            if (!Files.exists(iconFile)) {
                return null;
            }
            return Files.readAllBytes(iconFile);
        } catch (IOException e) {
            logger.error("Failed to read world icon", e);
            return null;
        }
    }
    
    /**
     * Gets the world icon as a BufferedImage.
     * 
     * @return the icon image, or null if no icon exists
     */
    public BufferedImage getIconImage() {
        try {
            if (!Files.exists(iconFile)) {
                return null;
            }
            return ImageIO.read(iconFile.toFile());
        } catch (IOException e) {
            logger.error("Failed to read world icon image", e);
            return null;
        }
    }
    
    /**
     * Checks if the world has a custom icon.
     * 
     * @return true if an icon exists
     */
    public boolean hasIcon() {
        return Files.exists(iconFile);
    }
    
    /**
     * Deletes the world icon.
     * 
     * @return true if deletion was successful
     */
    public boolean deleteIcon() {
        try {
            return Files.deleteIfExists(iconFile);
        } catch (IOException e) {
            logger.error("Failed to delete world icon", e);
            return false;
        }
    }
    
    /**
     * Resizes an image to the specified dimensions.
     * 
     * @param originalImage the original image
     * @param width the target width
     * @param height the target height
     * @return the resized image
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.drawImage(originalImage, 0, 0, width, height, null);
        } finally {
            g2d.dispose();
        }
        
        return resizedImage;
    }
    
    /**
     * Gets the icon file path.
     * 
     * @return the icon file path
     */
    public Path getIconFile() {
        return iconFile;
    }
}