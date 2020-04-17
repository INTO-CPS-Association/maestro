package org.intocps.maestro.typechecker.messages;

import org.antlr.v4.runtime.Token;

public class MableWarning extends MableMessage {
    public MableWarning(int number, String message, Token location) {
        super(number, message, location);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Warning ");
        sb.append(super.toString());

        for (String d : details) {
            sb.append("\n");
            sb.append(d);
        }

        return sb.toString();
    }
}