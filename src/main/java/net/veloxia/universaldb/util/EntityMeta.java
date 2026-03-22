package net.veloxia.universaldb.util;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Resolved metadata for a single entity class.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public record EntityMeta(String tableName, Field idField, String idColumnName, List<ColumnMeta> columns) {
}
