package net.veloxia.universaldb.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fluent query builder.
 *
 * <pre>{@code
 * Query<User> q = new Query<User>()
 *     .where("age").gte(18)
 *     .where("name").like("%Alice%")
 *     .orderBy("name", SortDirection.ASC)
 *     .limit(10)
 *     .offset(20);
 * }</pre>
 *
 * @author xRookieFight
 * @since 22/03/2026
 */
public class Query<T> {

    private final List<Condition> conditions = new ArrayList<>();
    private final List<Sort> sorts = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;

    /** Start a filter on the given field. */
    public ConditionBuilder<T> where(String field) {
        return new ConditionBuilder<>(field, this);
    }

    /** Sort by a field ascending. */
    public Query<T> orderBy(String field) {
        return orderBy(field, SortDirection.ASC);
    }

    /** Sort by a field in the given direction. */
    public Query<T> orderBy(String field, SortDirection direction) {
        sorts.add(new Sort(field, direction));
        return this;
    }

    /** Limit the number of results. */
    public Query<T> limit(int n) {
        this.limitValue = n;
        return this;
    }

    /** Skip the first N results. */
    public Query<T> offset(int n) {
        this.offsetValue = n;
        return this;
    }

    void addCondition(Condition c) { conditions.add(c); }

    public List<Condition> getConditions() { return conditions; }
    public List<Sort>      getSorts()      { return sorts; }
    public Integer         getLimit()      { return limitValue; }
    public Integer         getOffset()     { return offsetValue; }

    /**
     * Intermediate builder that connects a field name to its operator and value.
     */
    public static class ConditionBuilder<T> {
        private final String field;
        private final Query<T> query;

        ConditionBuilder(String field, Query<T> query) {
            this.field = field;
            this.query = query;
        }

        public Query<T> eq(Object value)               { return add(Operator.EQ,       value); }
        public Query<T> neq(Object value)              { return add(Operator.NEQ,      value); }
        public Query<T> gt(Object value)               { return add(Operator.GT,       value); }
        public Query<T> gte(Object value)              { return add(Operator.GTE,      value); }
        public Query<T> lt(Object value)               { return add(Operator.LT,       value); }
        public Query<T> lte(Object value)              { return add(Operator.LTE,      value); }
        public Query<T> like(String pattern)           { return add(Operator.LIKE,     pattern); }
        public Query<T> in(Collection<?> values)       { return add(Operator.IN,       values); }
        public Query<T> notIn(Collection<?> values)    { return add(Operator.NOT_IN,   values); }
        public Query<T> isNull()                       { return add(Operator.IS_NULL,  null); }
        public Query<T> isNotNull()                    { return add(Operator.IS_NOT_NULL, null); }

        private Query<T> add(Operator op, Object value) {
            query.addCondition(new Condition(field, op, value));
            return query;
        }
    }
}
