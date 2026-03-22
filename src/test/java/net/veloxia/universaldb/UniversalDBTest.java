package net.veloxia.universaldb;

import net.veloxia.universaldb.config.DatabaseConfig.SQLiteConfig;
import net.veloxia.universaldb.query.Query;
import net.veloxia.universaldb.query.SortDirection;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class UniversalDBTest {

    private static UniversalDB db;
    private Repository<User, Long> users;

    @BeforeAll
    static void initDb() {
        db = UniversalDB.connect(new SQLiteConfig(":memory:"));
    }

    @AfterAll
    static void closeDb() {
        db.close();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        users = (Repository<User, Long>) db.repository(User.class);
        users.clear();
    }

    @Test @DisplayName("insert: returns entity with generated ID")
    void insertGeneratesId() {
        User user = users.insert(new User("Alice", "alice@example.com", 30));
        assertNotNull(user.getId());
        assertTrue(user.getId() > 0);
    }

    @Test @DisplayName("insert: persists all fields")
    void insertPersistsAllFields() {
        users.insert(new User("Bob", "bob@example.com", 25, false));
        Optional<User> found = users.findMany(new Query<User>().where("name").eq("Bob")).stream().findFirst();
        assertTrue(found.isPresent());
        assertEquals(25, found.get().getAge());
        assertFalse(found.get().isActive());
    }

    @Test @DisplayName("insertAll: bulk insert with IDs")
    void insertAll() {
        List<User> saved = users.insertAll(List.of(
            new User("A", "a@x.com", 20),
            new User("B", "b@x.com", 21),
            new User("C", "c@x.com", 22)
        ));
        assertEquals(3, saved.size());
        saved.forEach(u -> assertNotNull(u.getId()));
    }

    @Test @DisplayName("findById: returns correct entity")
    void findById() {
        User inserted = users.insert(new User("Carol", "carol@example.com", 28));
        Optional<User> found = users.findById(inserted.getId());
        assertTrue(found.isPresent());
        assertEquals("carol@example.com", found.get().getEmail());
    }

    @Test @DisplayName("findById: empty for missing ID")
    void findByIdMissing() {
        assertTrue(users.findById(99999L).isEmpty());
    }

    @Test @DisplayName("findAll: returns all entities")
    void findAll() {
        users.insert(new User("Alice", "a@x.com", 30));
        users.insert(new User("Bob",   "b@x.com", 25));
        users.insert(new User("Carol", "c@x.com", 35));
        assertEquals(3, users.findAll().size());
    }

    @Test @DisplayName("query: eq filter")
    void queryEq() {
        users.insert(new User("Alice", "alice@x.com", 30));
        users.insert(new User("Bob",   "bob@x.com",   25));
        List<User> result = users.findMany(new Query<User>().where("name").eq("Alice"));
        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getName());
    }

    @Test @DisplayName("query: gt filter")
    void queryGt() {
        users.insert(new User("Young", "y@x.com", 17));
        users.insert(new User("Adult", "a@x.com", 25));
        users.insert(new User("Elder", "e@x.com", 65));
        assertEquals(2, users.findMany(new Query<User>().where("age").gt(18)).size());
    }

    @Test @DisplayName("query: gte + lte range")
    void queryRange() {
        users.insert(new User("A", "a@x.com", 10));
        users.insert(new User("B", "b@x.com", 20));
        users.insert(new User("C", "c@x.com", 30));
        users.insert(new User("D", "d@x.com", 40));
        assertEquals(2, users.findMany(new Query<User>().where("age").gte(20).where("age").lte(30)).size());
    }

    @Test @DisplayName("query: like filter")
    void queryLike() {
        users.insert(new User("Alice",   "alice@gmail.com",   25));
        users.insert(new User("Bob",     "bob@yahoo.com",     30));
        users.insert(new User("Charlie", "charlie@gmail.com", 28));
        assertEquals(2, users.findMany(new Query<User>().where("email").like("%gmail%")).size());
    }

    @Test @DisplayName("query: IN filter")
    void queryIn() {
        users.insert(new User("Alice", "a@x.com", 20));
        users.insert(new User("Bob",   "b@x.com", 25));
        users.insert(new User("Carol", "c@x.com", 30));
        assertEquals(2, users.findMany(new Query<User>().where("name").in(List.of("Alice", "Carol"))).size());
    }

    @Test @DisplayName("query: NOT IN filter")
    void queryNotIn() {
        users.insert(new User("Alice", "a@x.com", 20));
        users.insert(new User("Bob",   "b@x.com", 25));
        users.insert(new User("Carol", "c@x.com", 30));
        assertEquals(2, users.findMany(new Query<User>().where("name").notIn(List.of("Bob"))).size());
    }

    @Test @DisplayName("query: orderBy ASC")
    void queryOrderByAsc() {
        users.insert(new User("Charlie", "c@x.com", 30));
        users.insert(new User("Alice",   "a@x.com", 20));
        users.insert(new User("Bob",     "b@x.com", 25));
        List<User> sorted = users.findMany(new Query<User>().orderBy("name", SortDirection.ASC));
        assertEquals("Alice", sorted.get(0).getName());
        assertEquals("Bob",   sorted.get(1).getName());
    }

    @Test @DisplayName("query: orderBy DESC")
    void queryOrderByDesc() {
        users.insert(new User("Alice", "a@x.com", 20));
        users.insert(new User("Bob",   "b@x.com", 30));
        users.insert(new User("Carol", "c@x.com", 25));
        List<User> sorted = users.findMany(new Query<User>().orderBy("age", SortDirection.DESC));
        assertEquals(30, sorted.getFirst().getAge());
    }

    @Test @DisplayName("query: limit and offset (pagination)")
    void queryLimitOffset() {
        for (int i = 0; i < 10; i++)
            users.insert(new User("User" + i, i + "@x.com", 20 + i));
        List<User> page = users.findMany(new Query<User>().orderBy("age").limit(3).offset(2));
        assertEquals(3, page.size());
        assertEquals(22, page.getFirst().getAge());
    }

    @Test @DisplayName("query: findOne returns first match")
    void findOne() {
        users.insert(new User("Alice", "alice@x.com", 30));
        Optional<User> found = users.findOne(new Query<User>().where("name").eq("Alice"));
        assertTrue(found.isPresent());
    }

    @Test @DisplayName("query: findOne returns empty for no match")
    void findOneEmpty() {
        assertTrue(users.findOne(new Query<User>().where("name").eq("Nobody")).isEmpty());
    }

    @Test @DisplayName("count: total")
    void countTotal() {
        users.insert(new User("A", "a@x.com", 20));
        users.insert(new User("B", "b@x.com", 30));
        assertEquals(2L, users.count());
    }

    @Test @DisplayName("count: filtered")
    void countFiltered() {
        users.insert(new User("A", "a@x.com", 20));
        users.insert(new User("B", "b@x.com", 30));
        users.insert(new User("C", "c@x.com", 40));
        assertEquals(2L, users.count(new Query<User>().where("age").gte(30)));
    }

    @Test @DisplayName("exists: true when match found")
    void existsTrue() {
        users.insert(new User("Alice", "alice@x.com", 30));
        assertTrue(users.exists(new Query<User>().where("name").eq("Alice")));
    }

    @Test @DisplayName("exists: false when no match")
    void existsFalse() {
        assertFalse(users.exists(new Query<User>().where("name").eq("Nobody")));
    }

    @Test @DisplayName("update: modifies entity")
    void update() {
        User user = users.insert(new User("Alice", "alice@x.com", 30));
        user.setAge(31);
        users.update(user);
        assertEquals(31, users.findById(user.getId()).orElseThrow().getAge());
    }

    @Test @DisplayName("save: inserts when ID is null")
    void saveInserts() {
        User user = new User("New", "new@x.com", 22);
        users.save(user);
        assertNotNull(user.getId());
        assertEquals(1L, users.count());
    }

    @Test @DisplayName("save: updates when ID is present")
    void saveUpdates() {
        User user = users.insert(new User("Alice", "alice@x.com", 30));
        user.setAge(99);
        users.save(user);
        assertEquals(99, users.findById(user.getId()).orElseThrow().getAge());
        assertEquals(1L, users.count());
    }

    @Test @DisplayName("updateWhere: bulk update")
    @SuppressWarnings("unchecked")
    void updateWhere() {
        users.insert(new User("Alice", "a@x.com", 20, true));
        users.insert(new User("Bob",   "b@x.com", 25, false));
        users.insert(new User("Carol", "c@x.com", 30, false));
        int affected = users.updateWhere(
            new Query<User>().where("active").eq(false),
            Map.entry("age", 0)
        );
        assertEquals(2, affected);
        assertEquals(2L, users.count(new Query<User>().where("age").eq(0)));
    }

    @Test @DisplayName("deleteById: removes entity")
    void deleteById() {
        User user = users.insert(new User("Alice", "alice@x.com", 30));
        assertTrue(users.deleteById(user.getId()));
        assertTrue(users.findById(user.getId()).isEmpty());
    }

    @Test @DisplayName("deleteById: false for missing ID")
    void deleteByIdMissing() {
        assertFalse(users.deleteById(99999L));
    }

    @Test @DisplayName("delete: removes entity by object")
    void deleteByEntity() {
        User user = users.insert(new User("Dave", "dave@x.com", 28));
        users.delete(user);
        assertTrue(users.findById(user.getId()).isEmpty());
    }

    @Test @DisplayName("deleteWhere: removes matching entities")
    void deleteWhere() {
        users.insert(new User("Alice", "a@x.com", 20, false));
        users.insert(new User("Bob",   "b@x.com", 25, true));
        users.insert(new User("Carol", "c@x.com", 30, false));
        assertEquals(2, users.deleteWhere(new Query<User>().where("active").eq(false)));
        assertEquals(1L, users.count());
    }

    @Test @DisplayName("clear: removes all rows")
    void clear() {
        users.insert(new User("A", "a@x.com", 20));
        users.insert(new User("B", "b@x.com", 25));
        users.clear();
        assertEquals(0L, users.count());
    }

    @Test @DisplayName("multiple repositories work independently")
    @SuppressWarnings("unchecked")
    void multipleRepositories() {
        Repository<Product, Long> products = (Repository<Product, Long>) db.repository(Product.class);
        products.clear();
        users.insert(new User("Alice", "a@x.com", 25));
        products.insert(new Product("Widget", 9.99, 100));
        assertEquals(1L, users.count());
        assertEquals(1L, products.count());
    }
}
