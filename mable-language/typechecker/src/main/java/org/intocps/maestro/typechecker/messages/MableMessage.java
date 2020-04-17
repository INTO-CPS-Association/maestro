package org.intocps.maestro.typechecker.messages;

import org.antlr.v4.runtime.Token;

import java.util.List;
import java.util.Vector;

public class MableMessage {

    public final int number;
    public final String message;
    public final Token location;

    protected List<String> details = new Vector<String>();

    public MableMessage(int number) {
        this(number, "", null);
    }

    public MableMessage(int number, String message, Token location) {
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
