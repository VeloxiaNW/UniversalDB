package net.veloxia.universaldb;

import lombok.Getter;
import lombok.Setter;
import net.veloxia.universaldb.annotations.Id;
import net.veloxia.universaldb.annotations.Table;

@Setter
@Getter
@Table(name = "products")
public class Product {

    @Id
    private Long id;
    private String name;
    private double price;
    private int stock;

    public Product() {}

    public Product(String name, double price, int stock) {
        this.name  = name;
        this.price = price;
        this.stock = stock;
    }
}
