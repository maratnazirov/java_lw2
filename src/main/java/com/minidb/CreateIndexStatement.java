package com.minidb;

// Команда создания индекса: CREATE INDEX
public class CreateIndexStatement extends Statement {

    private final String indexName;
    private final String tableName;
    private final String columnName;

    public CreateIndexStatement(String indexName, String tableName, String columnName) {
        this.indexName  = indexName;
        this.tableName  = tableName;
        this.columnName = columnName;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    @Override
    public StatementType getType() {
        return StatementType.CREATE_INDEX;
    }

    @Override
    public String toString() {
        return "CreateIndexStatement{indexName='" + indexName
                + "', tableName='" + tableName
                + "', columnName='" + columnName + "'}";
    }
}
