package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.core.IRelation;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.values.ValueFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableBuilder.newVariable;


public class MablApiBuilder implements Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp> {

    static ScopeFmi2Api rootScope;
    final DynamicActiveBuilderScope dynamicScope;
    final TagNameGenerator nameGenerator = new TagNameGenerator();
    private final VariableCreatorFmi2Api currentVariableCreator;

    private final BooleanVariableFmi2Api globalExecutionContinue;
    private final IntVariableFmi2Api globalFmiStatus;
    private final MathBuilderFmi2Api mathBuilderApi;

    public MablApiBuilder() {
        rootScope = new ScopeFmi2Api(this);
        this.dynamicScope = new DynamicActiveBuilderScope(rootScope);
        this.currentVariableCreator = new VariableCreatorFmi2Api(dynamicScope, this);
        this.mathBuilderApi = new MathBuilderFmi2Api(dynamicScope, this);

        this.getDynamicScope().store(new ValueFmi2Api<>(newABoleanPrimitiveType(), false));

        //create global variables
        globalExecutionContinue =
                (BooleanVariableFmi2Api) createVariable(rootScope, newBoleanType(), newABoolLiteralExp(true), "global", "execution", "continue");
        globalFmiStatus = (IntVariableFmi2Api) createVariable(rootScope, newIntType(), null, "status");

    }

    public BooleanVariableFmi2Api getGlobalExecutionContinue() {
        return globalExecutionContinue;
    }

    public IntVariableFmi2Api getGlobalFmiStatus() {
        return globalFmiStatus;
    }

    @SuppressWarnings("rawtypes")
    private Variable createVariable(IMablScope scope, PType type, PExp initialValue, String... prefixes) {
        String name = nameGenerator.getName(prefixes);
        PStm var = newVariable(name, type, initialValue);
        scope.add(var);
        if (type instanceof ARealNumericPrimitiveType) {
            return new DoubleVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof ABooleanPrimitiveType) {
            return new BooleanVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AIntNumericPrimitiveType) {
            return new IntVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AStringPrimitiveType) {
            return new StringVariableFmi2Api(var, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        }

        return new VariableFmi2Api(var, type, scope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
    }

    public TagNameGenerator getNameGenerator() {
        return nameGenerator;
    }

    public MathBuilderFmi2Api getMathBuilder() {
        return this.mathBuilderApi;

    }

    @Override
    public IMablScope getRootScope() {
        return MablApiBuilder.rootScope;
    }

    @Override
    public DynamicActiveBuilderScope getDynamicScope() {
        return this.dynamicScope;
    }

    @Override
    public <V, T> Variable<T, V> getCurrentLinkedValue(Port port) {
        PortFmi2Api mp = (PortFmi2Api) port;
        if (mp.getSharedAsVariable() == null) {
            return null;
        }
        return mp.getSharedAsVariable();
    }

    @Override
    public VariableCreatorFmi2Api variableCreator() {
        return this.currentVariableCreator;
    }

    Pair<PStateDesignator, PExp> getDesignatorAndReferenceExp(PExp exp) {
        if (exp instanceof AArrayIndexExp) {
            AArrayIndexExp exp_ = (AArrayIndexExp) exp;
            // TODO
        } else if (exp instanceof AIdentifierExp) {
            AIdentifierExp exp_ = (AIdentifierExp) exp;
            return Pair.of(newAIdentifierStateDesignator(exp_.getName()), exp_);
        }

        throw new RuntimeException("Invalid expression of class: " + exp.getClass());
    }

    @Override
    public DoubleVariableFmi2Api getDoubleVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new DoubleVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());

    }

    @Override
    public IntVariableFmi2Api getIntVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new IntVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());
    }

    @Override
    public StringVariableFmi2Api getStringVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new StringVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());
    }

    @Override
    public BooleanVariableFmi2Api getBooleanVariableFrom(PExp exp) {
        Pair<PStateDesignator, PExp> t = getDesignatorAndReferenceExp(exp);
        return new BooleanVariableFmi2Api(null, rootScope, this.dynamicScope, t.getLeft(), t.getRight());
    }

    @Override
    public Map.Entry<String, ComponentVariableFmi2Api> getComponentVariableFrom(PExp exp,
            Fmi2SimulationEnvironment env) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        if (exp instanceof AIdentifierExp) {
            String componentName = ((AIdentifierExp) exp).getName().getText();
            ComponentInfo instance = env.getInstanceByLexName(componentName);
            ModelDescriptionContext modelDescriptionContext = new ModelDescriptionContext(instance.modelDescription);

            //This dummy statement is removed later. It ensures that the share variables are added to the root scope.
            PStm dummyStm = newABlockStm();
            this.getRootScope().add(dummyStm);

            ComponentVariableFmi2Api a =
                    new ComponentVariableFmi2Api(dummyStm, null, componentName, modelDescriptionContext, this, this.getRootScope(), null,
                            newAIdentifierExp(componentName));
            return Map.entry(componentName, a);
        } else {
            throw new RuntimeException("exp is not of type AIdentifierExp, but of type: " + exp.getClass());
        }
    }

    // TODO: NOT WORKING YET
    public void createBindings(Map<String, ComponentVariableFmi2Api> instances, ISimulationEnvironment env) throws Port.PortLinkException {
        if (env instanceof Fmi2SimulationEnvironment) {
            //            Fmi2SimulationEnvironment env_ = (Fmi2SimulationEnvironment)env;
            for (Map.Entry<String, ComponentVariableFmi2Api> entry : instances.entrySet()) {
                for (IRelation relation : env.getRelations(entry.getKey()).stream()
                        .filter(x -> x.getDirection() == Fmi2SimulationEnvironment.Relation.Direction.OutputToInput &&
                                x.getOrigin() == Fmi2SimulationEnvironment.Relation.InternalOrExternal.External).collect(Collectors.toList())) {
                    PortFmi2Api[] targets = relation.getTargets().entrySet().stream().map(x -> {
                        ComponentVariableFmi2Api instance = instances.get(x.getValue().getScalarVariable().getInstance().getText());
                        return instance.getPort(x.getValue().getScalarVariable().getScalarVariable().getName());
                    }).toArray(PortFmi2Api[]::new);
                    entry.getValue().getPort(relation.getSource().getScalarVariable().getScalarVariable().getName()).linkTo(targets);
                }
            }
        }
    }

    // TODO: NOT WORKING YET
    @Override
    public Map<String, ComponentVariableFmi2Api> getComponentVariablesFrom(PExp exp,
            Fmi2SimulationEnvironment env) throws IllegalAccessException, XPathExpressionException, InvocationTargetException {
        LexIdentifier componentsArrayName = ((AIdentifierExp) exp).getName();
        ABlockStm containingBlock = exp.getAncestor(ABlockStm.class);
        Optional<AVariableDeclaration> componentDeclaration =
                containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                        .map(ALocalVariableStm::getDeclaration)
                        .filter(decl -> decl.getName().equals(componentsArrayName) && !decl.getSize().isEmpty() && decl.getInitializer() != null)
                        .findFirst();

        if (!componentDeclaration.isPresent()) {
            throw new RuntimeException("Could not find names for components");
        }

        AArrayInitializer initializer = (AArrayInitializer) componentDeclaration.get().getInitializer();


        List<PExp> componentIdentifiers =
                initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast).collect(Collectors.toList());

        HashMap<String, ComponentVariableFmi2Api> fmuInstances = new HashMap<>();

        for (PExp componentName : componentIdentifiers) {
            Map.Entry<String, ComponentVariableFmi2Api> component = getComponentVariableFrom(componentName, env);
            fmuInstances.put(component.getKey(), component.getValue());
        }

        return fmuInstances;
    }


    @Override
    public FmuVariableFmi2Api getFmuVariableFrom(PExp exp) {
        return null;
    }

    @Override
    public PStm buildRaw() {
        return rootScope.getBlock().clone();
    }

    @Override
    public ASimulationSpecificationCompilationUnit build() throws AnalysisException {
        ABlockStm block = rootScope.getBlock().clone();

        //Post cleaning: Remove empty block statements
        block.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseABlockStm(ABlockStm node) throws AnalysisException {
                if (node.getBody().isEmpty()) {
                    if (node.parent() instanceof ABlockStm) {
                        ABlockStm pb = (ABlockStm) node.parent();
                        pb.getBody().remove(node);
                    } else if (node.parent() instanceof AIfStm) {
                        AIfStm ifStm = (AIfStm) node.parent();

                        if (ifStm.getElse() == node) {
                            ifStm.setElse(null);
                        }
                    }
                } else {
                    super.caseABlockStm(node);
                }

            }
        });

        ASimulationSpecificationCompilationUnit unit = new ASimulationSpecificationCompilationUnit();
        unit.setBody(block);
        unit.setFramework(Collections.singletonList(newAIdentifier("FMI2")));

        AConfigFramework config = new AConfigFramework();
        config.setName(newAIdentifier("FMI2"));
        //config.setConfig(StringEscapeUtils.escapeJava(simulationEnvironment.));
        // unit.setFrameworkConfigs(Arrays.asList(config));
        unit.setImports(Collections.singletonList(newAIdentifier("FMI2")));

        return unit;

    }

    public PExp getStartTime() {
        return null;
    }

    public PExp getEndTime() {
        return null;
    }
}
