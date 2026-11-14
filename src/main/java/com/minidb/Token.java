package com.minidb;

// Лексический токен (тип токена, строковое значение и позиция в тексте)
public final class Token {

    private final TokenType type;
    private final String value;
    private final int position;

    public Token(TokenType type, String value, int position) {
        this.type     = type;
        this.value    = value;
        this.position = position;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Token{type=" + type + ", value='" + value + "', pos=" + position + '}';
    }
}
