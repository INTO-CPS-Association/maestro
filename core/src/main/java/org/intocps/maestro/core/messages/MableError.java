package org.intocps.maestro.core.messages;

import org.antlr.v4.runtime.Token;

public class MableError extends MableMessage {

    public MableError(int number, String message, Token location) {
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
