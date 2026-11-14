package com.minidb;

import java.util.*;

// B-Tree индекс на базе TreeMap для диапазонных запросов O(log N)
public class TreeIndex implements Index {

    private final String tableName;
    private final String columnName;

    @SuppressWarnings("rawtypes")
    private final TreeMap<Comparable, List<Integer>> map;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TreeIndex(String tableName, String columnName) {
        this.tableName  = tableName.toLowerCase();
        this.columnName = columnName.toLowerCase();
        this.map        = new TreeMap<>((c1, c2) -> {
            if (c1 == c2) return 0;
            if (c1 == null) return -1;
            if (c2 == null) return 1;
            if (c1 instanceof Number && c2 instanceof Number) {
                return Double.compare(((Number) c1).doubleValue(), ((Number) c2).doubleValue());
            }
            if (c1.getClass().equals(c2.getClass())) {
                return c1.compareTo(c2);
            }
            return c1.toString().compareTo(c2.toString());
        });
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void insert(Object key, int rowIndex) {
        Comparable cKey = toComparable(key);
        if (cKey == null) return;
        map.computeIfAbsent(cKey, k -> new ArrayList<>()).add(rowIndex);
    }

    @Override
    public void delete(Object key) {
        Comparable<?> cKey = toComparable(key);
        if (cKey == null) return;
        map.remove(cKey);
    }

    @Override
    public void update(Object oldKey, Object newKey, int rowIndex) {
        delete(oldKey);
        insert(newKey, rowIndex);
    }

    @Override
    public List<Integer> lookup(Object key) {
        Comparable<?> cKey = toComparable(key);
        if (cKey == null) return Collections.emptyList();
        List<Integer> result = map.get(cKey);
        return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
    }

    @Override
    public List<Integer> range(Object value, String operator) {
        Comparable<?> cVal = toComparable(value);
        if (cVal == null) return Collections.emptyList();

        List<Integer> result = new ArrayList<>();
        switch (operator) {
            case "=":
                return lookup(value);
            case "!=":
                for (Map.Entry<Comparable, List<Integer>> e : map.entrySet()) {
                    if (e.getKey().compareTo(cVal) != 0) result.addAll(e.getValue());
                }
                break;
            case "<":
                for (List<Integer> list : map.headMap(cVal, false).values()) {
                    result.addAll(list);
                }
                break;
            case "<=":
                for (List<Integer> list : map.headMap(cVal, true).values()) {
                    result.addAll(list);
                }
                break;
            case ">":
                for (List<Integer> list : map.tailMap(cVal, false).values()) {
                    result.addAll(list);
                }
                break;
            case ">=":
                for (List<Integer> list : map.tailMap(cVal, true).values()) {
                    result.addAll(list);
                }
                break;
            default:
                break;
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

    @SuppressWarnings("rawtypes")
    private Comparable toComparable(Object o) {
        if (o instanceof Comparable) return (Comparable) o;
        return null;
    }

    @Override
    public String toString() {
        return "TreeIndex{" + tableName + "." + columnName + ", entries=" + map.size() + "}";
    }
}
