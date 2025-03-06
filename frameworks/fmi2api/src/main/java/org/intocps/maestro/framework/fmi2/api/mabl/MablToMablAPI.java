package org.intocps.maestro.framework.fmi2.api.mabl;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.RuntimeModuleVariable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.newAIdentifierExp;
import static org.intocps.maestro.ast.MableAstFactory.newANameType;

public class MablToMablAPI {

    private final MablApiBuilder mablApiBuilder;
    private BooleanBuilderFmi2Api booleanBuilderApi;
    private DataWriter dataWriter;
    private VariableStep variableStep;
    private MathBuilderFmi2Api mathBuilderFmi2Api;
    private LoggerFmi2Api runtimeLogger;

    public MablToMablAPI(MablApiBuilder mablApiBuilder) {
        this.mablApiBuilder = mablApiBuilder;
    }

    public static Stream<INode> getAncestors(INode node, Predicate<INode> filter) {
        INode parent = node.parent();
        if (parent == null) {
            return Stream.empty();
        } else if (parent instanceof SBlockStm) {
            SBlockStm block = (SBlockStm) parent;
            INode indexedNode = node;
            while (indexedNode != null && !(indexedNode instanceof PStm)) {
                indexedNode = indexedNode.parent();
            }

            int pos = block.getBody().indexOf(indexedNode);
            if (pos < 0) {
                return getAncestors(parent, filter);
            }

            return Stream.concat(block.getBody().stream().limit(pos).filter(filter), getAncestors(parent, filter));
        } else if (filter.test(parent)) {
            return Stream.concat(Stream.of(parent), getAncestors(parent, filter));
        } else {
            return getAncestors(parent, filter);
        }

    }


    public static AVariableDeclaration findDeclaration(INode node, PStm currentStm, boolean caseSensitive, String name) {

        if (currentStm == null) {
            currentStm = node.getAncestor(PStm.class);
        }

        //since the getAncestor function acts as an identity function when the current node matches the requested type it is nessesary to call parent
        INode tmp = (currentStm instanceof SBlockStm ? currentStm.parent() : currentStm);
        if (tmp == null) {
            return null;
        }
        SBlockStm block = tmp.getAncestor(SBlockStm.class);

        if (block == null) {
            return null;
        }

        while (currentStm.parent() != null && currentStm.parent() instanceof PStm && currentStm.parent() != block) {
            currentStm = (PStm) currentStm.parent();
        }

        if (currentStm.parent() == block) {
            //only search from before this statement

            for (int i = block.getBody().indexOf(currentStm) - 1; i >= 0 && i < block.getBody().size(); i--) {
                PStm s = block.getBody().get(i);
                if (s instanceof ALocalVariableStm) {
                    String declName = ((ALocalVariableStm) s).getDeclaration().getName().getText();
                    //match found is does shadow a name
                    if ((caseSensitive && declName.equals(name)) || (!caseSensitive && declName.equalsIgnoreCase(name))) {
                        return ((ALocalVariableStm) s).getDeclaration();
                    }
                }
            }
            return findDeclaration(node, block, caseSensitive, name);
        } else {
            if (currentStm.parent() != null && currentStm.parent() instanceof PStm) {
                return findDeclaration(node, (PStm) currentStm.parent(), caseSensitive, name);
            }
        }

        return null;
    }

    /**
     * Collect all names used for variables prior to the definition of the current node
     *
     * @param node the starting point in the tree
     * @return a set of names
     */
    public static Set<String> getPreviouslyUsedNamed(INode node) {
        return getPreviouslyUsedNamed(node, null);
    }

    /**
     * Collect all names used for variables prior to the definition of the current node
     *
     * @param node       the starting point in the tree
     * @param currentStm the current statement
     * @return a set of names
     */
    private static Set<String> getPreviouslyUsedNamed(INode node, PStm currentStm) {
        Set<String> names = new HashSet<>();

        if (currentStm == null) {
            currentStm = node.getAncestor(PStm.class);
        }

        //since the getAncestor function acts as an identity function when the current node matches the requested type it is nessesary to call parent
        INode tmp = (currentStm instanceof SBlockStm ? currentStm.parent() : currentStm);
        if (tmp == null) {
            return names;
        }
        SBlockStm block = tmp.getAncestor(SBlockStm.class);

        if (block == null) {
            return names;
        }

        while (currentStm.parent() != null && currentStm.parent() instanceof PStm && currentStm.parent() != block) {
            currentStm = (PStm) currentStm.parent();
        }

        if (currentStm.parent() == block) {
            //only search from before this statement

            for (int i = block.getBody().indexOf(currentStm) - 1; i >= 0 && i < block.getBody().size(); i--) {
                PStm s = block.getBody().get(i);
                if (s instanceof ALocalVariableStm) {
                    String declName = ((ALocalVariableStm) s).getDeclaration().getName().getText();
                    names.add(declName);
                }
            }
            names.addAll(getPreviouslyUsedNamed(node, block));
        } else {
            if (currentStm.parent() != null && currentStm.parent() instanceof PStm) {
                names.addAll(getPreviouslyUsedNamed(node, (PStm) currentStm.parent()));
            }
        }

        return names;
    }

    public void createExternalRuntimeLogger() {
        String name = "logger";
        RuntimeModuleVariable runtimeModule =
                new RuntimeModuleVariable(null, newANameType("Logger"), null, mablApiBuilder.getDynamicScope(), mablApiBuilder, null,
                        newAIdentifierExp(name), true);
        if (this.runtimeLogger == null) {
            this.runtimeLogger = new LoggerFmi2Api(this.mablApiBuilder, runtimeModule);
        }
        mablApiBuilder.setRuntimeLogger(runtimeLogger);
        mablApiBuilder.addExternalLoadedModuleIdentifier(name);
    }

    public VariableStep getVariableStep() {
        if (this.variableStep == null) {
            this.variableStep = new VariableStep(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.variableStep;
    }

    public DataWriter getDataWriter() {
        if (this.dataWriter == null) {
            this.dataWriter = new DataWriter(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.dataWriter;
    }

    public BooleanBuilderFmi2Api getBooleanBuilder() {
        if (this.booleanBuilderApi == null) {
            this.booleanBuilderApi = new BooleanBuilderFmi2Api(this.mablApiBuilder.dynamicScope, this.mablApiBuilder);
        }
        return this.booleanBuilderApi;
    }

    public MathBuilderFmi2Api getMathBuilder() {
        if (this.mathBuilderFmi2Api == null) {
            this.mathBuilderFmi2Api = new MathBuilderFmi2Api(this.mablApiBuilder.dynamicScope, this.mablApiBuilder,
                    new AIdentifierExp(new LexIdentifier("math", null)));
        }
        return this.mathBuilderFmi2Api;
    }
}
