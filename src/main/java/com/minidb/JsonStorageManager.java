package com.minidb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

// Хранилище таблиц в JSON с атомарной записью
public class JsonStorageManager implements StorageManager {

    private static final Logger LOG = Logger.getLogger(JsonStorageManager.class.getName());

    private static final String SCHEMA_FILE = "schema.json";
    private static final String DATA_FILE   = "data.json";

    private final Path dataDir;
    private final ObjectMapper mapper;

    public JsonStorageManager(String dataDir) throws IOException {
        this.dataDir = Paths.get(dataDir);
        Files.createDirectories(this.dataDir);

        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void save(Table table) throws IOException {
        Path tableDir = tableDir(table.getName());
        Files.createDirectories(tableDir);

        List<Map<String, Object>> schemaList = serializeSchema(table);
        atomicWrite(tableDir.resolve(SCHEMA_FILE), schemaList);

        List<Map<String, Object>> rowList = serializeRows(table);
        atomicWrite(tableDir.resolve(DATA_FILE), rowList);

        LOG.info("Saved table '" + table.getName() + "' (" + table.rowCount() + " rows).");
    }

    @Override
    public Table load(String tableName) throws IOException {
        Path tableDir = tableDir(tableName);
        Path schemaPath = tableDir.resolve(SCHEMA_FILE);
        Path dataPath   = tableDir.resolve(DATA_FILE);

        if (!Files.exists(schemaPath)) {
            throw new IOException("Schema file not found for table: " + tableName);
        }

        List<Map<String, Object>> schemaList = mapper.readValue(
                schemaPath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        List<ColumnDefinition> columns = deserializeSchema(schemaList);
        Table table = new Table(tableName, columns);

        if (Files.exists(dataPath)) {
            List<Map<String, Object>> rowList = mapper.readValue(
                dataPath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> rowMap : rowList) {
                Row row = deserializeRow(rowMap, columns);
                table.addRowUnchecked(row);
            }
        }

        LOG.info("Loaded table '" + tableName + "' (" + table.rowCount() + " rows).");
        return table;
    }

    @Override
    public List<Table> loadAll() throws IOException {
        List<Table> result = new ArrayList<>();
        if (!Files.exists(dataDir)) return result;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String tableName = entry.getFileName().toString();
                    if (tableName.startsWith("idx_") || tableName.equals("indexes")) continue;
                    Path schemaFile = entry.resolve(SCHEMA_FILE);
                    if (Files.exists(schemaFile)) {
                        try {
                            result.add(load(tableName));
                        } catch (IOException e) {
                            LOG.warning("Could not load table '" + tableName + "': " + e.getMessage());
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void delete(String tableName) throws IOException {
        Path tableDir = tableDir(tableName);
        if (Files.exists(tableDir)) {
            deleteDirectory(tableDir);
            LOG.info("Deleted table directory: " + tableDir);
        }
    }

    private List<Map<String, Object>> serializeSchema(Table table) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ColumnDefinition col : table.getColumns()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name",       col.getName());
            m.put("type",       col.getType().name());
            m.put("primaryKey", col.isPrimaryKey());
            m.put("notNull",    col.isNotNull());
            list.add(m);
        }
        return list;
    }

    private List<ColumnDefinition> deserializeSchema(List<Map<String, Object>> schemaList) {
        List<ColumnDefinition> cols = new ArrayList<>();
        for (Map<String, Object> m : schemaList) {
            String name   = (String) m.get("name");
            DataType type = DataType.valueOf((String) m.get("type"));
            boolean pk    = Boolean.TRUE.equals(m.get("primaryKey"));
            boolean nn    = Boolean.TRUE.equals(m.get("notNull"));
            cols.add(new ColumnDefinition(name, type, pk, nn));
        }
        return cols;
    }

    private List<Map<String, Object>> serializeRows(Table table) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Row row : table.getRows()) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (ColumnDefinition col : table.getColumns()) {
                Object val = row.get(col.getName());
                if (val instanceof LocalDate) {
                    val = val.toString();
                }
                m.put(col.getName(), val);
            }
            list.add(m);
        }
        return list;
    }

    private Row deserializeRow(Map<String, Object> rowMap, List<ColumnDefinition> columns) {
        Row row = new Row();
        for (ColumnDefinition col : columns) {
            Object raw = rowMap.get(col.getName());
            row.set(col.getName(), coerce(raw, col.getType()));
        }
        return row;
    }

    private Object coerce(Object raw, DataType type) {
        if (raw == null) return null;
        switch (type) {
            case INTEGER: return ((Number) raw).intValue();
            case FLOAT:   return ((Number) raw).doubleValue();
            case STRING:  return raw.toString();
            case BOOLEAN: return (Boolean) raw;
            case DATE:    return LocalDate.parse(raw.toString());
            default:      return raw;
        }
    }

    // Атомарная запись через .tmp файл для предотвращения повреждения при сбоях
    private void atomicWrite(Path target, Object content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            mapper.writeValue(tmp.toFile(), content);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
    }

    private Path tableDir(String tableName) {
        return dataDir.resolve(tableName.toLowerCase());
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public Path getDataDir() {
        return dataDir;
    }
}
