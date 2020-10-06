package org.intocps.maestro.core.messages;

import org.intocps.maestro.ast.LexToken;

public class MableWarning extends MableMessage {
    public MableWarning(int number, String message, LexToken location) {
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