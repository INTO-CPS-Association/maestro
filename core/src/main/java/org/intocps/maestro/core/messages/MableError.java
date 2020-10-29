package org.intocps.maestro.core.messages;

import org.intocps.maestro.ast.LexToken;

public class MableError extends MableMessage {

    public MableError(int number, String message, LexToken location) {
        super(number, message, location);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Error ");
        sb.append(super.toString());

        for (String d : details) {
            sb.append("\n");
            sb.append(d);
        }

        return sb.toString();
    }
}
