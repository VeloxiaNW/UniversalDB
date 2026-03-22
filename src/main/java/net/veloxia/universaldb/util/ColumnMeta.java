package net.veloxia.universaldb.util;

import java.lang.reflect.Field;

/**
 * Metadata for a single non-ID column.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public record ColumnMeta(Field field, String columnName, boolean nullable, boolean unique) { }
