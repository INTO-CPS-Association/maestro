package org.intocps.maestro.typechecker;

import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TypeResolver {

    final AstTypeResolver resolver = new AstTypeResolver();
    final MableAstFactory factory;
    final TypeCheckerErrors reporter;
    Map<INode, PType> resolvedTypes = new HashMap<>();

    public TypeResolver(MableAstFactory factory, TypeCheckerErrors reporter) {
        this.factory = factory;
        this.reporter = reporter;
    }

    //    public AFunctionType resolve(AFunctionDeclaration def, Environment env) {
    //
    //        AFunctionType type = new AFunctionType();
    //        type.setParameters(def.getFormals().stream().map(f -> f.getType().clone()).collect(Collectors.toList()));
    //        type.setResult(def.getReturnType().clone());
    //        return type;
    //    }
    //
    //    public AFunctionType resolve(ACallExp call, Environment env) {
    //        AFunctionType type = new AFunctionType();
    //        type.setParameters(def.getFormals().stream().map(f -> f.getType().clone()).collect(Collectors.toList()));
    //        type.setResult(call..getReturnType().clone());
    //        return type;
    //    }

    public PType resolve(INode node, Environment env) throws AnalysisException {
        PType type = resolvedTypes.getOrDefault(node, null);
        if (type == null) {
            type = node.apply(resolver, env);
            resolvedTypes.put(node, type);
        }
        return type;
    }


    private class AstTypeResolver extends QuestionAnswerAdaptor<Environment, PType> {

        @Override
        public PType caseString(String node, Environment question) throws AnalysisException {
            return factory.newAStringPrimitiveType();
        }

        @Override
        public PType caseBoolean(Boolean node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }

        @Override
        public PType caseInteger(Integer node, Environment question) throws AnalysisException {
            return MableAstFactory.newAIntNumericPrimitiveType();
        }

        @Override
        public PType caseDouble(Double node, Environment question) throws AnalysisException {
            return MableAstFactory.newARealNumericPrimitiveType();
        }

        @Override
        public PType caseLong(Long node, Environment question) throws AnalysisException {
            return factory.newAUIntPrimitiveType();
        }

        @Override
        public PType caseLexIdentifier(LexIdentifier node, Environment question) throws AnalysisException {
            return question.findName(node).apply(this, question);
        }


        @Override
        public PType caseARootDocument(ARootDocument node, Environment question) throws AnalysisException {
            return factory.newAUnknownType();
        }


        @Override
        public PType caseAImportedModuleCompilationUnit(AImportedModuleCompilationUnit node, Environment question) throws AnalysisException {
            return factory.newAUnknownType();
        }

        @Override
        public PType caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node,
                Environment question) throws AnalysisException {
            return factory.newAUnknownType();
        }


        @Override
        public PType caseAFunctionDeclaration(AFunctionDeclaration node, Environment question) throws AnalysisException {
            AFunctionType type = new AFunctionType();
            type.setParameters(node.getFormals().stream().map(f -> f.getType().clone()).collect(Collectors.toList()));
            type.setResult(node.getReturnType().clone());
            return type;
        }

        @Override
        public PType caseAVariableDeclaration(AVariableDeclaration node, Environment question) throws AnalysisException {
            return node.getType();
        }


        @Override
        public PType caseAExpInitializer(AExpInitializer node, Environment question) throws AnalysisException {
            return node.getExp().apply(this, question);
        }

        @Override
        public PType caseAArrayInitializer(AArrayInitializer node, Environment question) throws AnalysisException {
            return null;
        }


        @Override
        public PType caseAFormalParameter(AFormalParameter node, Environment question) throws AnalysisException {
            return node.getType();
        }


        @Override
        public PType caseAIdentifierExp(AIdentifierExp node, Environment question) throws AnalysisException {
            PDeclaration def = question.findName(node.getName());

            if (def == null) {
                reporter.report(0, "Definition not found for name: " + node.getName().getText(), node.getName().getSymbol());
                return null;
            }

            return def.apply(this, question);
        }


        @Override
        public PType caseALoadExp(ALoadExp node, Environment question) throws AnalysisException {

            PExp arg1 = node.getArgs().get(0);

            if (arg1 instanceof AIdentifierExp) {
                return factory.newAModuleType(((AIdentifierExp) arg1).getName());
            }

            return null;
        }

        @Override
        public PType caseAUnloadExp(AUnloadExp node, Environment question) throws AnalysisException {
            return factory.newAVoidType();
        }


        @Override
        public PType caseADotExp(ADotExp node, Environment question) throws AnalysisException {

            PType rootType = node.getRoot().apply(this, question);

            if (!(rootType instanceof AModuleType)) {
                return null;
            }

            Environment env = question;

            PDeclaration def = question.findName(((AModuleType) rootType).getName().getName());

            if (def instanceof AImportedModuleCompilationUnit) {
                env = new ModuleEnvironment(question, new ArrayList<>(((AImportedModuleCompilationUnit) def).getFunctions()));
            }

            return node.getExp().apply(this, env);
        }

        @Override
        public PType caseACallExp(ACallExp node, Environment question) throws AnalysisException {
            return node.getRoot().apply(this, question);
        }

        @Override
        public PType caseAStringLiteralExp(AStringLiteralExp node, Environment question) throws AnalysisException {
            return factory.newAStringPrimitiveType();
        }

        @Override
        public PType caseABoolLiteralExp(ABoolLiteralExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }


        @Override
        public PType caseAPlusBinaryExp(APlusBinaryExp node, Environment question) throws AnalysisException {

            //fixme: need to call type narrow, left + right

            return node.getLeft().apply(this, question);
        }

        @Override
        public PType caseAMinusBinaryExp(AMinusBinaryExp node, Environment question) throws AnalysisException {
            //fixme: need to call type narrow, left + right

            return node.getLeft().apply(this, question);
        }

        @Override
        public PType caseALessEqualBinaryExp(ALessEqualBinaryExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }

        @Override
        public PType caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }

        @Override
        public PType caseALessBinaryExp(ALessBinaryExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }

        @Override
        public PType caseAGreaterBinaryExp(AGreaterBinaryExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }

        @Override
        public PType caseAEqualBinaryExp(AEqualBinaryExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }

        @Override
        public PType caseANotEqualBinaryExp(ANotEqualBinaryExp node, Environment question) throws AnalysisException {
            return factory.newABooleanPrimitiveType();
        }


        //        @Override
        //        public PType caseABlockStm(ABlockStm node, Environment question) throws AnalysisException {
        //            return null;
        //        }
        //
        //        @Override
        //        public PType caseAAssigmentStm(AAssigmentStm node, Environment question) throws AnalysisException {
        //            return null;
        //        }
        //
        //        @Override
        //        public PType caseALocalVariableStm(ALocalVariableStm node, Environment question) throws AnalysisException {
        //            return null;
        //        }
        //
        //        @Override
        //        public PType caseAIfStm(AIfStm node, Environment question) throws AnalysisException {
        //            return null;
        //        }
        //
        //        @Override
        //        public PType caseAWhileStm(AWhileStm node, Environment question) throws AnalysisException {
        //            return factory.newAVoidType();
        //        }
        //
        //        @Override
        //        public PType caseAExpressionStm(AExpressionStm node, Environment question) throws AnalysisException {
        //            return factory.newAVoidType();
        //        }
        //
        //        @Override
        //        public PType caseAObservableStm(AObservableStm node, Environment question) throws AnalysisException {
        //            return factory.newAUnknownType();
        //        }
        //
        //        @Override
        //        public PType caseAExternalStm(AExternalStm node, Environment question) throws AnalysisException {
        //            return null;
        //        }

        @Override
        public PType defaultPStm(PStm node, Environment question) throws AnalysisException {
            return factory.newAVoidType();
        }

        @Override
        public PType defaultPType(PType node, Environment question) throws AnalysisException {
            return node;
        }

        @Override
        public PType createNewReturnValue(INode node, Environment question) throws AnalysisException {
            return factory.newAUnknownType();
        }

        @Override
        public PType createNewReturnValue(Object node, Environment question) throws AnalysisException {
            return factory.newAUnknownType();
        }
    }

}
