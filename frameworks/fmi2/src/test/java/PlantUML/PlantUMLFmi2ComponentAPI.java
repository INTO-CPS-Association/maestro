//package PlantUML;
//
//import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
//
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//public class PlantUMLFmi2ComponentAPI implements Fmi2Builder.Fmi2ComponentApi {
//    private final String variableName;
//    private final List<PlantUMLPort> ports = new ArrayList<>();
//    private final Map<String, PlantUMLPort> nameToPort = new HashMap<>();
//    private final Map<Integer, PlantUMLPort> valrefToPort = new HashMap<>();
//    private final PlantUMLBuilder builder;
//
//    public PlantUMLFmi2ComponentAPI(PlantUMLBuilder builder, String name, Map<String, Integer> portNameToValueReferences) {
//        this.variableName = name;
//        portNameToValueReferences.entrySet().forEach(entry -> {
//            PlantUMLPort p = new PlantUMLPort(builder, entry.getKey(), entry.getValue());
//            ports.add(p);
//            this.nameToPort.put(entry.getKey(), p);
//            this.valrefToPort.put(entry.getValue(), p);
//        });
//        this.builder = builder;
//    }
//
//    @Override
//    public List<Fmi2Builder.Port> getPorts(String... names) {
//        return Stream.of(names).map(x -> nameToPort.get(x)).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Fmi2Builder.Port> getPorts(int... valueReferences) {
//        return Stream.of(valueReferences).map(x -> valrefToPort.get(x)).collect(Collectors.toList());
//    }
//
//    @Override
//    public Fmi2Builder.Port getPort(String name) {
//        return nameToPort.get(name);
//    }
//
//    @Override
//    public Fmi2Builder.Port getPort(int valueReference) {
//        return valrefToPort.get(valueReference);
//    }
//
//    @Override
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(Fmi2Builder.Port... ports) {
//        return get(builder.getCurrentScope(), ports);
//    }
//
//    @Override
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(Fmi2Builder.Scope scope, Fmi2Builder.Port... ports) {
//        if (scope instanceof PlantUMLScope) {
//            return get(scope, ports);
//        }
//        return null;
//    }
//
//
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(PlantUMLScope scope, Fmi2Builder.Port... ports) {
//        Map<Fmi2Builder.Port, Fmi2Builder.Value> returnMap = new HashMap<>();
//        List<String> variablesToStoreResultIn = new ArrayList<>();
//        Stream.of(ports).forEach(x -> {
//            variablesToStoreResultIn.add(this.variableName + "_" + x.getName());
//            returnMap.put(x, new PlantUMLValue("value_" + x.getName()));
//        });
//        scope.scopeStrings.add(String.format("Maestro -> %s: [%s] = get(%s)", this.variableName, String.join(",", variablesToStoreResultIn),
//                Stream.of(ports).map(x -> x.getName()).collect(Collectors.joining(","))));
//        return returnMap;
//    }
//
//    @Override
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get() {
//        return this.get(this.ports.toArray(new Fmi2Builder.Port[0]));
//    }
//
//    @Override
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(int... valueReferences) {
//        Fmi2Builder.Port[] a = Arrays.stream(valueReferences).mapToObj(this.valrefToPort::get).toArray(Fmi2Builder.Port[]::new);
//        return this.get(a);
//    }
//
//    @Override
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> get(String... names) {
//        return this.get(Arrays.stream(names).map(x -> this.nameToPort.get(x)).toArray(Fmi2Builder.Port[]::new));
//    }
//
//    @Override
//    public Map<Fmi2Builder.Port, Fmi2Builder.Value> getAndShare(String... names) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Value getSingle(String name) {
//        return this.get(name).values().iterator().next();
//    }
//
//    @Override
//    public void set(Fmi2Builder.Scope scope, Map<Fmi2Builder.Port, Fmi2Builder.Value> value) {
//        if (scope instanceof PlantUMLScope) {
//            this.set((PlantUMLScope) scope, value);
//        }
//    }
//
//    public void set(PlantUMLScope scope, Map<Fmi2Builder.Port, Fmi2Builder.Value> value) {
//        List<String> portNames = new ArrayList<>();
//        List<String> values = new ArrayList<>();
//        value.entrySet().forEach(x -> {
//            portNames.add(x.getKey().getName());
//            values.add(((PlantUMLValue) x.getValue()).getValue());
//        });
//        scope.scopeStrings
//                .add(String.format("Maestro -> %s: set([%s],[%s])", this.variableName, String.join(",", portNames), String.join(",", values)));
//    }
//
//
//    @Override
//    public void set(Map<Fmi2Builder.Port, Fmi2Builder.Value> value) {
//        this.set(this.builder.getCurrentScope(), value);
//
//
//    }
//
//    @Override
//    public void set(String... names) {
//
//
//    }
//
//    @Override
//    public void setInt(Map<Integer, Fmi2Builder.Value> values) {
//
//    }
//
//    @Override
//    public void setString(Map<String, Fmi2Builder.Value> value) {
//
//    }
//
//    @Override
//    public void share(Map<Fmi2Builder.Port, Fmi2Builder.Value> values) {
//
//    }
//
//    @Override
//    public void share(Fmi2Builder.Port port, Fmi2Builder.Value value) {
//
//    }
//
//    @Override
//    public Fmi2Builder.TimeDeltaValue step(Fmi2Builder.TimeDeltaValue deltaTime) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.TimeDeltaValue step(Fmi2Builder.Variable<Fmi2Builder.TimeDeltaValue> deltaTime) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.TimeDeltaValue step(double deltaTime) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.TimeTaggedState getState() {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Time setState(Fmi2Builder.TimeTaggedState state) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Time setState() {
//        return null;
//    }
//}
