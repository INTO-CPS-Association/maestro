//package org.intocps.maestro.framework.fmi2;
//
//import org.intocps.maestro.ast.LexIdentifier;
//import org.intocps.maestro.fmi.fmi3.Fmi3ModelDescription;
//
//public class RelationVariable3 implements org.intocps.maestro.framework.core.RelationVariable {
//    public final Fmi3ModelDescription.Fmi3ScalarVariable scalarVariable;
//    public final LexIdentifier instance;
//
//    public RelationVariable3(Fmi3ModelDescription.Fmi3ScalarVariable scalarVariable, LexIdentifier instance) {
//        this.scalarVariable = scalarVariable;
//        this.instance = instance;
//    }
//
//    @Override
//    public LexIdentifier getInstance() {
//        return this.instance;
//    }
//
//    @Override
//    public String getName() {
//        return scalarVariable.getVariable().getName();
//    }
//
//    @Override
//    public <T> T getScalarVariable(Class<T> clz) {
//        if (clz.isAssignableFrom(scalarVariable.getClass())) {
//            return clz.cast(scalarVariable);
//        }
//        return null;
//    }
//
//    public Fmi3ModelDescription.Fmi3ScalarVariable getScalarVariable() {
//        return getScalarVariable(this.scalarVariable.getClass());
//    }
//
//    @Override
//    public String toString() {
//        return instance + "." + scalarVariable;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (o == this) {
//            return true;
//        }
//        if (!(o instanceof RelationVariable)) {
//            return false;
//        }
//
//        RelationVariable rv = (RelationVariable) o;
//        return rv.toString().equals(this.toString());
//    }
//}