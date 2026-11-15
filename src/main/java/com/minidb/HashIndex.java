package com.minidb;

import java.util.*;

/**
 * Хэш-индекс на базе HashMap для быстрого точечного поиска строк за константное время O(1).
 */
public class HashIndex implements Index {

    private final String tableName;
    private final String columnName;
    private final Map<Object, List<Integer>> map;

    public HashIndex(String tableName, String columnName) {
        this.tableName  = tableName.toLowerCase();
        this.columnName = columnName.toLowerCase();
        this.map        = new HashMap<>();
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public void insert(Object key, int rowIndex) {
        if (key == null) return;
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(rowIndex);
    }

    @Override
    public void delete(Object key) {
        if (key == null) return;
        map.remove(key);
    }

    @Override
    public void update(Object oldKey, Object newKey, int rowIndex) {
        delete(oldKey);
        insert(newKey, rowIndex);
    }

    /**
     * Выполняет точечный поиск строк по значению ключа со средней сложностью O(1).
     *
     * @param key искомое значение
     * @return список индексов строк с совпадающим значением
     */
    @Override
    public List<Integer> lookup(Object key) {
        if (key == null) return Collections.emptyList();
        List<Integer> result = map.get(key);
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    /**
     * Выполняет поиск по операторам сравнения:
     * для хэш-индекса перебирает элементы карты, так как хэш-таблица не упорядочена.
     *
     * @param value граничное значение
     * @param operator оператор сравнения
     * @return список индексов строк
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Integer> range(Object value, String operator) {
        if (value == null) return Collections.emptyList();
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Object, List<Integer>> entry : map.entrySet()) {
            Object k = entry.getKey();
            if (!(k instanceof Comparable) || !(value instanceof Comparable)) continue;
            int cmp = ((Comparable) k).compareTo(value);
            boolean match;
            switch (operator) {
                case "=":  match = (cmp == 0); break;
                case "!=": match = (cmp != 0); break;
                case "<":  match = (cmp < 0); break;
                case "<=": match = (cmp <= 0); break;
                case ">":  match = (cmp > 0); break;
                case ">=": match = (cmp >= 0); break;
                default:   match = false; break;
            }
            if (match) result.addAll(entry.getValue());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public String toString() {
        return "HashIndex{" + tableName + "." + columnName + ", entries=" + map.size() + "}";
    }
}
