package net.veloxia.universaldb.annotations;

import java.lang.annotation.*;

/**
 * Marks a class as a database entity.
 * The {@code name} attribute sets the table/collection name.
 * Defaults to the lowercase class name if not specified.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String name() default "";
}
