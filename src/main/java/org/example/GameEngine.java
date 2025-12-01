package org.example;

import java.time.LocalDate;

public class GameEngine {
    private int xp = 0, level = 1, gold = 0, dailyStreak = 0, monthHealth = 100;
    private LocalDate lastLogDate = null;
    private double monthlyBudget = 0; // optional; set via UI
    private double monthNet = 0;

    public void onTransactionAdded(String type, double amount, LocalDate today) {
        handleStreak(today);
        int gained = 5; // base

        if ("Income".equals(type)) {
            gained += (int)(amount / 50.0);
            gold += (int)(amount / 100.0);
            monthNet += amount;
        } else {
            gained += (int)(amount / 100.0);
            monthNet -= amount;
        }
        xp += gained;
        updateLevel();

        if (monthlyBudget > 0 && monthNet < -monthlyBudget) {
            monthHealth = Math.max(0, monthHealth - Math.min(5, (int)(amount / 100.0)));
        }
    }

    private void handleStreak(LocalDate today) {
        if (lastLogDate == null) { dailyStreak = 1; }
        else if (lastLogDate.plusDays(1).equals(today)) { dailyStreak++; }
        else if (!lastLogDate.equals(today)) { dailyStreak = 1; }
        lastLogDate = today;
    }

    private void updateLevel() { level = 1 + (xp / 100); }

    // Getters:
    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public int getGold() { return gold; }
    public int getDailyStreak() { return dailyStreak; }
    public int getMonthHealth() { return monthHealth; }
    public void setMonthlyBudget(double b) { this.monthlyBudget = b; }
    public void resetMonthlyNet() { monthNet = 0; monthHealth = 100; }
}