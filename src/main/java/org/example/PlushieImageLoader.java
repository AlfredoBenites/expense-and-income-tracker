package org.example;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to load and cache plushie images from resources
 * Images should be placed in src/main/resources/plushies/
 * Naming convention: plushie_<color>.png (e.g., plushie_red.png, plushie_green.png)
 */
public class PlushieImageLoader {
    private static final String PLUSHIE_DIR = "plushies/";
    private static final String PLUSHIE_PREFIX = "plushie_";
    private static final String PLUSHIE_SUFFIX = ".png";
    
    // Cache for loaded images to avoid reloading
    private static final Map<String, Image> imageCache = new HashMap<>();
    
    /**
     * Loads a plushie image for the given color name
     * @param colorName The color name (e.g., "red", "green", "blue")
     * @return The loaded Image, or null if the image file doesn't exist
     */
    public static Image loadPlushieImage(String colorName) {
        // Check cache first
        if (imageCache.containsKey(colorName)) {
            return imageCache.get(colorName);
        }
        
        try {
            // Convert color name to filename (handle spaces: "lime green" -> "lime_green")
            String filename = colorName.replace(" ", "_");
            String resourcePath = PLUSHIE_DIR + PLUSHIE_PREFIX + filename + PLUSHIE_SUFFIX;
            
            // Load image from resources
            java.io.InputStream imageStream = PlushieImageLoader.class
                    .getClassLoader()
                    .getResourceAsStream(resourcePath);
            
            if (imageStream != null) {
                Image image = ImageIO.read(imageStream);
                imageCache.put(colorName, image);
                imageStream.close();
                return image;
            } else {
                // Image not found - return null (caller should handle fallback)
                System.out.println("Warning: Plushie image not found: " + resourcePath);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error loading plushie image for color: " + colorName);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Preloads all plushie images for the given color names
     * This can be called at startup to load all images into cache
     * @param colorNames Array of color names to preload
     */
    public static void preloadImages(String[] colorNames) {
        for (String color : colorNames) {
            loadPlushieImage(color);
        }
    }
    
    /**
     * Clears the image cache (useful for testing or memory management)
     */
    public static void clearCache() {
        imageCache.clear();
    }
}

