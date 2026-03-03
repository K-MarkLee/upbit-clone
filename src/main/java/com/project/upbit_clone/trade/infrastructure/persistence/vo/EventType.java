package com.project.upbit_clone.trade.infrastructure.persistence.vo;

public enum EventType {
    TRADE_EXECUTED,
    ORDER_PARTIALLY_FILLED,
    ORDER_FILLED,
    ORDER_CANCELED,
    FUNDS_UNLOCKED,
    ORDER_BOOK_DELTA
}
