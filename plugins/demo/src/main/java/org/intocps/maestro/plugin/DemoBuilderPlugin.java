package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.Framework;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.framework.core.ISimulationEnvironment;
import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.DynamicActiveBuilderScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.intocps.maestro.ast.MableAstFactory.newAIdentifier;

@SimulationFramework(framework = Framework.FMI2)
public class DemoBuilderPlugin extends BasicMaestroExpansionPlugin {
    final AFunctionDeclaration f1 =
            getFunctionDeclarationBuilder("add").addArg(AIntNumericPrimitiveType.class, "a").addArg(AIntNumericPrimitiveType.class, "b").build();

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getVersion() {
        return "0.0.0";
    }

    public Set<AFunctionDeclaration> getDeclaredUnfoldFunctions() {
        return Stream.of(f1).collect(Collectors.toSet());
    }


    @Override
    public ConfigOption getConfigRequirement() {
        return ConfigOption.NotRequired;
    }

    @Override
    public <R> RuntimeConfigAddition<R> expandWithRuntimeAddition(AFunctionDeclaration declaredFunction,
            Fmi2Builder<PStm, ASimulationSpecificationCompilationUnit, PExp, ?> builder, List<Fmi2Builder.Variable<PStm, ?>> formalArguments,
            IPluginConfiguration config, ISimulationEnvironment env, IErrorReporter errorReporter) throws ExpandException {


        if (builder instanceof MablApiBuilder) {
            MablApiBuilder mb = (MablApiBuilder) builder;
            DynamicActiveBuilderScope ds = mb.getDynamicScope();

            //            NumericExpressionValueFmi2Api addition =
            //                    ((IntVariableFmi2Api) formalArguments.get(0)).toMath().addition(((IntVariableFmi2Api) formalArguments.get(0)));
            //why cant i store this result!
            //also the numeric stuff if too fragile

            //            ds.enterIf(addition.greaterEqualTo(new IntExpressionValue(0))).enterThen();
            ds.store("true", 1);
            //            ds.leave();


        }
        return super.expandWithRuntimeAddition(declaredFunction, builder, formalArguments, config, env, errorReporter);
    }

    @Override
    public AImportedModuleCompilationUnit getDeclaredImportUnit() {
        AImportedModuleCompilationUnit unit = new AImportedModuleCompilationUnit();
        unit.setImports(new Vector<>());
        AModuleDeclaration module = new AModuleDeclaration();
        module.setName(newAIdentifier(getName()));
        module.setFunctions(new ArrayList<>(getDeclaredUnfoldFunctions()));
        unit.setModule(module);
        return unit;
    }


}
