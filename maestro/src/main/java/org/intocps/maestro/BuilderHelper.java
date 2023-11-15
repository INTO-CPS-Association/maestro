package org.intocps.maestro;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptorQuestion;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.core.FrameworkUnitInfo;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.ComponentInfo;
import org.intocps.maestro.framework.fmi2.Fmi2SimulationEnvironment;
import org.intocps.maestro.framework.fmi2.InstanceInfo;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.FromMaBLToMaBLAPI;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext;
import org.intocps.maestro.framework.fmi2.api.mabl.ModelDescriptionContext3;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;
import org.intocps.maestro.typechecker.TypeComparator;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.newABlockStm;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class BuilderHelper {
    static final TypeComparator typeComparator = new TypeComparator();
    private final MablApiBuilder builder;
    List<FmiBuilder.Variable<PStm, ?>> variables;

    public BuilderHelper(ACallExp callToBeReplaced, Map<INode, PType> typesMap,
                         ISimulationEnvironment simulationEnvironment) throws FmiBuilder.Port.PortLinkException, AnalysisException {

        MablApiBuilder.MablSettings settings = new MablApiBuilder.MablSettings();
        settings.fmiErrorHandlingEnabled = true;
        this.builder = new MablApiBuilder(settings, callToBeReplaced);

        // Build a graph from AInstanceMapping. I.e. if FMU instance A is faultinjected by B then the graph should be
        // B -> A
        var simulationSpecification = callToBeReplaced.getAncestor(ASimulationSpecificationCompilationUnit.class);
        var graph = buildInstanceMappingGraph(simulationSpecification);

        Map<String, ComponentVariableFmi2Api> instances = new HashMap<>();
        Map<String, InstanceVariableFmi3Api> fmi3Instances = new HashMap<>();
        this.variables = callToBeReplaced.getArgs().stream()
                .map(exp -> wrapAsVariable(builder, typesMap, simulationEnvironment, typesMap.get(exp), exp, instances, fmi3Instances, graph))
                .collect(Collectors.toList());

        FromMaBLToMaBLAPI.createBindings(instances, simulationEnvironment);
        FromMaBLToMaBLAPI.createBindings3(fmi3Instances, simulationEnvironment);
    }

    private static String getLastVertexOfDirectedGraph(DefaultDirectedGraph<String, org.jgrapht.graph.DefaultEdge> graph, String startVertex) {
        String finalVertex = startVertex;
        if (graph.containsVertex(startVertex)) {
            DepthFirstIterator<String, org.jgrapht.graph.DefaultEdge> iterator = new DepthFirstIterator<>(graph, startVertex);

            while (iterator.hasNext()) {
                finalVertex = iterator.next();
            }
        }
        return finalVertex;
    }

    private static DefaultDirectedGraph<String, org.jgrapht.graph.DefaultEdge> buildInstanceMappingGraph(
            ASimulationSpecificationCompilationUnit spec) throws AnalysisException {
        DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);


        spec.apply(new DepthFirstAnalysisAdaptorQuestion<>() {
            @Override
            public void caseAInstanceMappingStm(AInstanceMappingStm node,
                                                DefaultDirectedGraph<String, DefaultEdge> question) throws AnalysisException {
                super.caseAInstanceMappingStm(node, question);

                String source = node.getIdentifier().getText();
                String target = node.getName();
                if (!source.equals(target)) {
                    question.addVertex(source);
                    question.addVertex(target);
                    question.addEdge(source, target);
                }
            }
        }, graph);

        return graph;
    }

    private static FmiBuilder.Variable<PStm, ?> wrapAsVariable(MablApiBuilder builder, Map<INode, PType> typesMap,
                                                               ISimulationEnvironment simulationEnvironment, PType type, PExp exp, Map<String, ComponentVariableFmi2Api> instances,
                                                               Map<String, InstanceVariableFmi3Api> fmi3Instances, DefaultDirectedGraph<String, org.jgrapht.graph.DefaultEdge> graph) {

        Fmi2SimulationEnvironment env = null;

        if (simulationEnvironment instanceof Fmi2SimulationEnvironment) {
            env = (Fmi2SimulationEnvironment) simulationEnvironment;
        }


        if (typeComparator.compatible(ARealNumericPrimitiveType.class, type)) {
            return new DoubleVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (typeComparator.compatible(AIntNumericPrimitiveType.class, type)) {
            return new IntVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (typeComparator.compatible(ABooleanPrimitiveType.class, type)) {
            return new BooleanVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (typeComparator.compatible(AStringPrimitiveType.class, type)) {
            return new StringVariableFmi2Api(null, null, null, null, exp.clone());
        } else if (env != null && type instanceof AModuleType) {
            String moduleTypeName = ((AModuleType) type).getName().getText();

            if (moduleTypeName.equals("FMI2") || moduleTypeName.equals("FMI3")) {
                if (exp instanceof AIdentifierExp) {
                    String componentName = ((AIdentifierExp) exp).getName().getText();

                    FrameworkUnitInfo instance = env.getInstanceByLexName(componentName);

                    //This dummy statement is removed later. It ensures that the share variables are added to the root scope.
                    PStm dummyStm = newABlockStm();
                    builder.getDynamicScope().add(dummyStm);

                    if (instance instanceof ComponentInfo) {
                        try {
                            ModelDescriptionContext mdc = new ModelDescriptionContext(((ComponentInfo) instance).modelDescription);

                            return new FmuVariableFmi2Api(instance.getOwnerIdentifier(), builder, mdc, dummyStm, newANameType("FMI2"),
                                    builder.getDynamicScope().getActiveScope(), builder.getDynamicScope(), null,
                                    new AIdentifierExp(new LexIdentifier(instance.getOwnerIdentifier().replace("{", "").replace("}", ""), null)));

                        } catch (IllegalAccessException | XPathExpressionException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (instance instanceof InstanceInfo) {

                        try {
                            ModelDescriptionContext3 mdc = new ModelDescriptionContext3(((InstanceInfo) instance).getModelDescription());

                            return new FmuVariableFmi3Api(instance.getOwnerIdentifier(), builder, mdc, dummyStm, newANameType("FMI3"),
                                    builder.getDynamicScope().getActiveScope(), builder.getDynamicScope(), null,
                                    new AIdentifierExp(new LexIdentifier(instance.getOwnerIdentifier().replace("{", "").replace("}", ""), null)));
                        } catch (IllegalAccessException | XPathExpressionException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    throw new RuntimeException("exp is not identifying as a component: " + instance);
                } else {
                    throw new RuntimeException("exp is not of type AIdentifierExp, but of type: " + exp.getClass());
                }

            } else if (moduleTypeName.equals("FMI2Component") || moduleTypeName.equals("FMI3Instance")) {
                try {
                    String environmentComponentName = ((AIdentifierExp) exp).getName().getText();
                    if (graph != null) {
                        environmentComponentName = getLastVertexOfDirectedGraph(graph, environmentComponentName);
                    }

                    if (moduleTypeName.equals("FMI2Component")) {
                        Map.Entry<String, ComponentVariableFmi2Api> component =
                                FromMaBLToMaBLAPI.getComponentVariableFrom(builder, exp, env, environmentComponentName);

                        instances.put(component.getKey(), component.getValue());
                        return component.getValue();
                    } else if (moduleTypeName.equals("FMI3Instance")) {
                        Map.Entry<String, InstanceVariableFmi3Api> instance =
                                FromMaBLToMaBLAPI.getInstanceVariableFrom(builder, exp, env, environmentComponentName);

                        fmi3Instances.put(instance.getKey(), instance.getValue());
                        return instance.getValue();
                    }
                } catch (IllegalAccessException | XPathExpressionException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }

        } else if (type instanceof AArrayType &&
                (((AArrayType) type).getType() instanceof ANameType || ((AArrayType) type).getType() instanceof AModuleType)) {
            LexIdentifier itemName = ((AIdentifierExp) exp).getName();
            SBlockStm containingBlock = exp.getAncestor(SBlockStm.class);
            Optional<AVariableDeclaration> itemDecl =
                    containingBlock.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                            .map(ALocalVariableStm::getDeclaration)
                            .filter(decl -> decl.getName().equals(itemName) && !decl.getSize().isEmpty() && decl.getInitializer() != null)
                            .findFirst();


            List<PExp> initializerExps;
            if (itemDecl.isEmpty()) {
//                throw new RuntimeException("Could not find names for components");
                initializerExps = new ArrayList<>();
            } else {

                AArrayInitializer initializer = (AArrayInitializer) itemDecl.get().getInitializer();


                initializerExps = initializer.getExp().stream().filter(AIdentifierExp.class::isInstance).map(AIdentifierExp.class::cast)
                        .collect(Collectors.toList());
            }

            return new ArrayVariableFmi2Api(null, type.clone(), null, null, null, exp, initializerExps.stream()
                    .map(e -> wrapAsVariable(builder, typesMap, simulationEnvironment, typesMap.get(e), e, instances, fmi3Instances, graph))
                    .collect(Collectors.toList()));
        }

        return null;
    }

    public FmiBuilder<PStm, ASimulationSpecificationCompilationUnit, PExp, MablApiBuilder.MablSettings> getBuilder() {
        return this.builder;
    }

    public List<FmiBuilder.Variable<PStm, ?>> getArgumentVariables() {
        return this.variables;
    }
}
