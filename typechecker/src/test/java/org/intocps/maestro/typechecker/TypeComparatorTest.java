package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.node.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class TypeComparatorTest {
    TypeComparator tc = new TypeComparator();

    public static PType mkFunc(PType ret, PType... args) {
        AFunctionType functionType = new AFunctionType();
        functionType.setResult(ret.clone());
        functionType.setParameters(Arrays.asList(args).stream().map(t -> (PType) t.clone()).collect(Collectors.toList()));
        return functionType;
    }

    public static PType mkFunc(Class<? extends PType> ret,
            Class<? extends PType>... argsClz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        PType[] atypes = new PType[argsClz == null ? 0 : argsClz.length];
        for (int i = 0; i < atypes.length; i++) {
            atypes[i] = argsClz[i].getDeclaredConstructor().newInstance();
        }

        return mkFunc(ret.getDeclaredConstructor().newInstance(), atypes);
    }

    public static PType mkArray(
            Class<? extends PType> type) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        AArrayType array = new AArrayType();
        array.setType(type.getDeclaredConstructor().newInstance());
        return array;
    }

    @Test
    public void testBasicTypes() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        check(ARealNumericPrimitiveType.class, ARealNumericPrimitiveType.class);
        check(ARealNumericPrimitiveType.class, AIntNumericPrimitiveType.class);
        checkNot(ARealNumericPrimitiveType.class, ABooleanPrimitiveType.class);
        checkNot(ARealNumericPrimitiveType.class, AStringPrimitiveType.class);

        check(AIntNumericPrimitiveType.class, AIntNumericPrimitiveType.class);
        checkNot(AIntNumericPrimitiveType.class, ARealNumericPrimitiveType.class);
        checkNot(AIntNumericPrimitiveType.class, ABooleanPrimitiveType.class);
        checkNot(AIntNumericPrimitiveType.class, AStringPrimitiveType.class);

        check(ABooleanPrimitiveType.class, ABooleanPrimitiveType.class);
        checkNot(ABooleanPrimitiveType.class, ARealNumericPrimitiveType.class);
        checkNot(ABooleanPrimitiveType.class, AIntNumericPrimitiveType.class);
        checkNot(ABooleanPrimitiveType.class, AStringPrimitiveType.class);

        check(AStringPrimitiveType.class, AStringPrimitiveType.class);
        checkNot(AStringPrimitiveType.class, ARealNumericPrimitiveType.class);
        checkNot(AStringPrimitiveType.class, AIntNumericPrimitiveType.class);
        checkNot(AStringPrimitiveType.class, ABooleanPrimitiveType.class);


        check(AUIntNumericPrimitiveType.class, AUIntNumericPrimitiveType.class);
        check(AUIntNumericPrimitiveType.class, AIntNumericPrimitiveType.class);
        checkNot(AUIntNumericPrimitiveType.class, ARealNumericPrimitiveType.class);
        checkNot(AUIntNumericPrimitiveType.class, ABooleanPrimitiveType.class);
        checkNot(AUIntNumericPrimitiveType.class, AStringPrimitiveType.class);
    }

    @Test
    public void testFunctionToBasic() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        check(ARealNumericPrimitiveType.class, mkFunc(ARealNumericPrimitiveType.class));
        check(ARealNumericPrimitiveType.class, mkFunc(AIntNumericPrimitiveType.class));
        checkNot(ARealNumericPrimitiveType.class, mkFunc(ABooleanPrimitiveType.class));
        checkNot(ARealNumericPrimitiveType.class, mkFunc(AStringPrimitiveType.class));

        check(AIntNumericPrimitiveType.class, mkFunc(AIntNumericPrimitiveType.class));
        checkNot(AIntNumericPrimitiveType.class, mkFunc(ARealNumericPrimitiveType.class));
        checkNot(AIntNumericPrimitiveType.class, mkFunc(ABooleanPrimitiveType.class));
        checkNot(AIntNumericPrimitiveType.class, mkFunc(AStringPrimitiveType.class));

        check(ABooleanPrimitiveType.class, mkFunc(ABooleanPrimitiveType.class));
        checkNot(ABooleanPrimitiveType.class, mkFunc(ARealNumericPrimitiveType.class));
        checkNot(ABooleanPrimitiveType.class, mkFunc(AIntNumericPrimitiveType.class));
        checkNot(ABooleanPrimitiveType.class, mkFunc(AStringPrimitiveType.class));

        check(AStringPrimitiveType.class, mkFunc(AStringPrimitiveType.class));
        checkNot(AStringPrimitiveType.class, mkFunc(ARealNumericPrimitiveType.class));
        checkNot(AStringPrimitiveType.class, mkFunc(AIntNumericPrimitiveType.class));
        checkNot(AStringPrimitiveType.class, mkFunc(ABooleanPrimitiveType.class));

        check(AUIntNumericPrimitiveType.class, mkFunc(AUIntNumericPrimitiveType.class));
        check(AUIntNumericPrimitiveType.class, mkFunc(AIntNumericPrimitiveType.class));
        checkNot(AUIntNumericPrimitiveType.class, mkFunc(ARealNumericPrimitiveType.class));
        checkNot(AUIntNumericPrimitiveType.class, mkFunc(ABooleanPrimitiveType.class));
        checkNot(AUIntNumericPrimitiveType.class, mkFunc(AStringPrimitiveType.class));
    }

    @Test
    public void testArrays() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        check(mkArray(ARealNumericPrimitiveType.class), mkArray(ARealNumericPrimitiveType.class));
        check(mkArray(ARealNumericPrimitiveType.class), mkArray(AIntNumericPrimitiveType.class));
        checkNot(mkArray(ARealNumericPrimitiveType.class), mkArray(ABooleanPrimitiveType.class));
        checkNot(mkArray(ARealNumericPrimitiveType.class), mkArray(AStringPrimitiveType.class));

        check(mkArray(AIntNumericPrimitiveType.class), mkArray(AIntNumericPrimitiveType.class));
        checkNot(mkArray(AIntNumericPrimitiveType.class), mkArray(ARealNumericPrimitiveType.class));
        checkNot(mkArray(AIntNumericPrimitiveType.class), mkArray(ABooleanPrimitiveType.class));
        checkNot(mkArray(AIntNumericPrimitiveType.class), mkArray(AStringPrimitiveType.class));

        check(mkArray(ABooleanPrimitiveType.class), mkArray(ABooleanPrimitiveType.class));
        checkNot(mkArray(ABooleanPrimitiveType.class), mkArray(ARealNumericPrimitiveType.class));
        checkNot(mkArray(ABooleanPrimitiveType.class), mkArray(AIntNumericPrimitiveType.class));
        checkNot(mkArray(ABooleanPrimitiveType.class), mkArray(AStringPrimitiveType.class));

        check(mkArray(AStringPrimitiveType.class), mkArray(AStringPrimitiveType.class));
        checkNot(mkArray(AStringPrimitiveType.class), mkArray(ARealNumericPrimitiveType.class));
        checkNot(mkArray(AStringPrimitiveType.class), mkArray(AIntNumericPrimitiveType.class));
        checkNot(mkArray(AStringPrimitiveType.class), mkArray(ABooleanPrimitiveType.class));

        check(mkArray(AUIntNumericPrimitiveType.class), mkArray(AUIntNumericPrimitiveType.class));
        check(mkArray(AUIntNumericPrimitiveType.class), mkArray(AIntNumericPrimitiveType.class));
        checkNot(mkArray(AUIntNumericPrimitiveType.class), mkArray(ARealNumericPrimitiveType.class));
        checkNot(mkArray(AUIntNumericPrimitiveType.class), mkArray(ABooleanPrimitiveType.class));
        checkNot(mkArray(AUIntNumericPrimitiveType.class), mkArray(AStringPrimitiveType.class));
    }


    public void check(Class<? extends PType> toClz,
            Class<? extends PType> fromClz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        PType to = toClz.getDeclaredConstructor().newInstance();
        PType from = fromClz.getDeclaredConstructor().newInstance();
        check(to, from);
    }

    public void check(Class<? extends PType> toClz,
            PType from) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        PType to = toClz.getDeclaredConstructor().newInstance();
        check(to, from);
    }

    public void check(PType to, PType from) {

        Assertions.assertTrue(tc.compatible(to, from), from + " should be compatible with " + to);
    }

    public void checkNot(Class<? extends PType> toClz,
            Class<? extends PType> fromClz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        PType to = toClz.getDeclaredConstructor().newInstance();
        PType from = fromClz.getDeclaredConstructor().newInstance();

        Assertions.assertFalse(tc.compatible(to, from), fromClz.getSimpleName() + " should NOT be compatible with " + toClz.getSimpleName());
    }

    public void checkNot(Class<? extends PType> toClz,
            PType from) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        PType to = toClz.getDeclaredConstructor().newInstance();
        checkNot(to, from);
    }

    public void checkNot(PType to, PType from) {

        Assertions.assertFalse(tc.compatible(to, from), from + " should NOT be compatible with " + to);
    }
}
