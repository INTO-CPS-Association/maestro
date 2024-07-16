package org.intocps.maestro.framework.core;

import org.intocps.maestro.ast.LexIdentifier;
import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.fmi.Fmi2ModelDescription;

public interface RelationVariable {
    /**
     * Get the instance reference  aka the lex name for the instance
     *
     * @return the lex identifier
     */
    LexIdentifier getInstance();

    /**
     * Get the scalar variable name as in the model description this name will not be changed due to slashes
     *
     * @return the name
     */
    String getName();

    /**
     * Get the underlying scalar variable object as is or null is not matching the requested type
     *
     * @param clz the underlying type to filter by
     * @param <T> the filter type
     * @return the underlying scalar variable object of the requested type
     */
    <T> T getScalarVariable(Class<T> clz);

    /**
     * Checks if any of the scalar variable enums apply for i.e. causality, Variability etc.
     *
     * @param scalarAttributeType object to check
     * @return true if present
     */
    boolean has(Object scalarAttributeType);


    Type<Object,Object> getType();

//    <TP> Type<TP> getType(Class<TP> clz);

    /**
     * The value reference of the scalar variable
     *
     * @return the value reference
     */
    long getValueReference();

    interface Type<T,TypeEnum> {

//        <TP> TP as(Class<TP> clz);

        /**
         * Check if the scala variable holds the requested type enum
         *
         * @param type type enum to check for
         * @return true if of the specified type
         */
        boolean hasType(TypeEnum type);

        T get();

        PType getLexType();

        PExp getLexDefaultValue();

        boolean isAssignableFrom(T other, boolean autoConvert);
    }
}
