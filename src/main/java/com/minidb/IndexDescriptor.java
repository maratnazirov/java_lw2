package com.minidb;

// Дескриптор метаданных индекса для сохранения на диск (JSON)
public class IndexDescriptor {

    private String indexName;
    private String tableName;
    private String columnName;
    private String indexType;

    public IndexDescriptor() {}

    public IndexDescriptor(String indexName, String tableName,
                           String columnName, String indexType) {
        this.indexName  = indexName;
        this.tableName  = tableName  != null ? tableName.toLowerCase()  : null;
        this.columnName = columnName != null ? columnName.toLowerCase() : null;
        this.indexType  = indexType  != null ? indexType.toUpperCase()  : null;
    }

    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) {
        this.tableName = tableName != null ? tableName.toLowerCase() : null;
    }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) {
        this.columnName = columnName != null ? columnName.toLowerCase() : null;
    }

    public String getIndexType() { return indexType; }
    public void setIndexType(String indexType) {
        this.indexType = indexType != null ? indexType.toUpperCase() : null;
    }

    @Override
    public String toString() {
        return "IndexDescriptor{indexName='" + indexName
                + "', table='" + tableName
                + "', column='" + columnName
                + "', type='" + indexType + "'}";
    }
}
