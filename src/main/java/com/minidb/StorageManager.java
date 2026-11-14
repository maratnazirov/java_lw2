package com.minidb;

import java.io.IOException;
import java.util.List;

// Интерфейс слоя хранения данных
public interface StorageManager {
    void save(Table table) throws IOException;
    Table load(String tableName) throws IOException;
    List<Table> loadAll() throws IOException;
    void delete(String tableName) throws IOException;
}
