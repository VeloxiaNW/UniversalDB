package net.veloxia.universaldb.util;

import net.veloxia.universaldb.annotations.Column;
import net.veloxia.universaldb.annotations.Id;
import net.veloxia.universaldb.annotations.Table;
import net.veloxia.universaldb.annotations.Transient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection-based entity metadata cache and object mapper.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class EntityReflection {

    private static final Map<Class<?>, EntityMeta> CACHE = new ConcurrentHashMap<>();

    /** Build (or return cached) metadata for the given entity class. */
    public static EntityMeta metaFor(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, EntityReflection::buildMeta);
    }

    private static EntityMeta buildMeta(Class<?> clazz) {
        // Table name
        Table tableAnn = clazz.getAnnotation(Table.class);
        String tableName = (tableAnn != null && !tableAnn.name().isBlank())
                ? tableAnn.name()
                : clazz.getSimpleName().toLowerCase();

        // Collect all fields (including superclass fields)
        List<Field> allFields = getAllFields(clazz);

        // Find @Id field
        Field idField = allFields.stream()
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseGet(() -> allFields.stream()
                        .filter(f -> f.getName().equals("id"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Entity " + clazz.getSimpleName() + " must have an @Id field or a field named 'id'")));

        idField.setAccessible(true);

        Column idColAnn = idField.getAnnotation(Column.class);
        String idColName = (idColAnn != null && !idColAnn.name().isBlank())
                ? idColAnn.name()
                : idField.getName();

        // Collect non-id, non-transient columns
        List<ColumnMeta> columns = new ArrayList<>();
        for (Field f : allFields) {
            if (f.getName().equals(idField.getName())) continue;
            if (f.isAnnotationPresent(Transient.class)) continue;
            f.setAccessible(true);
            Column colAnn = f.getAnnotation(Column.class);
            String colName = (colAnn != null && !colAnn.name().isBlank()) ? colAnn.name() : f.getName();
            boolean nullable = colAnn == null || colAnn.nullable();
            boolean unique   = colAnn != null && colAnn.unique();
            columns.add(new ColumnMeta(f, colName, nullable, unique));
        }

        return new EntityMeta(tableName, idField, idColName, columns);
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /** Get the value of a field from an entity instance. */
    public static Object getValue(Object entity, Field field) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot read field " + field.getName(), e);
        }
    }

    /** Set the value of a field on an entity instance (mutates the object). */
    public static void setValue(Object entity, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot write field " + field.getName(), e);
        }
    }

    /**
     * Create a new instance of the entity class using its no-arg constructor,
     * then populate all fields from the provided value map (keyed by column name).
     */
    public static <T> T fromMap(Class<T> clazz, Map<String, Object> values) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            T instance = ctor.newInstance();
            EntityMeta meta = metaFor(clazz);
            // set id
            Object idVal = values.get(meta.getIdColumnName());
            if (idVal != null) setValue(instance, meta.getIdField(), idVal);
            // set columns
            for (ColumnMeta col : meta.getColumns()) {
                Object v = values.get(col.columnName());
                if (v != null) setValue(instance, col.field(), coerce(v, col.field().getType()));
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getSimpleName(), e);
        }
    }

    /** Best-effort type coercion for JDBC results (Number → int/long/double/boolean). */
    @SuppressWarnings("unchecked")
    public static <T> T coerce(Object value, Class<T> target) {
        if (value == null || target.isInstance(value)) return (T) value;
        if (value instanceof Number num) {
            if (target == Integer.class || target == int.class)   return (T) Integer.valueOf(num.intValue());
            if (target == Long.class    || target == long.class)  return (T) Long.valueOf(num.longValue());
            if (target == Double.class  || target == double.class)return (T) Double.valueOf(num.doubleValue());
            if (target == Float.class   || target == float.class) return (T) Float.valueOf(num.floatValue());
            if (target == Short.class   || target == short.class) return (T) Short.valueOf(num.shortValue());
        }
        if ((target == Boolean.class || target == boolean.class)) {
            if (value instanceof Number n) return (T) Boolean.valueOf(n.intValue() != 0);
            if (value instanceof String s) return (T) Boolean.valueOf(s);
        }
        if (target == String.class) return (T) value.toString();
        return (T) value;
    }
}
