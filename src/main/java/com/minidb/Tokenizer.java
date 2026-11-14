package com.minidb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// Лексический анализатор SQL-запросов (разбивка на токены)
public class Tokenizer {

    // Шаблон даты формата YYYY-MM-DD
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    // Набор ключевых слов SQL
    private static final Set<String> KEYWORDS = new HashSet<>();

    static {
        String[] kws = {
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES",
            "UPDATE", "SET", "DELETE", "CREATE", "DROP", "TABLE", "TABLES",
            "SHOW", "DESCRIBE", "DESC",
            "INDEX", "ON", "PRIMARY", "KEY", "NOT", "NULL",
            "AND", "OR",
            "INT", "INTEGER", "FLOAT", "DOUBLE", "REAL",
            "VARCHAR", "TEXT", "STRING", "DATE", "BOOLEAN", "BOOL"
        };
        for (String kw : kws) {
            KEYWORDS.add(kw);
        }
    }

    // Токенизация входной строки SQL
    public List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = sql.length();

        while (i < len) {
            char c = sql.charAt(i);

            // Пропуск пробелов и BOM
            if (Character.isWhitespace(c) || c == '\uFEFF' || c == '\u200B') {
                i++;
                continue;
            }

            // Строковые литералы и даты в одинарных кавычках
            if (c == '\'') {
                int start = i;
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < len && sql.charAt(i) != '\'') {
                    sb.append(sql.charAt(i));
                    i++;
                }
                if (i >= len) {
                    throw new ParseException("Unterminated string literal", sql, start);
                }
                i++;
                String content = sb.toString();
                TokenType tt = DATE_PATTERN.matcher(content).matches()
                        ? TokenType.DATE_LITERAL
                        : TokenType.STRING_LITERAL;
                tokens.add(new Token(tt, content, start));
                continue;
            }

            // Числовые литералы (INTEGER / FLOAT)
            if (Character.isDigit(c)) {
                int start = i;
                StringBuilder sb = new StringBuilder();
                while (i < len && Character.isDigit(sql.charAt(i))) {
                    sb.append(sql.charAt(i));
                    i++;
                }
                boolean isFloat = false;
                if (i < len && sql.charAt(i) == '.') {
                    isFloat = true;
                    sb.append('.');
                    i++;
                    while (i < len && Character.isDigit(sql.charAt(i))) {
                        sb.append(sql.charAt(i));
                        i++;
                    }
                }
                TokenType tt = isFloat ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL;
                tokens.add(new Token(tt, sb.toString(), start));
                continue;
            }

            // Слова (ключевые слова, логические константы, идентификаторы)
            if (Character.isLetter(c) || c == '_') {
                int start = i;
                StringBuilder sb = new StringBuilder();
                while (i < len && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    sb.append(sql.charAt(i));
                    i++;
                }
                String word = sb.toString().toUpperCase();
                TokenType tt;
                if (word.equals("TRUE") || word.equals("FALSE")) {
                    tt = TokenType.BOOLEAN_LITERAL;
                } else if (KEYWORDS.contains(word)) {
                    tt = TokenType.KEYWORD;
                } else {
                    tt = TokenType.IDENTIFIER;
                }
                tokens.add(new Token(tt, word, start));
                continue;
            }

            // Двухсимвольные операторы (!=, <=, >=)
            if (c == '!' && i + 1 < len && sql.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.NOT_EQUALS, "!=", i));
                i += 2;
                continue;
            }
            if (c == '<' && i + 1 < len && sql.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.LESS_EQ, "<=", i));
                i += 2;
                continue;
            }
            if (c == '>' && i + 1 < len && sql.charAt(i + 1) == '=') {
                tokens.add(new Token(TokenType.GREATER_EQ, ">=", i));
                i += 2;
                continue;
            }

            // Односимвольные токены
            switch (c) {
                case '=':
                    tokens.add(new Token(TokenType.EQUALS,    "=", i)); i++; break;
                case '<':
                    tokens.add(new Token(TokenType.LESS,      "<", i)); i++; break;
                case '>':
                    tokens.add(new Token(TokenType.GREATER,   ">", i)); i++; break;
                case ',':
                    tokens.add(new Token(TokenType.COMMA,     ",", i)); i++; break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN,    "(", i)); i++; break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN,    ")", i)); i++; break;
                case ';':
                    tokens.add(new Token(TokenType.SEMICOLON, ";", i)); i++; break;
                case '*':
                    tokens.add(new Token(TokenType.STAR,      "*", i)); i++; break;
                default:
                    throw new ParseException(
                            "Unrecognised character '" + c + "'", sql, i);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", len));
        return tokens;
    }
}
