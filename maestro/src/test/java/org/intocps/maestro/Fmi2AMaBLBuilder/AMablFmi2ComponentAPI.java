package org.intocps.maestro.Fmi2AMaBLBuilder;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.Fmi2AMaBLBuilder.scopebundle.IBasicScopeBundle;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.MableAstFactory;
import org.intocps.maestro.ast.MableBuilder;
import org.intocps.maestro.ast.node.AAssigmentStm;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class AMablFmi2ComponentAPI implements Fmi2Builder.Fmi2ComponentApi {

    private final AMablFmu2Api parent;
    private final String name;
    private final IBasicScopeBundle scopeBundle;
    ModelDescriptionContext modelDescriptionContext;
    AMablVariable<AMablFmi2ComponentAPI> variable;

    public AMablFmi2ComponentAPI(AMablFmu2Api parent, String name, AMablVariable<AMablFmi2ComponentAPI> variable,
            ModelDescriptionContext modelDescriptionContext, IBasicScopeBundle scopeBundle) {
        this.parent = parent;
        this.name = name;
        this.variable = variable;
        this.modelDescriptionContext = modelDescriptionContext;
        this.scopeBundle = scopeBundle;
    }

    @Override
    public List<Fmi2Builder.Port> getPorts(String... names) {
        return Arrays.stream(names).map(this::getPort).collect(Collectors.toList());
    }

    @Override
    public List<Fmi2Builder.Port> getPorts(int... valueReferences) {
        return Arrays.stream(valueReferences).mapToObj(this::getPort).collect(Collectors.toList());
    }

    @Override
    public AMablPort getPort(String name) {
        return this.getPort(this.modelDescriptionContext.nameToSv.get(name));
    }

    @Override
    public AMablPort getPort(int valueReference) {
        return this.getPort(this.modelDescriptionContext.valRefToSv.get(valueReference));
    }

    private AMablPort getPort(ModelDescription.ScalarVariable sv) {
        Supplier<AMablPort> portCreator = () -> {
            return new AMablPort(this, sv);
        };
        PortIdentifier pi = PortIdentifier.of(this, sv);
        return AMablBuilder.getOrCreatePort(pi, portCreator);
    }

    @Override
    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(Fmi2Builder.Port... ports) {
        return null;
    }

    @Override
    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(Fmi2Builder.Scope scope, Fmi2Builder.Port... ports) {
        return null;
    }

    @Override
    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get() {
        return null;
    }

    @Override
    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(int... valueReferences) {
        return null;
    }

    @Override
    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(String... names) {
        return null;
    }

    @Override
    /**
     * Stores the final value in rootScope
     * Uses the rootScope for valueReferences
     */ public Map<Fmi2Builder.Port, Fmi2Builder.Value> getAndShare(String... names) {
        // 1. Ensure that rootscope contains a variable fit for purpose
        // 1.1: Otherwise create it
        // 2. Locally create the data necessary to retrieve the get
        // 2.1 Store the result in the global variable.
        Map<Fmi2Builder.Port, Fmi2Builder.Value> returnMap = new HashMap<>();
        List<Fmi2Builder.Port> ports = this.getPorts(names);
        ports.forEach(p -> {
            AMablPort p_ = (AMablPort) p;
            returnMap.putAll(getAndShare(p_));
        });

        return returnMap;
    }

    public Map<Fmi2Builder.Port, Fmi2Builder.Value> getAndShare(AMablPort... ports) {
        Map<Fmi2Builder.Port, Fmi2Builder.Value> returnMap = new HashMap<>();
        Arrays.stream(ports).forEach(p -> {
            if (p.relatedVariable == null) {
                AMablVariable variableForPort = AMablBuilder.rootScope.variableCreator.createVariableForPort(p);
                p.relatedVariable = variableForPort;
            }
            createGetStm(AMablBuilder.rootScope, p);
            returnMap.put(p, null);
        });
        return returnMap;
    }

    private void createGetStm(AMaBLScope scope, AMablPort p) {
        if (this.variable.position instanceof AMaBLVariableLocation.BasicPosition) {
            Pair<LexIdentifier, List<PStm>> valRefArray = scope.findOrCreateValueReferenceArrayAndAssign(new long[]{p.getPortReferenceValue()});
            scope.addStatements(valRefArray.getRight());
            AAssigmentStm stm = MableAstFactory.newAAssignmentStm(AMablBuilder.getStatus().getStateDesignator(), MableBuilder
                    .call(this.variable.getName(), createFunctionName(Function.GET, p), MableAstFactory.newAIdentifierExp(valRefArray.getLeft()),
                            MableAstFactory.newAUIntLiteralExp(1L), MableAstFactory.newAIdentifierExp(p.relatedVariable.getName())));
            scope.addStatement(stm);
        }
    }

    private String createFunctionName(Function get, AMablPort p) {
        return createFunctionName(get, p.scalarVariable.getType().type);
    }

    private String createFunctionName(Function f, ModelDescription.Types type) {
        String functionName = "";
        switch (f) {
            case GET:
                functionName += "get";
                break;
        }
        functionName += type.name();
        return functionName;
    }

    @Override
    public Fmi2Builder.Value getSingle(String name) {
        return null;
    }

    @Override
    public void set(Fmi2Builder.Scope scope, Map<Fmi2Builder.Port, Fmi2Builder.Value> value) {

    }

    @Override
    public void set(Map<Fmi2Builder.Port, Fmi2Builder.Value> value) {

    }

    @Override
    public void set(String... names) {

        List<Fmi2Builder.Port> ports = this.getPorts(names);
        ports.forEach(p -> {
            // Find the port that is the source of the given value
            AMablPort p_ = (AMablPort) p;
            AMablPort companionPort = p_.getCompanionOutputPort();
            // Create valuereference set array
            Pair<LexIdentifier, List<PStm>> valRefArray =
                    scopeBundle.getScope().findOrCreateValueReferenceArrayAndAssign(new long[]{p.getPortReferenceValue()});
            // TODO: Create the value set array
            //AMablBuilder.rootScope.findOrCreateArrayOfSize(p_);


        });


    }

    @Override
    public void setInt(Map<Integer, Fmi2Builder.Value> values) {

    }

    @Override
    public void setString(Map<String, Fmi2Builder.Value> value) {

    }

    @Override
    public void share(Map<Fmi2Builder.Port, Fmi2Builder.Value> values) {

    }

    @Override
    public void share(Fmi2Builder.Port port, Fmi2Builder.Value value) {

    }

    @Override
    public Fmi2Builder.TimeDeltaValue step(Fmi2Builder.TimeDeltaValue deltaTime) {
        return null;
    }

    @Override
    public Fmi2Builder.TimeDeltaValue step(Fmi2Builder.Variable<Fmi2Builder.TimeDeltaValue> deltaTime) {
        return null;
    }

    @Override
    public Fmi2Builder.TimeDeltaValue step(double deltaTime) {
        return null;
    }

    @Override
    public Fmi2Builder.TimeTaggedState getState() {
        return null;
    }

    @Override
    public Fmi2Builder.Time setState(Fmi2Builder.TimeTaggedState state) {
        return null;
    }

    @Override
    public Fmi2Builder.Time setState() {
        return null;
    }

    public AMablFmu2Api getParent() {
        return this.parent;
    }

    public String getName() {
        return this.name;
    }

    public enum Function {
        GET
    }

}
