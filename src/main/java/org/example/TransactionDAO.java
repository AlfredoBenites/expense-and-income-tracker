package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// DAO (Data Access Object) class for handling transactions in the database
public class TransactionDAO {

    // Method to retrieve all transactions from the database
    public static List<Transaction> getAllTransaction(){

        // Create a list to store Transaction objects
        List<Transaction> transactions = new ArrayList<>();

        // Use try-with-resources to ensure connection, statement, and result set are closed
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement("SELECT * FROM `transaction_table`");
             ResultSet rs = ps.executeQuery()) {

            // Iterate through the result set obtained from the SQL query
            while (rs.next()) {
                // Extract transaction details from the result set
                int id = rs.getInt("id");
                String type = rs.getString("transaction_type");
                String description = rs.getString("description");
                double amount = rs.getDouble("amount");

                // Create a Transaction object with the retrieved details
                Transaction transaction = new Transaction(id, type, description, amount);
                // Add the Transaction object to the list
                transactions.add(transaction);
            }

        } catch (SQLException ex) {
            Logger.getLogger(TransactionDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Return the list of transactions
        return transactions;
    }

    /**
     * Method to add a new transaction to the database
     * @param type The transaction type (Income or Expense)
     * @param description Description of the transaction
     * @param amount The transaction amount
     * @return true if successful, false otherwise
     */
    public static boolean addTransaction(String type, String description, double amount) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String insertQuery = "INSERT INTO `transaction_table`(`transaction_type`, `description`, `amount`) VALUES (?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
                ps.setString(1, type);
                ps.setString(2, description);
                ps.setDouble(3, amount);
                ps.executeUpdate();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(TransactionDAO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     * Method to clear all transactions from the database
     * @return true if successful, false otherwise
     */
    public static boolean clearAllTransactions() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM `transaction_table`")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(TransactionDAO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     * Method to reset the AUTO_INCREMENT ID to 0
     * @return true if successful, false otherwise
     */
    public static boolean resetAutoIncrement() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement("ALTER TABLE `transaction_table` AUTO_INCREMENT = 1")) {
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(TransactionDAO.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

}