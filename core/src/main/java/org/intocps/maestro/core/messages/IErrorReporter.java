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

    class SilentReporter implements IErrorReporter {
        int errorCount = 0;
        int warningCount = 0;

        @Override
        public void report(int number, String problem, LexToken location) {
            errorCount++;
        }

        @Override
        public void warning(int number, String problem, LexToken location) {
            warningCount++;
        }

        @Override
        public void detail(String tag, Object obj) {

        }

        @Override
        public void detail2(String tag1, Object obj1, String tag2, Object obj2) {

        }

        @Override
        public int getErrorCount() {
            return errorCount;
        }

        @Override
        public int getWarningCount() {
            return warningCount;
        }

        @Override
        public List<MableError> getErrors() {
            return null;
        }

        @Override
        public List<MableWarning> getWarnings() {
            return null;
        }

        @Override
        public void printErrors(PrintWriter out) {

        }

        @Override
        public void printWarnings(PrintWriter out) {

        }
    }

}
