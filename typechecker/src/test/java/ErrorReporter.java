package org.intocps.maestro;

import org.intocps.maestro.ast.LexToken;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.core.messages.MableError;
import org.intocps.maestro.core.messages.MableMessage;
import org.intocps.maestro.core.messages.MableWarning;
import org.intocps.maestro.typechecker.InternalException;

import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

public class ErrorReporter implements IErrorReporter {
    private final int MAX = 200;
    private final List<MableError> errors = new Vector<>();
    private final List<MableWarning> warnings = new Vector<>();
    private boolean suppress = false;
    private MableMessage lastMessage = null;

    public void suppressErrors(boolean sup) {
        suppress = sup;
    }

    @Override
    public void report(int number, String problem, LexToken location) {
        if (suppress) {
            return;
        }
        MableError error = new MableError(number, problem, location);
        // System.out.println(error.toString());
        errors.add(error);
        lastMessage = error;


        if (errors.size() >= MAX - 1) {
            errors.add(new MableError(10, "Too many errors", location));
            throw new InternalException(10, "Too many errors");
        }
    }

    @Override
    public void warning(int number, String problem, LexToken location) {
        if (suppress) {
            return;
        }
        MableWarning warning = new MableWarning(number, problem, location);
        warnings.add(warning);
        lastMessage = warning;


    }

    @Override
    public void detail(String tag, Object obj) {
        if (suppress) {
            return;
        }
        if (lastMessage != null) {
            lastMessage.add(tag + ": " + obj);
        }
    }

    @Override
    public void detail2(String tag1, Object obj1, String tag2, Object obj2) {
        detail(tag1, obj1);
        detail(tag2, obj2);
    }

    public void clearErrors() {
        errors.clear();
        warnings.clear();
    }

    @Override
    public int getErrorCount() {
        return errors.size();
    }

    @Override
    public int getWarningCount() {
        return warnings.size();
    }

    @Override
    public List<MableError> getErrors() {
        return errors;
    }

    @Override
    public List<MableWarning> getWarnings() {
        return warnings;
    }

    @Override
    public void printErrors(PrintWriter out) {
        for (MableError e : errors) {
            out.println(e.toString());
        }
    }

    @Override
    public void printWarnings(PrintWriter out) {
        for (MableWarning w : warnings) {
            out.println(w.toString());
        }
    }
}
