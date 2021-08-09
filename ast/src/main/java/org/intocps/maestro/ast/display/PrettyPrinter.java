package org.intocps.maestro.ast.display;

import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AParallelBlockStm;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAdaptor;
import org.intocps.maestro.ast.node.*;

import java.util.Iterator;
import java.util.List;
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

        if (node.getFramework() != null && !node.getFramework().isEmpty()) {
            sb.append(indent(question) + "\n@Framework( " +
                    node.getFramework().stream().map(LexIdentifier::getText).map(s -> "\"" + s + "\"").collect(Collectors.joining(",")) + ");");
        }

        if (node.getFrameworkConfigs() != null && !node.getFrameworkConfigs().isEmpty()) {
            node.getFrameworkConfigs().forEach(
                    x -> sb.append(indent(question) + "\n@FrameworkConfig( \"" + x.getName().getText() + "\", \"" + x.getConfig() + "\")" + ";"));
        }

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

        if (node instanceof SBlockStm) {
            printABlockStm(((SBlockStm) node).getBody(), indentation - 1, true, node instanceof AParallelBlockStm);
        } else {
            node.apply(this, indentation);
        }
    }

    @Override
    public void caseAConfigStm(AConfigStm node, Integer question) throws AnalysisException {
        sb.append(indent(question) + "@Config(\"" + node.getConfig() + "\");");
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
    public void caseALocalVariableStm(ALocalVariableStm node, Integer question) throws AnalysisException {

        node.getDeclaration().apply(this, question);
    }

    @Override
    public void caseAVariableDeclaration(AVariableDeclaration node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        node.getType().apply(this, question);
        sb.append(" ");
        sb.append(node.getName().getText());
        for (PExp s : node.getSize()) {
            sb.append("[");
            s.apply(this, question);
            sb.append("]");
        }
        if (node.getInitializer() != null) {
            sb.append(" = ");
            node.getInitializer().apply(this, question);
        }

        sb.append(";");


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
    public void defaultPType(PType node, Integer question) throws AnalysisException {
        if (node instanceof AArrayType) {
            ((AArrayType) node).getType().apply(this, question);
        } else {
            sb.append(node.toString());
        }
    }

    @Override
    public void defaultPInitializer(PInitializer node, Integer question) throws AnalysisException {
        sb.append(node.toString());
    }

    @Override
    public void caseABasicBlockStm(ABasicBlockStm node, Integer question) throws AnalysisException {
        printABlockStm(node.getBody(), question, false, false);
    }

    @Override
    public void caseAParallelBlockStm(AParallelBlockStm node, Integer question) throws AnalysisException {
        printABlockStm(node.getBody(), question, false, true);
    }

    @Override
    public void caseATryStm(ATryStm node, Integer question) throws AnalysisException {
        sb.append(indent(question) + "try \n");
        sb.append(indent(question) + "{\n");
        applyBodyIntendedScoping(node.getBody(), question + 1);
        sb.append("\n" + indent(question) + "}");
        sb.append(indent(question) + "finally \n");
        sb.append(indent(question) + "{\n");
        applyBodyIntendedScoping(node.getFinally(), question + 1);
        sb.append("\n" + indent(question) + "}");
    }

    public void printABlockStm(List<? extends PStm> body, Integer question, boolean skipBracket, boolean parallel) throws AnalysisException {
        if (body.isEmpty()) {
            return;
        }

        //        if (body.size() == 1) {
        //            body.get(0).apply(this, question);
        //            return;
        //        }
        if (!skipBracket) {
            sb.append(indent(question) + (parallel ? "||" : "") + "{\n ");
        }

        Iterator<? extends PStm> itr = body.iterator();
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
