package com.minidb;

/**
 * Оптимизатор запросов: анализирует оператор в секции WHERE и выбирает наиболее эффективный тип индекса.
 */
public class IndexOptimizer {

    public IndexOptimizer() {}

    /**
     * Выбирает оптимальный индекс для фильтрации таблицы по указанной колонке и оператору.
     * Для оператора точного равенства '=' допускается HashIndex,
     * а для диапазонных запросов (&gt;, &lt;, &gt;=, &lt;=) выбирается TreeIndex.
     *
     * @param tableName имя таблицы
     * @param columnName имя колонки в секции WHERE
     * @param operator оператор сравнения
     * @param manager менеджер индексов
     * @return подходящий экземпляр Index или null, если индекс неприменим
     */
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
