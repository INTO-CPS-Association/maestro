package org.intocps.maestro;

import org.intocps.maestro.ast.LexLocation;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.testutils.FmuTestModelRepository;
import org.intocps.maestro.testutils.TestFmu;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

final class TestFmuCollector {
    private TestFmuCollector() {
    }

    static EnumSet<TestFmu> collectFmus(INode spec, boolean updatePath) throws AnalysisException {
        EnumSet<TestFmu> models = EnumSet.noneOf(TestFmu.class);
        if (spec == null) {
            return models;
        }
        spec.apply(new DepthFirstAnalysisAdaptor() {
            @Override
            public void caseALoadExp(ALoadExp node) {
                if (node.getArgs().size() != 3 || !(node.getArgs().get(0) instanceof AStringLiteralExp) ||
                        !(node.getArgs().get(2) instanceof AStringLiteralExp)) {
                    return;
                }
                String type = ((AStringLiteralExp) node.getArgs().get(0)).getValue();
                if (!type.equals("FMI2") && !type.equals("FMI3")) {
                    return;
                }
                String path = ((AStringLiteralExp) node.getArgs().get(2)).getValue().replace('\\', '/');
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                if (fileName.startsWith("file:")) {
                    fileName = fileName.substring("file:".length());
                }
                TestFmu model = TestFmu.fromFileName(fileName).orElse(null);
                if (model == null) {
                    return;
                }
                models.add(model);
                if (updatePath) {
                    List<PExp> arguments = new ArrayList<>(node.getArgs());
                    arguments.set(2, new AStringLiteralExp(new LexLocation("unknown", 0, 0),
                            FmuTestModelRepository.cachePath(model).toString().replace('\\', '/')));
                    node.setArgs(arguments);
                }
            }
        });
        return models;
    }
}