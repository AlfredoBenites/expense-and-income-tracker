package org.example;

/**
 * Represents a plushie order with a color and quantity
 */
public class PlushieOrder {
    private String color;
    private int quantity;
    
    public PlushieOrder(String color, int quantity) {
        this.color = color;
        this.quantity = quantity;
    }
    
    public String getColor() {
        return color;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    /**
     * Decreases the quantity by 1
     * @return true if quantity is now 0 or less, false otherwise
     */
    public boolean decreaseQuantity() {
        this.quantity--;
        return this.quantity <= 0;
    }
}


