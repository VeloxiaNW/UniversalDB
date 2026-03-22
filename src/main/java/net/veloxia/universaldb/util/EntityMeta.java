package net.veloxia.universaldb.util;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Resolved metadata for a single entity class.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class EntityMeta {
    private final String tableName;
    private final Field idField;
    private final String idColumnName;
    private final List<ColumnMeta> columns;

    public EntityMeta(String tableName, Field idField, String idColumnName, List<ColumnMeta> columns) {
        this.tableName    = tableName;
        this.idField      = idField;
        this.idColumnName = idColumnName;
        this.columns      = columns;
    }

    public String          getTableName()    { return tableName; }
    public Field           getIdField()      { return idField; }
    public String          getIdColumnName() { return idColumnName; }
    public List<ColumnMeta> getColumns()     { return columns; }
}
