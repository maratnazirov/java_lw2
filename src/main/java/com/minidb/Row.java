package com.minidb;

import java.util.*;

// Строка таблицы базы данных
public class Row implements Iterable<Map.Entry<String, Object>> {

    private final LinkedHashMap<String, Object> data;

    public Row() {
        this.data = new LinkedHashMap<>();
    }

    public Row(LinkedHashMap<String, Object> data) {
        this.data = new LinkedHashMap<>(data);
    }

    public void set(String columnName, Object value) {
        data.put(columnName.toLowerCase(), value);
    }

    public Object get(String columnName) {
        return data.get(columnName.toLowerCase());
    }

    public Object getAt(int index) {
        int i = 0;
        for (Object v : data.values()) {
            if (i++ == index) return v;
        }
        throw new IndexOutOfBoundsException("Column index " + index + " out of range");
    }

    public List<String> columnNames() {
        return Collections.unmodifiableList(new ArrayList<>(data.keySet()));
    }

    public List<Object> values() {
        return Collections.unmodifiableList(new ArrayList<>(data.values()));
    }

    public int size() {
        return data.size();
    }

    public boolean hasColumn(String columnName) {
        return data.containsKey(columnName.toLowerCase());
    }

    public LinkedHashMap<String, Object> toMap() {
        return new LinkedHashMap<>(data);
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return data.entrySet().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Row)) return false;
        Row entries = (Row) o;
        return Objects.equals(data, entries.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
