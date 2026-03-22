# UniversalDB

A lightweight, type-safe database library for Veloxia Network supporting **SQLite**, **MySQL**, and **MongoDB** - all through one unified API.

---

## Setup

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.VeloxiaNW:UniversalDB:1.0.0")
}
```

---

## Quick Start

### 1. Define an entity

```java
import net.veloxia.universaldb.annotations.*;

@Table("users")
public class User {

    @Id
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private int age;
    private boolean active;

    // Required: no-arg constructor
    public User() {}

    public User(String name, String email, int age) {
        this.name  = name;
        this.email = email;
        this.age   = age;
        this.active = true;
    }

    // Getters & setters...
    public Long    getId()    { return id; }
    public String  getName()  { return name; }
    public void    setAge(int age) { this.age = age; }
    // ...
}
```

### 2. Connect to a database

```java
import net.veloxia.universaldb.*;
import net.veloxia.universaldb.config.DatabaseConfig.*;

// SQLite
UniversalDB db = UniversalDB.connect(new SQLiteConfig("myapp.db"));
UniversalDB db = UniversalDB.connect(new SQLiteConfig(":memory:")); // in-memory

// MySQL
UniversalDB db = UniversalDB.connect(new MySQLConfig.Builder("myapp", "root", "secret")
    .host("localhost")
    .port(3306)
    .build());

// MongoDB
UniversalDB db = UniversalDB.connect(new MongoDBConfig("myapp"));
UniversalDB db = UniversalDB.connect(new MongoDBConfig("mongodb://user:pass@host:27017", "myapp"));
```

### 3. Get a repository

```java
// SQL databases → ID type is Long
Repository<User, Long> users = (Repository<User, Long>) db.repository(User.class);

// MongoDB → ID type is String
Repository<User, String> users = (Repository<User, String>) db.repository(User.class);
```

---

## CRUD Operations

```java
// INSERT - ID is set on the entity after insert
User alice = users.insert(new User("Alice", "alice@example.com", 30));
System.out.println(alice.getId()); // e.g. 1

// FIND BY ID
Optional<User> found = users.findById(alice.getId());

// FIND ALL
List<User> everyone = users.findAll();

// UPDATE
alice.setAge(31);
users.update(alice);

// SAVE (insert if no ID, update otherwise)
users.save(alice);

// DELETE
users.deleteById(alice.getId());
users.delete(alice);
```

---

## Query DSL

```java
import net.veloxia.universaldb.query.*;

// Filter by field
List<User> adults = users.findMany(
    new Query<User>().where("age").gte(18)
);

// Multiple conditions (chained)
List<User> results = users.findMany(
    new Query<User>()
        .where("active").eq(true)
        .where("age").lt(40)
);

// Sorting + pagination
List<User> page2 = users.findMany(
    new Query<User>()
        .where("active").eq(true)
        .orderBy("name", SortDirection.ASC)
        .limit(10)
        .offset(10)
);

// Pattern matching
List<User> gmailUsers = users.findMany(
    new Query<User>().where("email").like("%@gmail.com")
);

// IN / NOT IN
List<User> specific = users.findMany(
    new Query<User>().where("name").in(List.of("Alice", "Bob"))
);

// First match
Optional<User> admin = users.findOne(
    new Query<User>().where("email").eq("admin@example.com")
);

// Count
long total  = users.count();
long active = users.count(new Query<User>().where("active").eq(true));

// Exists
boolean hasAdmin = users.exists(
    new Query<User>().where("email").eq("admin@example.com")
);

// Bulk update
users.updateWhere(
    new Query<User>().where("active").eq(false),
    Map.entry("age", 0)
);

// Bulk delete
users.deleteWhere(new Query<User>().where("active").eq(false));
```

---

## Supported Operators

| Method          | SQL equivalent         |
|-----------------|------------------------|
| `.eq(value)`    | `field = ?`            |
| `.neq(value)`   | `field != ?`           |
| `.gt(value)`    | `field > ?`            |
| `.gte(value)`   | `field >= ?`           |
| `.lt(value)`    | `field < ?`            |
| `.lte(value)`   | `field <= ?`           |
| `.like(pattern)`| `field LIKE ?`         |
| `.in(list)`     | `field IN (...)`       |
| `.notIn(list)`  | `field NOT IN (...)`   |
| `.isNull()`     | `field IS NULL`        |
| `.isNotNull()`  | `field IS NOT NULL`    |

---

## Annotations

| Annotation                            | Description                                            |
|---------------------------------------|--------------------------------------------------------|
| `@Table("name")`                      | Table/collection name (defaults to lowercase class name) |
| `@Id`                                 | Primary key / MongoDB `_id`                            |
| `@Column(name, nullable, unique)`     | Customize column name and constraints                  |
| `@Transient`                          | Exclude a field from persistence                       |

---

## Requirements

- Java 21+
- Entity classes must have a **no-arg constructor**
- Entity classes must have an `@Id` field (or a field named `id`)

---

## Running Tests

```bash
./gradlew test
```

## Building

```bash
./gradlew build
```
