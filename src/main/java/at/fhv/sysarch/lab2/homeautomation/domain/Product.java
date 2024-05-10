package at.fhv.sysarch.lab2.homeautomation.domain;

import java.util.Objects;

public class Product {
    private String name;
    private double price;
    private double weight;
    private int space;

    public Product(String name, double price, double weight) {
        this.name = name;
        this.price = price;
        this.weight = weight;
        this.space = 1;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public double getWeight() {
        return weight;
    }

    public double getSpace() {
        return space;
    }

    @Override
    public String toString() {
        return "Product{" +
                "name='" + name + '\'' +
                ", price=" + price +
                ", weight=" + weight +
                ", space=" + space +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Product other = (Product) obj;
        return Objects.equals(name, other.name);
    }
}
