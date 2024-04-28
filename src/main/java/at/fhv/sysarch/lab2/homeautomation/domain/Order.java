package at.fhv.sysarch.lab2.homeautomation.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {
    private int id;
    private Product product;
    private LocalDateTime orderDate;
    private static int nextID = 1;

    public Order(Product product) {
        this.id = nextID++;
        this.product = product;
        this.orderDate = LocalDateTime.now();
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static int getNextID() {
        return nextID;
    }

    public static void setNextID(int nextID) {
        Order.nextID = nextID;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", product=" + product +
                ", orderDate=" + orderDate +
                '}';
    }
}
