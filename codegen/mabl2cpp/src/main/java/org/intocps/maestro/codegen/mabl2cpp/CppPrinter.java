package org.intocps.maestro.codegen.mabl2cpp;

import org.apache.commons.codec.digest.DigestUtils;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptorQuestion;
import org.intocps.maestro.ast.display.PrettyPrinter;
import org.intocps.maestro.ast.node.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class CppPrinter extends DepthFirstAnalysisAdaptorQuestion<Integer> {

    private final Map<INode, PType> types;
    StringBuilder sb = new StringBuilder();

    public CppPrinter(Map<INode, PType> types) {

        this.types = types;
    }

    static String indent(int indentionCount) {
        return IntStream.range(0, indentionCount).mapToObj(i -> "\t").collect(Collectors.joining());
    }


    public static Map<String, String> print(INode node, Map<INode, PType> types) throws AnalysisException {
        CppPrinter printer = new CppPrinter(types);
        printer.sb.append("#include \"co-sim.hxx\"\n");
        node.apply(printer, 0);

        Map<String, String> sources = new HashMap<>();
        sources.put("co-sim.cxx", printer.sb.toString());

        String specSha1 = DigestUtils.sha1Hex(PrettyPrinter.print(node));
        sources.put("co-sim.hxx",
                "#ifndef COSIM\n#define COSIM\nvoid simulate(const char* __runtimeConfigPath);\n#define SPEC_SHA1 \"" + specSha1 + "\"\n#define " +
                        "SPEC_GEN_TIME \"" + new Date() + "\"\n#endif");
        return sources;
    }

    @Override
    public void caseALoadExp(ALoadExp node, Integer question) throws AnalysisException {
        AStringLiteralExp name = (AStringLiteralExp) node.getArgs().get(0);

        sb.append("load_" + name.getValue() + "(");

        //inject config path for MEnv
        List<PExp> arguments = name.getValue().equals("MEnv") || name.getValue().equals("DataWriter") ? Stream
                .concat(node.getArgs().stream().limit(1),
                        Stream.concat(Stream.of(new AIdentifierExp(new LexIdentifier("__runtimeConfigPath", null))), node.getArgs().stream().skip(1)))
                .collect(Collectors.toList()) : node.getArgs();
        for (int i = 1; i < arguments.size(); i++) {
            if (i > 1) {
                sb.append(", ");
            }
            arguments.get(i).apply(this, question);
        }
        sb.append(")");
    }

    @Override
    public void caseAUnloadExp(AUnloadExp node, Integer question) throws AnalysisException {
        //        sb.append("unload(");
        for (int i = 0; i < node.getArgs().size(); i++) {

            sb.append("delete ");

            node.getArgs().get(i).apply(this, question);

            sb.append(";\n" + indent(question));
            node.getArgs().get(i).apply(this, question);
            sb.append(" = nullptr");
        }
        //        sb.append(");");
    }

    @Override
    public void caseANullExp(ANullExp node, Integer question) throws AnalysisException {
        sb.append("nullptr");
    }

    @Override
    public void caseACallExp(ACallExp node, Integer question) throws AnalysisException {

        boolean isFmuComp = false;
        boolean isFmu = false;
        boolean isDataWriter = false;

        if (node.getObject() != null) {
            node.getObject().apply(this, question);

            PType objType = types.get(node.getObject());
            isFmuComp = (objType instanceof AModuleType && ((AModuleType) objType).getName().getText().equals("FMI2Component"));
            isFmu = (objType instanceof AModuleType && ((AModuleType) objType).getName().getText().equals("FMI2"));
            isDataWriter = (objType instanceof AModuleType && ((AModuleType) objType).getName().getText().equals("DataWriter"));
            if (isFmuComp) {
                sb.append("->");
                sb.append("fmu");
            }

            sb.append("->");

        }

        if (isFmuComp) {
            if (node.getMethodName().getText().equals("getState")) {
                sb.append("getFMUstate(");
            } else if (node.getMethodName().getText().equals("setState")) {
                sb.append("setFMUstate(");
            } else if (node.getMethodName().getText().equals("freeState")) {
                sb.append("freeFMUstate(");
            } else {
                sb.append(node.getMethodName().getText() + "(");
            }
        } else {
            sb.append(node.getMethodName().getText() + "(");
        }

        if (isFmuComp) {
            node.getObject().apply(this, question);
            sb.append("->");
            sb.append("comp");
            if (!node.getArgs().isEmpty()) {
                sb.append(", ");
            }
        }

        if (isDataWriter) {
            if (node.getMethodName().getText().equals("writeDataPoint")) {
                sb.append("\"");
                for (PExp arg : node.getArgs().stream().skip(2).collect(Collectors.toList())) {
                    PType at = types.get(arg);
                    if (at instanceof ABooleanPrimitiveType) {
                        sb.append("i");
                    } else if (at instanceof AIntNumericPrimitiveType) {
                        sb.append("i");
                    } else if (at instanceof AUIntNumericPrimitiveType) {
                        sb.append("u");
                    } else if (at instanceof ARealNumericPrimitiveType) {
                        sb.append("r");
                    } else if (at instanceof AStringPrimitiveType) {
                        sb.append("+");
                    } else {
                        sb.append("?");
                    }
                }
                sb.append("\", ");
            } else if (node.getMethodName().getText().equals("writeHeader")) {
                //TODO not safe
                PExp exp = node.getArgs().get(0);
                AIdentifierExp id = (AIdentifierExp) exp;
                LexIdentifier headerName = id.getName();

                ARootDocument doc = exp.getAncestor(ARootDocument.class);
                Optional<AVariableDeclaration> var = NodeCollector.collect(doc, AVariableDeclaration.class).orElse(new Vector<>()).stream()
                        .filter(v -> v.getName().equals(headerName)).findFirst();

                if (var.isPresent()) {
                    var.get().getSize().get(0).apply(this, question);
                    sb.append(", ");
                }


            }

        }

        for (int i = 0; i < node.getArgs().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            node.getArgs().get(i).apply(this, question);

            if (isFmuComp && node.getMethodName().getText().equals("getRealStatus") && i == 0) {
                //we need to convert int to enum
                int index = sb.lastIndexOf(",");
                String numberToReplace = sb.substring(index + 1);
                String newValue;
                int ival = Integer.parseInt(numberToReplace.trim());
                switch (ival) {
                    case 0:
                        newValue = "fmi2DoStepStatus";
                        break;
                    case 1:
                        newValue = "fmi2PendingStatus";
                        break;
                    case 2:
                        newValue = "fmi2LastSuccessfulTime";
                        break;
                    case 3:
                        newValue = "fmi2Terminated";
                        break;
                    default:
                        throw new RuntimeException("Unknown integer used in status");
                }


                sb.replace(index + 1, index + numberToReplace.length() + 1, " " + newValue);
            } else if (isFmu && node.getMethodName().getText().equals("freeInstance")) {
                sb.append("->");
                sb.append("comp");
            }

        }
        sb.append(")");
    }

    @Override
    public void caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node, Integer question) throws AnalysisException {

        sb.append("#include <stdint.h>\n");
        sb.append("#include <string>\n");
        node.getImports().stream().filter(im -> !im.getText().equals("FMI2Component"))
                .forEach(im -> sb.append("#include \"" + im.getText().replace("FMI2", "SimFmi2").replace("Math", "SimMath") + ".h" + "\"\n"));
        sb.append("void simulate(const char* __runtimeConfigPath)\n");
        node.getBody().apply(this, question);

    }

    @Override
    public void caseAParallelBlockStm(AParallelBlockStm node, Integer question) throws AnalysisException {
        //TODO should be parallel
        sb.append(indent(question));
        sb.append("{\n");
        for (PStm stm : node.getBody()) {
            stm.apply(this, question + 1);
            sb.append("\n");
        }
        sb.append(indent(question));
        sb.append("}\n");
    }

    @Override
    public void caseABasicBlockStm(ABasicBlockStm node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        sb.append("{\n");
        for (PStm stm : node.getBody()) {
            stm.apply(this, question + 1);
            sb.append("\n");
        }
        sb.append(indent(question));
        sb.append("}\n");
    }

    @Override
    public void caseAVariableDeclaration(AVariableDeclaration node, Integer question) throws AnalysisException {

        sb.append(indent(question));
        node.getType().apply(this, question);
        sb.append(" ");
        sb.append(node.getName().getText());

        node.getSize().forEach(s -> sb.append("[").append(s).append("]"));

        if (node.getInitializer() != null) {
            sb.append(" = ");
            node.getInitializer().apply(this, question);
        }
        if (!node.getSize().isEmpty() && node.getInitializer() == null) {
            //linux seems to have problem with these none initialized arrays
            sb.append(" = {}");
        }
        sb.append(";");
    }


    @Override
    public void caseAArrayInitializer(AArrayInitializer node, Integer question) throws AnalysisException {
        sb.append("{");
        for (PExp exp : node.getExp()) {
            exp.apply(this, question);
            if (node.getExp().indexOf(exp) != node.getExp().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
    }

    @Override
    public void caseAAssigmentStm(AAssigmentStm node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        node.getTarget().apply(this, question);
        sb.append(" = ");
        node.getExp().apply(this, question);
        sb.append(";");
    }

    @Override
    public void caseAWhileStm(AWhileStm node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        sb.append("while(");
        node.getTest().apply(this, question);
        sb.append(")\n");
        node.getBody().apply(this, question);
    }

    @Override
    public void caseAIfStm(AIfStm node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        sb.append("if(");
        node.getTest().apply(this, question);
        sb.append(")\n");
        node.getThen().apply(this, question);
        if (node.getElse() != null) {
            sb.append(indent(question));
            sb.append("else\n");
            node.getElse().apply(this, question);
        }
    }

    @Override
    public void caseAExpressionStm(AExpressionStm node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        node.getExp().apply(this, question);
        sb.append(";");
    }

    @Override
    public void caseABreakStm(ABreakStm node, Integer question) throws AnalysisException {
        sb.append(indent(question));
        sb.append("break");
        sb.append(";");
    }


    @Override
    public void caseAIdentifierExp(AIdentifierExp node, Integer question) throws AnalysisException {
        sb.append(node.getName().getText());
    }

    @Override
    public void caseAExpInitializer(AExpInitializer node, Integer question) throws AnalysisException {
        super.caseAExpInitializer(node, question);
    }

    @Override
    public void caseARealNumericPrimitiveType(ARealNumericPrimitiveType node, Integer question) throws AnalysisException {
        sb.append("double");
    }

    @Override
    public void caseABooleanPrimitiveType(ABooleanPrimitiveType node, Integer question) throws AnalysisException {
        sb.append("int");
    }

    @Override
    public void caseAIntNumericPrimitiveType(AIntNumericPrimitiveType node, Integer question) throws AnalysisException {
        sb.append("int");
    }

    @Override
    public void caseAStringPrimitiveType(AStringPrimitiveType node, Integer question) throws AnalysisException {
        sb.append("const char*");
    }

    @Override
    public void caseAUIntNumericPrimitiveType(AUIntNumericPrimitiveType node, Integer question) throws AnalysisException {
        sb.append("unsigned int");
    }


    @Override
    public void caseANameType(ANameType node, Integer question) throws AnalysisException {
        sb.append(node.getName().getText());
    }

    @Override
    public void caseAReferenceType(AReferenceType node, Integer question) throws AnalysisException {
        node.getType().apply(this, question);
        sb.append("&");
    }

    @Override
    public void caseAArrayType(AArrayType node, Integer question) throws AnalysisException {
        node.getType().apply(this, question);
        sb.append("*");
    }

    @Override
    public void caseARefExp(ARefExp node, Integer question) throws AnalysisException {
        sb.append("&");
        node.getExp().apply(this, question);
    }

    @Override
    public void caseABoolLiteralExp(ABoolLiteralExp node, Integer question) throws AnalysisException {
        sb.append(node.getValue() + "");
    }

    @Override
    public void caseAIntLiteralExp(AIntLiteralExp node, Integer question) throws AnalysisException {
        sb.append(node.getValue());
    }

    @Override
    public void caseARealLiteralExp(ARealLiteralExp node, Integer question) throws AnalysisException {
        sb.append(node.getValue());
    }

    @Override
    public void caseAUIntLiteralExp(AUIntLiteralExp node, Integer question) throws AnalysisException {
        sb.append(node.getValue());
    }

    @Override
    public void caseAStringLiteralExp(AStringLiteralExp node, Integer question) throws AnalysisException {
        sb.append("\"" + node.getValue() + "\"");
    }

    @Override
    public void caseAIdentifierStateDesignator(AIdentifierStateDesignator node, Integer question) throws AnalysisException {
        sb.append(node.getName().getText());
    }

    @Override
    public void caseAArrayStateDesignator(AArrayStateDesignator node, Integer question) throws AnalysisException {
        node.getTarget().apply(this, question);
        sb.append("[");
        node.getExp().apply(this, question);
        sb.append("]");
    }

    @Override
    public void caseAEqualBinaryExp(AEqualBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" == ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseANotEqualBinaryExp(ANotEqualBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" != ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAOrBinaryExp(AOrBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" || ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAAndBinaryExp(AAndBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" && ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAPlusBinaryExp(APlusBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" + ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAMinusBinaryExp(AMinusBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" - ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseALessBinaryExp(ALessBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" < ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseALessEqualBinaryExp(ALessEqualBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" <= ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" > ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAGreaterBinaryExp(AGreaterBinaryExp node, Integer question) throws AnalysisException {
        node.getLeft().apply(this, question);
        sb.append(" >= ");
        node.getRight().apply(this, question);
    }

    @Override
    public void caseAArrayIndexExp(AArrayIndexExp node, Integer question) throws AnalysisException {
        node.getArray().apply(this, question);
        for (int i = 0; i < node.getIndices().size(); i++) {
            sb.append("[");
            node.getIndices().get(i).apply(this, question);
            sb.append("]");
        }
    }

    @Override
    public void caseANotUnaryExp(ANotUnaryExp node, Integer question) throws AnalysisException {
        sb.append("!");
        node.getExp().apply(this, question);
    }
}
