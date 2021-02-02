//package PlantUML;
//
//import org.intocps.maestro.framework.fmi2.api.Fmi2Builder;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class PlantUMLScope implements Fmi2Builder.Scope {
//    public List<String> scopeStrings = new ArrayList<>();
//    public List<Fmi2Builder.Scope> childrenScopes;
//    public Map<String, Fmi2Builder.Value> variables;
//
//
//    @Override
//    public Fmi2Builder.WhileScope enterWhile(Fmi2Builder.LogicBuilder.Predicate predicate) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.IfScope enterIf(Fmi2Builder.LogicBuilder.Predicate predicate) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Scope leave() {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.LiteralCreator literalCreator() {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.VariableCreator variableCreator() {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Value store(double value) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Value store(Fmi2Builder.Value tag) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.Value store(Fmi2Builder.Value tag, Fmi2Builder.Value value) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.MDouble doubleFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.MInt intFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
//        return null;
//    }
//
//    @Override
//    public Fmi2Builder.MBoolean booleanFromExternalFunction(String functionName, Fmi2Builder.Value... arguments) {
//        return null;
//    }
//}
