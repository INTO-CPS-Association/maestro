package org.intocps.maestro.framework.fmi2.api;

import org.intocps.orchestration.coe.modeldefinition.ModelDescription;

import javax.xml.xpath.XPathExpressionException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Fmi2Builder<S> {
    S build() throws Exception;

    /**
     * Gets the default scope
     *
     * @return
     */
    Scope getRootScope();
    //    public abstract void pushScope(Scope scope);

    DynamicActiveScope getDynamicScope();

    /**
     * Get handle to the current time
     *
     * @return
     */
    Time getCurrentTime();

    /**
     * Gets a specific time from a number
     *
     * @param time
     * @return
     */
    Time getTime(double time);

    /**
     * Gets a tag to the last value obtained for the given port
     *
     * @param port
     * @return
     */
    Value getCurrentLinkedValue(Port port);

    TimeDeltaValue createTimeDeltaValue(double getMinimum);

    VariableCreator variableCreator();

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

        LiteralCreator literalCreator();

        VariableCreator getVariableCreator();

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

        DoubleVariable<T> doubleFromExternalFunction(String functionName, Value... arguments);

        IntVariable<T> intFromExternalFunction(String functionName, Value... arguments);

        MBoolean booleanFromExternalFunction(String functionName, Value... arguments);
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
    }

    /**
     * While
     */
    interface WhileScope<T> extends Scope<T> {
    }


    interface LogicBuilder {

        Predicate isEqual(Port a, Port b);

        Predicate isEqual(Time a, Time b);

        Predicate isLess(Time a, Time b);

        <T> Predicate isLess(Numeric<T> a, Numeric<T> b);

        Predicate isGreater(Value<Double> a, double b);

        Predicate fromValue(Value value);

        Predicate fromExternalFunction(String name, Value... args);


        interface Predicate {
            Predicate and(Predicate p);

            Predicate or(Predicate p);

            Predicate not();
        }

    }

    interface Type {
    }

    interface Numeric<A> extends Value, Type {
        void set(A value);

        @Override
        A get();
    }

    interface MBoolean extends LogicBuilder.Predicate, Value {
        void set(Boolean value);

        @Override
        Boolean get();
    }

    /**
     * Current time
     */
    interface Time {
    }

    /**
     * time value from number
     */
    interface TimeValue extends Time {
    }

    /**
     * Delta value for a time
     */
    interface TimeDeltaValue extends Time {

    }


    interface TimeTaggedState {
        void release();
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

    interface IntValue extends Value<Integer> {
    }

    interface BoolValue extends Value<Boolean> {
    }

    interface DoubleValue extends Value<Double> {
    }

    interface StringValue extends Value<String> {
    }

    interface NamedValue extends Value<Object> {
    }


    interface VariableCreator<T> {
        //  BoolVariable<T> createBoolean(String label);

        //  IntVariable<T> createInteger(String label);

        // Variable<T, TimeDeltaValue> createTimeDeltaValue(String label);

        // DoubleVariable<T> createDouble(String label);

        // Variable<T, Time> createTimeValue(String step_size);

        Fmu2Variable<T> createFMU(String name, ModelDescription modelDescription,
                URI path) throws XPathExpressionException, InvocationTargetException, IllegalAccessException;
    }

    interface LiteralCreator {

        Time createTime(Double value);

        TimeDeltaValue createTimeDelta(double v);
    }


    interface IntVariable<T> extends Variable<T, IntValue> {
        void decrement();

        void increment();

        //void set(int value);
    }

    interface DoubleVariable<T> extends Variable<T, DoubleValue> {
        TimeDeltaValue toTimeDelta();

        void set(Double value);
    }

    interface BoolVariable<T> extends Variable<T, BoolValue> {
        //void set(Boolean value);
    }

    interface StringVariable<T> extends Variable<T, StringValue> {
        //void set(String value);
    }

    interface NamedVariable<T> extends Variable<T, NamedValue> {
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

        void setupExperiment(boolean toleranceDefined, double tolerance, DoubleVariable<T> startTime, boolean endTimeDefined,
                DoubleVariable<T> endTime);

        void setupExperiment(boolean toleranceDefined, double tolerance, double startTime, boolean endTimeDefined, double endTime);

        void enterInitializationMode();

        void exitInitializationMode();

        void setupExperiment(Scope<T> scope, boolean toleranceDefined, double tolerance, DoubleVariable<T> startTime, boolean endTimeDefined,
                DoubleVariable<T> endTime);

        void setupExperiment(Scope<T> scope, boolean toleranceDefined, double tolerance, double startTime, boolean endTimeDefined, double endTime);

        void enterInitializationMode(Scope<T> scope);

        void exitInitializationMode(Scope<T> scope);

        /**
         * @param scope
         * @param currentCommunicationPoint
         * @param communicationStepSize
         * @param noSetFMUStatePriorToCurrentPoint a pair representing (full step completed, current time after step)
         * @return
         */
        Map.Entry<BoolVariable, DoubleVariable<T>> step(Scope<T> scope, DoubleVariable<T> currentCommunicationPoint,
                DoubleVariable<T> communicationStepSize, BoolVariable<T> noSetFMUStatePriorToCurrentPoint);

        Map.Entry<BoolVariable, DoubleVariable<T>> step(Scope<T> scope, DoubleVariable<T> currentCommunicationPoint,
                DoubleVariable<T> communicationStepSize);

        Map.Entry<BoolVariable, DoubleVariable<T>> step(DoubleVariable<T> currentCommunicationPoint, DoubleVariable<T> communicationStepSize,
                BoolVariable<T> noSetFMUStatePriorToCurrentPoint);

        Map.Entry<BoolVariable, DoubleVariable<T>> step(DoubleVariable<T> currentCommunicationPoint, DoubleVariable<T> communicationStepSize);


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
        Map<Port, Variable> get(Port... ports);

        Map<Port, Variable> get(Scope<T> scope, Port... ports);

        /**
         * Get all (linked) port values
         *
         * @return
         */
        Map<Port, Variable> get();

        /**
         * get filter by value reference
         *
         * @param valueReferences
         * @return
         */
        Map<Port, Variable> get(int... valueReferences);

        /**
         * Get filter by names
         *
         * @param names
         * @return
         */
        Map<Port, Variable> get(String... names);

        Map<Port, Variable> getAndShare(String... names);

        /**
         * Get the value of a single port
         *
         * @param name
         * @return
         */
        Value getSingle(String name);

        void set(Scope<T> scope, PortValueMap value);


        void set(Scope<T> scope, PortVariableMap value);

        /**
         * Set port values (if ports is not from this fmu then the links are used to remap)
         *
         * @param value
         */
        void set(PortValueMap value);

        void set(Port port, Value value);

        void set(PortVariableMap value);

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
        void setInt(Map<Integer, Value<Integer>> values);

        /**
         * Set this fmy ports by name
         *
         * @param value
         */
        void setString(Map<String, Value<String>> value);

        /**
         * Makes the values publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param values
         */
        void share(Map<Port, Variable> values);

        /**
         * Makes the value publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param value
         */
        void share(Port port, Variable value);

        /**
         * Step the fmu for the given time
         *
         * @param deltaTime
         * @return
         */
        // TimeDeltaValue step(TimeDeltaValue deltaTime);

        //  TimeDeltaValue step(Variable<T, TimeDeltaValue> deltaTime);

        /**
         * Step the fmu for the given time
         *
         * @param deltaTime
         * @return
         */
        // TimeDeltaValue step(double deltaTime);

        /**
         * Get the current state
         *
         * @return
         */
        TimeTaggedState getState();

        /**
         * Sets the current state
         *
         * @param state
         * @return
         */
        Time setState(TimeTaggedState state);

        /**
         * Sets the current state based on the last retrieved state
         *
         * @return
         */

        Time setState();

        public interface PortVariableMap extends Map<Port, Variable> {
        }

        public interface PortValueMap extends Map<Port, Value> {
        }
    }

    interface Variable<T, V> {
        String getName();

        // T getValue();

        void setValue(V value);

        void setValue(V value, Scope<T> scope);

        Scope<T> getDeclaredScope();
    }
}
