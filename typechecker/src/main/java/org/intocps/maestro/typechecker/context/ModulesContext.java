package org.intocps.maestro.typechecker.context;

import org.intocps.maestro.ast.AModuleDeclaration;
import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.PDeclaration;
import org.intocps.maestro.typechecker.DeclarationList;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModulesContext extends Context {
    private final Map<LexIdentifier, Map.Entry<AModuleDeclaration, DeclarationList>> modules;


    public ModulesContext(List<? extends AModuleDeclaration> modules, Context outerContext) {
        super(outerContext);
        this.modules = modules.stream().filter(distinctByKey(AModuleDeclaration::getName))
                .collect(Collectors.toMap(AModuleDeclaration::getName, m -> Map.entry(m, new DeclarationList(m.getFunctions()))));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @Override
    public PDeclaration findDeclaration(LexIdentifier module, LexIdentifier name) {
        if (modules.containsKey(module)) {
            PDeclaration decl = modules.get(module).getValue().findDeclaration(name);
            if (decl != null) {
                return decl;
            }
        }
        return super.findDeclaration(module, name);
    }

    @Override
    public PDeclaration findDeclaration(LexIdentifier name) {
        if (modules.containsKey(name)) {
            return modules.get(name).getKey();
        }
        return super.findDeclaration(name);
    }

}
