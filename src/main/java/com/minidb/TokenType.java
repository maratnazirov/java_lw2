package com.minidb;

// Категории лексических токенов SQL
public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    STRING_LITERAL,
    INTEGER_LITERAL,
    FLOAT_LITERAL,
    BOOLEAN_LITERAL,
    DATE_LITERAL,
    COMMA,
    LPAREN,
    RPAREN,
    SEMICOLON,
    STAR,
    EQUALS,
    NOT_EQUALS,
    LESS,
    LESS_EQ,
    GREATER,
    GREATER_EQ,
    EOF
}
