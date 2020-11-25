package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.node.*;

import java.util.List;

public class TypeComparator {

    public synchronized boolean compatible(Class<? extends PType> to, PType from) {
        if (to == AUnknownType.class) {
            return true;
        }

        if (from instanceof AFunctionType) {
            return to.isAssignableFrom(((AFunctionType) from).getResult().getClass());
        }

        return to.isAssignableFrom(from.getClass());

    }

    public synchronized boolean compatible(PType to, PType from) {

        //same class
        if (to.equals(from)) {
            return true;
        }


        //one of them are unknown
        if (to instanceof AUnknownType || from instanceof AUnknownType) {
            return true;
        }


        if (to instanceof AFunctionType && from instanceof AFunctionType) {
            return compatible((AFunctionType) to, (AFunctionType) from);
        }

        if (from instanceof AFunctionType) {
            return compatible(to, ((AFunctionType) from).getResult());
        }

        // If they are both array types, then get the inner type
        if (to instanceof AArrayType && from instanceof AArrayType) {
            return compatible(((AArrayType) to).getType(), ((AArrayType) from).getType());
        }

        //references
        if (to instanceof AReferenceType && from instanceof AReferenceType) {
            return compatible(((AReferenceType) to).getType(), ((AReferenceType) from).getType());
        }

        //numbers
        if (to instanceof ARealNumericPrimitiveType && (from instanceof ARealNumericPrimitiveType || from instanceof AIntNumericPrimitiveType)) {
            return true;
        } else if (to instanceof AUIntNumericPrimitiveType && from instanceof AIntNumericPrimitiveType) {
            //allowed even through it could overflow the uint
            return true;
        } /*else if (to instanceof
        AIntNumericPrimitiveType &&
                (from instanceof AIntNumericPrimitiveType || from instanceof AUIntNumericPrimitiveType)) {
            return true;
        }*/

        return false;
    }


    public synchronized boolean compatible(List<? extends PType> to, List<? extends PType> from) {
        // TODO: If the last type in TO is ?, then it is varargs.
        if (to.size() > 0 && to.get(to.size() - 1) instanceof AUnknownType) {
            if (from.size() < to.size() - 1) {
                // The arguments prior to ? has to match in size.
                return false;
            }

            for (int i = 0; i < to.size() - 1; i++) {
                // The arguments prior to ? has to match in type.
                if (!compatible(to.get(i), from.get(i))) {
                    return false;
                }
            }
        } else {
            if (to.size() != from.size()) {
                return false;
            }

            for (int i = 0; i < to.size(); i++) {
                if (!compatible(to.get(i), from.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }


    public synchronized boolean compatible(AFunctionType to, AFunctionType from) {
        return compatible(to.getResult(), from.getResult()) && compatible(to.getParameters(), from.getParameters());
    }


}