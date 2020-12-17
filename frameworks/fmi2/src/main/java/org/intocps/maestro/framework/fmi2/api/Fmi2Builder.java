package org.intocps.maestro.framework.fmi2.api;

import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class Fmi2Builder {

    public abstract Fmu2Api createFmu(File path);

    //    public abstract void popScope();

    public abstract Scope getDefaultScope();
    //    public abstract void pushScope(Scope scope);

    public abstract Time getCurrentTime();

    public abstract Time getTime(double time);

    public interface Scoping {
        WhileScope enterWhile(LogicBuilder.Predicate predicate);

        IfScope enterIf(LogicBuilder.Predicate predicate);

        Scope leave();
    }

    public interface Scope extends Scoping {
    }

    public interface IfScope extends Scope {

        // void test(LogicBuilder.Predicate predicate);
        Scope enterThen();

        Scope enterElse();
    }

    public interface WhileScope extends Scope {
        void condition(LogicBuilder.Predicate p);
    }

    public interface Fmu2Api {
        Fmi2ComponentApi create();
    }


    public interface LogicBuilder {

        Predicate isEqual(Port a, Port b);

        Predicate isEqual(Time a, Time b);

        Predicate isLess(Time a, Time b);

        Predicate isGreater(TimeTaggedValue a, double b);


        interface Predicate {
            Predicate and(Predicate p);

            Predicate or(Predicate p);

            Predicate not();
        }

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

    public interface Fmi2ComponentApi {

        List<Port> getPorts(String... names);

        List<Port> getPorts(int... valueReferences);

        Port getPort(String name);

        Port getPort(int valueReference);

        Map<Port, TimeTaggedValue> get(Port... ports);

        Map<Port, TimeTaggedValue> get();

        Map<Port, TimeTaggedValue> get(int... valueReferences);

        Map<Port, TimeTaggedValue> get(String... names);

        TimeTaggedValue getSingle(String names);

        void share(Map<Port, TimeTaggedValue> values);

        void share(Port port, TimeTaggedValue value);

        void step(double step);

        TimeTaggedState getState();
    }

    public interface TimeTaggedState {
        //        public void release();
    }

    public interface Port {
        String getName();

        void linkTo(Port... receiver);

        void breakLink(Port... receiver);
    }

    public interface TimeTaggedValue {
    }
}
