package org.intocps.maestro.framework.fmi2.api.mabl.variables;

import org.intocps.maestro.ast.node.AIdentifierExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.fmi3.Fmi3TypeEnum;
import org.intocps.maestro.framework.fmi2.api.mabl.MablApiBuilder;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.IMablScope;
import org.intocps.maestro.framework.fmi2.api.mabl.scoping.ScopeFmi2Api;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.intocps.maestro.ast.MableAstFactory.*;

class Buffers<FMI_TYPE> {

    public enum BufferTypes {IO, Share, Tentative}

    final MablApiBuilder builder;
    final String instance_name;

    public Buffers(MablApiBuilder builder, String instance_name, PStm instanceDeclStm, ScopeFmi2Api instanceDeclaredScope,
                   Map<FMI_TYPE, Integer> variableMaxSize) {
        this.builder = builder;
        this.instance_name = instance_name;
        this.instanceDeclStm = instanceDeclStm;
        this.instanceDeclaredScope = instanceDeclaredScope;
        this.variableMaxSize = variableMaxSize;
    }

    final PStm instanceDeclStm;
    final ScopeFmi2Api instanceDeclaredScope;

    final Map<FMI_TYPE, Integer> variableMaxSize;

    final Map<BufferTypes, Map<FMI_TYPE, ArrayVariableFmi2Api<Object>>> buffers = new HashMap<>();

    public ArrayVariableFmi2Api<Object> getBuffer(BufferTypes bufType, PType type, FMI_TYPE fmiType) {
        return getBuffer(buffers.computeIfAbsent(bufType, bufferType -> new HashMap<>()), fmiType, type, getPrefix(fmiType, bufType),
                bufType == BufferTypes.Share ? 0 : variableMaxSize.get(fmiType), this.instanceDeclaredScope);
    }

    private String getPrefix(FMI_TYPE fmiType, BufferTypes bufferType) {
        return fmiType.toString().replace("Type", "") + bufferType;
    }


    private ArrayVariableFmi2Api<Object> getBuffer(Map<FMI_TYPE, ArrayVariableFmi2Api<Object>> buffer, FMI_TYPE fmiType, PType type, String prefix, int size,
                                                   IMablScope scope) {
        ArrayVariableFmi2Api<Object> buf = buffer.get(fmiType);
        if (buf != null) {
            return buf;
        }

        ArrayVariableFmi2Api<Object> value = createBuffer(type, prefix, size, scope);
        buffer.put(fmiType, value);
        return value;


    }


    private ArrayVariableFmi2Api<Object> createBuffer(PType type, String prefix, int length, IMablScope scope) {


        //lets find a good place to store the buffer.
        String ioBufName = builder.getNameGenerator().getName(this.instance_name, prefix);

        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, length, null));

        instanceDeclaredScope.addAfter(instanceDeclStm, var);

        List<VariableFmi2Api<Object>> items = IntStream.range(0, length).mapToObj(i -> new VariableFmi2Api<>(var, type, scope, builder.getDynamicScope(),
                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());

        return new ArrayVariableFmi2Api<>(var, type, instanceDeclaredScope, builder.getDynamicScope(), newAIdentifierStateDesignator(newAIdentifier(ioBufName)),
                newAIdentifierExp(ioBufName), items);


    }

    public ArrayVariableFmi2Api<Object> growBuffer(BufferTypes bufType, ArrayVariableFmi2Api<Object> buffer, int increaseByCount, FMI_TYPE fmiType) {
        if (bufType != BufferTypes.Share) {
            throw new RuntimeException("can only grow shared buffers");
        }
        String ioBufName = ((AIdentifierExp) buffer.getReferenceExp()).getName().getText();

        int length = buffer.size() + increaseByCount;
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), buffer.type.clone(), length, null));

        buffer.getDeclaringStm().parent().replaceChild(buffer.getDeclaringStm(), var);
        // getDeclaredScope().addAfter(getDeclaringStm(), var);

        List<VariableFmi2Api<Object>> items = IntStream.range(buffer.size(), length).mapToObj(
                i -> new VariableFmi2Api<>(var, buffer.type.clone(), instanceDeclaredScope, builder.getDynamicScope(),
                        newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                        newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i))))).collect(Collectors.toList());

        //we can not replace these as some of them may be used and could potential have reference problems (they should not but just to be sure)
        items.addAll(0, buffer.items());

        ArrayVariableFmi2Api<Object> extendedVar = new ArrayVariableFmi2Api<>(var, buffer.type.clone(), instanceDeclaredScope, builder.getDynamicScope(),
                newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIdentifierExp(ioBufName), items);

        buffers.get(bufType).remove(fmiType);
        buffers.get(bufType).put(fmiType, extendedVar);

        return extendedVar;
    }


}
