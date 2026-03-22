package net.veloxia.universaldb;

import lombok.Getter;
import lombok.Setter;
import net.veloxia.universaldb.annotations.Column;
import net.veloxia.universaldb.annotations.Id;
import net.veloxia.universaldb.annotations.Table;

@Setter
@Getter
@Table(name = "users")
public class User {

    @Id
    private Long id;
    private String name;

    @Column(unique = true)
    private String email;

    private int age;
    private boolean active;

    /** Required no-arg constructor for reflection-based instantiation. */
    public User() {}

    public User(String name, String email, int age) {
        this(name, email, age, true);
    }

    public User(String name, String email, int age, boolean active) {
        this.name   = name;
        this.email  = email;
        this.age    = age;
        this.active = active;
    }

    @Override public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', age=" + age + "}";
    }
}
