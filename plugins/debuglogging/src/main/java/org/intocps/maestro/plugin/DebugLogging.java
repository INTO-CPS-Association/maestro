package org.intocps.maestro.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intocps.maestro.ast.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;
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
    private final static String CATEGORY_STATUS = "category_status";
    final AFunctionDeclaration funEnable = newAFunctionDeclaration(newAIdentifier("enableDebugLogging"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAArrayType(newAStringPrimitiveType()), newAIdentifier("categories")),
                    newAFormalParameter(newAUIntNumericPrimitiveType(), newAIdentifier("categoriesSize"))), newAVoidType());
    final AFunctionDeclaration funDisable = newAFunctionDeclaration(newAIdentifier("disableDebugLogging"),
            Arrays.asList(newAFormalParameter(newAArrayType(newANameType("FMI2Component")), newAIdentifier("component")),
                    newAFormalParameter(newAArrayType(newAStringPrimitiveType()), newAIdentifier("categories")),
                    newAFormalParameter(newAUIntNumericPrimitiveType(), newAIdentifier("categoriesSize"))), newAVoidType());

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

        LexIdentifier name = ((AIdentifierExp) formalArguments.get(0)).getName();

        List<PStm> statements = new Vector<>();


        //fmi2Status fmi2SetDebugLogging(fmi2Component c, fmi2Boolean loggingOn, size_t nCategories, const fmi2String categories[]);
        AIdentifierExp categories = (AIdentifierExp) formalArguments.get(1);
        AIntLiteralExp size = (AIntLiteralExp) formalArguments.get(2);

        LexIdentifier statusIdentifier = newAIdentifier(CATEGORY_STATUS);
        statements.add(newALocalVariableStm(MableAstFactory.newAVariableDeclaration(statusIdentifier, MableAstFactory.newAIntNumericPrimitiveType(),
                MableAstFactory.newAExpInitializer(newACallExp(newAIdentifierExp((LexIdentifier) name.clone()), newAIdentifier("fmi2SetDebugLogging"),
                        Arrays.asList(newABoolLiteralExp(selectedFun == funEnable), size.clone(), categories.clone()))))));

        statements.add(newIf(newOr(newPar(newEqual(newAIdentifierExp((LexIdentifier) statusIdentifier.clone()), newAIntLiteralExp(FMI_ERROR))),
                newPar(newEqual(newAIdentifierExp((LexIdentifier) statusIdentifier.clone()), newAIntLiteralExp(FMI_FATAL)))),
                newAAssignmentStm(newAIdentifierStateDesignator(newAIdentifier("global_execution_continue")), newABoolLiteralExp(false)), null));

        return Arrays.asList(newIf(newAIdentifierExp("global_execution_continue"), newABlockStm(statements), null));
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

    class FixedstepConfig implements IPluginConfiguration {
        final int endTime;

        public FixedstepConfig(int endTime) {
            this.endTime = endTime;
        }
    }
}
