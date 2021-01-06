package org.intocps.maestro.framework.fmi2.api;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class Fmi2Builder {

    /**
     * Generates a handle to a loaded fmu
     *
     * @param path
     * @return
     */
    public abstract Fmu2Api createFmu(File path);

    //    public abstract void popScope();

    /**
     * Gets the default scope
     *
     * @return
     */
    public abstract Scope getDefaultScope();
    //    public abstract void pushScope(Scope scope);

    /**
     * Get handle to the current time
     *
     * @return
     */
    public abstract Time getCurrentTime();

    /**
     * Gets a specific time from a number
     *
     * @param time
     * @return
     */
    public abstract Time getTime(double time);

    /**
     * Gets a tag to the last value obtained for the given port
     *
     * @param port
     * @return
     */
    public abstract Value getCurrentLinkedValue(Port port);

    /**
     * New boolean that can be used as a predicate
     *
     * @param
     * @return
     */

    /**
     * Scoping functions
     */
    public interface Scoping {
        WhileScope enterWhile(LogicBuilder.Predicate predicate);

        IfScope enterIf(LogicBuilder.Predicate predicate);

        Scope leave();

        public abstract LiteralCreator literalCreator();

        public abstract VariableCreator variableCreator();
    }

    /**
     * Basic scope. Allows a value to be stored or override a tag
     */
    public interface Scope extends Scoping {
        /**
         * Store a given value
         *
         * @param value
         * @return
         */
        Value store(double value);

        /**
         * Store the given value and get a tag for it. Copy
         *
         * @param tag
         * @return
         */
        Value store(Value tag);

        /**
         * Override a tags value from an existing value. Copy
         *
         * @param tag
         * @param value
         * @return
         */
        Value store(Value tag, Value value);
    }

    /**
     * If scope, default scope is then
     */
    public interface IfScope extends Scope {
        /**
         * Switch to then scope
         *
         * @return
         */
        Scope enterThen();

        /**
         * Switch to else scope
         *
         * @return
         */
        Scope enterElse();
    }

    /**
     * While
     */
    public interface WhileScope extends Scope {
    }

    /**
     * Handle for an fmu for the creation of component
     */
    public interface Fmu2Api {
        Fmi2ComponentApi create();
    }


    public interface LogicBuilder {

        Predicate isEqual(Port a, Port b);

        Predicate isEqual(Time a, Time b);

        Predicate isLess(Time a, Time b);

        <T> Predicate isLess(Numeric<T> a, Numeric<T> b);

        Predicate isGreater(Value a, double b);

        Predicate fromValue(Value value);

        Predicate fromExternalFunction(String name, Value... args);


        interface Predicate {
            Predicate and(Predicate p);

            Predicate or(Predicate p);

            Predicate not();
        }

    }

    public interface Numeric<A> {
        void set(A value);

        A get();
    }

    public interface MBoolean extends LogicBuilder.Predicate {
        void set(Boolean value);

        Boolean get();
    }

    /**
     * Current time
     */
    public interface Time {
    }

    /**
     * time value from number
     */
    public interface TimeValue extends Time {
    }

    /**
     * Delta value for a time
     */
    public interface TimeDeltaValue extends Time, MDouble {
    }

    /**
     * Interface for an fmi compoennt
     */
    public interface Fmi2ComponentApi {

        /**
         * Get ports by name
         *
         * @param names
         * @return
         */
        List<Port> getPorts(String... names);

        /**
         * Get ports by ref val
         *
         * @param valueReferences
         * @return
         */
        List<Port> getPorts(int... valueReferences);

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
        Map<Port, Value> get(Port... ports);

        /**
         * Get all (linked) port values
         *
         * @return
         */
        Map<Port, Value> get();

        /**
         * get filter by value reference
         *
         * @param valueReferences
         * @return
         */
        Map<Port, Value> get(int... valueReferences);

        /**
         * Get filter by names
         *
         * @param names
         * @return
         */
        Map<Port, Value> get(String... names);

        Map<Port, Value> getAndShare(String... names);

        /**
         * Get the value of a single port
         *
         * @param name
         * @return
         */
        Value getSingle(String name);

        /**
         * Set port values (if ports is not from this fmu then the links are used to remap)
         *
         * @param value
         */
        void set(Map<Port, Value> value);


        /**
         * Set this fmu port by name and link
         */
        void set(String... names);


        /**
         * Set this fmu ports by val ref
         *
         * @param values
         */
        void setInt(Map<Integer, Value> values);

        /**
         * Set this fmy ports by name
         *
         * @param value
         */
        void setString(Map<String, Value> value);

        /**
         * Makes the values publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param values
         */
        void share(Map<Port, Value> values);

        /**
         * Makes the value publicly available to all linked connections. On next set these ports will be resolved to the values given for
         * other fmu
         *
         * @param value
         */
        void share(Port port, Value value);

        /**
         * Step the fmu for the given time
         *
         * @param deltaTime
         * @return
         */
        TimeDeltaValue step(TimeDeltaValue deltaTime);

        /**
         * Step the fmu for the given time
         *
         * @param deltaTime
         * @return
         */
        TimeDeltaValue step(double deltaTime);

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
    }

    public interface TimeTaggedState {
        public void release();
    }

    public interface Port {
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
        int getPortReferenceValue();

        /**
         * Link the current port to the receiving port. After this the receiving port will resolve its linked value to the value of this port
         *
         * @param receiver
         */
        void linkTo(Port... receiver);

        /**
         * Break the link from the receivers or all if none is given
         *
         * @param receiver
         */
        void breakLink(Port... receiver);
    }

    public interface Value {
        static Value of(double a) {
            return null;
        }
    }

    public interface VariableCreator {
        Variable<MBoolean> createBoolean(String label);

        Variable<MInt> createInteger(String label);

        Variable<TimeDeltaValue> createTimeDeltaValue(String label);

        Variable<MDouble> createDouble(String label);
    }

    public interface LiteralCreator {
        MInt createMInt(Integer value);
    }

    public interface MDouble extends Numeric<Double> {
    }

    public interface MInt extends Numeric<Integer> {
        void decrement();
    }

    public interface Variable<T> {
        void setName();

        T getValue();

        void setValue(T value);
    }
}
