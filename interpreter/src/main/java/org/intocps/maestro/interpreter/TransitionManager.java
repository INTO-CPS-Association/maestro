package org.intocps.maestro.interpreter;

import org.intocps.maestro.ast.AVariableDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TransitionManager implements ITransitionManager {
    final static Logger logger = LoggerFactory.getLogger(TransitionManager.class);
    private final ISpecificationProvider specificationProvider;

    public TransitionManager(ISpecificationProvider specificationProvider) {
        this.specificationProvider = specificationProvider;
    }

    @Override
    public ITTransitionInfo getTransferInfo(ATransferStm node, Context ctxt, String transferToName) throws AnalysisException {
        //lets check if we have something to transfer to
        ISpecificationProvider provider = getSpecificationProvider();
        if (provider != null) {
            logger.trace("Transfer look for specifications");
            Map<Path, ARootDocument> candidates = transferToName == null ? provider.get() : provider.get(transferToName);
            logger.trace("Transfer {} candidates found", candidates.size());
            for (Map.Entry<Path, ARootDocument> candidate : candidates.entrySet()) {

                Map<String, PType> externals = extractExternals(candidate.getValue());

                Context remappedContext = createRemappedTransferContext(ctxt, node);

                if (externals.keySet().stream().map(remappedContext::lookup).filter(Objects::nonNull).count() == externals.size()) {
                    logger.debug("Candidate {} selected. All externals are available: '{}'", candidate.getKey(),
                            String.join(", ", externals.keySet()));

                    //new root context
                    Context transferContext = new Context(null);
                    externals.keySet().forEach(name -> transferContext.put(new LexIdentifier(name, null), remappedContext.lookup(name)));
                    return new ITTransitionInfo() {
                        @Override
                        public String describe() {
                            return candidate.getKey().toString();
                        }

                        @Override
                        public Path workingDirectory() {
                            //we set the candidate parent file as the working directory
                            return candidate.getKey().getParent();
                        }

                        @Override
                        public Context getContext() {
                            return transferContext;
                        }

                        @Override
                        public ARootDocument getSpecification() {
                            return candidate.getValue();
                        }
                    };
                }

            }

        }
        return null;
    }

    /**
     * Create a new context which renamed variables according to the {@link ATransferStm} nodes found in the specification
     *
     * @param ctxt    the context to use as base
     * @param current the current node. This may be any sub node of a {@link  ASimulationSpecificationCompilationUnit} tree
     * @return a new context where the new names have been added with a mapping to the original variable value
     * @throws AnalysisException if analysis fails
     */
    Context createRemappedTransferContext(Context ctxt, INode current) throws AnalysisException {
        Map<String, String> remapping = new HashMap<>();
        current.getAncestor(ASimulationSpecificationCompilationUnit.class).apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseATransferAsStm(ATransferAsStm node) {
                if (node.parent() instanceof SBlockStm) {
                    //ok we have a block so the annotation can be attached to a statement, lets find it
                    SBlockStm block = (SBlockStm) node.parent();
                    for (int i = 0; i < block.getBody().size(); i++) {
                        if (block.getBody().get(i) == node) {
                            //ok we found this node. Lets see if there is something after this
                            if (i + 1 < block.getBody().size()) {
                                PStm nextStm = block.getBody().get(i + 1);
                                if (nextStm instanceof ALocalVariableStm) {
                                    remapping.put(((ALocalVariableStm) nextStm).getDeclaration().getName().getText(),
                                            node.getNames().get(0).getValue());
                                }
                            }
                        }
                    }
                }
            }
        });
        Context remappedContext = new Context(ctxt);
        remapping.forEach((source, remappedName) -> {
            logger.debug("Transfer remapping {} -> {}", source, remappedName);
            remappedContext.put(new LexIdentifier(remappedName, null), ctxt.lookup(source));
        });
        return remappedContext;
    }

    /**
     * Extract external variables for a given specification
     *
     * @param candidate the candidate to search
     * @return a mapping between external variable name to its type
     * @throws AnalysisException
     */
    private Map<String, PType> extractExternals(INode candidate) throws AnalysisException {
        Map<String, PType> externals = new HashMap<>();

        candidate.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseAVariableDeclaration(AVariableDeclaration node) {
                if (node.getExternal() != null && node.getExternal()) {
                    externals.put(node.getName().getText(), node.getType());
                }
            }
        });
        return externals;
    }


    @Override
    public void transfer(Interpreter interpreter, ITTransitionInfo info) throws AnalysisException {
        logger.debug("##########################################################################");
        logger.debug("# Transferring into new specification: {}", info.describe());
        logger.debug("##########################################################################");
        getSpecificationProvider().remove(info.getSpecification());
        info.getSpecification().apply(interpreter, info.getContext());
    }

    @Override
    public ISpecificationProvider getSpecificationProvider() {
        return specificationProvider;
    }
}

