package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Window that displays 9 clickable Among Us plushies
 * Players click on plushies to fulfill customer orders
 */
public class PlushieSelectionWindow extends JFrame {
    
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 500;
    private static final int PLUSHIE_SIZE = 120;
    
    // Base colors: green, blue, red, lime green, pink, white, orange, cyan, purple
    private static final String[] BASE_PLUSHIE_COLORS = {
        "green", "blue", "red", "lime green", "pink", "white", "orange", "cyan", "purple"
    };
    
    // Instance variable to hold shuffled plushie colors (can be randomized)
    private List<String> plushieColors;
    
    // Color to RGB mapping for rendering
    private static final Map<String, Color> COLOR_MAP = new HashMap<String, Color>() {{
        put("green", new Color(46, 204, 113));      // Green
        put("blue", new Color(52, 152, 219));       // Blue
        put("red", new Color(231, 76, 60));         // Red
        put("lime green", new Color(50, 205, 50));  // Lime green
        put("pink", new Color(255, 105, 180));      // Pink
        put("white", new Color(236, 240, 241));     // White
        put("orange", new Color(255, 165, 0));      // Orange
        put("cyan", new Color(52, 211, 235));       // Cyan
        put("purple", new Color(155, 89, 182));     // Purple
    }};
    
    private PlushieShopGame game;  // Reference to the main game
    private PlushiePanel plushiePanel;
    
    public PlushieSelectionWindow(PlushieShopGame game) {
        this.game = game;
        setTitle("Select Plushie");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);  // Don't close independently
        setResizable(false);
        
        // Initialize plushie colors list and shuffle it
        plushieColors = new ArrayList<>(Arrays.asList(BASE_PLUSHIE_COLORS));
        
        // Preload all plushie images
        PlushieImageLoader.preloadImages(BASE_PLUSHIE_COLORS);
        
        shufflePlushies();
        
        // Position window to the left of the game window
        Point gameLocation = game.getLocation();
        setLocation(gameLocation.x - WINDOW_WIDTH - 10, gameLocation.y);
        
        plushiePanel = new PlushiePanel();
        add(plushiePanel);
    }
    
    /**
     * Shuffles the positions of plushies in the 3x3 grid
     * Called after every customer is served and after every new day
     */
    public void shufflePlushies() {
        Collections.shuffle(plushieColors);
        if (plushiePanel != null) {
            plushiePanel.repaint();
        }
    }
    
    /**
     * Inner panel that draws and handles plushie clicks
     */
    private class PlushiePanel extends JPanel {
        
        public PlushiePanel() {
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            
            // Add mouse listener for clicking plushies
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handlePlushieClick(e.getX(), e.getY());
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw 9 plushies in a 3x3 grid
            int cols = 3;
            int spacing = 20;
            int startX = 40;
            int startY = 40;
            
            for (int i = 0; i < plushieColors.size(); i++) {
                int row = i / cols;
                int col = i % cols;
                
                int x = startX + col * (PLUSHIE_SIZE + spacing);
                int y = startY + row * (PLUSHIE_SIZE + spacing);
                
                drawPlushie(g2d, x, y, plushieColors.get(i), i);
            }
        }
        
        /**
         * Draws a single Among Us plushie using PNG image if available, otherwise falls back to drawing
         */
        private void drawPlushie(Graphics2D g2d, int x, int y, String colorName, int index) {
            // Try to load PNG image first
            Image plushieImage = PlushieImageLoader.loadPlushieImage(colorName);
            
            if (plushieImage != null) {
                // Draw the PNG image, scaled to PLUSHIE_SIZE
                g2d.drawImage(plushieImage, x, y, PLUSHIE_SIZE, PLUSHIE_SIZE, null);
            } else {
                // Fallback to programmatic drawing if image not found
                drawPlushieFallback(g2d, x, y, colorName);
            }
        }
        
        /**
         * Fallback method to draw plushie programmatically if PNG image is not available
         */
        private void drawPlushieFallback(Graphics2D g2d, int x, int y, String colorName) {
            Color color = COLOR_MAP.get(colorName);
            
            // Body (bean shape)
            g2d.setColor(color);
            g2d.fillOval(x, y + 30, PLUSHIE_SIZE, PLUSHIE_SIZE - 30);
            
            // Backpack
            g2d.setColor(color.darker());
            g2d.fillOval(x + PLUSHIE_SIZE - 25, y + 35, 20, 50);
            
            // Visor (light grey with highlight)
            g2d.setColor(new Color(200, 200, 200));  // Light grey
            g2d.fillOval(x + 15, y + 50, 60, 45);
            
            // Visor highlight
            g2d.setColor(new Color(240, 240, 240));
            g2d.fillOval(x + 25, y + 55, 40, 30);
            
            // Shadow
            g2d.setColor(new Color(50, 50, 50, 100));
            g2d.fillOval(x + 10, y + PLUSHIE_SIZE, PLUSHIE_SIZE - 20, 10);
        }
        
        /**
         * Handles mouse click on plushies
         */
        private void handlePlushieClick(int mouseX, int mouseY) {
            int cols = 3;
            int spacing = 20;
            int startX = 40;
            int startY = 40;
            
            // Check which plushie was clicked
            for (int i = 0; i < plushieColors.size(); i++) {
                int row = i / cols;
                int col = i % cols;
                
                int x = startX + col * (PLUSHIE_SIZE + spacing);
                int y = startY + row * (PLUSHIE_SIZE + spacing);
                
                // Check if click is within plushie bounds
                if (mouseX >= x && mouseX <= x + PLUSHIE_SIZE &&
                    mouseY >= y && mouseY <= y + PLUSHIE_SIZE) {
                    
                    String color = plushieColors.get(i);
                    game.handlePlushieClick(color);
                    break;
                }
            }
        }
    }
}


