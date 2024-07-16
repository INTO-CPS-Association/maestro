package org.intocps.maestro.framework.fmi2.api;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface FmiBuilder<AST, B, E, SETTINGS> {
    B build() throws Exception;

    SETTINGS getSettings();

    IFunctionBuilder getFunctionBuilder();

    /**
     * Returns whether the build has been used
     *
     * @return true if the builder contains user added statements
     */
    boolean isDirty();

    /**
     * Reset the dirty flag
     */
    void resetDirty();

    PStm buildRaw() throws Exception;

    RuntimeModule<AST> loadRuntimeModule(String name, Object... args);

    RuntimeModule<AST> loadRuntimeModule(TryScope<AST> scope, String name, Object... args);

    /**
     * Gets the default scope
     *
     * @return
     */
    Scope<AST> getRootScope();

    DynamicActiveScope<AST> getDynamicScope();

    /**
     * Gets a tag to the last value obtained for the given port
     *
     * @param port
     * @return
     */
    <V, T> Variable<T, V> getCurrentLinkedValue(Port port);

    DoubleVariable<AST> getDoubleVariableFrom(E exp);

    IntVariable<AST> getIntVariableFrom(E exp);

    StringVariable<AST> getStringVariableFrom(E exp);

    BoolVariable<AST> getBooleanVariableFrom(E exp);

    <V, T> Variable<T, V> getFmuVariableFrom(E exp);


    interface RuntimeModule<AST> extends FmiBuilder.Variable<AST, NamedVariable<AST>> {
        void initialize(List<RuntimeFunction> declaredFuncs);

        void initialize(RuntimeFunction... declaredFuncs);

        //not sure how to allow a mix of double, int and var except for object
        void callVoid(RuntimeFunction functionId, Object... args);

        void callVoid(Scope<AST> scope, RuntimeFunction functionId, Object... args);

        <V> Variable<AST, V> call(Scope<AST> scope, RuntimeFunction functionId, Object... args);

        <V> Variable<AST, V> call(RuntimeFunction functionId, Object... args);

        //        void destroy();
        //
        //        void destroy(Scope<S> scope);
    }

    interface NumericValue {
    }

    interface IFunctionBuilder {
        IFunctionBuilder setName(String name);

        IFunctionBuilder setReturnType(String name);

        IFunctionBuilder setReturnType(FmiBuilder.RuntimeFunction.FunctionType.Type type);

        IFunctionBuilder addArgument(String name, FmiBuilder.RuntimeFunction.FunctionType.Type type);

        IFunctionBuilder useVargs();

        IFunctionBuilder addArgument(String name, String type);

        FmiBuilder.RuntimeFunction build();
    }

    interface RuntimeFunction {
        String getName();

        /**
         * List of arg (name,class) pairs
         *
         * @return
         */
        List<Map.Entry<String, FunctionType>> getArgs();

        FunctionType getReturnType();

        boolean usingVargs();


        class FunctionType {
            final Type nativeType;
            final String namedType;

            public FunctionType(Type type) {
                this.nativeType = type;
                this.namedType = null;
            }

            public FunctionType(String name) {
                this.nativeType = null;
                this.namedType = name;
            }

            public Type getNativeType() {
                return nativeType;
            }

            public String getNamedType() {
                return namedType;
            }

            public boolean isNative() {
                return nativeType != null;
            }

            public enum Type {
                Void,
                Int,
                UInt,
                Double,
                String,
                Boolean,
                /**
                 * This should be used with care as it disabled any type checking
                 */
                Any
            }
        }
    }

    /**
     * Scoping element which defines a scope like a block, if, while etc.
     *
     * @param <AST> the type the scoping element encloses
     */

    interface ScopeElement<AST> {
        /**
         * The parent element of this element or null if root
         *
         * @return the parent
         */
        ScopeElement<AST> parent();

        /**
         * The declaration node that defined the underlying scope
         *
         * @return the scope
         */
        AST getDeclaration();

        /**
         * Find a prent element of a specific type
         *
         * @param clz the class type to search for
         * @param <P> the type of class
         * @return the parent of the specified type or null
         */
        <P extends ScopeElement<AST>> P findParent(Class<P> clz);
    }


    /**
     * Scoping functions
     */
    interface Scoping<AST> extends ScopeElement<AST> {
        WhileScope<AST> enterWhile(Predicate predicate);

        IfScope<AST> enterIf(Predicate predicate);

        TryScope<AST> enterTry();

        Scoping<AST> parallel();

        Scoping<AST> enterScope();

        Scope<AST> leave();


        void add(AST... commands);

        void addAll(Collection<AST> commands);

        void addBefore(AST item, AST... commands);

        void addAfter(AST item, AST... commands);

        Scoping<AST> activate();


    }

    /**
     * Basic scope. Allows a value to be stored or override a tag
     */
    interface Scope<AST> extends Scoping<AST> {
        @Override
        Scope<AST> activate();

        /**
         * Store a given value
         *
         * @param value
         * @return
         */
        DoubleVariable<AST> store(double value);

        StringVariable<AST> store(String value);

        BoolVariable<AST> store(boolean value);

        IntVariable<AST> store(int value);

        /**
         * Store a given value with a prefix name
         *
         * @param value
         * @return
         */
        DoubleVariable<AST> store(String name, double value);

        StringVariable<AST> store(String name, String value);

        BoolVariable<AST> store(String name, boolean value);

        IntVariable<AST> store(String name, int value);

        <CV> ArrayVariable<AST, CV> store(String name, CV[] value);

         <V > ArrayVariable<AST,V> createArray(String name,Class<? extends V> type, IntVariable<AST>...sizes ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

        /**
         * Store the given value and get a tag for it. Copy
         *
         * @param tag
         * @return
         */
        @Deprecated
        <V> Variable<AST, V> store(Value<V> tag);

        <PS> Fmu2Variable<AST, PS> createFMU(String name, String loaderName, String... args) throws Exception;

        <PS> Fmu3Variable<AST> createFMU3(String name, String loaderName, String... args) throws Exception;

        void markTransferPoint(String... names);

        void addTransferAs(String... names);

    }

    /**
     * Dynamic scope which always reflects the current active scope of the builder
     */
    interface DynamicActiveScope<AST> extends Scope<AST> {

    }

    /**
     * If scope, default scope is then
     */
    interface IfScope<AST> extends ScopeElement<AST> {
        /**
         * Switch to then scope
         *
         * @return
         */
        Scope<AST> enterThen();

        /**
         * Switch to else scope
         *
         * @return
         */
        Scope<AST> enterElse();

        Scope<AST> leave();
    }

    /**
     * Try finally scope, default scope is body
     */
    interface TryScope<AST> extends ScopeElement<AST> {
        /**
         * Switch to body scope
         *
         * @return
         */
        Scope<AST> enter();

        /**
         * Switch to finally scope
         *
         * @return
         */
        Scope<AST> enterFinally();

        Scope<AST> leave();


        Scope<AST> getBody();

        Scope<AST> getFinallyBody();
    }

    /**
     * While
     */
    interface WhileScope<AST> extends Scope<AST>, ScopeElement<AST> {

    }


    interface Predicate {
        Predicate and(Predicate p);

        Predicate or(Predicate p);

        Predicate not();
    }

    interface Type {
    }

    interface Numeric<A extends Number> extends Value<Number>, Type {
        void set(A value);

        @Override
        A get();
    }


    interface Port<PORT_SCALAR_TYPE, AST> {

        /**
         * Gets the fully qualified port name including its source reference. Often on the form source.name
         *
         * @return
         */
        String getQualifiedName();


        /**
         * Get the owner of this port. This is the object that should be used to get/set its values
         *
         * @return the instance owning the port
         */
        FmiSimulationInstance<AST, PORT_SCALAR_TYPE> getOwner();

        /**
         * Gets the underlying objects from which the port is created
         *
         * @return
         */
        PORT_SCALAR_TYPE getSourceObject();

        /**
         * Get the port name
         *
         * @return
         */
        String getName();

        /**
         * Get the port reference value
         *
         * @return
         */
        Long getPortReferenceValue();

        /**
         * Link the current port to the receiving port. After this the receiving port will resolve its linked value to the value of this port
         *
         * @param receiver
         */
        void linkTo(Port<PORT_SCALAR_TYPE, AST>... receiver) throws PortLinkException;

        /**
         * Break the source link
         */
        void breakLink() throws PortLinkException;

        /**
         * Indicates if the current port is linked
         *
         * @return true if linked
         */
        boolean isLinked();

        /**
         * Indicates if this is linked into other @{@link Port}s
         *
         * @return true if linked
         */
        boolean isLinkedAsOutputProvider();

        /**
         * Indicates if another @{@link Port} is linked into this
         *
         * @return true if linked
         */
        boolean isLinkedAsInputConsumer();

        class PortLinkException extends Exception {
            Port port;

            public PortLinkException(String message, Port port) {
                super(message);
                this.port = port;
            }
        }
    }


    interface Value<V> {
        V get();
    }


    interface NamedValue extends Value<Object>,ProvidesTypedReferenceExp {
    }


    interface IntVariable<AST> extends Variable<AST, IntExpressionValue>, ProvidesTypedReferenceExp, NumericTypedReferenceExp {
        void decrement();

        void increment();
    }

    interface UIntVariable<AST> extends Variable<AST, UIntExpressionValue>, ProvidesTypedReferenceExp, NumericTypedReferenceExp{
        void decrement();

        void increment();
    };


    interface ProvidesTypedReferenceExp {
        PType getType();

        PExp getExp();

    }

    interface NumericTypedReferenceExp extends ProvidesTypedReferenceExp {
    }

    interface DoubleVariable<AST> extends Variable<AST, DoubleExpressionValue>, ProvidesTypedReferenceExp, NumericTypedReferenceExp {

        void set(Double value);
    }

    interface BoolVariable<AST> extends Variable<AST, BooleanExpressionValue>, ProvidesTypedReferenceExp {
        Predicate toPredicate();
    }

    interface StringVariable<T> extends Variable<T, StringExpressionValue>, ProvidesTypedReferenceExp {

    }


    interface NamedVariable<AST> extends Variable<AST, NamedValue> {
    }

    interface StateVariable<AST> extends Variable<AST, Object> {
        /**
         * Sets this state on the owning component in the active scope
         */
        void set() throws IllegalStateException;

        /**
         * Sets this state on the owning component in the given scope
         */
        void set(Scope<AST> scope) throws IllegalStateException;

        /**
         * Destroys the state in the active scope. After this no other operation on the state is allowed
         */
        void destroy() throws IllegalStateException;

        /**
         * Destroys the state in the active scope. After this no other operation on the state is allowed
         */
        void destroy(Scope<AST> scope) throws IllegalStateException;
    }


    /**
     * Handle for an fmu for the creation of component
     */
    interface Fmu2Variable<AST, PORT_SCALAR_TYPE> extends Variable<AST, NamedVariable<AST>> {
        Fmi2ComponentVariable<AST, PORT_SCALAR_TYPE> instantiate(String name, String environmentname);

        Fmi2ComponentVariable<AST, PORT_SCALAR_TYPE> instantiate(String name);

        //    /**
        //     * Performs null check and frees the instance
        //     *
        //     * @param scope
        //     * @param comp
        //     */
        //    private void freeInstance(Fmi2Builder.Scope<PStm> scope, Fmi2Builder.Fmi2ComponentVariable<PStm> comp) {
        //        if (comp instanceof ComponentVariableFmi2Api) {
        //            scope.add(newIf(newNotEqual(((ComponentVariableFmi2Api) comp).getReferenceExp().clone(), newNullExp()), newABlockStm(
        //                    MableAstFactory.newExpressionStm(
        //                            call(getReferenceExp().clone(), "freeInstance", ((ComponentVariableFmi2Api) comp).getReferenceExp().clone())),
        //                    newAAssignmentStm(((ComponentVariableFmi2Api) comp).getDesignatorClone(), newNullExp())), null));
        //        } else {
        //            throw new RuntimeException("Argument is not an FMU instance - it is not an instance of ComponentVariableFmi2API");
        //        }
        //    }
        Fmi2ComponentVariable<AST, PORT_SCALAR_TYPE> instantiate(String namePrefix, TryScope<PStm> enclosingTryScope, Scope<PStm> scope,
                                                                 String environmentName);

        Fmi2ComponentVariable<AST, PORT_SCALAR_TYPE> instantiate(String namePrefix, FmiBuilder.TryScope<PStm> enclosingTryScope,
                                                                 FmiBuilder.Scope<PStm> scope, String environmentName, boolean loggingOn);

        Fmi2ComponentVariable<AST, PORT_SCALAR_TYPE> instantiate(String name, TryScope<AST> enclosingTryScope, Scope<AST> scope);

        //void freeInstance(Fmi2ComponentVariable<S> comp);

        //void freeInstance(Scope<S> scope, Fmi2ComponentVariable<S> comp);

        //        void unload();
        //
        //        void unload(Scope<S> scope);
    }

    /**
     * Handle for an fmu for the creation of component
     */
    interface Fmu3Variable<AST> extends Variable<AST, NamedVariable<AST>> {


    }

    /**
     * Generic type for all simulation instances
     *
     * @param <AST>
     */
    interface SimulationInstance<AST> extends Variable<AST, NamedVariable<AST>> {
    }

    /**
     * Type that represents common FMI functions accross versions for now 2 and 3
     *
     * @param <AST>              the construction type. Often a kind of AST statement
     * @param <PORT_SCALAR_TYPE> the port internal reference type. Often a kind of scalar variable from FMI
     */
    interface FmiSimulationInstance<AST, PORT_SCALAR_TYPE> extends SimulationInstance<AST> {

        void setDebugLogging(List<String> categories, boolean enableLogging);


        List<? extends Port<PORT_SCALAR_TYPE, AST>> getPorts();

        /**
         * Get ports by name
         *
         * @param names
         * @return
         */
        List<? extends Port<PORT_SCALAR_TYPE, AST>> getPorts(String... names);

        /**
         * Get ports by ref val
         *
         * @param valueReferences
         * @return
         */
        List<? extends Port<PORT_SCALAR_TYPE, AST>> getPorts(int... valueReferences);

        /**
         * Get port by name
         *
         * @param name
         * @return
         */
        Port<PORT_SCALAR_TYPE, AST> getPort(String name);

        /**
         * Get port by ref val
         *
         * @param valueReference
         * @return
         */
        Port<PORT_SCALAR_TYPE, AST> getPort(int valueReference);

        /**
         * Get port values aka fmiGet
         *
         * @param ports
         * @return
         */
        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> get(Port<PORT_SCALAR_TYPE, AST>... ports);

        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> get(Scope<AST> scope, Port<PORT_SCALAR_TYPE, AST>... ports);

        /**
         * Get all (linked) port values
         *
         * @return
         */
        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> get();

        /**
         * get filter by value reference
         *
         * @param valueReferences
         * @return
         */
        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> get(int... valueReferences);

        /**
         * Get filter by names
         *
         * @param names
         * @return
         */
        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> get(String... names);

        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> getAndShare(String... names);

        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> getAndShare(Port<PORT_SCALAR_TYPE, AST>... ports);

        <V> Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> getAndShare();

        <V> Variable<AST, V> getShared(String name);

        <V> Variable<AST, V> getShared(Port<PORT_SCALAR_TYPE, AST> port);

        /**
         * Get the value of a single port
         *
         * @param name
         * @return
         */
        <V> Variable<AST, V> getSingle(String name);

        <V> Variable<AST, V> getSingle(Port<PORT_SCALAR_TYPE, AST> port);

        <V> void set(Scope<AST> scope, PortValueMap<V, PORT_SCALAR_TYPE, AST> value);


        <V> void set(Scope<AST> scope, PortVariableMap<V, PORT_SCALAR_TYPE, AST> value);

        /**
         * Set port values (if ports is not from this fmu then the links are used to remap)
         *
         * @param value
         */
        <V> void set(PortValueMap<V, PORT_SCALAR_TYPE, AST> value);

        <V> void set(Port<PORT_SCALAR_TYPE, AST> port, Value<V> value);

        <V> void set(Port<PORT_SCALAR_TYPE, AST> port, Variable<AST, V> value);

        <V> void set(Scope<AST> scope, Port<PORT_SCALAR_TYPE, AST> port, Variable<AST, V> value);

        <V> void set(PortVariableMap<V, PORT_SCALAR_TYPE, AST> value);

        /**
         * Set this fmu port by name and link
         */
        void setLinked(Scope<AST> scope, Port<PORT_SCALAR_TYPE, AST>... filterPorts);

        void setLinked();

        void setLinked(Port<PORT_SCALAR_TYPE, AST>... filterPorts);

        void setLinked(String... filterNames);

        void setLinked(long... filterValueReferences);

        /**
         * Set this fmu ports by val ref
         *
         * @param values
         */
        void setInt(Map<? extends Integer, ? extends Value<Integer>> values);

        /**
         * Set this fmy ports by name
         *
         * @param value
         */
        void setString(Map<? extends String, ? extends Value<String>> value);

        /**
         * Makes the values publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param values
         */
        <V> void share(Map<? extends Port<PORT_SCALAR_TYPE, AST>, ? extends Variable<AST, V>> values);

        /**
         * Makes the value publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param value
         */
        <V> void share(Port<PORT_SCALAR_TYPE, AST> port, Variable<AST, V> value);

        /**
         * Get the current state
         *
         * @return
         */
        StateVariable<AST> getState() throws XPathExpressionException;

        /**
         * Get the current state
         *
         * @return
         */
        StateVariable<AST> getState(Scope<AST> scope) throws XPathExpressionException;

        interface PortVariableMap<V, PORT_SCALAR_TYPE, AST> extends Map<Port<PORT_SCALAR_TYPE, AST>, Variable<AST, V>> {
        }

        interface PortValueMap<V, PORT_SCALAR_TYPE, AST> extends Map<Port<PORT_SCALAR_TYPE, AST>, Value<V>> {
        }

        interface PortExpressionValueMap<PORT_SCALAR_TYPE, AST> extends Map<Port<PORT_SCALAR_TYPE, AST>, ExpressionValue> {
        }
    }

    /**
     * Simulation instance for FMI3
     *
     * @param <AST>              building block
     * @param <PORT_SCALAR_TYPE> fmi3 scalar variable type
     */
    interface Fmi3InstanceVariable<AST, PORT_SCALAR_TYPE> extends FmiSimulationInstance<AST, PORT_SCALAR_TYPE> {


        void setupExperiment(DoubleVariable<AST> startTime, DoubleVariable<AST> endTime, BoolVariable<AST> endTimeDefined, Double tolerance);

        void setupExperiment(double startTime, Double endTime, Double tolerance);

        void enterInitializationMode(boolean toleranceDefined, double tolerance, double startTime, boolean stopTimeDefined, double stopTime);

        void enterInitializationMode(Scope<AST> scope, FmiBuilder.BoolVariable<PStm> toleranceDefined, FmiBuilder.DoubleVariable<PStm> tolerance,
                                     FmiBuilder.DoubleVariable<PStm> startTime, FmiBuilder.BoolVariable<PStm> stopTimeDefined,
                                     FmiBuilder.DoubleVariable<PStm> stopTime);

        void enterInitializationMode(FmiBuilder.BoolVariable<PStm> toleranceDefined, FmiBuilder.DoubleVariable<PStm> tolerance,
                                     FmiBuilder.DoubleVariable<PStm> startTime, FmiBuilder.BoolVariable<PStm> stopTimeDefined,
                                     FmiBuilder.DoubleVariable<PStm> stopTime);

        void exitInitializationMode();

        void setupExperiment(Scope<AST> scope, DoubleVariable<AST> startTime, DoubleVariable<AST> endTime, BoolVariable<AST> endTimeDefined,
                             Double tolerance);

        void setupExperiment(Scope<AST> scope, double startTime, Double endTime, Double tolerance);

        void enterInitializationMode(Scope<AST> scope, boolean toleranceDefined, double tolerance, double startTime, boolean stopTimeDefined,
                                     double stopTime);

        void exitInitializationMode(Scope<AST> scope);

        void terminate(Scope<AST> scope);

        void terminate();


    }

    /**
     * Interface for an fmi component.
     * <p>
     * Note that all methods that do not take a scope uses the builders dynamic scope and adds the underlying instructions int he active scope.
     *
     * @param <AST>              building block
     * @param <PORT_SCALAR_TYPE> fmi2 scalar variable type
     */
    interface Fmi2ComponentVariable<AST, PORT_SCALAR_TYPE> extends FmiSimulationInstance<AST, PORT_SCALAR_TYPE> {

        void setDebugLogging(List<String> categories, boolean enableLogging);

        void setupExperiment(DoubleVariable<AST> startTime, DoubleVariable<AST> endTime, BoolVariable<AST> endTimeDefined, Double tolerance);

        void setupExperiment(double startTime, Double endTime, Double tolerance);

        void enterInitializationMode();

        void exitInitializationMode();

        void setupExperiment(Scope<AST> scope, DoubleVariable<AST> startTime, DoubleVariable<AST> endTime, BoolVariable<AST> endTimeDefined,
                             Double tolerance);

        void setupExperiment(Scope<AST> scope, double startTime, Double endTime, Double tolerance);

        void enterInitializationMode(Scope<AST> scope);

        void exitInitializationMode(Scope<AST> scope);

        void terminate(Scope<AST> scope);

        void terminate();


        /**
         * @param scope
         * @param currentCommunicationPoint
         * @param communicationStepSize
         * @param noSetFMUStatePriorToCurrentPoint a pair representing (full step completed, current time after step)
         * @return
         */
        Map.Entry<BoolVariable<AST>, DoubleVariable<AST>> step(Scope<AST> scope, DoubleVariable<AST> currentCommunicationPoint,
                                                               DoubleVariable<AST> communicationStepSize, BoolVariable<AST> noSetFMUStatePriorToCurrentPoint);

        Map.Entry<BoolVariable<AST>, DoubleVariable<AST>> step(Scope<AST> scope, DoubleVariable<AST> currentCommunicationPoint,
                                                               DoubleVariable<AST> communicationStepSize);

        Map.Entry<BoolVariable<AST>, DoubleVariable<AST>> step(DoubleVariable<AST> currentCommunicationPoint,
                                                               DoubleVariable<AST> communicationStepSize, BoolVariable<AST> noSetFMUStatePriorToCurrentPoint);

        Map.Entry<BoolVariable<AST>, DoubleVariable<AST>> step(DoubleVariable<AST> currentCommunicationPoint,
                                                               DoubleVariable<AST> communicationStepSize);
    }

    interface Variable<AST, V> extends ProvidesTypedReferenceExp {
        String getName();

        void setValue(V value);

        void setValue(Variable<AST, V> variable);

        void setValue(Scope<AST> scope, Variable<AST, V> variable);

        void setValue(Scope<AST> scope, V value);


        Scope<AST> getDeclaredScope();
    }

    interface ArrayVariable<AST, CV> extends Variable<AST, FmiBuilder.NamedVariable<AST>> {
        int size();

        List<? extends Variable<AST, CV>> items();

        void setValue(IntExpressionValue index, ExpressionValue value);
    }

    interface ExpressionValue extends ProvidesTypedReferenceExp {
    }

    interface BooleanExpressionValue extends FmiBuilder.ExpressionValue {
    }

    interface DoubleExpressionValue extends NumericExpressionValue {
    }

    interface IntExpressionValue extends NumericExpressionValue {
    }

    interface UIntExpressionValue extends IntExpressionValue {
    }

    interface StringExpressionValue extends FmiBuilder.ExpressionValue {
    }


    interface NumericExpressionValue extends ExpressionValue, NumericTypedReferenceExp {

    }

}
