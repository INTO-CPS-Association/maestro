package org.intocps.maestro.ast.display;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAdaptor;

import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrettyPrinter extends QuestionAdaptor<Integer> {

    StringBuilder sb = new StringBuilder();

    public static String print(INode node) throws AnalysisException {
        PrettyPrinter printer = new PrettyPrinter();
        node.apply(printer, 0);
        return printer.sb.toString();
    }

    public static String printLineNumbers(INode node) throws AnalysisException {
        PrettyPrinter printer = new PrettyPrinter();
        node.apply(printer, 0);
        int lineNumber = 1;
        StringBuilder sb = new StringBuilder();
        int decimals = 3;
        for (String line : printer.sb.toString().split("\n")) {
            sb.append(String.format("%1$" + decimals + "s", (lineNumber++) + "  ")).append(line).append("\n");

        }
        return sb.toString();
    }

    static String indent(int indentionCount) {
        return IntStream.range(0, indentionCount).mapToObj(i -> "\t").collect(Collectors.joining());
    }


    @Override
    public void caseARootDocument(ARootDocument node, Integer question) throws AnalysisException {


        for (PCompilationUnit unit : node.getContent()) {
            unit.apply(this, question);
        }
    }

    @Override
    public void caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node, Integer question) throws AnalysisException {

        sb.append(indent(question) + "simulation ");
        node.getImports().forEach(x -> sb.append(indent(question) + "\nimport " + x.getText() + ";"));
        sb.append("\n");
        node.getBody().apply(this, question);
        //"simulation "+$ $[imports]$.stream().map( s-> "import " + "" + s.toString()).collect(Collectors.joining(";\n","\n",";\n"))$ + [body]
        //        return indent(question) + "simulation " + node.getBody().apply(this, question);
    }

    @Override
    public void caseAIfStm(AIfStm node, Integer question) throws AnalysisException {
        sb.append(indent(question) + "if( ");
        node.getTest().apply(this, question);
        sb.append(" )\n");
        sb.append(indent(question) + "{\n");
        applyBodyIntendedScoping(node.getThen(), question + 2);
        sb.append("\n" + indent(question) + "}");
        if (node.getElse() != null) {
            sb.append("\n" + indent(question) + "else\n" + indent(question) + "{\n");
            applyBodyIntendedScoping(node.getElse(), question + 2);
            sb.append("\n" + indent(question) + "}");
        }
        //
        //        String tmp = indent(question) + "if( " + node.getTest().apply(this, question) + ")\n";
        //        tmp += indentScope(question, node.getThen().apply(this, question + 1) + ";");
        //        return tmp;
    }

    void applyBodyIntendedScoping(INode node, int indentation) throws AnalysisException {
        if (node == null) {
            return;
        }

        if (node instanceof ABlockStm) {
            printABlockStm((ABlockStm) node, indentation - 1, true);
        } else {
            node.apply(this, indentation);
        }
    }

    @Override
    public void caseAWhileStm(AWhileStm node, Integer question) throws AnalysisException {
        sb.append(indent(question) + "while( ");
        node.getTest().apply(this, question);
        sb.append(" )\n");
        sb.append(indent(question) + "{\n");
        applyBodyIntendedScoping(node.getBody(), question + 1);
        sb.append("\n" + indent(question) + "}");
    }

    @Override
    public void defaultPStm(PStm node, Integer question) throws AnalysisException {
        sb.append(indent(question) + (node.toString().endsWith(";") ? node.toString() : node.toString() + ";"));
    }

    @Override
    public void defaultPExp(PExp node, Integer question) throws AnalysisException {
        sb.append(node.toString());
    }

    @Override
    public void caseABlockStm(ABlockStm node, Integer question) throws AnalysisException {
        printABlockStm(node, question, false);
    }

    public void printABlockStm(ABlockStm node, Integer question, boolean skipBracket) throws AnalysisException {
        if (node.getBody().isEmpty()) {
            return;
        }

        if (node.getBody().size() == 1) {
            node.getBody().get(0).apply(this, question);
            return;
        }
        if (!skipBracket) {
            sb.append(indent(question) + "{\n ");
        }

        Iterator<PStm> itr = node.getBody().iterator();
        while (itr.hasNext()) {
            itr.next().apply(this, question + 1);
            if (itr.hasNext()) {
                sb.append("\n");
            }
        }
        if (!skipBracket) {
            sb.append("\n" + indent(question) + "}");
        }
    }

    @Override
    public void caseAInstanceMappingStm(AInstanceMappingStm node, Integer question) throws AnalysisException {
        sb.append(indent(question) + "@map " + node.getIdentifier().getText() + " -> \"" + node.getName() + "\";");
    }
}
