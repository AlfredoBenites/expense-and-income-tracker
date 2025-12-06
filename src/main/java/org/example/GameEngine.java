package org.example;

import java.time.LocalDate;
import java.util.List;

public class GameEngine {
    private int score = 0;  // Single score value (replaces XP/level system)
    private int currentStreak = 0;  // Consecutive correct customer orders
    private int dailyStreak = 0;
    private LocalDate lastLogDate = null;
    private double monthlyBudget = 0; // optional; set via UI
    private double monthNet = 0;
    
    // Streak multiplier constants
    private static final double STREAK_GROWTH_FACTOR = 0.1;  // 10% increase per streak
    private static final double BASE_MULTIPLIER = 1.0;  // Base multiplier at streak 0

    public void onTransactionAdded(String type, double amount, LocalDate today) {
        handleStreak(today);

        if ("Income".equals(type)) {
            monthNet += amount;
        } else {
            monthNet -= amount;
        }
        // Note: Score is now only added through gameplay (addScore method), not transactions
    }

    /**
     * Recalculates game state from all existing transactions.
     * This is called on app startup to restore game state.
     */
    public void recalculateFromTransactions(List<Transaction> transactions) {
        // Reset state (but keep score and currentStreak - they persist across days)
        monthNet = 0;
        dailyStreak = 0;
        lastLogDate = null;
        
        LocalDate today = LocalDate.now();
        
        // Process all transactions to recalculate monthNet
        for (Transaction transaction : transactions) {
            String type = transaction.getType();
            double amount = transaction.getAmount();
            
            if ("Income".equals(type)) {
                monthNet += amount;
            } else {
                monthNet -= amount;
            }
        }
        
        // For streak, check if there are any transactions today
        // Since we don't store transaction dates, we'll set streak to 1 if there are transactions
        // and reset it if there are no transactions today (simplified approach)
        if (!transactions.isEmpty()) {
            // If there are transactions, assume at least one was today
            dailyStreak = 1;
            lastLogDate = today;
        }
    }

    private void handleStreak(LocalDate today) {
        if (lastLogDate == null) { dailyStreak = 1; }
        else if (lastLogDate.plusDays(1).equals(today)) { dailyStreak++; }
        else if (!lastLogDate.equals(today)) { dailyStreak = 1; }
        lastLogDate = today;
    }

    /**
     * Adds score points directly (not tied to transactions)
     * Used for gameplay-based score rewards (e.g., speed bonuses)
     * Score persists across customers and days
     * @param basePoints The base number of points (before streak multiplier)
     */
    public void addScore(int basePoints) {
        // Calculate streak multiplier: 1.0 + (streak * growth factor)
        // Example: streak 0 = 1.0x, streak 5 = 1.5x, streak 10 = 2.0x, streak 20 = 3.0x
        double multiplier = BASE_MULTIPLIER + (currentStreak * STREAK_GROWTH_FACTOR);
        int finalPoints = (int)(basePoints * multiplier);
        score += finalPoints;
    }
    
    /**
     * Increments the current streak (called when a customer order is completed correctly)
     */
    public void incrementStreak() {
        currentStreak++;
    }
    
    /**
     * Resets the current streak to 0 (called when a customer order is wrong or failed)
     */
    public void resetStreak() {
        currentStreak = 0;
    }
    
    /**
     * Resets the score to 0 (called on full game restart)
     */
    public void resetScore() {
        score = 0;
    }

    // Getters:
    public int getScore() { return score; }
    public int getCurrentStreak() { return currentStreak; }
    public int getDailyStreak() { return dailyStreak; }
    public void setMonthlyBudget(double b) { this.monthlyBudget = b; }
    public void resetMonthlyNet() { monthNet = 0; }
}