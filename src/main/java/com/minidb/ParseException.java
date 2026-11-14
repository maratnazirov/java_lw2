package com.minidb;

// Исключение, выбрасываемое при синтаксических ошибках разбора SQL
public class ParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String query;
    private final int position;

    public ParseException(String message, String query, int position) {
        super(message + " (position " + position + " in: " + query + ")");
        this.query    = query;
        this.position = position;
    }

    public String getQuery() {
        return query;
    }

    public int getPosition() {
        return position;
    }
}
