package com.minidb;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Рекурсивный нисходящий парсер SQL-запросов (DDL и DML).
 */
public class SqlParser {

    private List<Token> tokens;
    private int pos;
    private String originalSql;

    /**
     * Выполняет синтаксический разбор входной строки SQL и возвращает типизированный объект Statement.
     * Определяет тип инструкции по первому ключевому слову и делегирует разбор соответствующему методу.
     *
     * @param sql исходная строка SQL-запроса
     * @return разобранный объект Statement (команда AST)
     * @throws ParseException при синтаксической ошибке или неожиданном токене
     */
    public Statement parse(String sql) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        this.originalSql = trimmed;
        this.tokens      = new Tokenizer().tokenize(trimmed);
        this.pos         = 0;

        Token first = peek();
        if (first.getType() == TokenType.EOF) {
            throw new ParseException("Empty SQL statement", originalSql, 0);
        }

        String keyword = advance().getValue().toUpperCase();
        switch (keyword) {
            case "CREATE":   return parseCreate();
            case "DROP":     return parseDrop();
            case "INSERT":   return parseInsert();
            case "SELECT":   return parseSelect();
            case "UPDATE":   return parseUpdate();
            case "DELETE":   return parseDelete();
            case "SHOW":     return parseShow();
            case "TABLES":   return new ShowTablesStatement();
            case "TABLE":    return parseTable();
            case "DESC":
            case "DESCRIBE": return parseDescribe();
            default:
                throw new ParseException(
                        "Unknown statement type: " + keyword, originalSql, tokens.get(0).getPosition());
        }
    }

    private Statement parseCreate() {
        String next = expectKeywordValue();
        switch (next) {
            case "TABLE": return parseCreateTable();
            case "INDEX": return parseCreateIndex();
            default:
                throw error("Expected TABLE or INDEX after CREATE, got: " + next);
        }
    }

    /**
     * Разбирает инструкцию создания таблицы CREATE TABLE:
     * парсит имя таблицы, список колонок в круглых скобках и их ограничения.
     *
     * @return объект CreateTableStatement с метаданными схемы таблицы
     */
    private CreateTableStatement parseCreateTable() {
        String tableName = expectIdentifierOrKeywordAsName();
        consume(TokenType.LPAREN, "(");

        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(parseColumnDef());
        while (peek().getType() == TokenType.COMMA) {
            advance();
            columns.add(parseColumnDef());
        }

        consume(TokenType.RPAREN, ")");
        expectEof();
        return new CreateTableStatement(tableName, columns);
    }

    /**
     * Разбирает спецификацию отдельной колонки таблицы: имя, тип данных и флаги PRIMARY KEY / NOT NULL.
     *
     * @return объект ColumnDefinition с конфигурацией колонки
     */
    private ColumnDefinition parseColumnDef() {
        String colName = expectIdentifierOrKeywordAsName();
        String typeToken = expectIdentifierOrKeywordAsName();

        DataType dataType;
        try {
            dataType = DataType.fromSqlToken(typeToken);
        } catch (IllegalArgumentException e) {
            throw error("Unknown column data type: " + typeToken);
        }

        boolean primaryKey = false;
        boolean notNull    = false;

        while (true) {
            Token t = peek();
            if (t.getType() == TokenType.KEYWORD && t.getValue().equals("PRIMARY")) {
                advance();
                consumeKeyword("KEY");
                primaryKey = true;
            } else if (t.getType() == TokenType.KEYWORD && t.getValue().equals("NOT")) {
                advance();
                consumeKeyword("NULL");
                notNull = true;
            } else {
                break;
            }
        }

        return new ColumnDefinition(colName, dataType, primaryKey, notNull);
    }

    private DropTableStatement parseDrop() {
        consumeKeyword("TABLE");
        String tableName = expectIdentifierOrKeywordAsName();
        expectEof();
        return new DropTableStatement(tableName);
    }

    /**
     * Разбирает инструкцию вставки INSERT INTO:
     * поддерживает как явное указание целевых колонок, так и позиционную вставку значений по умолчанию.
     *
     * @return объект InsertStatement с именем таблицы, списком колонок и значениями
     */
    private InsertStatement parseInsert() {
        consumeKeyword("INTO");
        String tableName = expectIdentifierOrKeywordAsName();

        List<String> columnNames = null;

        if (peek().getType() == TokenType.LPAREN) {
            int savedPos = pos;
            advance();
            Token after = peek();
            if (after.getType() == TokenType.IDENTIFIER
                    || (after.getType() == TokenType.KEYWORD && !isLiteralKeyword(after.getValue()))) {
                columnNames = new ArrayList<>();
                columnNames.add(expectIdentifierOrKeywordAsName());
                while (peek().getType() == TokenType.COMMA) {
                    advance();
                    columnNames.add(expectIdentifierOrKeywordAsName());
                }
                consume(TokenType.RPAREN, ")");
            } else {
                pos = savedPos;
            }
        }

        consumeKeyword("VALUES");
        consume(TokenType.LPAREN, "(");
        List<Object> values = new ArrayList<>();
        values.add(parseValue());
        while (peek().getType() == TokenType.COMMA) {
            advance();
            values.add(parseValue());
        }
        consume(TokenType.RPAREN, ")");
        expectEof();
        return new InsertStatement(tableName, columnNames, values);
    }

    /**
     * Разбирает запрос выборки SELECT:
     * обрабатывает проекции колонок (или символ *), имя таблицы и необязательное условие WHERE.
     *
     * @return объект SelectStatement
     */
    private SelectStatement parseSelect() {
        List<String> columns = new ArrayList<>();

        if (peek().getType() == TokenType.STAR) {
            advance();
        } else {
            columns.add(expectIdentifierOrKeywordAsName());
            while (peek().getType() == TokenType.COMMA) {
                advance();
                columns.add(expectIdentifierOrKeywordAsName());
            }
        }

        consumeKeyword("FROM");
        String tableName = expectIdentifierOrKeywordAsName();

        WhereCondition where = null;
        if (peek().getType() == TokenType.KEYWORD && peek().getValue().equals("WHERE")) {
            advance();
            where = parseWhereCondition();
        }

        expectEof();
        return new SelectStatement(tableName, columns, where);
    }

    /**
     * Разбирает инструкцию обновления UPDATE:
     * парсит список пар колонка=значение и необязательное условие фильтрации WHERE.
     *
     * @return объект UpdateStatement
     */
    private UpdateStatement parseUpdate() {
        String tableName = expectIdentifierOrKeywordAsName();
        consumeKeyword("SET");

        Map<String, Object> assignments = new LinkedHashMap<>();
        parseAssignment(assignments);
        while (peek().getType() == TokenType.COMMA) {
            advance();
            parseAssignment(assignments);
        }

        WhereCondition where = null;
        if (peek().getType() == TokenType.KEYWORD && peek().getValue().equals("WHERE")) {
            advance();
            where = parseWhereCondition();
        }

        expectEof();
        return new UpdateStatement(tableName, assignments, where);
    }

    private void parseAssignment(Map<String, Object> map) {
        String colName = expectIdentifierOrKeywordAsName();
        consume(TokenType.EQUALS, "=");
        Object value = parseValue();
        map.put(colName.toLowerCase(), value);
    }

    /**
     * Разбирает инструкцию удаления DELETE FROM с опциональным предикатом WHERE.
     *
     * @return объект DeleteStatement
     */
    private DeleteStatement parseDelete() {
        consumeKeyword("FROM");
        String tableName = expectIdentifierOrKeywordAsName();

        WhereCondition where = null;
        if (peek().getType() == TokenType.KEYWORD && peek().getValue().equals("WHERE")) {
            advance();
            where = parseWhereCondition();
        }

        expectEof();
        return new DeleteStatement(tableName, where);
    }

    /**
     * Разбирает инструкцию создания индекса CREATE INDEX:
     * парсит имя индекса, целевую таблицу и индексируемую колонку.
     *
     * @return объект CreateIndexStatement
     */
    private CreateIndexStatement parseCreateIndex() {
        String indexName = expectIdentifierOrKeywordAsName();
        consumeKeyword("ON");
        String tableName = expectIdentifierOrKeywordAsName();
        consume(TokenType.LPAREN, "(");
        String columnName = expectIdentifierOrKeywordAsName();
        consume(TokenType.RPAREN, ")");
        expectEof();
        return new CreateIndexStatement(indexName, tableName, columnName);
    }

    private Statement parseShow() {
        Token next = peek();
        if (next.getType() == TokenType.KEYWORD || next.getType() == TokenType.IDENTIFIER) {
            advance();
        }
        expectEof();
        return new ShowTablesStatement();
    }

    private Statement parseTable() {
        Token next = peek();
        if (next.getType() == TokenType.EOF || next.getType() == TokenType.SEMICOLON) {
            return new ShowTablesStatement();
        }
        String tableName = expectIdentifierOrKeywordAsName();
        expectEof();
        return new SelectStatement(tableName, java.util.Collections.emptyList(), null);
    }

    private Statement parseDescribe() {
        Token next = peek();
        if (next.getType() == TokenType.KEYWORD && next.getValue().equalsIgnoreCase("TABLE")) {
            advance();
        }
        String tableName = expectIdentifierOrKeywordAsName();
        expectEof();
        return new DescribeTableStatement(tableName);
    }

    /**
     * Разбирает условие секции WHERE в формате: [колонка] [оператор_сравнения] [значение_литерала].
     *
     * @return сконфигурированный объект WhereCondition
     */
    private WhereCondition parseWhereCondition() {
        String colName = expectIdentifierOrKeywordAsName();
        String op      = parseOperator();
        Object value   = parseValue();
        return new WhereCondition(colName.toLowerCase(), op, value);
    }

    private String parseOperator() {
        Token t = advance();
        switch (t.getType()) {
            case EQUALS:     return "=";
            case NOT_EQUALS: return "!=";
            case LESS:       return "<";
            case LESS_EQ:    return "<=";
            case GREATER:    return ">";
            case GREATER_EQ: return ">=";
            default:
                throw error("Expected comparison operator, got: " + t.getValue());
        }
    }

    /**
     * Преобразует токен литерала в соответствующий типизированный Java-объект
     * (Integer, Double, String, LocalDate, Boolean или null).
     *
     * @return типизированное значение литерала
     */
    private Object parseValue() {
        Token t = advance();
        switch (t.getType()) {
            case INTEGER_LITERAL:
                try {
                    return Integer.parseInt(t.getValue());
                } catch (NumberFormatException e) {
                    throw error("Invalid integer literal: " + t.getValue());
                }
            case FLOAT_LITERAL:
                try {
                    return Double.parseDouble(t.getValue());
                } catch (NumberFormatException e) {
                    throw error("Invalid float literal: " + t.getValue());
                }
            case STRING_LITERAL:
                return t.getValue();
            case DATE_LITERAL:
                try {
                    return LocalDate.parse(t.getValue());
                } catch (Exception e) {
                    throw error("Invalid date literal: " + t.getValue());
                }
            case BOOLEAN_LITERAL:
                return Boolean.parseBoolean(t.getValue());
            case KEYWORD:
                if (t.getValue().equals("TRUE"))  return Boolean.TRUE;
                if (t.getValue().equals("FALSE")) return Boolean.FALSE;
                if (t.getValue().equals("NULL"))  return null;
                throw error("Expected a value literal, got keyword: " + t.getValue());
            default:
                throw error("Expected a value literal, got: " + t.getValue()
                        + " (" + t.getType() + ")");
        }
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        Token t = tokens.get(pos);
        if (t.getType() != TokenType.EOF) {
            pos++;
        }
        return t;
    }

    private String expectKeywordValue() {
        Token t = advance();
        if (t.getType() != TokenType.KEYWORD) {
            throw error("Expected keyword, got: " + t.getValue() + " (" + t.getType() + ")");
        }
        return t.getValue();
    }

    private void consumeKeyword(String keyword) {
        Token t = advance();
        if (t.getType() != TokenType.KEYWORD || !t.getValue().equalsIgnoreCase(keyword)) {
            throw error("Expected keyword '" + keyword + "', got: " + t.getValue());
        }
    }

    private void consume(TokenType type, String description) {
        Token t = advance();
        if (t.getType() != type) {
            throw error("Expected '" + description + "', got: " + t.getValue());
        }
    }

    private String expectIdentifierOrKeywordAsName() {
        Token t = advance();
        if (t.getType() == TokenType.IDENTIFIER || t.getType() == TokenType.KEYWORD) {
            return t.getValue().toLowerCase();
        }
        throw error("Expected identifier/name, got: " + t.getValue() + " (" + t.getType() + ")");
    }

    private void expectEof() {
        Token t = peek();
        if (t.getType() != TokenType.EOF) {
            throw error("Unexpected token at end of statement: " + t.getValue());
        }
    }

    private boolean isLiteralKeyword(String value) {
        return value.equals("TRUE") || value.equals("FALSE") || value.equals("NULL");
    }

    private ParseException error(String message) {
        int position = (pos < tokens.size())
                ? tokens.get(pos).getPosition()
                : originalSql.length();
        return new ParseException(message, originalSql, position);
    }
}
