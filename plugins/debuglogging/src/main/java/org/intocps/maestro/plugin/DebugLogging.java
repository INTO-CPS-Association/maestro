package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.plugin.env.ISimulationEnvironment;
import org.intocps.maestro.plugin.env.fmi2.ComponentInfo;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.*;

@SimulationFramework(framework = Framework.FMI2)
public class DebugLogging implements IMaestroExpansionPlugin {
    final static String fixedStepStatus = "fix_status";
    final static Logger logger = LoggerFactory.getLogger(DebugLogging.class);
    private final static int FMI_OK = 0;
    private final static int FMI_WARNING = 1;
    private final static int FMI_DISCARD = 2;
    private final static int FMI_ERROR = 3;
    private final static int FMI_FATAL = 4;
    private final static int FMI_PENDING = 5;
    final AFunctionDeclaration funEnable = newAFunctionDeclaration(newAIdentifier("enableDebugLogging"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component"))), newAVoidType());
    final AFunctionDeclaration funDisable = newAFunctionDeclaration(newAIdentifier("disableDebugLogging"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component"))), newAVoidType());
    private final String data_HeadersIdentifier = "data_headers";
    private final String dataWriter = "dataWriter";
    private final String data_valuesIdentifier = "data_values";
    private final String data_configuration = "dataWriter_configuration";

    @Override
    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(funEnable, funDisable).collect(Collectors.toSet());
    }

    @Override
    public List<PStm> expand(AFunctionDeclaration declaredFunction, List<PExp> formalArguments, IPluginConfiguration config,
            ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {

        logger.info("Unfolding with fixed step: {}", declaredFunction.toString());

        if (!getDeclaredUnfoldFunctions().contains(declaredFunction)) {
            throw new ExpandException("Unknown function declaration");
        }

        AFunctionDeclaration selectedFun = declaredFunction;

        if (formalArguments == null || formalArguments.size() != selectedFun.getFormals().size()) {
            throw new ExpandException("Invalid args");
        }

        if (env == null) {
            throw new ExpandException("Simulation environment must not be null");
        }

        List<LexIdentifier> knownComponentNames = null;

        if (formalArguments.get(0) instanceof AIdentifierExp) {
            LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();
            ABlockStm containingBlock = formalArguments.get(0).getAncestor(ABlockStm.class);

            Optional<AVariableDeclaration> compDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(name) && decl.getIsArray() && decl.getInitializer() != null).findFirst();

            if (!compDecl.isPresent()) {
                throw new ExpandException("Could not find names for comps");
            }

            AArrayInitializer initializer = (AArrayInitializer) compDecl.get().getInitializer();

            knownComponentNames = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                    .map(AIdentifierExp::getName).collect(Collectors.toList());
        }

        if (knownComponentNames == null || knownComponentNames.isEmpty()) {
            throw new ExpandException("No components found cannot fixed step with 0 components");
        }

        final List<LexIdentifier> componentNames = knownComponentNames;

        List<PStm> statements = new Vector<>();

        LexIdentifier statusIdentifier = newAIdentifier("category_status");
        statements.add(newALocalVariableStm(newAVariableDeclaration((LexIdentifier) statusIdentifier.clone(), newAIntNumericPrimitiveType(),
                newAExpInitializer(newAIntLiteralExp(0)))));


        for (LexIdentifier comp : componentNames) {
            ComponentInfo info = env.getUnitInfo(comp, Framework.FMI2);
            List<ModelDescription.LogCategory> categories = null;
            try {
                categories = info.modelDescription.getLogCategories();
            } catch (XPathExpressionException e) {
                throw new ExpandException("Could not read log categories from model description for instance: " + comp.getText(), e);
            }

            if (categories.isEmpty()) {
                continue;
            }

            //fmi2Status fmi2SetDebugLogging(fmi2Component c, fmi2Boolean loggingOn, size_t nCategories, const fmi2String categories[]);
            List<PStm> body = new Vector<>();

            body.add(newALocalVariableStm(
                    newAVariableDeclaration(newAIdentifier("categories"), newAArrayType(newAStringPrimitiveType(), categories.size()),
                            newAArrayInitializer(
                                    categories.stream().map(m -> m.name).map(MableAstFactory::newAStringLiteralExp).collect(Collectors.toList())))));

            body.add(newAAssignmentStm(newAIdentifierStateDesignator((LexIdentifier) statusIdentifier.clone()),
                    newACallExp(newAIdentifierExp((LexIdentifier) comp.clone()), newAIdentifier("fmi2SetDebugLogging"),
                            Arrays.asList(newABoolLiteralExp(selectedFun == funEnable), newAIntLiteralExp(categories.size()),
                                    newAIdentifierExp("categories")))));

            body.add(newIf(newOr(newPar(newEqual(newAIdentifierExp((LexIdentifier) statusIdentifier.clone()), newAIntLiteralExp(FMI_ERROR))),
                    newPar(newEqual(newAIdentifierExp((LexIdentifier) statusIdentifier.clone()), newAIntLiteralExp(FMI_FATAL)))),
                    newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")), newABoolLiteralExp(false)), null));


            statements.add(newABlockStm(body));

        }


        return statements.size() == 1 ? new Vector<>() : Arrays
                .asList(newIf(newAIdentifierExp("global_execution_continue"), newABlockStm(statements), null));
    }


    private String getFmiGetName(ModelDescription.Types type, UsageType usage) {

        String fun = usage == UsageType.In ? "set" : "get";
        switch (type) {
            case Boolean:
                return fun + "Boolean";
            case Real:
                return fun + "Real";
            case Integer:
                return fun + "Integer";
            case String:
                return fun + "String";
            case Enumeration:
            default:
                return null;
        }
    }

    LexIdentifier getStateName(LexIdentifier comp) {
        return newAIdentifier(comp.getText() + "State");
    }

    SPrimitiveType convert(ModelDescription.Types type) {
        switch (type) {

            case Boolean:
                return newABoleanPrimitiveType();
            case Real:
                return newARealNumericPrimitiveType();
            case Integer:
                return newAIntNumericPrimitiveType();
            case String:
                return newAStringPrimitiveType();
            case Enumeration:
            default:
                return null;
        }
    }

    LexIdentifier getBufferName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {
        return getBufferName(comp, convert(type), usage);
    }


    LexIdentifier getBufferName(LexIdentifier comp, SPrimitiveType type, UsageType usage) {

        String t = getTypeId(type);

        return newAIdentifier(comp.getText() + t + usage);
    }

    private String getTypeId(SPrimitiveType type) {
        String t = type.getClass().getSimpleName();

        if (type instanceof ARealNumericPrimitiveType) {
            t = "R";
        } else if (type instanceof AIntNumericPrimitiveType) {
            t = "I";
        } else if (type instanceof AStringPrimitiveType) {
            t = "S";
        } else if (type instanceof ABooleanPrimitiveType) {
            t = "B";
        }
        return t;
    }

    LexIdentifier getVrefName(LexIdentifier comp, ModelDescription.Types type, UsageType usage) {

        return newAIdentifier(comp.getText() + "Vref" + getTypeId(convert(type)) + usage);
    }

    @Override
    public boolean requireConfig() {
        return false;
    }

    @Override
    public IPluginConfiguration parseConfig(InputStream is) throws IOException {
        return new FixedstepConfig(new ObjectMapper().readValue(is, Integer.class));
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    enum UsageType {
        In,
        Out
    }

    class FixedstepConfig implements IPluginConfiguration {
        final int endTime;

        public FixedstepConfig(int endTime) {
            this.endTime = endTime;
        }
    }
}
