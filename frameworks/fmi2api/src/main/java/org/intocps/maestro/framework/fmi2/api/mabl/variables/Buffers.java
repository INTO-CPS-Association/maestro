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

    public static enum BufferTypes {IO, Share}

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

    final Map<BufferTypes, Map<PType, ArrayVariableFmi2Api<Object>>> buffers = new HashMap<>();

    public ArrayVariableFmi2Api<Object> getBuffer(BufferTypes bufType, PType type, FMI_TYPE fmiType) {
        return getBuffer(buffers.computeIfAbsent(bufType, bufferType -> new HashMap<>()), type,
                type.toString().toUpperCase() + fmiType.toString().replace("Type", ""),
                bufType == BufferTypes.Share ? 0 : variableMaxSize.get(fmiType),
                this.instanceDeclaredScope);
    }

    public ArrayVariableFmi2Api<Object> getBuffer(BufferTypes bufType, PType type, FMI_TYPE fmiType, Integer size) {
        return getBuffer(buffers.computeIfAbsent(bufType, bufferType -> new HashMap<>()), type,
                type.toString().toUpperCase() + fmiType.toString().replace("Type", ""),
                size,
                this.instanceDeclaredScope);
    }

    private ArrayVariableFmi2Api<Object> getBuffer(Map<PType, ArrayVariableFmi2Api<Object>> buffer, PType type, String prefix, int size,
                                                   IMablScope scope) {
        Optional<PType> first = buffer.keySet().stream().filter(x -> x.toString().equals(type.toString())).findFirst();
        if (first.isEmpty()) {
            ArrayVariableFmi2Api<Object> value = createBuffer(type, prefix, size, scope);
            buffer.put(type, value);
            return value;

        } else {
            return buffer.get(first.get());
        }
    }

    public void setSharedBuffer(BufferTypes bufType, ArrayVariableFmi2Api<Object> newBuf, PType type) {
        this.buffers.get(bufType).entrySet().removeIf(x -> x.getKey().toString().equals(type.toString()));
        this.buffers.get(bufType).put(type, newBuf);

    }

    private ArrayVariableFmi2Api<Object> createBuffer( PType type, String prefix, int length, IMablScope scope) {


        //lets find a good place to store the buffer.
        String ioBufName = builder.getNameGenerator().getName(this.instance_name, type + "", prefix);

        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), type, length, null));

        instanceDeclaredScope.addAfter(instanceDeclStm, var);

        List<VariableFmi2Api<Object>> items = IntStream.range(0, length).mapToObj(
                        i -> new VariableFmi2Api<>(var, type, scope, builder.getDynamicScope(),
                                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                                newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i)))))
                .collect(Collectors.toList());

       return new ArrayVariableFmi2Api<>(var, type, instanceDeclaredScope, builder.getDynamicScope(),
                newAIdentifierStateDesignator(newAIdentifier(ioBufName)),
                newAIdentifierExp(ioBufName), items);


    }

    public ArrayVariableFmi2Api<Object> growBuffer(BufferTypes bufType,ArrayVariableFmi2Api<Object> buffer, int increaseByCount) {
        if (bufType != BufferTypes.Share) {
            throw new RuntimeException("can only grow shared buffers");
        }
        String ioBufName = ((AIdentifierExp) buffer.getReferenceExp()).getName().getText();

        int length = buffer.size() + increaseByCount;
        PStm var = newALocalVariableStm(newAVariableDeclaration(newAIdentifier(ioBufName), buffer.type, length, null));

        buffer.getDeclaringStm().parent().replaceChild(buffer.getDeclaringStm(), var);
        // getDeclaredScope().addAfter(getDeclaringStm(), var);

        List<VariableFmi2Api<Object>> items = IntStream.range(buffer.size(), length).mapToObj(
                        i -> new VariableFmi2Api<>(var, buffer.type, instanceDeclaredScope, builder.getDynamicScope(),
                                newAArayStateDesignator(newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIntLiteralExp(i)),
                                newAArrayIndexExp(newAIdentifierExp(ioBufName), Collections.singletonList(newAIntLiteralExp(i)))))
                .collect(Collectors.toList());

        //we can not replace these as some of them may be used and could potential have reference problems (they should not but just to be sure)
        items.addAll(0, buffer.items());

        ArrayVariableFmi2Api<Object> extendedVar = new ArrayVariableFmi2Api<>(var, buffer.type, instanceDeclaredScope, builder.getDynamicScope(),
                newAIdentifierStateDesignator(newAIdentifier(ioBufName)), newAIdentifierExp(ioBufName), items);
        setSharedBuffer(BufferTypes.Share, extendedVar, buffer.getType());
        return extendedVar;
    }


}
