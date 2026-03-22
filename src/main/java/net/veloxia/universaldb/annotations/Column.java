package net.veloxia.universaldb.annotations;

import java.lang.annotation.*;

/**
 * Customizes the column/field name in the database.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    /** The column name to use. Defaults to the field name. */
    String name() default "";
    /** Whether the column allows NULL values (SQL only). */
    boolean nullable() default true;
    /** Whether the column has a UNIQUE constraint (SQL only). */
    boolean unique() default false;
}
