package org.intocps.maestro.ast;

import java.util.Objects;

public class LexIdentifier implements ExternalNode {
    private final String text;
    private final LexToken symbol;

    public LexIdentifier(String text, LexToken symbol) {
        this.text = text;
        this.symbol = symbol;
    }

    public LexToken getSymbol() {
        return symbol;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public Object clone() {
        return new LexIdentifier(text, symbol);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LexIdentifier that = (LexIdentifier) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }
}
