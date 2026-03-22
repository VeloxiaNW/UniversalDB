package net.veloxia.universaldb.query;

/**
 * A single filter condition.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class Condition {
    private final String field;
    private final Operator operator;
    private final Object value;

    public Condition(String field, Operator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String   getField()    { return field; }
    public Operator getOperator() { return operator; }
    public Object   getValue()    { return value; }
}
