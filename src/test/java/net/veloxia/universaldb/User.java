package net.veloxia.universaldb;

import net.veloxia.universaldb.annotations.Column;
import net.veloxia.universaldb.annotations.Id;
import net.veloxia.universaldb.annotations.Table;

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

    public Long    getId()      { return id; }
    public void    setId(Long id) { this.id = id; }
    public String  getName()    { return name; }
    public void    setName(String name) { this.name = name; }
    public String  getEmail()   { return email; }
    public void    setEmail(String email) { this.email = email; }
    public int     getAge()     { return age; }
    public void    setAge(int age) { this.age = age; }
    public boolean isActive()   { return active; }
    public void    setActive(boolean active) { this.active = active; }

    @Override public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', age=" + age + "}";
    }
}
