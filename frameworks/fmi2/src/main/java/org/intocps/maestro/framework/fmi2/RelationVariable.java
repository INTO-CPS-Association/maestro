package org.intocps.maestro.framework.fmi2;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3Causality;
import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
import org.intocps.maestro.fmi.fmi3.Fmi3TypeEnum;

import java.util.Optional;

import static org.intocps.maestro.ast.MableAstFactory.*;


public class RelationVariable<SCALAR_TYPE> implements org.intocps.maestro.framework.core.RelationVariable {
    private final SCALAR_TYPE scalarVariable;
    final String name;
    // instance is necessary because:
    // If you look up the relations for FMU Component A,
    // and there is a dependency from FMU Component B Input as Source to FMU Component A as Target.
    // Then it is only possible to figure out that Source actually belongs to FMU Component B if instance is part of Source.
    public final LexIdentifier instance;
    private final long valueReference;
    private final Object type;

    public RelationVariable(SCALAR_TYPE scalarVariable, String name, LexIdentifier instance, long valueReference, Type type) {
        this.scalarVariable = scalarVariable;
        this.name = name;
        this.instance = instance;
        this.valueReference = valueReference;
        this.type = type;
    }

    public RelationVariable(Fmi2ModelDescription.ScalarVariable scalarVariable, String name, LexIdentifier instance) {
        this((SCALAR_TYPE) scalarVariable, name, instance, scalarVariable.getValueReference(), new RelationFmi2Type(scalarVariable.getType()));

    }

    public RelationVariable(Fmi3ModelDescription.Fmi3ScalarVariable scalarVariable, String name, LexIdentifier instance) {
        this((SCALAR_TYPE) scalarVariable, name, instance, scalarVariable.getVariable().getValueReferenceAsLong(),
                new RelationFmi3Type(scalarVariable.getVariable().getTypeIdentifier()));

    }

    @Override
    public LexIdentifier getInstance() {
        return this.instance;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T> T getScalarVariable(Class<T> clz) {
        if (clz.isAssignableFrom(scalarVariable.getClass())) {
            return clz.cast(scalarVariable);
        }
        return null;
    }

    @Override
    public boolean has(Object scalarAttributeType) {
        if (scalarAttributeType instanceof Fmi2ModelDescription.Causality) {
            return has((Fmi2ModelDescription.Causality) scalarAttributeType);
        } else if (scalarAttributeType instanceof Fmi3Causality) {
            return has((Fmi3Causality) scalarAttributeType);
        }
        return false;
    }

    @Override
    public Type<Object, Object> getType() {
        return (Type<Object, Object>) type;
    }

    public RelationFmi2Type getType2() {
        if (this.type instanceof RelationFmi2Type) {
            return (RelationFmi2Type) this.type;
        } else if (this.type instanceof RelationFmi3Type) {
            //todo convert if possible
        }
        return null;
    }

    public RelationFmi3Type getType3() {
        if (this.type instanceof RelationFmi2Type) {
            //todo convert if possible
        } else if (this.type instanceof RelationFmi3Type) {
            return (RelationFmi3Type) this.type;
        }
        return null;
    }

    //    @Override
    //    public <TP> Type<TP> getType(Class<TP> clz) {
    //
    //        if (type != null && clz.isAssignableFrom(type.get().getClass())) {
    //            return (Type<TP>) type;
    //        }
    //        return null;
    //    }

    @Override
    public long getValueReference() {
        return valueReference;
    }

    public boolean has(Fmi2ModelDescription.Causality scalarAttributeType) {
        return getFmi2ScalarVariable().map(s -> s.causality == scalarAttributeType).orElse(false);
    }

    public boolean has(Fmi3Causality scalarAttributeType) {
        return getFmi3ScalarVariable().map(s -> s.getVariable().getCausality() == scalarAttributeType).orElse(false);
    }


    public Optional<Fmi2ModelDescription.ScalarVariable> getFmi2ScalarVariable() {
        Fmi2ModelDescription.ScalarVariable sv = getScalarVariable(Fmi2ModelDescription.ScalarVariable.class);
        return sv == null ? Optional.empty() : Optional.of(sv);
    }

    public Optional<Fmi3ModelDescription.Fmi3ScalarVariable> getFmi3ScalarVariable() {
        Fmi3ModelDescription.Fmi3ScalarVariable sv = getScalarVariable(Fmi3ModelDescription.Fmi3ScalarVariable.class);
        return sv == null ? Optional.empty() : Optional.of(sv);
    }

    public SCALAR_TYPE getScalarVariable() {
        return scalarVariable;
    }

    @Override
    public String toString() {
        return instance + "." + scalarVariable;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RelationVariable)) {
            return false;
        }

        RelationVariable rv = (RelationVariable) o;
        return rv.toString().equals(this.toString());
    }

    public static class RelationFmi2Type implements org.intocps.maestro.framework.core.RelationVariable.Type<Fmi2ModelDescription.Type, Fmi2ModelDescription.Types> {

        final Fmi2ModelDescription.Type type;

        public RelationFmi2Type(Fmi2ModelDescription.Type type) {
            this.type = type;
        }

        //        @Override
        //        public <T> T as(Class<T> clz) {
        //
        //            if (clz.isAssignableFrom(type.getClass())) {
        //                return (T) clz.cast(type);
        //            }
        //
        //            return null;
        //        }

        @Override
        public boolean hasType(Fmi2ModelDescription.Types type) {
            return this.type.type == type;
        }

        @Override
        public Fmi2ModelDescription.Type get() {
            return this.type;
        }

        @Override
        public PType getLexType() {
            switch (this.type.type) {
                case Boolean:
                    return newABoleanPrimitiveType();
                case Real:
                    return newARealNumericPrimitiveType();
                case Integer:
                    return newAIntNumericPrimitiveType();
                case String:
                    return newAStringPrimitiveType();
                default:
                    throw new UnsupportedOperationException("Converting fmi type: " + type + " to mabl type is not supported.");
            }
        }

        @Override
        public PExp getLexDefaultValue() {


            switch (this.type.type) {

                case Boolean:
                    return newABoolLiteralExp(false);
                case Real:
                    return newARealLiteralExp(0.0);
                case Integer:
                case Enumeration:
                    return newAIntLiteralExp(0);
                case String:
                    return newAStringLiteralExp("");


            }
            throw new RuntimeException("Unknown type");
        }

        @Override
        public boolean isAssignableFrom(Fmi2ModelDescription.Type other, boolean autoConvert) {
            return this.type.isAssignableFrom(other, autoConvert);
        }
    }

    public static class RelationFmi3Type implements org.intocps.maestro.framework.core.RelationVariable.Type<Fmi3TypeEnum, Fmi3TypeEnum> {

        final Fmi3TypeEnum type;

        public RelationFmi3Type(Fmi3TypeEnum type) {
            this.type = type;
        }

        //        @Override
        //        public <T> T as(Class<T> clz) {
        //
        //            if (clz.isAssignableFrom(type.getClass())) {
        //                return (T) clz.cast(type);
        //            }
        //
        //            return null;
        //        }

        @Override
        public boolean hasType(Fmi3TypeEnum type) {
            return this.type == type;
        }

        @Override
        public Fmi3TypeEnum get() {
            return this.type;
        }

        @Override
        public PType getLexType() {
            switch (this.type) {
                case Float32Type:
                    break;
                case Float64Type:
                    break;
                case Int8Type:
                    break;
                case UInt8Type:
                    break;
                case Int16Type:
                    break;
                case UInt16Type:
                    break;
                case Int32Type:
                    return newAIntNumericPrimitiveType();
                case UInt32Type:
                    break;
                case Int64Type:
                    break;
                case UInt64Type:
                    return newARealNumericPrimitiveType();
                case BooleanType:
                    return newABoleanPrimitiveType();
                case StringType:
                    return newAStringPrimitiveType();
                case BinaryType:
                    break;
                case EnumerationType:
                    break;
                case ClockType:
                    break;
                default:
                    throw new UnsupportedOperationException("Converting fmi type: " + type + " to mabl type is not supported.");
            }
            return null;
        }

        @Override
        public PExp getLexDefaultValue() {

            switch (this.type) {

                case Float32Type:
                    break;
                case Float64Type:
                    break;
                case Int8Type:
                    break;
                case UInt8Type:
                    break;
                case Int16Type:
                    break;
                case UInt16Type:
                    break;
                case Int32Type:
                    return newAIntLiteralExp(0);
                case UInt32Type:
                    break;
                case Int64Type:
                    break;
                case UInt64Type:
                    break;
                case BooleanType:
                    return newABoolLiteralExp(false);
                case StringType:
                    return newAStringLiteralExp("");
                case BinaryType:
                    break;
                case EnumerationType:
                    break;
                case ClockType:
                    break;
            }
            throw new RuntimeException("Unknown type");
        }

        @Override
        public boolean isAssignableFrom(Fmi3TypeEnum other, boolean autoConvert) {
            return this.type.equals(other);
        }
    }
}
