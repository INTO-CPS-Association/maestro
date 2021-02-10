package org.intocps.maestro.framework.fmi2.api;

import org.intocps.maestro.ast.node.PExp;
import org.intocps.maestro.ast.node.PStm;
import org.intocps.maestro.ast.node.PType;
import org.intocps.maestro.framework.fmi2.api.mabl.variables.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface Fmi2Builder<S, B, E> {
    B build() throws Exception;

    PStm buildRaw() throws Exception;

    RuntimeModule<S> loadRuntimeModule(String name, Object... args);

    RuntimeModule<S> loadRuntimeModule(Scope<S> scope, String name, Object... args);


    /**
     * Gets the default scope
     *
     * @return
     */
    Scope<S> getRootScope();

    DynamicActiveScope<S> getDynamicScope();

    /**
     * Gets a tag to the last value obtained for the given port
     *
     * @param port
     * @return
     */
    <V, T> Variable<T, V> getCurrentLinkedValue(Port port);
    //    public abstract void pushScope(Scope scope);


    /**
     * Get handle to the current time
     *
     * @return
     */
    //Time getCurrentTime();

    DoubleVariableFmi2Api getDoubleVariableFrom(E exp);

    IntVariableFmi2Api getIntVariableFrom(E exp);

    StringVariableFmi2Api getStringVariableFrom(E exp);

    BooleanVariableFmi2Api getBooleanVariableFrom(E exp);

    FmuVariableFmi2Api getFmuVariableFrom(E exp);


    interface RuntimeModule<S> extends Fmi2Builder.Variable<S, NamedVariable<S>> {
        void initialize(List<RuntimeFunction> declaredFuncs);

        void initialize(RuntimeFunction... declaredFuncs);

        //not sure how to allow a mix of double, int and var except for object
        void callVoid(RuntimeFunction functionId, Object... args);

        void callVoid(Scope<S> scope, RuntimeFunction functionId, Object... args);

        <V> Variable<S, V> call(Scope<S> scope, RuntimeFunction functionId, Object... args);

        <V> Variable<S, V> call(RuntimeFunction functionId, Object... args);

        void destroy();

        void destroy(Scope<S> scope);
    }

    interface NumericValue {
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


        static public class FunctionType {
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

            static public enum Type {
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
     * New boolean that can be used as a predicate
     *
     * @param
     * @return
     */

    /**
     * Scoping functions
     */
    interface Scoping<T> {
        WhileScope<T> enterWhile(LogicBuilder.Predicate predicate);

        IfScope<T> enterIf(LogicBuilder.Predicate predicate);

        Scope<T> leave();


        void add(T... commands);

        void addAll(Collection<T> commands);

        void addBefore(T item, T... commands);

        void addAfter(T item, T... commands);

        Scoping<T> activate();
    }

    /**
     * Basic scope. Allows a value to be stored or override a tag
     */
    interface Scope<T> extends Scoping<T> {
        @Override
        Scope<T> activate();

        /**
         * Store a given value
         *
         * @param value
         * @return
         */
        DoubleVariable<T> store(double value);

        StringVariable<T> store(String value);

        BoolVariable<T> store(boolean value);

        IntVariable<T> store(int value);

        /**
         * Store a given value with a prefix name
         *
         * @param value
         * @return
         */
        DoubleVariable<T> store(String name, double value);

        StringVariable<T> store(String name, String value);

        BoolVariable<T> store(String name, boolean value);

        IntVariable<T> store(String name, int value);

        <ValType extends Object, Val extends Value<ValType>, Var extends Variable<T, Val>> Var store(String name, Var value);

        <ValType extends Object, Val extends Value<ValType>, Var extends Variable<T, Val>> Var copy(String name, Var value);

        /**
         * Store the given value and get a tag for it. Copy
         *
         * @param tag
         * @return
         */
        @Deprecated
        <V> Variable<T, V> store(Value<V> tag);


        //   /**
        //   * Override a tags value from an existing value. Copy
        //   *
        //   * @param tag
        //   * @param value
        //   * @return
        //   */
        // <V extends Value> Variable<T, V> store(V tag, V value);
        //TODO add overload with name prefix, tag override is done through variable and not the scope


        Fmu2Variable<T> createFMU(String name, String loaderName, String... args) throws Exception;
    }

    /**
     * Dynamic scope which always reflects the current active scope of the builder
     */
    interface DynamicActiveScope<T> extends Scope<T> {

    }

    /**
     * If scope, default scope is then
     */
    interface IfScope<T> {
        /**
         * Switch to then scope
         *
         * @return
         */
        Scope<T> enterThen();

        /**
         * Switch to else scope
         *
         * @return
         */
        Scope<T> enterElse();

        Scope<T> leave();
    }

    /**
     * While
     */
    interface WhileScope<T> extends Scope<T> {

    }


    interface LogicBuilder {

        //        Predicate isEqual(Port a, Port b);
        //
        //
        //        <T> Predicate isLess(T a, T b);
        //
        //        <T> Predicate isLessOrEqualTo(Variable a, Variable b);
        //
        //        Predicate isGreater(Value<Double> a, double b);
        //
        //        <T> Predicate fromValue(Value<T> value);

        // Predicate fromExternalFunction(String name, Value... args);


        interface Predicate {
            Predicate and(Predicate p);

            Predicate or(Predicate p);

            Predicate not();
        }

    }

    interface Type {
    }

    interface Numeric<A extends Number> extends Value<Number>, Type {
        void set(A value);

        @Override
        A get();
    }


    interface Port {

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
        void linkTo(Port... receiver) throws PortLinkException;

        /**
         * Break the source link
         */
        void breakLink() throws PortLinkException;

        class PortLinkException extends Exception {
            Port port;

            public PortLinkException(String message, Port port) {
                super(message);
                this.port = port;
            }
        }
    }


    interface Value<V> {
        static Value<Double> of(double a) {
            return null;
        }

       /* static Value of(Variable var) {
            return null;
        }*/

        V get();
    }

    interface IntValue extends Value<Integer>, NumericExpressionValue {
    }

    interface BoolValue extends Value<Boolean> {
    }

    interface DoubleValue extends Value<Double>, NumericExpressionValue {
    }

    interface StringValue extends Value<String> {
    }

    interface ReferenceValue extends Value<PExp> {
    }

    interface NamedValue extends Value<Object> {
    }


    interface IntVariable<T> extends Variable<T, IntValue>, ProvidesTypedReferenceExp, NumericTypedReferenceExp {
        void decrement();

        void increment();
    }


    //FIXME why is this still here this is mable expression is very specific its not needed to should be deleted
    @Deprecated
    interface ProvidesTypedReferenceExp {
        PType getType();

        PExp getExp();

    }

    interface NumericTypedReferenceExp extends ProvidesTypedReferenceExp {
    }

    interface DoubleVariable<T> extends Variable<T, DoubleValue>, ProvidesTypedReferenceExp, NumericTypedReferenceExp {

        void set(Double value);
    }

    interface BoolVariable<T> extends Variable<T, BoolValue>, ProvidesTypedReferenceExp {
        LogicBuilder.Predicate toPredicate();
    }

    interface StringVariable<T> extends Variable<T, StringValue>, ProvidesTypedReferenceExp {

    }


    interface NamedVariable<T> extends Variable<T, NamedValue> {
    }

    interface StateVariable<T> extends Variable<T, Object> {
        /**
         * Sets this state on the owning component in the active scope
         */
        void set() throws IllegalStateException;

        /**
         * Sets this state on the owning component in the given scope
         */
        void set(Scope<T> scope) throws IllegalStateException;

        /**
         * Destroys the state in the active scope. After this no other operation on the state is allowed
         */
        void destroy() throws IllegalStateException;

        /**
         * Destroys the state in the active scope. After this no other operation on the state is allowed
         */
        void destroy(Scope<T> scope) throws IllegalStateException;
    }


    /**
     * Handle for an fmu for the creation of component
     */
    interface Fmu2Variable<S> extends Variable<S, NamedVariable<S>> {
        Fmi2ComponentVariable<S> instantiate(String name);

        Fmi2ComponentVariable<S> instantiate(String name, Scope<S> scope);

        void unload();

        void unload(Scope<S> scope);
    }

    /**
     * Interface for an fmi compoennt.
     * <p>
     * Note that all methods that do not take a scope uses the builders dynamic scope and adds the underlying instructions int he active scope.
     */
    interface Fmi2ComponentVariable<T> extends Variable<T, NamedVariable<T>> {


        void setupExperiment(DoubleVariable<T> startTime, DoubleVariable<T> endTime, Double tolerance);

        void setupExperiment(double startTime, Double endTime, Double tolerance);

        void enterInitializationMode();

        void exitInitializationMode();

        void setupExperiment(Scope<T> scope, DoubleVariable<T> startTime, DoubleVariable<T> endTime, Double tolerance);

        void setupExperiment(Scope<T> scope, double startTime, Double endTime, Double tolerance);

        void enterInitializationMode(Scope<T> scope);

        void exitInitializationMode(Scope<T> scope);

        /**
         * @param scope
         * @param currentCommunicationPoint
         * @param communicationStepSize
         * @param noSetFMUStatePriorToCurrentPoint a pair representing (full step completed, current time after step)
         * @return
         */
        Map.Entry<BoolVariable<T>, DoubleVariable<T>> step(Scope<T> scope, DoubleVariable<T> currentCommunicationPoint,
                DoubleVariable<T> communicationStepSize, BoolVariable<T> noSetFMUStatePriorToCurrentPoint);

        Map.Entry<BoolVariable<T>, DoubleVariable<T>> step(Scope<T> scope, DoubleVariable<T> currentCommunicationPoint,
                DoubleVariable<T> communicationStepSize);

        Map.Entry<BoolVariable<T>, DoubleVariable<T>> step(DoubleVariable<T> currentCommunicationPoint, DoubleVariable<T> communicationStepSize,
                BoolVariable<T> noSetFMUStatePriorToCurrentPoint);

        Map.Entry<BoolVariable<T>, DoubleVariable<T>> step(DoubleVariable<T> currentCommunicationPoint, DoubleVariable<T> communicationStepSize);


        List<? extends Port> getPorts();

        /**
         * Get ports by name
         *
         * @param names
         * @return
         */
        List<? extends Port> getPorts(String... names);

        /**
         * Get ports by ref val
         *
         * @param valueReferences
         * @return
         */
        List<? extends Port> getPorts(int... valueReferences);

        /**
         * Get port by name
         *
         * @param name
         * @return
         */
        Port getPort(String name);

        /**
         * Get port by ref val
         *
         * @param valueReference
         * @return
         */
        Port getPort(int valueReference);

        /**
         * Get port values aka fmiGet
         *
         * @param ports
         * @return
         */
        <V> Map<? extends Port, ? extends Variable<T, V>> get(Port... ports);

        <V> Map<? extends Port, ? extends Variable<T, V>> get(Scope<T> scope, Port... ports);

        /**
         * Get all (linked) port values
         *
         * @return
         */
        <V> Map<? extends Port, ? extends Variable<T, V>> get();

        /**
         * get filter by value reference
         *
         * @param valueReferences
         * @return
         */
        <V> Map<? extends Port, ? extends Variable<T, V>> get(int... valueReferences);

        /**
         * Get filter by names
         *
         * @param names
         * @return
         */
        <V> Map<? extends Port, ? extends Variable<T, V>> get(String... names);

        <V> Map<? extends Port, ? extends Variable<T, V>> getAndShare(String... names);

        <V> Map<? extends Port, ? extends Variable<T, V>> getAndShare(Port... ports);

        <V> Map<? extends Port, ? extends Variable<T, V>> getAndShare();

        <V> Variable<T, V> getShared(String name);

        <V> Variable<T, V> getShared(Port port);

        /**
         * Get the value of a single port
         *
         * @param name
         * @return
         */
        <V> Variable<T, V> getSingle(String name);

        <V> Variable<T, V> getSingle(Port port);

        <V> void set(Scope<T> scope, PortValueMap<V> value);


        <V> void set(Scope<T> scope, PortVariableMap<T, V> value);

        /**
         * Set port values (if ports is not from this fmu then the links are used to remap)
         *
         * @param value
         */
        <V> void set(PortValueMap<V> value);

        <V> void set(Port port, Value<V> value);

        <V> void set(Port port, VariableFmi2Api<V> value);

        <V> void set(Scope<T> scope, Port port, VariableFmi2Api<V> value);

        <V> void set(PortVariableMap<T, V> value);

        /**
         * Set this fmu port by name and link
         */
        void setLinked(Scope<T> scope, Port... filterPorts);

        void setLinked();

        void setLinked(Port... filterPorts);

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
        <V> void share(Map<? extends Port, ? extends Variable<T, V>> values);

        /**
         * Makes the value publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param value
         */
        <V> void share(Port port, Variable<T, V> value);

        /**
         * Get the current state
         *
         * @return
         */
        StateVariable<T> getState();

        /**
         * Get the current state
         *
         * @return
         */
        StateVariable<T> getState(Scope<T> scope);


        interface PortVariableMap<S, V> extends Map<Port, Variable<S, V>> {
        }

        interface PortValueMap<V> extends Map<Port, Value<V>> {
        }

        interface PortExpressionValueMap extends Map<Port, ExpressionValue> {
        }
    }

    interface Variable<T, V> {
        String getName();

        void setValue(V value);

        void setValue(Variable<T, V> variable);

        void setValue(Scope<PStm> scope, Variable<PStm, V> variable);

        void setValue(Scope<T> scope, V value);


        Scope<T> getDeclaredScope();
    }

    public interface ExpressionValue extends ProvidesTypedReferenceExp {
    }

    public interface NumericExpressionValue extends ExpressionValue, NumericTypedReferenceExp {

    }

}
