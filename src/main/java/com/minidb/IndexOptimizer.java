package com.minidb;

// Оптимизатор запросов: выбор подходящего индекса (HashIndex или TreeIndex) для условия WHERE
public class IndexOptimizer {

    public IndexOptimizer() {}

    // Выбор оптимального индекса для заданного оператора фильтрации
    public Index selectIndex(String tableName, String columnName,
                             String operator, IndexManager manager) {
        Index index = manager.getIndex(tableName, columnName);
        if (index == null) {
            return null;
        }

        // Хэш-индекс эффективен только для точного совпадения (=)
        if (index instanceof HashIndex) {
            return "=".equals(operator) ? index : null;
        }

        // Древовидный индекс поддерживает как равенство, так и диапазонные операторы
        if (index instanceof TreeIndex) {
            switch (operator) {
                case "=":
                case "!=":
                case ">":
                case "<":
                case ">=":
                case "<=":
                    return index;
                default:
                    return null;
            }
        }

        return null;
    }
}
