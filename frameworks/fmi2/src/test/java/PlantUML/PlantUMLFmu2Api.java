//package PlantUML;
//
//import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
//
//public class PlantUMLFmu2Api implements Fmi2Builder.Fmu2Api {
//    private final PlantUMLBuilder builder;
//    String fmuVariableName;
//
//    public PlantUMLFmu2Api(String fmuVariableName, PlantUMLBuilder builder) {
//        this.fmuVariableName = fmuVariableName;
//        this.builder = builder;
//    }
//
//    @Override
//    public Fmi2Builder.Fmi2ComponentApi create(String name) {
//        return create(name, builder.currentScope);
//    }
//
//    @Override
//    public Fmi2Builder.Fmi2ComponentApi create(String name, Fmi2Builder.Scope scope) {
//        PlantUMLScope scope_ = (PlantUMLScope) scope;
//        scope_.scopeStrings.add(String.format("%s -> %s **: createInstance", fmuVariableName, name));
//        return new PlantUMLFmi2ComponentAPI(this.builder, name, null);
//    }
//}
