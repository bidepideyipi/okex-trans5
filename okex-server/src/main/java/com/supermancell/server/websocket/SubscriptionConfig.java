package com.supermancell.server.websocket;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SubscriptionConfig {

    private final List<String> symbols;
    private final List<String> intervals;

    public SubscriptionConfig(List<String> symbols, List<String> intervals) {
        this.symbols = symbols == null ? Collections.emptyList() : symbols;
        this.intervals = intervals == null ? Collections.emptyList() : intervals;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public List<String> getIntervals() {
        return intervals;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionConfig that = (SubscriptionConfig) o;
        return Objects.equals(symbols, that.symbols) && Objects.equals(intervals, that.intervals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbols, intervals);
    }

    @Override
    public String toString() {
        return "SubscriptionConfig{" +
                "symbols=" + symbols +
                ", intervals=" + intervals +
                '}';
    }
}
