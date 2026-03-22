package net.veloxia.universaldb.annotations;

import java.lang.annotation.*;

/**
 * Marks a field to be ignored during persistence.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {
}
