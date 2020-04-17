package org.intocps.maestro.typechecker;

import org.antlr.v4.runtime.Token;
import org.intocps.maestro.typechecker.messages.MableError;
import org.intocps.maestro.typechecker.messages.MableMessage;
import org.intocps.maestro.typechecker.messages.MableWarning;

import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

public class TypeCheckerErrors {
    private boolean suppress = false;
    private List<MableError> errors = new Vector<>();
    private List<MableWarning> warnings = new Vector<>();

    private MableMessage lastMessage = null;
    private final int MAX = 200;

    public void suppressErrors(boolean sup) {
        suppress = sup;
    }

    public void report(int number, String problem, Token location) {
        if (suppress) {
            return;
        }
        MableError error = new MableError(number, problem, location);
        // System.out.println(error.toString());
        errors.add(error);
        lastMessage = error;


        if (errors.size() >= MAX - 1) {
            errors.add(new MableError(10, "Too many type checking errors", location));
            throw new InternalException(10, "Too many type checking errors");
        }
    }

    public void warning(int number, String problem, Token location) {
        if (suppress) {
            return;
        }
        MableWarning warning = new MableWarning(number, problem, location);
        warnings.add(warning);
        lastMessage = warning;


    }

    public void detail(String tag, Object obj) {
        if (suppress) {
            return;
        }
        if (lastMessage != null) {
            lastMessage.add(tag + ": " + obj);
        }
    }

    public void detail2(String tag1, Object obj1, String tag2, Object obj2) {
        detail(tag1, obj1);
        detail(tag2, obj2);
    }

    public void clearErrors() {
        errors.clear();
        warnings.clear();
    }

    public int getErrorCount() {
        return errors.size();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public List<MableError> getErrors() {
        return errors;
    }

    public List<MableWarning> getWarnings() {
        return warnings;
    }

    public void printErrors(PrintWriter out) {
        for (MableError e : errors) {
            out.println(e.toString());
        }
    }

    public void printWarnings(PrintWriter out) {
        for (MableWarning w : warnings) {
            out.println(w.toString());
        }
    }

}
