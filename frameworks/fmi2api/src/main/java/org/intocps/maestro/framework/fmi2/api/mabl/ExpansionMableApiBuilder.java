package org.intocps.maestro.framework.fmi2.api.mabl;

import org.apache.commons.lang3.tuple.Pair;
import org.intocps.maestro.ast.ABasicBlockStm;
import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;
import static org.intocps.maestro.ast.MableAstFactory.newAIdentifierStateDesignator;
import static org.intocps.maestro.ast.MableBuilder.newVariable;

public class ExpansionMableApiBuilder extends MablApiBuilder {

    final ScopeFmi2Api rootOfExpandScope;

    /**
     * Create a MablApiBuilder
     *
     * @param settings
     */
    public ExpansionMableApiBuilder(MablSettings settings, INode lastNodePriorToBuilderTakeOver) {
        super(settings);

        var existingScopes2currentRoot = buildExistingScopes(lastNodePriorToBuilderTakeOver);

        rootOfExpandScope = existingScopes2currentRoot.getFirst();
        rootScope = new ScopeFmi2Api(this, rootOfExpandScope, new ABasicBlockStm());

        mainErrorHandlingScope = rootScope.enterTry();
        this.dynamicScope = new DynamicActiveBuilderScope(mainErrorHandlingScope.getBody());

        if (settings.fmiErrorHandlingEnabled) {
            //create new variables
//            Function<String, IntVariableFmi2Api> f = (str) -> new IntVariableFmi2Api(null, null, null, null, newAIdentifierExp(str));
            for (FmiStatus s : FmiStatus.values()) {
                //if not existing then create
                var match = findDecleration(rootScope, s.name());
                if (match == null) {
                    //create the status as it was not found
                    ScopeFmi2Api scope = (ScopeFmi2Api) this.dynamicScope.getActiveScope();
                    fmiStatusVariables.put(s.getValue(), scope.store(s::name, s.getValue()));
                } else if (match.getValue() instanceof ALocalVariableStm local) {
                    //if exists then link to previous declaration
//                    fmiStatusVariables.put(s, f.apply(local.getDeclaration().getName().getText()));
                    fmiStatusVariables.put(s.getValue(), new IntVariableFmi2Api(match.getValue(), match.getKey(), this.dynamicScope, null,
                            newAIdentifierExp(local.getDeclaration().getName().getText())));
                }

            }
        }

        String status_varname = "status";


        var decl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, status_varname);
        if (decl == null) {
            globalFmiStatus = rootScope.store(status_varname, FmiStatus.FMI_OK.getValue());
        } else {
            globalFmiStatus = (IntVariableFmi2Api) createVariableExact(rootScope, newIntType(), null, decl.getName().getText(), true);
        }

        //lets find the existing loaded instances
        var declaredAndLoadedAssignments = MablToMablAPI.getAncestors(lastNodePriorToBuilderTakeOver, n -> n instanceof AAssigmentStm)
                .map(AAssigmentStm.class::cast).filter(n -> n.getExp() instanceof ALoadExp).toList();

        var loadedDefinitions = declaredAndLoadedAssignments.stream()
                .collect(Collectors.toMap(n -> n, AAssigmentStm::getTarget));

        Function<AAssigmentStm, String> loadedModuleName = n -> ((AStringLiteralExp) (((ALoadExp) n.getExp()).getArgs().get(0))).getValue();

        var c = loadedDefinitions.entrySet().stream()
                .filter(map -> (!loadedModuleName.apply(map.getKey()).startsWith("FMI")))
                .collect(Collectors.toMap(map -> loadedModuleName.apply(map.getKey()), map ->

                        new RuntimeModuleVariable(null, new ANameType(new LexIdentifier(loadedModuleName.apply(map.getKey()), null)), rootScope,
                                getDynamicScope(), this, map.getValue().clone(),
                                newAIdentifierExp(((AIdentifierStateDesignator) map.getValue()).getName().getText()))));
        fromExistingSpecInstanceCache.putAll(c);


        AVariableDeclaration loggerDecl = MablToMablAPI.findDeclaration(lastNodePriorToBuilderTakeOver, null, false, "logger");
        if (loggerDecl != null) {
            this.getMablToMablAPI().createExternalRuntimeLogger();
        }


        //reserve all previously names to avoid clashing with these
        MablToMablAPI.getPreviouslyUsedNamed(lastNodePriorToBuilderTakeOver).forEach(this.nameGenerator::addUsedIdentifier);


        resetDirty();

    }

    @Override
    protected void initializeGlobalStatusVariables() {


    }

    private static PStm findDecleration(SBlockStm block, String identifier) {
        var declaredStm = block.getBody().stream().filter(ALocalVariableStm.class::isInstance).map(ALocalVariableStm.class::cast)
                .map(ALocalVariableStm::getDeclaration)
                .filter(decl -> decl instanceof AVariableDeclaration declVar && declVar.getName().getText().equals(identifier)).findFirst();
        return declaredStm.map(aVariableDeclaration -> (PStm) aVariableDeclaration.parent()).orElse(null);
    }

    public static Pair<IMablScope, PStm> findDecleration(FmiBuilder.ScopeElement<PStm> scope, String identifier) {
        IMablScope declaringScope = (IMablScope) scope;
        PStm declaredStm = null;

        var scopeDeclaration = scope.getDeclaration();
        if (scopeDeclaration instanceof SBlockStm block) {
            declaredStm = findDecleration(block, identifier);
        } else if (scopeDeclaration instanceof AWhileStm whileStm) {
            if (whileStm.getBody() instanceof SBlockStm block) {
                declaredStm = findDecleration(block, identifier);
            }
        } else if (scopeDeclaration instanceof ATryStm tryStm) {
            if (tryStm.getBody() instanceof SBlockStm block) {
                declaredStm = findDecleration(block, identifier);
            }

        } else if (scopeDeclaration instanceof AIfStm tryStm) {
            if (tryStm.getThen() instanceof SBlockStm block) {
                declaredStm = findDecleration(block, identifier);
            }
            if (declaredStm == null) {
                if (tryStm.getElse() instanceof SBlockStm block) {
                    declaredStm = findDecleration(block, identifier);
                }
            }
        }

        if (declaredStm == null) {
            var parent = scope.parent();
            declaringScope = (IMablScope) parent;
            if (parent != null) {
                return findDecleration(parent, identifier);
            }
        }

        if (declaredStm != null) {
            return Pair.of(declaringScope, declaredStm);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Variable createVariable(IMablScope scope, PType type, PExp initialValue, String... prefixes) {
        String name = nameGenerator.getName(prefixes);
        return createVariableExact(scope, type, initialValue, name, false);
    }

    private Variable createVariableExact(IMablScope scope, PType type, PExp initialValue, String name, boolean external) {
        PStm var = newVariable(name, type, initialValue);
        if (!external) {
            scope.add(var);
        }
        this.externalScope.add(var);
        if (type instanceof ARealNumericPrimitiveType) {
            return new DoubleVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof ABooleanPrimitiveType) {
            return new BooleanVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AIntNumericPrimitiveType) {
            return new IntVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        } else if (type instanceof AStringPrimitiveType) {
            return new StringVariableFmi2Api(var, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
        }

        return new VariableFmi2Api(var, type, externalScope, dynamicScope, newAIdentifierStateDesignator(name), newAIdentifierExp(name));
    }

    /**
     * Returns the scope hierarchy from root to current scope for the node passed as argument
     *
     * @param currentNode
     * @return
     */
    private List<ScopeFmi2Api> buildExistingScopes(INode currentNode) {
        if (currentNode == null) {
            return new ArrayList<>();
        }
        SBlockStm block;
        List<Pair<SBlockStm, INode>> scope2Node = new ArrayList<>();
        while (currentNode!=null && (block = currentNode.getAncestor(SBlockStm.class)) != null) {
            scope2Node.add(Pair.of(block, currentNode));
            currentNode = block.parent();
        }


        List<ScopeFmi2Api> scopes = new ArrayList<>();
        IMablScope currentScope = null;
        for (var pair : scope2Node.reversed()) {
            scopes.add(new ScopeFmi2Api(this, currentScope, pair.getKey()));

        }

        return scopes;
    }


    @Override
    public ASimulationSpecificationCompilationUnit build() throws AnalysisException {
        var noParentScope = new ScopeFmi2Api(this, null, rootScope.getBlock());
        return internalBuild(noParentScope);
    }
}
