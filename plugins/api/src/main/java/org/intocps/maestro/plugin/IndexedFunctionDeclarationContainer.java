package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.AFunctionDeclaration;
import org.intocps.maestro.ast.node.AFormalParameter;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.FmiBuilder;

import java.util.List;
import java.util.Vector;

import static org.intocps.maestro.ast.MableAstFactory.*;

public class IndexedFunctionDeclarationContainer<T> {
    public AFunctionDeclaration getDecl() {
        return decl;
    }

    private final AFunctionDeclaration decl;
    private final List<T> indexes;

    private IndexedFunctionDeclarationContainer(AFunctionDeclaration decl, List<T> indexes) {
        this.decl = decl;
        this.indexes = indexes;
    }

    static <T> Builder<T> newBuilder(String name, Class<T> cls) {
        return new Builder<>(name);
    }

    public FmiBuilder.Variable<PStm, ?> getArgumentValue(List<FmiBuilder.Variable<PStm, ? extends Object>> formalArguments, T index) {

        int idx = indexes.indexOf(index);
        if (idx == -1) {
            return null;
        } else {
            return formalArguments.get(idx);
        }

    }

    static class Builder<T> {
        private final String name;

        public Builder(String name) {
            this.name = name;
        }

        final List<AFormalParameter> parameters = new Vector<>();
        final List<T> indexes = new Vector<>();

        public Builder<T> addArg(T key, String name, PType type) {
            indexes.add(key);
            parameters.add(newAFormalParameter(type, newAIdentifier(name)));
            return this;
        }

        public IndexedFunctionDeclarationContainer<T> build() {
            return new IndexedFunctionDeclarationContainer<>(newAFunctionDeclaration(newAIdentifier(name), parameters, newAVoidType()), indexes);
        }
    }

}
