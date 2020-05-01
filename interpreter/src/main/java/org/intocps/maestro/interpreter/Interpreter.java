package org.intocps.maestro.interpreter;

import org.intocps.fmi.*;
import org.intocps.fmi.jnifmuapi.Factory;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.interpreter.values.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

class Interpreter extends QuestionAnswerAdaptor<Context, Value> {
    final static Logger logger = LoggerFactory.getLogger(Interpreter.class);

    List<Value> evaluate(List<? extends PExp> list, Context ctxt) throws AnalysisException {
        List<Value> values = new Vector<>();
        for (PExp item : list) {
            values.add(item.apply(this, ctxt));
        }
        return values;
    }

    @Override
    public Value caseARootDocument(ARootDocument node, Context question) throws AnalysisException {

        return node.getContent().stream().filter(f -> f instanceof ASimulationSpecificationCompilationUnit).findFirst().map(spec -> {
            try {
                return spec.apply(this, question);
            } catch (AnalysisException e) {
                e.printStackTrace();
                return null;
            }
        }).orElse(null);

    }

    @Override
    public Value caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node,
            Context question) throws AnalysisException {
        return node.getBody().apply(this, question);

    }

    @Override
    public Value caseABlockStm(ABlockStm node, Context question) throws AnalysisException {

        for (PStm stm : node.getBody()) {
            stm.apply(this, question);
        }
        return new VoidValue();
    }

    @Override
    public Value caseALocalVariableStm(ALocalVariableStm node, Context question) throws AnalysisException {

        return node.getDeclaration().apply(this, question);
    }

    @Override
    public Value caseALoadExp(ALoadExp node, Context question) throws AnalysisException {

        List<Value> args = evaluate(node.getArgs(), question);

        String type = ((StringValue) args.get(0)).getValue();
        String name = ((StringValue) args.get(1)).getValue();
        String guid = ((StringValue) args.get(2)).getValue();
        String path = ((StringValue) args.get(3)).getValue();

        if (type.equals("FMI2")) {
            try {
                IFmu fmu = Factory.create(new File(path));
                fmu.load();


                Map<String, Value> functions = new HashMap<>();

                functions.put("instantiate", new FunctionValue.ExternalFunctionValue(fargs -> {
                    try {
                        IFmiComponent component = fmu.instantiate(guid, name, true, true, new IFmuCallback() {
                            @Override
                            public void log(String instanceName, Fmi2Status status, String category, String message) {
                                logger.info("NATIVE: instance: '{}', status: '{}', category: '{}', message: {}", instanceName, status, category,
                                        message);
                            }

                            @Override
                            public void stepFinished(Fmi2Status status) {

                            }
                        });

                        //populate component functions
                        Map<String, Value> members = new HashMap<>();
                        return new ModuleValue(members);


                    } catch (XPathExpressionException | FmiInvalidNativeStateException e) {
                        e.printStackTrace();
                    }

                    return null;
                }));

                return new ModuleValue(functions);
            } catch (IOException | FmuInvocationException e) {
                throw new AnalysisException("load error", e);
            } catch (FmuMissingLibraryException e) {
                throw new AnalysisException("FMU load error", e);
            }
        }
        throw new AnalysisException("Load of unknown type");
    }


    @Override
    public Value caseAAssigmentStm(AAssigmentStm node, Context question) throws AnalysisException {
        //FIXME
        // question.put(node.getIdentifier(), node.getExp().apply(this, question));

        return new VoidValue();
    }

    @Override
    public Value caseAVariableDeclaration(AVariableDeclaration node, Context question) throws AnalysisException {

        question.put(node.getName(), node.getInitializer() == null ? new UndefinedValue() : node.getInitializer().apply(this, question));

        return new VoidValue();
    }

    @Override
    public Value caseAExpInitializer(AExpInitializer node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }

    @Override
    public Value caseADotExp(ADotExp node, Context question) throws AnalysisException {

        Value root = node.getRoot().apply(this, question);

        if (root instanceof ModuleValue) {

            return node.getExp().apply(this, new ModuleContext((ModuleValue) root));

        }
        return null;
    }

    @Override
    public Value caseAExpressionStm(AExpressionStm node, Context question) throws AnalysisException {
        return node.getExp().apply(this, question);
    }


    @Override
    public Value caseAIdentifierExp(AIdentifierExp node, Context question) throws AnalysisException {
        return question.lookup(node.getName());
    }

    @Override
    public Value caseACallExp(ACallExp node, Context question) throws AnalysisException {

        Value function = node.getRoot().apply(this, question);

        if (function instanceof FunctionValue) {

            return ((FunctionValue) function).evaluate(evaluate(node.getArgs(), question));
        }

        return null;
    }

    @Override
    public Value caseARealLiteralExp(ARealLiteralExp node, Context question) throws AnalysisException {
        return new RealValue(node.getValue());
    }

    @Override
    public Value caseABoolLiteralExp(ABoolLiteralExp node, Context question) throws AnalysisException {
        return new BooleanValue(node.getValue());
    }

    @Override
    public Value caseAStringLiteralExp(AStringLiteralExp node, Context question) throws AnalysisException {
        return new StringValue(node.getValue());
    }

    @Override
    public Value createNewReturnValue(INode node, Context question) throws AnalysisException {
        logger.debug("Unhandled interpreter node: {}", node.getClass().getSimpleName());
        return null;
    }

    @Override
    public Value createNewReturnValue(Object node, Context question) throws AnalysisException {
        return null;
    }
}
