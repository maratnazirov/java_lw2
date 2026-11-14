package com.minidb;

// Базовый класс для всех распарсенных SQL-команд
public abstract class Statement {

    // Возвращает тип SQL-команды
    public abstract StatementType getType();
}
