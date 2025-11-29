package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class ExpenseAndIncomeTrackerApp {

    // Variables for the main frame and UI Components
    private JFrame frame;
    private JPanel titleBar;
    private JLabel titleLabel;
    private JLabel closeLabel;
    private JLabel minimizeLabel;
    private JPanel dashboardPanel;
    private JPanel buttonsPanel;
    private JButton addTransactionButton;
    private JButton removeTransactionButton;
    private JTable transactionsTable;
    private DefaultTableModel tableModel;

    // Variables to store the total amount
    private double totalAmount = 0.0;

    // Arraylist to store data panel values
    private ArrayList<String> dataPanelValues = new ArrayList<String>();

    // Variables for form dragging
    private boolean isDragging = false;
    private Point mouseOffset;

    // Constructor
    public ExpenseAndIncomeTrackerApp() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);
        // Remove form border and default close and minimize buttons
        frame.setUndecorated(true);
        // Set custom border to the frame
        frame.getRootPane().setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, new Color(52, 73, 94)));

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new ExpenseAndIncomeTrackerApp();
    }



}
