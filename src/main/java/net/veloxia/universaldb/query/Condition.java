package net.veloxia.universaldb.query;

import lombok.Getter;

/**
 * A single filter condition.
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
@Getter
public class Condition {
    private final String field;
    private final Operator operator;
    private final Object value;

    public Condition(String field, Operator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

}
