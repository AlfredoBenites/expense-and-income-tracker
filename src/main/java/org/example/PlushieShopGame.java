package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

/**
 * PlushieShopGame - A simple 2D Java game where the player sells Among Us plushies
 * 
 * GAME LOOP EXPLANATION:
 * This game uses Swing's event-driven architecture rather than a traditional game loop.
 * Instead of a continuous loop that runs every frame, Swing uses:
 * - ActionListeners for button clicks (handled immediately when user clicks)
 * - Timer class for periodic updates (like customer animations)
 * - paintComponent() for rendering (called automatically by Swing when needed)
 * 
 * INPUT HANDLING:
 * - Button clicks: Handled via ActionListener on the "Sell Plushie" button
 * - The button processes the first customer in queue at the counter
 * 
 * TRACKER INTEGRATION:
 * - When plushies are sold, the game:
 *   1. Adds an Income transaction to the database via TransactionDAO
 *   2. Refreshes financial calculations using TransactionValuesCalculation
 *   3. Displays a popup dialog showing Total Income, Total Expenses, and Net Profit
 */
public class PlushieShopGame extends JFrame {
    
    // ========== GAME CONSTANTS ==========
    private static final double PLUSHIE_PRICE = 10.0;  // Price per plushie
    private static final int GAME_WIDTH = 900;
    private static final int GAME_HEIGHT = 600;
    private static final int CUSTOMER_SPAWN_INTERVAL = 4000; // Spawn customer every 4 seconds
    private static final int COUNTER_X = 350;  // X position of the counter (where customers stop)
    private static final int COUNTER_Y = GAME_HEIGHT - 220;  // Y position of counter top
    private static final int BASE_ORDER_TIMER_SECONDS = 20;  // Base time limit (Day 1)
    private static final int MIN_ORDER_TIMER_SECONDS = 10;  // Minimum time limit
    private static final int TIMER_DECREASE_PER_DAY = 2;  // Seconds to decrease per day
    
    // ========== RANK SYSTEM ==========
    // Score thresholds for ranking system (based on final total score)
    // Ranks: Noob (0-999), Pro (1000-2999), Hacker (3000-4999), God (5000+)
    private static final int RANK_PRO_THRESHOLD = 1000;      // 1000-2999: Pro
    private static final int RANK_HACKER_THRESHOLD = 3000;   // 3000-4999: Hacker
    private static final int RANK_GOD_THRESHOLD = 5000;      // 5000+: God
    
    // ========== PLUSHIE COLORS ==========
    // Colors in order: green, blue, red, lime green, pink, white, orange, cyan, purple
    private static final String[] PLUSHIE_COLORS = {
        "green", "blue", "red", "lime green", "pink", "white", "orange", "cyan", "purple"
    };
    
    // Color to RGB mapping for rendering
    private static final Map<String, Color> COLOR_MAP = new HashMap<String, Color>() {{
        put("green", new Color(46, 204, 113));      // Green
        put("blue", new Color(52, 152, 219));       // Blue
        put("red", new Color(231, 76, 60));         // Red
        put("lime green", new Color(50, 205, 50));  // Lime green (brighter green)
        put("pink", new Color(255, 105, 180));      // Pink
        put("white", new Color(236, 240, 241));     // White
        put("orange", new Color(255, 165, 0));      // Orange
        put("cyan", new Color(52, 211, 235));       // Cyan
        put("purple", new Color(155, 89, 182));     // Purple
    }};
    
    // ========== GAME STATE VARIABLES ==========
    private GamePanel gamePanel;              // Custom panel that draws the game scene
    private List<Customer> customers;         // List of customers in the shop (queue)
    private Random random;                    // Random number generator
    private int plushiesSold = 0;             // Counter for plushies sold
    private JLabel salesCounterLabel;         // Label showing total sales
    private Customer currentCustomer;         // Customer currently at the counter
    private ExpenseAndIncomeTrackerApp trackerApp;  // Reference to the tracker app
    private PlushieSelectionWindow plushieSelectionWindow;  // Reference to the plushie selection window
    private Timer orderTimer;                 // Timer that counts down when customer is at counter
    private int timeRemaining;                // Time remaining in seconds
    private boolean timerActive;              // Whether the timer is currently running
    private int currentDay;                   // Current day number (starts at 1)
    private int customersServedToday;         // Number of customers served in current day
    private int lives;                        // Number of lives remaining (starts at 3 each day)
    private boolean dayOverlayVisible;        // Whether the day overlay is currently showing
    private boolean levelFailedOverlayVisible; // Whether the level failed overlay is currently showing
    private boolean gameCompletedOverlayVisible; // Whether the game completed overlay is currently showing
    private boolean gameCompleted;            // Whether the game has been completed (Day 7 finished)
    private static final int MAX_DAY = 7;      // Maximum day number (game ends after Day 7)
    private Timer dayOverlayTimer;            // Timer for day overlay display
    private Timer levelFailedOverlayTimer;    // Timer for level failed overlay display
    private Timer customerSpawnTimer;         // Timer for spawning customers
    private Image backgroundImage;           // Background image for the shop (loaded from PNG)
    
    /**
     * Constructor - Sets up the game window and initializes components
     */
    public PlushieShopGame() {
        // Initialize the random number generator
        random = new Random();
        customers = new ArrayList<>();
        currentCustomer = null;
        timeRemaining = getOrderTimerSeconds();
        timerActive = false;
        currentDay = 1;                      // Start at day 1
        customersServedToday = 0;            // No customers served yet
        lives = 3;                           // Start with 3 lives
        dayOverlayVisible = true;            // Show overlay at start
        levelFailedOverlayVisible = false;   // Level failed overlay not visible initially
        gameCompletedOverlayVisible = false; // Game completed overlay not visible initially
        gameCompleted = false;              // Game not completed initially
        dayOverlayTimer = null;
        levelFailedOverlayTimer = null;
        
        // Set up the main game window
        setTitle("Among Us Plushie Shop - Game");
        setSize(GAME_WIDTH, GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);  // Center the window on screen
        setResizable(false);
        setLayout(new BorderLayout());
        
        // Load background image if available
        loadBackgroundImage();
        
        // Create the game panel (where the shop scene is drawn)
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
        
        // Add keyboard listener for game completion controls (ESC to exit, R to restart)
        setFocusable(true);
        requestFocusInWindow();
        addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Only process keys when game is completed
                if (gameCompleted && gameCompletedOverlayVisible) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        // Exit the game
                        System.exit(0);
                    } else if (e.getKeyCode() == KeyEvent.VK_R) {
                        // Restart the game
                        restartGame();
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
            
            @Override
            public void keyTyped(KeyEvent e) {}
        });
        
        // Create the control panel at the bottom
        createControlPanel();
        
        // Initialize tracker app (but don't show it yet)
        // It will be shown when first customer reaches counter
        trackerApp = null;
        
        // Create and show the plushie selection window
        plushieSelectionWindow = new PlushieSelectionWindow(this);
        plushieSelectionWindow.setVisible(true);
        
        // Set up customer spawning timer
        // This creates a "game loop" effect by periodically adding customers
        // Note: Timer will be started after day overlay disappears
        customerSpawnTimer = new Timer(CUSTOMER_SPAWN_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Only spawn customers if day overlay is not visible
                if (!dayOverlayVisible) {
                    spawnCustomer();
                }
            }
        });
        // Don't start immediately - wait for day overlay to finish
        
        // Show day overlay at start (for 5 seconds)
        showDayOverlay();
        
        // Set up animation timer to update customer positions
        // This creates a continuous update loop for smooth animations
        Timer animationTimer = new Timer(50, new ActionListener() {  // Updates every 50ms (20 FPS)
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGame();
            }
        });
        animationTimer.start();
        
        // Make the window visible
        setVisible(true);
    }
    
    /**
     * Creates the control panel at the bottom with sales counter only
     */
    private void createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(52, 73, 94));
        controlPanel.setPreferredSize(new Dimension(GAME_WIDTH, 60));
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
        
        // Create sales counter label
        salesCounterLabel = new JLabel("Plushies Sold: 0");
        salesCounterLabel.setFont(new Font("Arial", Font.BOLD, 18));
        salesCounterLabel.setForeground(Color.WHITE);
        controlPanel.add(salesCounterLabel);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Shows the ExpenseAndIncomeTrackerApp window positioned next to the game window
     */
    private void showTrackerApp() {
        if (trackerApp == null) {
            // Create the tracker app
            trackerApp = new ExpenseAndIncomeTrackerApp();
            JFrame trackerFrame = trackerApp.getFrame();
            
            // Position the tracker window to the right of the game window
            Point gameLocation = this.getLocation();
            int trackerX = gameLocation.x + this.getWidth() + 10;  // 10 pixels to the right
            int trackerY = gameLocation.y;
            trackerFrame.setLocation(trackerX, trackerY);
            
            // Make sure it's visible
            trackerFrame.setVisible(true);
            trackerFrame.toFront();
        } else {
            // If already created, just make sure it's visible and bring to front
            JFrame trackerFrame = trackerApp.getFrame();
            trackerFrame.setVisible(true);
            trackerFrame.toFront();
        }
    }
    
    /**
     * Generates a random plushie order for a customer
     * Orders can be simple (one color) or complex (multiple colors)
     */
    private List<PlushieOrder> generateRandomOrder() {
        List<PlushieOrder> orders = new ArrayList<>();
        
        // Random number of different colors (1-3)
        int numColors = 1 + random.nextInt(3);
        
        // Total plushies requested (1-10)
        int totalPlushies = 1 + random.nextInt(10);
        
        // Distribute plushies across colors
        List<String> availableColors = new ArrayList<>();
        for (String color : PLUSHIE_COLORS) {
            availableColors.add(color);
        }
        
        int remaining = totalPlushies;
        for (int i = 0; i < numColors && !availableColors.isEmpty() && remaining > 0; i++) {
            // Pick a random color
            int colorIndex = random.nextInt(availableColors.size());
            String color = availableColors.remove(colorIndex);
            
            // Calculate quantity for this color
            int quantity;
            if (i == numColors - 1) {
                // Last color gets all remaining
                quantity = remaining;
            } else {
                // Distribute remaining plushies
                quantity = 1 + random.nextInt(Math.min(remaining, 5));
                remaining -= quantity;
            }
            
            orders.add(new PlushieOrder(color, quantity));
        }
        
        return orders;
    }
    
    /**
     * Spawns a new customer from the right side of the screen
     */
    private void spawnCustomer() {
        // Don't spawn customers if game is completed or overlays are visible
        if (gameCompleted || dayOverlayVisible || levelFailedOverlayVisible || gameCompletedOverlayVisible) {
            return;
        }
        
        // Limit the number of customers in queue to prevent overcrowding
        if (customers.size() >= 5) {
            return;
        }
        
        // Create a new customer starting from the right side
        int startX = GAME_WIDTH + 50;  // Start off-screen on the right
        int customerY = COUNTER_Y + 10;  // Align with counter height
        
        // Generate random order
        List<PlushieOrder> orders = generateRandomOrder();
        
        Customer customer = new Customer(startX, customerY, orders);
        customers.add(customer);
        
        // If no customer is at the counter, make this one the current customer
        // Only the first customer in queue should move toward the counter
        if (currentCustomer == null && !customers.isEmpty()) {
            currentCustomer = customers.get(0);
        }
        
        // Redraw the game panel to show the new customer
        gamePanel.repaint();
    }
    
    /**
     * Loads the background image from resources if available
     * Image should be placed at: src/main/resources/shop_background.png
     */
    private void loadBackgroundImage() {
        try {
            java.io.InputStream imageStream = PlushieShopGame.class
                    .getClassLoader()
                    .getResourceAsStream("shop_background.png");
            
            if (imageStream != null) {
                backgroundImage = javax.imageio.ImageIO.read(imageStream);
                imageStream.close();
                System.out.println("Background image loaded successfully");
            } else {
                backgroundImage = null;
                System.out.println("Background image not found, using programmatic drawing");
            }
        } catch (Exception e) {
            System.err.println("Error loading background image: " + e.getMessage());
            backgroundImage = null;
        }
    }
    
    /**
     * UPDATE GAME LOOP: This method is called periodically by the animation timer
     * It updates customer positions and animations
     */
    private void updateGame() {
        // Don't update game state if game is completed or overlays are visible
        if (gameCompleted || dayOverlayVisible || levelFailedOverlayVisible || gameCompletedOverlayVisible) {
            return;
        }
        
        boolean needsRepaint = false;
        boolean customerReachedCounter = false;
        
        // Only update the current customer's position (first in queue)
        // Other customers wait in line
        if (currentCustomer != null) {
            if (currentCustomer.update(COUNTER_X)) {
                needsRepaint = true;
            }
            
            // Check if customer just reached the counter
            if (currentCustomer.hasReachedCounter() && !currentCustomer.hasBeenNotified()) {
                customerReachedCounter = true;
                currentCustomer.setNotified(true);
                
                // Show the ExpenseAndIncomeTrackerApp when customer reaches counter
                showTrackerApp();
                
                // Start the order timer (20 seconds countdown)
                startOrderTimer();
            }
        }
        
        // Update positions of waiting customers (they line up behind the counter)
        // They should stop behind the customer in front of them
        // Customers at index 0 is the currentCustomer, so queue starts at index 1
        for (int i = 1; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            Customer customerInFront = customers.get(i - 1);
            if (customer.updateWaitingPosition(customerInFront)) {
                needsRepaint = true;
            }
        }
        
        // Redraw if something changed
        if (needsRepaint || customerReachedCounter) {
            gamePanel.repaint();
        }
    }
    
    /**
     * Calculates the order timer seconds based on the current day
     * Day 1: 20 seconds, decreases by 2 seconds per day, minimum 10 seconds
     */
    private int getOrderTimerSeconds() {
        int timerSeconds = BASE_ORDER_TIMER_SECONDS - (currentDay - 1) * TIMER_DECREASE_PER_DAY;
        return Math.max(MIN_ORDER_TIMER_SECONDS, timerSeconds);
    }
    
    /**
     * Starts the order timer when a customer reaches the counter
     */
    private void startOrderTimer() {
        timeRemaining = getOrderTimerSeconds();
        timerActive = true;
        
        // Stop any existing timer
        if (orderTimer != null) {
            orderTimer.stop();
        }
        
        // Create and start countdown timer (updates every second)
        orderTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!timerActive) {
                    ((Timer) e.getSource()).stop();
                    return;
                }
                
                timeRemaining--;
                
                // Update the display
                gamePanel.repaint();
                
                // Check if timer expired
                if (timeRemaining <= 0) {
                    timerActive = false;
                    ((Timer) e.getSource()).stop();
                    handleTimerExpired();
                }
            }
        });
        orderTimer.start();
    }
    
    /**
     * Stops and resets the order timer
     */
    private void stopOrderTimer() {
        timerActive = false;
        if (orderTimer != null) {
            orderTimer.stop();
        }
        timeRemaining = getOrderTimerSeconds();
        gamePanel.repaint();
    }
    
    /**
     * Handles when the order timer expires - customer leaves and penalty is added
     */
    private void handleTimerExpired() {
        if (currentCustomer == null) {
            return;
        }
        
        // Add expense for lost customer
        TransactionDAO.addTransaction("Expense", "lost customer", 10.0);
        
        // Remove the customer who left
        customers.remove(currentCustomer);
        currentCustomer = null;
        
        // Lose a life for failing the customer
        loseLife();
        
        // Reset streak for failed customer
        if (trackerApp != null) {
            trackerApp.resetStreak();
        }
        
        // Move the next customer in line to be the current customer
        if (!customers.isEmpty()) {
            currentCustomer = customers.get(0);
        }
        
        // Shuffle plushie positions after customer leaves
        if (plushieSelectionWindow != null) {
            plushieSelectionWindow.shufflePlushies();
        }
        
        // Reset timer
        stopOrderTimer();
        
        // Refresh tracker app to show the penalty
        if (trackerApp != null) {
            trackerApp.refreshTransactions();
        }
        
        // Redraw the game panel
        gamePanel.repaint();
    }
    
    /**
     * Decrements a life and checks if the level has failed
     */
    private void loseLife() {
        lives--;
        
        // Check if all lives are lost
        if (lives <= 0) {
            showLevelFailedOverlay();
        }
    }
    
    /**
     * Shows the level failed overlay and resets the day
     */
    private void showLevelFailedOverlay() {
        levelFailedOverlayVisible = true;
        
        // Stop customer spawning while overlay is visible
        if (customerSpawnTimer != null) {
            customerSpawnTimer.stop();
        }
        
        // Stop any active order timer
        stopOrderTimer();
        
        // Clear all customers
        customers.clear();
        currentCustomer = null;
        
        // Reset day state (but keep the same day number)
        resetDayState();
        
        gamePanel.repaint();
        
        // Hide overlay after 5 seconds and restart the day
        if (levelFailedOverlayTimer != null) {
            levelFailedOverlayTimer.stop();
        }
        
        levelFailedOverlayTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                levelFailedOverlayVisible = false;
                ((Timer) e.getSource()).stop();
                gamePanel.repaint();
                
                // Show day overlay to restart the day
                showDayOverlay();
            }
        });
        levelFailedOverlayTimer.setRepeats(false);
        levelFailedOverlayTimer.start();
    }
    
    /**
     * Resets the day state when level fails (customers served, lives, but keeps day number)
     */
    private void resetDayState() {
        customersServedToday = 0;
        lives = 3;
        
        // Reset streak on day failure
        if (trackerApp != null) {
            trackerApp.resetStreak();
        }
        
        // Reset all transaction data for the restarted day
        if (trackerApp != null) {
            trackerApp.resetForNewDay();
        }
        
        // Shuffle plushie positions for the restarted day
        if (plushieSelectionWindow != null) {
            plushieSelectionWindow.shufflePlushies();
        }
    }
    
    /**
     * Shows the game completed overlay when Day 7 is successfully completed
     */
    private void showGameCompletedOverlay() {
        gameCompletedOverlayVisible = true;
        
        // Stop customer spawning
        if (customerSpawnTimer != null) {
            customerSpawnTimer.stop();
        }
        
        // Stop any active order timer
        stopOrderTimer();
        
        // Clear all customers
        customers.clear();
        currentCustomer = null;
        
        gamePanel.repaint();
        
        // Note: The overlay will remain visible until the user presses ESC to exit
        // or clicks a restart button (if we add one in the future)
    }
    
    /**
     * Restarts the game from Day 1
     * Called when user wants to restart after game completion
     */
    private void restartGame() {
        gameCompleted = false;
        gameCompletedOverlayVisible = false;
        currentDay = 1;
        customersServedToday = 0;
        lives = 3;
        plushiesSold = 0;
        
        // Reset all transaction data
        if (trackerApp != null) {
            trackerApp.resetForNewDay();
        }
        
        // Reset score and streak on full game restart
        if (trackerApp != null) {
            trackerApp.resetScoreAndStreak();
        }
        
        // Shuffle plushie positions
        if (plushieSelectionWindow != null) {
            plushieSelectionWindow.shufflePlushies();
        }
        
        // Show day overlay to start the game
        showDayOverlay();
    }
    
    /**
     * Determines the rank based on the final total score
     * @param score The final total score
     * @return The rank string ("Noob", "Pro", "Hacker", or "God")
     */
    private String getRankForScore(int score) {
        if (score >= RANK_GOD_THRESHOLD) {
            return "God";
        } else if (score >= RANK_HACKER_THRESHOLD) {
            return "Hacker";
        } else if (score >= RANK_PRO_THRESHOLD) {
            return "Pro";
        } else {
            return "Noob";
        }
    }
    
    /**
     * Shows the day overlay for 5 seconds, then hides it and starts customer spawning
     */
    private void showDayOverlay() {
        dayOverlayVisible = true;
        
        // Reset lives to 3 when starting a new day (or restarting after failure)
        lives = 3;
        
        // Stop customer spawning while overlay is visible
        if (customerSpawnTimer != null) {
            customerSpawnTimer.stop();
        }
        
        // Stop any active order timer
        stopOrderTimer();
        
        // Clear all customers to start the new day fresh
        customers.clear();
        currentCustomer = null;
        
        gamePanel.repaint();
        
        // Hide overlay after 5 seconds and start customer spawning
        if (dayOverlayTimer != null) {
            dayOverlayTimer.stop();
        }
        
        dayOverlayTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dayOverlayVisible = false;
                ((Timer) e.getSource()).stop();
                gamePanel.repaint();
                
                // Start customer spawning after overlay disappears
                if (customerSpawnTimer != null) {
                    customerSpawnTimer.start();
                }
            }
        });
        dayOverlayTimer.setRepeats(false);
        dayOverlayTimer.start();
    }
    
    /**
     * Automatically completes the current customer's order and moves to the next customer
     */
    private void completeCurrentOrder() {
        if (currentCustomer == null) {
            return;
        }
        
        // Calculate score based on time remaining (faster completion = more points)
        // Store time remaining before stopping timer
        int timeRemainingForScore = timeRemaining;
        int maxTime = getOrderTimerSeconds();
        
        // Calculate score: base points + bonus based on time remaining
        // Formula: base 10 points + (time remaining / max time) * 20 bonus points
        // This gives 10-30 points range (10 for very slow, 30 for very fast)
        int basePoints = 10;
        int bonusPoints = (int)((timeRemainingForScore / (double)maxTime) * 20);
        int pointsEarned = basePoints + bonusPoints;
        
        // Add score to the tracker app's game engine (score persists across days)
        if (trackerApp != null) {
            trackerApp.addScoreToGame(pointsEarned);
        }
        
        // Stop the timer since order is complete
        stopOrderTimer();
        
        // Get how many plushies this customer ordered
        int plushiesOrdered = currentCustomer.getOriginalOrderTotal();
        
        // Remove the customer (order completed!)
        customers.remove(currentCustomer);
        currentCustomer = null;
        plushiesSold += plushiesOrdered;
        
        // Increment customers served today
        customersServedToday++;
        
        // Check if we've served 10 customers AND still have lives
        if (customersServedToday >= 10 && lives > 0) {
            // Check if this was Day 7 completion (game completed)
            if (currentDay >= MAX_DAY) {
                // Game completed! Show completion overlay
                gameCompleted = true;
                showGameCompletedOverlay();
            } else {
                // Start next day
                customersServedToday = 0;
                currentDay++;
                lives = 3;  // Reset lives for new day
                
                // Reset all transaction data for the new day
                if (trackerApp != null) {
                    trackerApp.resetForNewDay();
                }
                
                // Shuffle plushie positions for the new day
                if (plushieSelectionWindow != null) {
                    plushieSelectionWindow.shufflePlushies();
                }
                
                showDayOverlay();  // Show overlay, will restart spawning after
            }
        } else {
            // Shuffle plushie positions after each customer is served
            if (plushieSelectionWindow != null) {
                plushieSelectionWindow.shufflePlushies();
            }
        }
        
        // Update the sales counter display
        salesCounterLabel.setText("Plushies Sold: " + plushiesSold);
        
        // Move the next customer in line to be the current customer
        if (!customers.isEmpty()) {
            currentCustomer = customers.get(0);
        }
        
        // Redraw the game panel (customer was removed, next one moves forward)
        gamePanel.repaint();
    }
    
    /**
     * Handles when a plushie is clicked in the selection window
     * If correct color clicked: fulfills order and adds income transaction
     * If wrong color clicked: customer leaves and adds expense penalty
     */
    public void handlePlushieClick(String color) {
        // Don't process clicks if game is completed
        if (gameCompleted || gameCompletedOverlayVisible) {
            return;
        }
        
        if (currentCustomer == null || !currentCustomer.hasReachedCounter()) {
            return;  // No customer at counter
        }
        
        // Check if customer needs this color plushie
        if (currentCustomer.needsPlushie(color)) {
            // CORRECT CLICK - Add income transaction
            String description = "1 " + color + " plushie";
            TransactionDAO.addTransaction("Income", description, PLUSHIE_PRICE);
            
            // Decrease the quantity needed
            currentCustomer.fulfillPlushie(color);
            
            // Refresh tracker app if it's open
            if (trackerApp != null) {
                trackerApp.refreshTransactions();
            }
            
            // Check if order is complete (all plushies fulfilled correctly)
            if (currentCustomer.isOrderComplete()) {
                // Automatically complete the order and move to next customer
                completeCurrentOrder();
            }
        } else {
            // WRONG CLICK - Customer leaves angry, add penalty expense
            TransactionDAO.addTransaction("Expense", "Wrong plushie clicked - customer left", 10.0);
            
            // Stop the timer
            stopOrderTimer();
            
            // Remove the angry customer
            customers.remove(currentCustomer);
            currentCustomer = null;
            
            // Lose a life for wrong click
            loseLife();
            
            // Reset streak for wrong order
            if (trackerApp != null) {
                trackerApp.resetStreak();
            }
            
            // Move the next customer in line to be the current customer
            if (!customers.isEmpty()) {
                currentCustomer = customers.get(0);
            }
            
            // Shuffle plushie positions after wrong click
            if (plushieSelectionWindow != null) {
                plushieSelectionWindow.shufflePlushies();
            }
            
            // Refresh tracker app to show the penalty
            if (trackerApp != null) {
                trackerApp.refreshTransactions();
            }
            
            // Redraw the game panel
            gamePanel.repaint();
        }
    }
    
    /**
     * Main method to start the game
     */
    public static void main(String[] args) {
        // Ensure database connection is available
        try (java.sql.Connection testConnection = DatabaseConnection.getConnection()) {
            // Connection test successful - will be auto-closed by try-with-resources
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, 
                "Error: Could not connect to database.\nPlease check your config.properties file.", 
                "Database Connection Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // Create and start the game on the Event Dispatch Thread (required for Swing)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PlushieShopGame();
            }
        });
    }
    
    /**
     * Inner class: Custom panel that draws the shop scene
     * This is where all the 2D graphics rendering happens
     */
    private class GamePanel extends JPanel {
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            // Enable anti-aliasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Draw the shop background
            drawShopBackground(g2d);
            
            // Note: Counter, cash register, plushies, and sign removed - using PNG background instead
            
            // Draw all customers (they will be on the right side, approaching the counter)
            for (Customer customer : customers) {
                customer.draw(g2d);
            }
            
            // Draw the timer overlay if timer is active
            if (timerActive && currentCustomer != null && currentCustomer.hasReachedCounter()) {
                drawTimerOverlay(g2d);
            }
            
            // Draw the day overlay if it's visible
            if (dayOverlayVisible) {
                drawDayOverlay(g2d);
            }
            
            // Draw the level failed overlay if it's visible
            if (levelFailedOverlayVisible) {
                drawLevelFailedOverlay(g2d);
            }
            
            // Draw the game completed overlay if it's visible
            if (gameCompletedOverlayVisible) {
                drawGameCompletedOverlay(g2d);
            }
        }
        
        /**
         * Draws the day overlay that appears at the start and after every 10 customers
         */
        private void drawDayOverlay(Graphics2D g2d) {
            // Semi-transparent dark background covering entire screen
            g2d.setColor(new Color(0, 0, 0, 180));  // Semi-transparent black
            g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            
            // White border rectangle
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(5));
            int borderPadding = 50;
            g2d.drawRect(borderPadding, borderPadding, 
                        GAME_WIDTH - (borderPadding * 2), 
                        GAME_HEIGHT - (borderPadding * 2));
            
            // Day text - large and centered
            String dayText = "Day " + currentDay;
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            FontMetrics fm = g2d.getFontMetrics();
            
            // Draw text with subtle shadow for depth
            g2d.setColor(new Color(0, 0, 0, 150));  // Shadow
            int textX = (GAME_WIDTH - fm.stringWidth(dayText)) / 2 + 3;
            int textY = (GAME_HEIGHT + fm.getAscent()) / 2 + 3;
            g2d.drawString(dayText, textX, textY);
            
            // Draw main text
            g2d.setColor(Color.WHITE);
            textX = (GAME_WIDTH - fm.stringWidth(dayText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2;
            g2d.drawString(dayText, textX, textY);
        }
        
        /**
         * Draws the level failed overlay that appears when all lives are lost
         */
        private void drawLevelFailedOverlay(Graphics2D g2d) {
            // Semi-transparent dark background covering entire screen
            g2d.setColor(new Color(0, 0, 0, 200));  // Darker than day overlay
            g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            
            // Red border rectangle to indicate failure
            g2d.setColor(new Color(231, 76, 60));  // Red color
            g2d.setStroke(new BasicStroke(5));
            int borderPadding = 50;
            g2d.drawRect(borderPadding, borderPadding, 
                        GAME_WIDTH - (borderPadding * 2), 
                        GAME_HEIGHT - (borderPadding * 2));
            
            // Level Failed text - large and centered
            String failedText = "Day Failed";
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            FontMetrics fm = g2d.getFontMetrics();
            
            // Draw text with subtle shadow for depth
            g2d.setColor(new Color(0, 0, 0, 150));  // Shadow
            int textX = (GAME_WIDTH - fm.stringWidth(failedText)) / 2 + 3;
            int textY = (GAME_HEIGHT + fm.getAscent()) / 2 + 3;
            g2d.drawString(failedText, textX, textY);
            
            // Main text in red
            g2d.setColor(new Color(231, 76, 60));  // Red
            textX = (GAME_WIDTH - fm.stringWidth(failedText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2;
            g2d.drawString(failedText, textX, textY);
            
            // Subtitle text
            String subtitleText = "Restarting Day " + currentDay + "...";
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2d.getFontMetrics();
            g2d.setColor(Color.WHITE);
            textX = (GAME_WIDTH - fm.stringWidth(subtitleText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2 + 60;
            g2d.drawString(subtitleText, textX, textY);
        }
        
        /**
         * Draws the game completed overlay that appears when Day 7 is successfully completed
         */
        private void drawGameCompletedOverlay(Graphics2D g2d) {
            // Semi-transparent dark background covering entire screen
            g2d.setColor(new Color(0, 0, 0, 220));  // Very dark background
            g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
            
            // Gold border rectangle to indicate success
            g2d.setColor(new Color(255, 215, 0));  // Gold color
            g2d.setStroke(new BasicStroke(5));
            int borderPadding = 50;
            g2d.drawRect(borderPadding, borderPadding, 
                        GAME_WIDTH - (borderPadding * 2), 
                        GAME_HEIGHT - (borderPadding * 2));
            
            // Game Completed text - large and centered
            String completedText = "Game Completed";
            g2d.setFont(new Font("Arial", Font.BOLD, 72));
            FontMetrics fm = g2d.getFontMetrics();
            
            // Draw text with subtle shadow for depth
            g2d.setColor(new Color(0, 0, 0, 150));  // Shadow
            int textX = (GAME_WIDTH - fm.stringWidth(completedText)) / 2 + 3;
            int textY = (GAME_HEIGHT + fm.getAscent()) / 2 - 120 + 3;
            g2d.drawString(completedText, textX, textY);
            
            // Main text in gold
            g2d.setColor(new Color(255, 215, 0));  // Gold
            textX = (GAME_WIDTH - fm.stringWidth(completedText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2 - 120;
            g2d.drawString(completedText, textX, textY);
            
            // Get final score and rank
            int finalScore = 0;
            if (trackerApp != null) {
                finalScore = trackerApp.getScore();
            }
            String rank = getRankForScore(finalScore);
            
            // Total Score display - large and prominent
            String scoreText = "Total Score: " + String.format("%,d", finalScore);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            fm = g2d.getFontMetrics();
            g2d.setColor(Color.WHITE);
            textX = (GAME_WIDTH - fm.stringWidth(scoreText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2 - 20;
            g2d.drawString(scoreText, textX, textY);
            
            // Rank display - styled based on rank level
            String rankText = "Rank: " + rank;
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            fm = g2d.getFontMetrics();
            
            // Color code the rank for visual appeal
            Color rankColor;
            switch (rank) {
                case "God":
                    rankColor = new Color(255, 215, 0);  // Gold
                    break;
                case "Hacker":
                    rankColor = new Color(0, 255, 127);  // Green
                    break;
                case "Pro":
                    rankColor = new Color(135, 206, 250);  // Light blue
                    break;
                default:  // Noob
                    rankColor = new Color(192, 192, 192);  // Silver/Gray
                    break;
            }
            
            g2d.setColor(rankColor);
            textX = (GAME_WIDTH - fm.stringWidth(rankText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2 + 40;
            g2d.drawString(rankText, textX, textY);
            
            // Instructions text
            String instructionText = "Press ESC to exit or R to restart";
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            fm = g2d.getFontMetrics();
            g2d.setColor(new Color(200, 200, 200));  // Light gray
            textX = (GAME_WIDTH - fm.stringWidth(instructionText)) / 2;
            textY = (GAME_HEIGHT + fm.getAscent()) / 2 + 100;
            g2d.drawString(instructionText, textX, textY);
        }
        
        /**
         * Draws the timer overlay in the top-right corner
         */
        private void drawTimerOverlay(Graphics2D g2d) {
            int timerX = GAME_WIDTH - 150;
            int timerY = 30;
            int timerWidth = 130;
            int timerHeight = 60;
            
            // Background rectangle with semi-transparent black
            g2d.setColor(new Color(0, 0, 0, 200));  // Semi-transparent black
            g2d.fillRoundRect(timerX, timerY, timerWidth, timerHeight, 10, 10);
            
            // Border
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(timerX, timerY, timerWidth, timerHeight, 10, 10);
            
            // Timer label
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            String label = "Time Left:";
            FontMetrics fm = g2d.getFontMetrics();
            int labelX = timerX + (timerWidth - fm.stringWidth(label)) / 2;
            g2d.drawString(label, labelX, timerY + 25);
            
            // Timer value - change color based on time remaining
            Color timerColor;
            if (timeRemaining > 10) {
                timerColor = new Color(46, 204, 113);  // Green
            } else if (timeRemaining > 5) {
                timerColor = new Color(241, 196, 15);  // Yellow
            } else {
                timerColor = new Color(231, 76, 60);   // Red (urgent!)
            }
            
            g2d.setColor(timerColor);
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            String timeText = String.valueOf(timeRemaining);
            fm = g2d.getFontMetrics();
            int timeX = timerX + (timerWidth - fm.stringWidth(timeText)) / 2;
            g2d.drawString(timeText, timeX, timerY + 55);
        }
        
        /**
         * Draws the shop background - uses PNG image if available, otherwise falls back to programmatic drawing
         */
        private void drawShopBackground(Graphics2D g2d) {
            if (backgroundImage != null) {
                // Draw the background image, scaled to fit the game window
                g2d.drawImage(backgroundImage, 0, 0, GAME_WIDTH, GAME_HEIGHT, null);
            } else {
                // Fallback to programmatic drawing if image not found
                // Background - light colored wall
                g2d.setColor(new Color(245, 245, 245));  // Light gray/white background
                g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);
                
                // Draw dark brown wooden shelves in the background
                drawWoodenShelves(g2d);
            }
        }
        
        /**
         * Draws dark brown wooden shelves in the background
         */
        private void drawWoodenShelves(Graphics2D g2d) {
            Color woodColor = new Color(101, 67, 33);  // Dark brown wood
            
            // Draw horizontal shelf boards
            g2d.setColor(woodColor);
            g2d.setStroke(new BasicStroke(6));
            
            int shelfY1 = 150;
            int shelfY2 = 250;
            int shelfY3 = 350;
            
            // Horizontal shelf boards
            g2d.drawLine(100, shelfY1, GAME_WIDTH - 100, shelfY1);
            g2d.drawLine(100, shelfY2, GAME_WIDTH - 100, shelfY2);
            g2d.drawLine(100, shelfY3, GAME_WIDTH - 100, shelfY3);
            
            // Vertical supports
            g2d.drawLine(100, shelfY1, 100, GAME_HEIGHT - 120);
            g2d.drawLine(GAME_WIDTH - 100, shelfY1, GAME_WIDTH - 100, GAME_HEIGHT - 120);
            
            // Additional vertical supports for realism
            for (int x = 300; x < GAME_WIDTH - 100; x += 200) {
                g2d.drawLine(x, shelfY1, x, GAME_HEIGHT - 120);
            }
        }
        
        /**
         * Draws the dark brown wooden counter
         * DISABLED: Counter removed - using PNG background instead
         */
        @SuppressWarnings("unused")
        private void drawShopCounter(Graphics2D g2d) {
            Color woodColor = new Color(101, 67, 33);  // Dark brown wood
            Color woodDark = new Color(80, 50, 25);    // Darker brown for depth
            
            int counterHeight = 40;
            int counterWidth = GAME_WIDTH - 100;  // Width of counter
            int counterX = 50;  // Starting X position
            
            // Counter top - dark brown wood
            g2d.setColor(woodColor);
            g2d.fillRect(counterX, COUNTER_Y, counterWidth, counterHeight);
            
            // Draw wood grain lines on counter top
            g2d.setColor(woodDark);
            g2d.setStroke(new BasicStroke(1));
            for (int y = COUNTER_Y + 5; y < COUNTER_Y + counterHeight; y += 8) {
                g2d.drawLine(counterX, y, counterX + counterWidth, y);
            }
            
            // Counter front face - darker brown with paneling
            g2d.setColor(woodDark);
            g2d.fillRect(counterX, COUNTER_Y + counterHeight, counterWidth, 30);
            
            // Draw vertical panel lines on counter front
            g2d.setColor(new Color(60, 35, 15));
            g2d.setStroke(new BasicStroke(2));
            for (int x = counterX + 50; x < counterX + counterWidth; x += 150) {
                g2d.drawLine(x, COUNTER_Y + counterHeight, x, COUNTER_Y + counterHeight + 30);
            }
        }
        
        /**
         * Draws the yellow oval sign hanging above the counter
         * DISABLED: Sign removed - using PNG background instead
         */
        @SuppressWarnings("unused")
        private void drawHangingSign(Graphics2D g2d) {
            int signCenterX = GAME_WIDTH / 2;
            int signY = 50;
            int signWidth = 350;
            int signHeight = 100;
            
            // Dark brown hanging rods
            g2d.setColor(new Color(101, 67, 33));  // Dark brown
            g2d.setStroke(new BasicStroke(4));
            int rodLength = 20;
            int rodX1 = signCenterX - signWidth / 2;
            int rodX2 = signCenterX + signWidth / 2;
            g2d.drawLine(rodX1, signY - rodLength, rodX1, signY);
            g2d.drawLine(rodX2, signY - rodLength, rodX2, signY);
            
            // Yellow oval sign background
            g2d.setColor(new Color(255, 255, 200));  // Light yellow
            g2d.fillOval(rodX1, signY, signWidth, signHeight);
            
            // Dark brown border
            g2d.setColor(new Color(101, 67, 33));  // Dark brown border
            g2d.setStroke(new BasicStroke(4));
            g2d.drawOval(rodX1, signY, signWidth, signHeight);
            
            // Sign text - "AMONG US PLUSHIES: $10"
            g2d.setColor(new Color(101, 67, 33));  // Dark brown text
            g2d.setFont(new Font("Arial", Font.BOLD, 22));
            
            // Split text into lines
            String line1 = "AMONG US";
            String line2 = "PLUSHIES:";
            String line3 = "$10";
            
            FontMetrics fm = g2d.getFontMetrics();
            int centerX = rodX1 + signWidth / 2;
            
            // Line 1
            int line1X = centerX - fm.stringWidth(line1) / 2;
            g2d.drawString(line1, line1X, signY + 35);
            
            // Line 2
            int line2X = centerX - fm.stringWidth(line2) / 2;
            g2d.drawString(line2, line2X, signY + 60);
            
            // Line 3
            int line3X = centerX - fm.stringWidth(line3) / 2;
            g2d.drawString(line3, line3X, signY + 85);
        }
        
        /**
         * Draws the cash register on the left side of the counter
         * DISABLED: Cash register removed - using PNG background instead
         */
        @SuppressWarnings("unused")
        private void drawCashRegister(Graphics2D g2d) {
            int registerX = 80;
            int registerY = COUNTER_Y - 40;
            int registerWidth = 60;
            int registerHeight = 50;
            
            // Cash register body - dark gray
            g2d.setColor(new Color(80, 80, 80));
            g2d.fillRect(registerX, registerY, registerWidth, registerHeight);
            
            // Screen/display area
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRect(registerX + 10, registerY + 10, registerWidth - 20, 15);
            
            // Buttons/keypad area
            g2d.setColor(new Color(60, 60, 60));
            g2d.fillRect(registerX + 5, registerY + 30, registerWidth - 10, registerHeight - 35);
            
            // Small buttons
            g2d.setColor(new Color(40, 40, 40));
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 2; j++) {
                    g2d.fillOval(registerX + 15 + (i * 12), registerY + 35 + (j * 8), 8, 8);
                }
            }
        }
        
        /**
         * Draws 8 plushies on the counter (matching the reference image colors)
         * DISABLED: Counter plushies removed - using PNG background instead
         */
        @SuppressWarnings("unused")
        private void drawCounterPlushies(Graphics2D g2d) {
            // Colors in order from image: white, dark blue, light blue, light green, medium green, dark green, orange, red
            Color[] plushieColors = {
                new Color(236, 240, 241),    // White
                new Color(41, 128, 185),     // Dark blue
                new Color(52, 152, 219),     // Light blue
                new Color(46, 204, 113),     // Light green
                new Color(39, 174, 96),      // Medium green
                new Color(27, 120, 66),      // Dark green
                new Color(255, 165, 0),      // Orange
                new Color(231, 76, 60)       // Red
            };
            
            // Starting position for plushies
            int startX = 200;
            int plushieY = COUNTER_Y - 50;
            int spacing = 75;  // Space between plushies
            int plushieSize = 50;
            
            for (int i = 0; i < plushieColors.length; i++) {
                int plushieX = startX + (i * spacing);
                drawPlushieOnCounter(g2d, plushieX, plushieY, plushieColors[i], plushieSize);
            }
        }
        
        /**
         * Draws a single plushie on the counter using PNG image if available
         * Note: This method uses Color to determine which image to load
         * You may need to map colors to color names for proper image loading
         */
        private void drawPlushieOnCounter(Graphics2D g2d, int x, int y, Color bodyColor, int size) {
            // Try to find matching color name for the image
            // Map the Color back to a color name string
            String colorName = getColorNameFromColor(bodyColor);
            
            if (colorName != null) {
                Image plushieImage = PlushieImageLoader.loadPlushieImage(colorName);
                if (plushieImage != null) {
                    // Draw the PNG image, scaled to size
                    g2d.drawImage(plushieImage, x, y, size, size, null);
                    return;
                }
            }
            
            // Fallback to programmatic drawing if image not found
            drawPlushieOnCounterFallback(g2d, x, y, bodyColor, size);
        }
        
        /**
         * Maps a Color object to a color name string for image loading
         */
        private String getColorNameFromColor(Color color) {
            // Map colors to their names (matching the COLOR_MAP in PlushieSelectionWindow)
            if (color.equals(new Color(236, 240, 241))) return "white";
            if (color.equals(new Color(41, 128, 185))) return "blue";
            if (color.equals(new Color(52, 152, 219))) return "blue";
            if (color.equals(new Color(46, 204, 113))) return "green";
            if (color.equals(new Color(39, 174, 96))) return "green";
            if (color.equals(new Color(27, 120, 66))) return "green";
            if (color.equals(new Color(255, 165, 0))) return "orange";
            if (color.equals(new Color(231, 76, 60))) return "red";
            return null;
        }
        
        /**
         * Fallback method to draw plushie programmatically if PNG image is not available
         */
        private void drawPlushieOnCounterFallback(Graphics2D g2d, int x, int y, Color bodyColor, int size) {
            // Body (bean shape)
            g2d.setColor(bodyColor);
            g2d.fillOval(x, y, size, size - 10);
            
            // Backpack
            g2d.setColor(bodyColor.darker());
            g2d.fillOval(x + size - 15, y + 5, 12, 35);
            
            // Visor (light grey with highlight)
            g2d.setColor(new Color(200, 200, 200));  // Light grey
            g2d.fillOval(x + 8, y + 15, 25, 20);
            
            // Visor highlight
            g2d.setColor(new Color(240, 240, 240));
            g2d.fillOval(x + 12, y + 17, 17, 14);
        }
    }
    
    /**
     * Inner class: Represents a customer in the shop
     */
    private class Customer {
        private int x, y;
        private int speed;
        private List<PlushieOrder> orders;  // List of plushie orders (color + quantity)
        private int originalOrderTotal;  // Total plushies originally ordered (before any fulfillments)
        private Color hairColor;
        private Color shirtColor;
        private boolean atCounter;
        private boolean notified;  // Whether we've shown the tracker app for this customer
        
        /**
         * Creates a new customer at the specified position
         * @param x Starting X position (will move toward counter)
         * @param y Y position (stays constant)
         * @param orders List of plushie orders this customer wants
         */
        public Customer(int x, int y, List<PlushieOrder> orders) {
            this.x = x;
            this.y = y;
            this.speed = 2 + random.nextInt(2);  // Random speed between 2-3
            this.orders = new ArrayList<>(orders);  // Copy the list
            this.atCounter = false;
            this.notified = false;
            
            // Calculate original order total
            this.originalOrderTotal = 0;
            for (PlushieOrder order : this.orders) {
                this.originalOrderTotal += order.getQuantity();
            }
            
            // Random appearance for variety
            Color[] hairColors = {
                new Color(101, 67, 33),    // Brown
                new Color(220, 20, 60),    // Red
                new Color(139, 69, 19),    // Dark brown
                Color.BLACK
            };
            
            Color[] shirtColors = {
                new Color(52, 152, 219),   // Blue
                new Color(155, 89, 182),   // Purple
                new Color(241, 196, 15),   // Yellow
                new Color(46, 204, 113),   // Green
                Color.WHITE
            };
            
            this.hairColor = hairColors[random.nextInt(hairColors.length)];
            this.shirtColor = shirtColors[random.nextInt(shirtColors.length)];
        }
        
        /**
         * Updates the customer's position (called in the game loop)
         * Customer walks toward the counter until they reach it
         * @param counterX The X position where the counter is
         * @return true if position changed, false otherwise
         */
        public boolean update(int counterX) {
            if (atCounter) {
                return false;  // Already at counter, don't move
            }
            
            int oldX = x;
            int targetX = counterX + 90;  // 90 is offset for customer to stand at counter (1.5x scale)
            
            // Move toward the counter
            if (x > targetX) {
                x -= speed;
                // Don't go past the counter
                if (x <= targetX) {
                    x = targetX;
                    atCounter = true;
                }
            } else {
                atCounter = true;
            }
            
            return x != oldX;
        }
        
        /**
         * Updates the position of a waiting customer (they line up behind others)
         * @param customerInFront The customer standing in front of this one
         * @return true if position changed, false otherwise
         */
        public boolean updateWaitingPosition(Customer customerInFront) {
            if (atCounter) {
                return false;  // Already at counter, don't move
            }
            
            int oldX = x;
            // Wait behind the customer in front with proper spacing (customer width is ~60px at 1.5x scale, so use 30px spacing)
            int customerWidth = 60;  // Approximate customer width (scaled 1.5x from 40)
            int spacing = 30;  // Gap between customers (scaled 1.5x from 20)
            int targetX = customerInFront.getX() + customerWidth + spacing;
            
            // Don't move past the counter area - wait in queue behind the counter
            int maxQueueX = COUNTER_X + 250;  // Maximum queue position
            if (targetX > maxQueueX) {
                targetX = maxQueueX;
            }
            
            // Move toward the target position
            if (x > targetX) {
                x -= speed;
                if (x <= targetX) {
                    x = targetX;
                }
            } else if (x < targetX - spacing) {
                // Don't move forward if there's not enough space
                // Stay in place until customer in front moves
            }
            
            return x != oldX;
        }
        
        /**
         * Returns the current X position of the customer
         */
        public int getX() {
            return x;
        }
        
        /**
         * Returns whether the tracker app has been shown for this customer
         */
        public boolean hasBeenNotified() {
            return notified;
        }
        
        /**
         * Sets the notified flag
         */
        public void setNotified(boolean notified) {
            this.notified = notified;
        }
        
        /**
         * Draws the customer on screen with speech bubble
         */
        public void draw(Graphics2D g2d) {
            // Draw speech bubble only if customer is at the counter
            if (atCounter) {
                drawSpeechBubble(g2d);
            }
            
            // Draw the customer character (human figure)
            drawCustomerCharacter(g2d);
        }
        
        /**
         * Draws a speech bubble above the customer showing how many plushies they want
         */
        private void drawSpeechBubble(Graphics2D g2d) {
            // Get the order text
            String text = getOrderText();
            if (text.isEmpty()) {
                return;
            }
            
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            
            // Handle multi-line text if needed (wrap at 200 pixels)
            int maxWidth = 200;
            if (textWidth > maxWidth) {
                // Simple word wrapping - split into multiple lines
                String[] words = text.split(" ");
                List<String> lines = new ArrayList<>();
                StringBuilder currentLine = new StringBuilder();
                
                for (String word : words) {
                    String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                    if (fm.stringWidth(testLine) <= maxWidth) {
                        currentLine = new StringBuilder(testLine);
                    } else {
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                        }
                        currentLine = new StringBuilder(word);
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                
                // Draw multi-line text
                int bubbleHeight = (textHeight * lines.size()) + 20;
                int bubbleWidth = maxWidth + 30;
                int bubbleX = x - (bubbleWidth / 2);
                int bubbleY = y - (int)(90 * 1.5) - (bubbleHeight - textHeight);  // Scaled 1.5x to account for larger customer
                
                // Speech bubble background (white)
                g2d.setColor(Color.WHITE);
                g2d.fillOval(bubbleX, bubbleY, bubbleWidth, bubbleHeight);
                
                // Speech bubble outline (black)
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(bubbleX, bubbleY, bubbleWidth, bubbleHeight);
                
                // Draw the tail pointing down to customer (scaled 1.5x)
                int[] tailX = {x, x - 15, x + 15};
                int[] tailY = {y - (int)(45 * 1.5), y - (int)(35 * 1.5), y - (int)(35 * 1.5)};
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(tailX, tailY, 3);
                g2d.setColor(Color.BLACK);
                g2d.drawPolyline(tailX, tailY, 3);
                
                // Draw text lines
                g2d.setColor(Color.BLACK);
                int lineY = bubbleY + textHeight + 5;
                for (String line : lines) {
                    int lineX = bubbleX + (bubbleWidth - fm.stringWidth(line)) / 2;
                    g2d.drawString(line, lineX, lineY);
                    lineY += textHeight;
                }
            } else {
                // Single line text
                int bubbleWidth = textWidth + 30;
                int bubbleHeight = textHeight + 20;
                int bubbleX = x - (bubbleWidth / 2);
                int bubbleY = y - (int)(90 * 1.5);  // Scaled 1.5x to account for larger customer
                
                // Speech bubble background (white)
                g2d.setColor(Color.WHITE);
                g2d.fillOval(bubbleX, bubbleY, bubbleWidth, bubbleHeight);
                
                // Speech bubble outline (black)
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(bubbleX, bubbleY, bubbleWidth, bubbleHeight);
                
                // Draw the tail pointing down to customer (scaled 1.5x)
                int[] tailX = {x, x - 15, x + 15};
                int[] tailY = {y - (int)(45 * 1.5), y - (int)(35 * 1.5), y - (int)(35 * 1.5)};
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(tailX, tailY, 3);
                g2d.setColor(Color.BLACK);
                g2d.drawPolyline(tailX, tailY, 3);
                
                // Text inside speech bubble
                g2d.setColor(Color.BLACK);
                int textX = bubbleX + (bubbleWidth - textWidth) / 2;
                int textY = bubbleY + (bubbleHeight + fm.getAscent()) / 2 - 5;
                g2d.drawString(text, textX, textY);
            }
        }
        
        /**
         * Draws the customer character (human figure) - scaled 1.5x
         */
        private void drawCustomerCharacter(Graphics2D g2d) {
            // Scale factor: 1.5x
            double scale = 1.5;
            
            // Head (circle) - scaled from 40x40 to 60x60
            g2d.setColor(new Color(255, 220, 177));  // Skin color
            int headSize = (int)(40 * scale);
            int headOffset = headSize / 2;
            g2d.fillOval(x - headOffset, y - headOffset, headSize, headSize);
            
            // Hair - scaled
            g2d.setColor(hairColor);
            g2d.fillArc(x - headOffset, y - headOffset, headSize, (int)(35 * scale), 0, 180);
            
            // Shirt (simple rectangle) - scaled from 36x45 to 54x68
            g2d.setColor(shirtColor);
            int shirtWidth = (int)(36 * scale);
            int shirtHeight = (int)(45 * scale);
            int shirtOffsetX = shirtWidth / 2;
            g2d.fillRect(x - shirtOffsetX, y + (int)(18 * scale), shirtWidth, shirtHeight);
            
            // Arms - scaled from 12x35 to 18x53
            int armWidth = (int)(12 * scale);
            int armHeight = (int)(35 * scale);
            g2d.fillOval(x - (int)(25 * scale), y + (int)(20 * scale), armWidth, armHeight);
            g2d.fillOval(x + (int)(13 * scale), y + (int)(20 * scale), armWidth, armHeight);
            
            // Eyes - scaled
            int eyeSize = (int)(5 * scale);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(x - (int)(8 * scale), y - (int)(5 * scale), eyeSize, eyeSize);
            g2d.fillOval(x + (int)(3 * scale), y - (int)(5 * scale), eyeSize, eyeSize);
            
            // Smile - scaled
            g2d.setStroke(new BasicStroke((float)(2 * scale)));
            g2d.drawArc(x - (int)(8 * scale), y + (int)(2 * scale), (int)(16 * scale), (int)(10 * scale), 0, 180);
        }
        
        /**
         * Returns true if customer has reached the counter
         */
        public boolean hasReachedCounter() {
            return atCounter;
        }
        
        /**
         * Returns the total number of plushies this customer wants
         */
        public int getTotalPlushiesWanted() {
            int total = 0;
            for (PlushieOrder order : orders) {
                total += order.getQuantity();
            }
            return total;
        }
        
        /**
         * Checks if the customer still needs a plushie of the given color
         */
        public boolean needsPlushie(String color) {
            for (PlushieOrder order : orders) {
                if (order.getColor().equals(color) && order.getQuantity() > 0) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Fulfills one plushie of the given color (decreases quantity)
         */
        public void fulfillPlushie(String color) {
            for (PlushieOrder order : orders) {
                if (order.getColor().equals(color) && order.getQuantity() > 0) {
                    order.decreaseQuantity();
                    break;
                }
            }
        }
        
        /**
         * Checks if the order is complete (all quantities are 0 or less)
         */
        public boolean isOrderComplete() {
            for (PlushieOrder order : orders) {
                if (order.getQuantity() > 0) {
                    return false;
                }
            }
            return true;
        }
        
        /**
         * Returns the original total number of plushies ordered (before any fulfillments)
         */
        public int getOriginalOrderTotal() {
            return originalOrderTotal;
        }
        
        /**
         * Gets the order text for the speech bubble
         */
        public String getOrderText() {
            List<String> parts = new ArrayList<>();
            for (PlushieOrder order : orders) {
                if (order.getQuantity() > 0) {
                    String part;
                    if (order.getQuantity() == 1) {
                        part = "1 " + order.getColor() + " plushie";
                    } else {
                        part = order.getQuantity() + " " + order.getColor() + " plushies";
                    }
                    parts.add(part);
                }
            }
            
            if (parts.isEmpty()) {
                return "";
            } else if (parts.size() == 1) {
                return parts.get(0) + ", please!";
            } else {
                // Join with " and " for last item
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.size() - 1; i++) {
                    sb.append(parts.get(i));
                    if (i < parts.size() - 2) {
                        sb.append(", ");
                    }
                }
                sb.append(" and ").append(parts.get(parts.size() - 1));
                sb.append(", please!");
                return sb.toString();
            }
        }
        
    }
}
