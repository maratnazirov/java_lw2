package com.minidb;

import java.util.function.Predicate;

// Условие фильтрации WHERE вида <колонка> <оператор> <значение>
public class WhereCondition {

    private final String columnName;
    private final String operator;
    private final Object value;

    public WhereCondition(String columnName, String operator, Object value) {
        this.columnName = columnName;
        this.operator   = operator;
        this.value      = value;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Преобразует условие WHERE в предикат фильтрации строк {@link Row}.
     * <p>
     * Логика сравнения:
     * 1. Если значение в ячейке равно null, предикат возвращает false.
     * 2. Для булевых значений поддерживаются операции '=' и '!='.
     * 3. Для разнородных числовых типов (например, Integer и Double) выполняется приведение к Double.
     * 4. Для остальных Comparable-типов (строки, даты) выполняется стандартный compareTo.
     * 5. Поддерживаются операторы: '=', '!=', '<', '<=', '>', '>='.
     *
     * @return функциональный предикат для проверки строки на соответствие условию
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Predicate<Row> toPredicate() {
        return row -> {
            Object cellValue = row.get(columnName.toLowerCase());
            if (cellValue == null) {
                return false;
            }

            // Сравнение логических значений (BOOLEAN)
            if (value instanceof Boolean) {
                if (!(cellValue instanceof Boolean)) return false;
                boolean bCell  = (Boolean) cellValue;
                boolean bValue = (Boolean) value;
                switch (operator) {
                    case "=":  return bCell == bValue;
                    case "!=": return bCell != bValue;
                    default:   return false;
                }
            }

            if (!(cellValue instanceof Comparable) || !(value instanceof Comparable)) {
                return false;
            }

            Comparable cellComp  = (Comparable) cellValue;
            Comparable valueComp = (Comparable) value;

            // Если типы чисел отличаются (например, Integer и Double), приводим к Double
            int cmp;
            try {
                if (!cellComp.getClass().equals(valueComp.getClass())) {
                    if (cellComp instanceof Number && valueComp instanceof Number) {
                        double d1 = ((Number) cellComp).doubleValue();
                        double d2 = ((Number) valueComp).doubleValue();
                        cmp = Double.compare(d1, d2);
                    } else {
                        cmp = cellComp.toString().compareTo(valueComp.toString());
                    }
                } else {
                    cmp = cellComp.compareTo(valueComp);
                }
            } catch (ClassCastException e) {
                return false;
            }

            switch (operator) {
                case "=":  return cmp == 0;
                case "!=": return cmp != 0;
                case "<":  return cmp <  0;
                case "<=": return cmp <= 0;
                case ">":  return cmp >  0;
                case ">=": return cmp >= 0;
                default:
                    throw new ParseException(
                            "Unknown operator '" + operator + "'", "", 0);
            }
        };
    }

    @Override
    public String toString() {
        return columnName + " " + operator + " " + value;
    }
}
