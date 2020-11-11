package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.node.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypeComparator {

    public synchronized boolean compatible(PType to, PType from) {
        // If they are both array types, then get the inner type
        if (to instanceof AArrayType && from instanceof AArrayType) {
            to = ((AArrayType) to).getType();
            from = ((AArrayType) from).getType();
        }
        //same class
        if (to.equals(from)) {
            return true;
        }


        //one of them are unknown
        if (to instanceof AUnknownType || from instanceof AUnknownType) {
            return true;
        }


        //TODO expand this to a proper structure

        //numbers
        Set<Class<? extends SPrimitiveTypeBase>> ints =
                Stream.of(ABooleanPrimitiveType.class, ABooleanPrimitiveType.class, AIntNumericPrimitiveType.class, AUIntNumericPrimitiveType.class)
                        .collect(Collectors.toSet());

        if (ints.contains(this.getBasicType(to).getClass()) &&
                (ints.contains(getBasicType(from).getClass()) || getBasicType(from) instanceof ARealNumericPrimitiveType)) {
            //might truncate
            return true;
        }


        if (to instanceof ARealNumericPrimitiveType && (ints.contains(from)) || from instanceof ARealNumericPrimitiveType) {
            return true;
        }

        //other

        if (from instanceof AFunctionType && to instanceof AFunctionType) {
            return compatible((AFunctionType) to, (AFunctionType) from);
        }


        return false;
    }

    private PType getBasicType(PType type) {
        if (type instanceof AArrayType) {
            return ((AArrayType) type).getType();
        } else {
            return type;
        }
    }

    public synchronized boolean compatible(List<? extends PType> to, List<? extends PType> from) {
        if (to.size() != from.size()) {
            return false;
        }

        for (int i = 0; i < to.size(); i++) {
            if (!compatible(to.get(i), from.get(i))) {
                return false;
            }
        }
        return true;
    }


    public synchronized boolean compatible(AFunctionType to, AFunctionType from) {
        return compatible(to.getResult(), from.getResult()) && compatible(to.getParameters(), from.getParameters());
    }

}