package org.intocps.maestro.core.messages;


import org.intocps.maestro.ast.LexToken;

import java.io.PrintWriter;
import java.util.List;

public interface IErrorReporter {

    void report(int number, String problem, LexToken location);

    void warning(int number, String problem, LexToken location);

    void detail(String tag, Object obj);

    void detail2(String tag1, Object obj1, String tag2, Object obj2);

    int getErrorCount();

    int getWarningCount();

    List<MableError> getErrors();

    List<MableWarning> getWarnings();

    void printErrors(PrintWriter out);

    void printWarnings(PrintWriter out);

}
