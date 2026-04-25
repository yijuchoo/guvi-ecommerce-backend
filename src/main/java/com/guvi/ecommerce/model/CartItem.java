package com.guvi.ecommerce.model;

/*
Classes without @Document = they don't live independently. They are embedded inside another document
CartItem → lives inside a Cart document
Classes with @Document = have their own collection in the database
Items are just a JSON array field within the cart
*/
public class CartItem {

    private String productId;
    private String productName;
    private int quantity;
    private double price;

    public CartItem() {
    }

    public CartItem(String productId, String productName, int quantity, double price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
