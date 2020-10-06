package org.intocps.maestro.core.messages;

import org.intocps.maestro.ast.LexToken;

import java.util.List;
import java.util.Vector;

public class MableMessage {

    public final int number;
    public final String message;
    public final LexToken location;

    protected List<String> details = new Vector<>();

    public MableMessage(int number) {
        this(number, "", null);
    }

    public MableMessage(int number, String message, LexToken location) {
        this.number = number;
        this.message = message;
        this.location = location;
    }

    @Override
    public String toString() {
        return String.format("%04d: %s %s", number, message, location);
    }

    public void add(String det) {
        details.add(det);
    }
}
