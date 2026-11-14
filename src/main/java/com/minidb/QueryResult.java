package com.minidb;

import java.util.List;

// Результат выполнения SQL-запроса
public class QueryResult {

    private final List<String> columns;
    private final List<Row> rows;
    private final int affectedRows;
    private final String message;
    private final boolean success;

    public QueryResult(List<String> columns, List<Row> rows) {
        this(columns, rows, rows.size() + " row(s) returned.");
    }

    public QueryResult(List<String> columns, List<Row> rows, String message) {
        this.columns      = columns;
        this.rows         = rows;
        this.affectedRows = rows.size();
        this.message      = message;
        this.success      = true;
    }

    public QueryResult(int affectedRows, String message) {
        this.columns      = null;
        this.rows         = null;
        this.affectedRows = affectedRows;
        this.message      = message;
        this.success      = true;
    }

    public static QueryResult error(String errorMessage) {
        return new QueryResult(false, errorMessage);
    }

    private QueryResult(boolean success, String message) {
        this.columns      = null;
        this.rows         = null;
        this.affectedRows = 0;
        this.message      = message;
        this.success      = success;
    }

    public List<String> getColumns() { return columns; }

    public List<Row> getRows() { return rows; }

    public int getAffectedRows() { return affectedRows; }

    public String getMessage() { return message; }

    public boolean isSuccess() { return success; }

    public boolean hasData() { return rows != null; }
}
