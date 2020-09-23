package org.intocps.maestro.plugin;

import org.intocps.maestro.ast.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

class MableBuilder {
    public static PStm newVariable(LexIdentifier name, PType type, PExp value) {
        return newVariable(name.getText(), type, value);
    }

    public static PStm newVariable(String name, PType type, PExp value) {
        return newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), type.clone(), newAExpInitializer(value.clone())));
    }

    public static PStm newVariable(LexIdentifier name, PType type, List<PExp> values) {
        return newVariable(name.getText(), type, values);

    }

    public static PStm newVariable(String name, PType type, List<PExp> values) {
        return newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), newAArrayType(type.clone(), values.size()),
                newAArrayInitializer(values.stream().map(PExp::clone).collect(Collectors.toList()))));

    }

    public static PStm newVariable(String name, PType type, int size) {
        return newALocalVariableStm(newAVariableDeclaration(newAIdentifier(name), newAArrayType(type.clone(), size), null));

    }

    public static PExp call(String object, String method, PExp... args) {
        return call(object, method, args == null ? null : Arrays.asList(args));
    }

    public static PExp call(String object, String method, List<PExp> args) {
        return call(newAIdentifierExp(object), method, args);
    }

    public static PExp call(PExp object, String method, List<PExp> args) {
        return newACallExp(object, newAIdentifier(method), args);
    }

    public static PExp call(PExp object, String method, PExp... args) {
        return newACallExp(object, newAIdentifier(method), Arrays.asList(args));
    }

    public static PExp call(String object, String method) {
        return call(object, method, (List<PExp>) null);
    }

    public static PExp call(String method, PExp... args) {
        return call(method, args == null ? null : Arrays.asList(args));
    }

    public static PExp call(String method, List<PExp> args) {
        return newACallExp(newAIdentifier(method), args);
    }

    public static PExp call(String method) {
        return call(method, (List<PExp>) null);
    }

    public static PExp arrayGet(String name, PExp index) {
        return newAArrayIndexExp(newAIdentifierExp(name), Collections.singletonList(index));
    }

    public static PExp arrayGet(PExp name, PExp index) {
        return newAArrayIndexExp(name.clone(), Collections.singletonList(index.clone()));
    }

    public static PExp arrayGet(String name, int index) {
        return arrayGet(name, newAIntLiteralExp(index));
    }


    public static PExp arrayGet(LexIdentifier name, AIntLiteralExp index) {
        return arrayGet(name.getText(), index);
    }

    public static PExp arrayGet(LexIdentifier name, int index) {
        return arrayGet(name.getText(), index);
    }

    public static PStm arraySet(String name, Integer index, PExp value) {
        PStateDesignator stateDesignator = newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(name)), newAIntLiteralExp(index));
        return new AAssigmentStm(stateDesignator, value);
    }
}
