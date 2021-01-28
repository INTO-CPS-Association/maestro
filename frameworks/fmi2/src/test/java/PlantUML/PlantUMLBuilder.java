//package PlantUML;
//
//import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class PlantUMLBuilder extends Fmi2Builder {
//    List<String> plantUML = new ArrayList<>();
//    Map<String, List<String>> scopes = new HashMap<>();
//    PlantUMLScope rootScope = new PlantUMLScope();
//    PlantUMLScope currentScope = rootScope;
//
//    public PlantUMLBuilder() {
//    }
//
//    @Override
//    public Fmu2Api createFmu(String name, File path) {
//        currentScope.scopeStrings.add(String.format("Maestro -> %s **: loadFMU(%s)", name, path));
//        return new PlantUMLFmu2Api(name, this);
//
//    }
//
//    @Override
//    public PlantUMLScope getRootScope() {
//        return currentScope;
//    }
//
//    @Override
//    public PlantUMLScope getCurrentScope() {
//        return currentScope;
//    }
//
//    @Override
//    public Time getCurrentTime() {
//        return null;
//    }
//
//    @Override
//    public Time getTime(double time) {
//        return null;
//    }
//
//    @Override
//    public Value getCurrentLinkedValue(Port port) {
//        return null;
//    }
//
//    @Override
//    public TimeDeltaValue createTimeDeltaValue(MDouble getMinimum) {
//        return null;
//    }
//}
