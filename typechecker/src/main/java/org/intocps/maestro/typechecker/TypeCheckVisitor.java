package org.intocps.maestro.typechecker;


import org.intocps.maestro.ast.*;
import org.intocps.maestro.ast.analysis.AnalysisException;
import org.intocps.maestro.ast.analysis.DepthFirstAnalysisAdaptor;
import org.intocps.maestro.ast.analysis.QuestionAnswerAdaptor;
import org.intocps.maestro.ast.node.*;
import org.intocps.maestro.core.InternalException;
import org.intocps.maestro.core.messages.IErrorReporter;
import org.intocps.maestro.typechecker.context.Context;
import org.intocps.maestro.typechecker.context.GlobalContext;
import org.intocps.maestro.typechecker.context.LocalContext;
import org.intocps.maestro.typechecker.context.ModulesContext;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.intocps.maestro.ast.MableAstFactory.*;

class TypeCheckVisitor extends QuestionAnswerAdaptor<Context, PType> {
    private final IErrorReporter errorReporter;
    private final Map<INode, PType> resolvedTypes = new HashMap<>();
    TypeComparator typeComparator;
    MableAstFactory astFactory;
    Map<INode, PType> checkedTypes = new HashMap<>();

    public TypeCheckVisitor(IErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        this.typeComparator = new TypeComparator();
        astFactory = new MableAstFactory();
    }

    static SNumericPrimitiveType detectType(int value) {
        if (value <= 0xff) {
            return new AByteNumericPrimitiveType();
        } else if (value <= Short.MAX_VALUE) {
            return new AShortNumericPrimitiveType();
        } else {
            return new AIntNumericPrimitiveType();
        }
    }

    static SNumericPrimitiveType detectType(long value) {
        if (value <= Integer.MAX_VALUE) {
            return detectType((int) value);
        } else {
            return new ALongNumericPrimitiveType();
        }
    }

    static SNumericPrimitiveType detectType(double value) {

        if ((value % 1) == 0) {
            //whole number
            return detectType((long) value);
        } else if (value <= Float.MAX_VALUE) {
            return new AFloatNumericPrimitiveType();
        } else {
            return new ARealNumericPrimitiveType();
        }
    }

    @Override
    public PType createNewReturnValue(INode node, Context info) throws AnalysisException {
        return null;
    }

    @Override
    public PType createNewReturnValue(Object node, Context info) throws AnalysisException {
        return null;
    }

    @Override
    public PType defaultPInitializer(PInitializer node, Context ctxt) throws AnalysisException {
        return super.defaultPInitializer(node, ctxt);
    }

    @Override
    public PType caseAErrorStm(AErrorStm node, Context ctxt) throws AnalysisException {
        return store(node, new AVoidType());
    }

    @Override
    public PType caseATryStm(ATryStm node, Context ctxt) throws AnalysisException {

        if (node.getBody() != null) {
            node.getBody().apply(this, ctxt);
        }
        if (node.getFinally() != null) {
            node.getFinally().apply(this, ctxt);
        }
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType defaultPType(PType node, Context ctxt) throws AnalysisException {
        return store(node, node.clone());
    }

    @Override
    public PType caseAArrayType(AArrayType node, Context ctxt) throws AnalysisException {
        //FIXME should not need clone
        return store(node, MableAstFactory.newAArrayType(node.getType().apply(this, ctxt).clone()));
    }

    @Override
    public PType caseAExpInitializer(AExpInitializer node, Context ctxt) throws AnalysisException {
        return store(node, node.getExp().apply(this, ctxt));
    }

    @Override
    public PType caseARefExp(ARefExp node, Context question) throws AnalysisException {
        PType type = node.getExp().apply(this, question);
        return store(node, new AReferenceType(type.clone()));
    }

    @Override
    public PType caseAReferenceType(AReferenceType node, Context question) throws AnalysisException {
        PType type = node.getType().apply(this, question);
        return store(node, new AReferenceType(type.clone()));
    }

    @Override
    public PType caseANullExp(ANullExp node, Context question) throws AnalysisException {
        return store(node, MableAstFactory.newAUnknownType());
    }

    @Override
    public PType caseAInstanceMappingStm(AInstanceMappingStm node, Context question) throws AnalysisException {
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAFmuMappingStm(AFmuMappingStm node, Context question) throws AnalysisException {
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAArrayIndexExp(AArrayIndexExp node, Context ctxt) throws AnalysisException {

        for (PExp index : node.getIndices()) {
            PType type = index.apply(this, ctxt);
            if (!typeComparator.compatible(new AIntNumericPrimitiveType(), type)) {
                errorReporter.report(-5, "Array index must be an integer actual: " + type, null);
            }
        }
        PType type = node.getArray().apply(this, ctxt);

        if (!typeComparator.compatible(AArrayType.class, type)) {
            errorReporter.report(0, "Only array types can be indexed", null);
            return store(node, MableAstFactory.newAUnknownType());
        }

        PType iterationType = type.clone();
        //TODO: Step down through the types according to the size of the indicesLinkedList
        for (int i = 0; i < node.getIndices().size(); i++) {
            // If this happens, then we are finished already.
            if (!(iterationType instanceof AArrayType)) {
                errorReporter.report(0, "Cannot index none array expression", null);
                return store(node, MableAstFactory.newAUnknownType());
            } else {
                iterationType = ((AArrayType) iterationType).getType();
            }
        }
        return store(node, iterationType);
    }

    @Override
    public PType caseALoadExp(ALoadExp node, Context ctxt) throws AnalysisException {

        // See https://github.com/INTO-CPS-Association/maestro/issues/66
        if (node.getArgs() == null || node.getArgs().isEmpty()) {
            errorReporter.report(-5, "Wrong number of arguments to load. At least a type is required: " + node, null);
        } else {

            PExp loaderNameArg = node.getArgs().get(0);
            if (!(loaderNameArg instanceof AStringLiteralExp)) {
                errorReporter.report(-5, "First argument must be a string representing a loader.: " + node, null);
            }

            for (int i = 1; i < node.getArgs().size(); i++) {
                node.getArgs().get(i).apply(this, ctxt);
            }

            //            if (typeArg instanceof AStringLiteralExp) {
            //
            //                String moduleName = ((AStringLiteralExp) typeArg).getValue();
            //
            //                PDeclaration decl = ctxt.findDeclaration(new LexIdentifier(moduleName, null));
            //                if (decl == null) {
            //                    errorReporter.report(0, "Decleration not found: " + moduleName, null);
            //                } else {
            //                    PType moduleType = checkedTypes.get(decl);
            //                    if (moduleType instanceof AModuleType) {
            //                        //allow assignment to any type
            //                        //return store(node, moduleType.clone());
            //                    } else {
            //                        errorReporter.report(0, "Argument 1 does not refer to a module type: " + moduleName, null);
            //                    }
            //                }
            //
            //
            //            }
        }

        return store(node, newAUnknownType());
    }

    @Override
    public PType caseAMinusUnaryExp(AMinusUnaryExp node, Context ctxt) throws AnalysisException {

        return store(node, checkUnaryNumeric(node, ctxt));
    }

    private PType checkUnaryNumeric(SUnaryExp node, Context ctxt) throws AnalysisException {
        PType type = node.getExp().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, type)) {
            errorReporter.report(-5, "Expected a numeric expression: " + type, null);
        }
        return store(node, type);
    }

    @Override
    public PType caseAPlusUnaryExp(APlusUnaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkUnaryNumeric(node, ctxt));
    }

    @Override
    public PType caseAArrayInitializer(AArrayInitializer node, Context ctxt) throws AnalysisException {

        PType type = newAUnknownType();

        if (node.getExp().size() > 0) {

            List<PType> types = new Vector<>();
            for (PExp exp : node.getExp()) {
                types.add(exp.apply(this, ctxt));
            }

            type = narrow(types);

            if (type == null) {
                return newAUnknownType();
            }

            for (int i = 0; i < node.getExp().size(); i++) {
                PType elementType = node.getExp().get(i).apply(this, ctxt);
                if (!typeComparator.compatible(type, elementType)) {
                    //is ok
                    if (!(node.parent() instanceof AVariableDeclaration &&
                            ((AVariableDeclaration) node.parent()).getType() instanceof AUnknownType)) {
                        //we only print this id the variable type is known
                        errorReporter.warning(0, "Array initializer types mixed. Expected: " + type + " but found: " + elementType, null);
                    }
                    type = newAUnknownType();
                }
            }
        } else {
//            errorReporter.report(0, "Array initializer must not be empty: " + node, null);
        }
        return store(node, newAArrayType(type).clone());
    }

    PType narrow(List<PType> types) {
        if (types.isEmpty()) {
            return null;
        }

        boolean isNumeric = types.stream().allMatch(t -> t instanceof SNumericPrimitiveType);

        //FIXME we allow bool to act as a numeric type in array initializers
        isNumeric |= types.stream().anyMatch(t -> t instanceof ABooleanPrimitiveType);

        PType type = types.get(0);
        for (int i = 1; i < types.size(); i++) {
            PType from = types.get(i);
            if (!typeComparator.compatible(type, from)) {
                if (isNumeric) {
                    //for numeric types we expand the type if not fitting
                    type = from;
                } else {
                    errorReporter.report(-5,
                            String.format("Array initializer contains mixed types. Expected '%s', got '%s' at position %d", type + "", from + "", i),
                            null);
                }
            }
        }
        return type;
    }

    @Override
    public PType caseACallExp(ACallExp node, Context ctxt) throws AnalysisException {

        String functionDisplayName = (node.getObject() == null ? "" : node.getObject() + ".") + node.getMethodName();
        PDeclaration def = null;


        if (node.getObject() != null) {
            // This is a module
            // Ensure that the object is of module type
            PType objectType = node.getObject().apply(this, ctxt);
            PDeclaration decl = new DefinitionFinder(this.checkedTypes::get).find(node.getObject(), ctxt);

            if (decl == null) {
                errorReporter.report(0, "Unknown object type: '" + node.getObject() + "' in function call: " + functionDisplayName,
                        node.getMethodName().getSymbol());
            }
            if (decl instanceof AModuleDeclaration) {
                if (node.getExpand() != null) {
                    def = ctxt.findDeclaration(((AModuleType) objectType).getName(), node.getMethodName());
                } else {
                    errorReporter.report(0, "Static calls not allowed on type: '" + node.getObject() + "' in function call: " + functionDisplayName,
                            node.getMethodName().getSymbol());
                    return store(node, newAUnknownType());
                }
            } else if (decl instanceof AVariableDeclaration) {


                //ok so its variable to lets see if its type is callable
                if (objectType instanceof AModuleType) {
                    def = ctxt.findDeclaration(((AModuleType) objectType).getName(), node.getMethodName());
                }


            }

            //            if (node.getExpand() != null) {
            //                //static call required
            //                if (objectType instanceof AModuleType) {
            //                    def = ctxt.findDeclaration(((AModuleType) objectType).getName(), node.getMethodName());
            //                } else {
            //                    errorReporter.report(0, "Unknown object type: '" + node.getObject() + "' in function call: " + functionDisplayName,
            //                            node.getMethodName().getSymbol());
            //                }
            //            } else {
            //                //static call not allowed
            //                if (objectType instanceof AVarType && ((AVarType) objectType).getType() instanceof AModuleType) {
            //                    def = ctxt.findDeclaration(((AModuleType) ((AVarType) objectType).getType()).getName(), node.getMethodName());
            //                } else {
            //                    errorReporter.report(0, "Unknown object type: '" + node.getObject() + "' in function call: " + functionDisplayName,
            //                            node.getMethodName().getSymbol());
            //                }
            //            }


        } else {
            def = ctxt.findDeclaration(node.getMethodName());
        }

        if (def == null) {
            errorReporter.report(0, "Call declaration not found: " + functionDisplayName, node.getMethodName().getSymbol());
        } else {
            PType type = checkedTypes.get(def).clone();


            if (type instanceof AFunctionType) {
                AFunctionType targetType = (AFunctionType) type;


                List<PType> callArgTypes = new Vector<>();

                //                if (targetType.getParameters().size() != node.getArgs().size()) {
                //                    errorReporter.report(0, "Wrong number of arguments in call '" + node.getMethodName().getText() + "'. Expected: " +
                //                            targetType.getParameters().size() + " " + "found: " + node.getArgs().size(), node.getMethodName().getSymbol());
                //                } else {
                for (int i = 0; i < node.getArgs().size(); i++) {
                    PExp arg = node.getArgs().get(i);
                    PType argType = arg.apply(this, ctxt);
                    callArgTypes.add(argType.clone());
                    //                        PType argTargetType = targetType.getParameters().get(i);
                    //                        if (!typeComparator.compatible(argTargetType, argType)) {
                    //                            errorReporter.report(-5, "Parameter type: " + argType + " not matching expected type: " + argTargetType, null);
                    //
                    //                        }
                }
                //                }
                //                if (targetType.getParameters().size() != node.getArgs().size()) {
                //                    System.out.println();
                //                }

                AFunctionType callType = new AFunctionType();
                callType.setResult(targetType.getResult().clone());
                callType.setParameters(callArgTypes);

                if (!typeComparator.compatible(targetType, callType)) {
                    errorReporter.report(0,
                            "Function '" + functionDisplayName + "' applied with wrong argument types. Expected: " + targetType + " " + "Actual: " +
                                    callType, node.getMethodName().getSymbol());
                }


                return store(node, callType);
            } else {
                errorReporter.report(0, "Expected a function decleration for '" + functionDisplayName + "' : " + def.getName().getText(),
                        def.getName().getSymbol());
            }
        }
        return store(node, newAUnknownType());

    }

    @Override
    public PType caseANameType(ANameType node, Context ctxt) throws AnalysisException {

        PDeclaration def = ctxt.findDeclaration(node.getName());

        if (!(def instanceof AModuleDeclaration)) {
            if (def == null) {
                errorReporter.report(0, "Unresolved name: " + node.getName().getText() + ". Did you forgot to include " + "a module?",
                        node.getName().getSymbol());
            } else {
                errorReporter.report(0, "Type: " + node.getName().getText() + " Is not a module.", node.getName().getSymbol());
            }
            return store(node, newAUnknownType());
        }


        PType type = checkedTypes.get(def).clone();
        if (type == null) {
            errorReporter.report(-5, "No type found for decleration " + node.getName(), null);
        } else {
            return store(node, type);
        }
        return store(node, newAUnknownType());
    }

    @Override
    public PType defaultSNumericPrimitiveType(SNumericPrimitiveType node, Context ctxt) throws AnalysisException {
        return store(node, node.clone());
    }

    @Override
    public PType caseABooleanPrimitiveType(ABooleanPrimitiveType node, Context ctxt) throws AnalysisException {
        return store(node, node.clone());
    }

    @Override
    public PType caseAVariableDeclaration(AVariableDeclaration node, Context ctxt) throws AnalysisException {

        //check shadowing
        LexIdentifier shadowingOrigin = findShadowingOrigin(node, null, true);
        boolean shadowReported = false;
        if (shadowingOrigin != null) {
            shadowReported = true;
            errorReporter.report(0,
                    "Name '" + node.getName().getText() + "' shadows previous definition " + shadowingOrigin.getSymbol() + "" + " " + ".",
                    node.getName().getSymbol());
        }

        if (!shadowReported) {
            shadowingOrigin = findShadowingOrigin(node, null, false);
            if (shadowingOrigin != null) {
                errorReporter.warning(0,
                        "Name '" + node.getName().getText() + "' shadows previous definition " + shadowingOrigin.getSymbol() + "" + " " + ".",
                        node.getName().getSymbol());
            }
        }

        PType type = node.getType().apply(this, ctxt);


        if (node.getSize() != null && !node.getSize().isEmpty()) {
            if (type instanceof AArrayType) {
                errorReporter.report(0, "Either declare a new array using C-style brackets or use Java style for declaring an array type.", null);
            } else {
                //using C style for initializer so we need to convert the simple type into an array
                for (int i = 0; i < node.getSize().size(); i++) {
                    type = new AArrayType(type);
                }
            }
        }

        if (!node.getSize().isEmpty()) {
            //only one dimensional arrays for now
            //type = MableAstFactory.newAArrayType(type);

            for (PExp sizeExp : node.getSize()) {
                PType sizeType = sizeExp.apply(this, ctxt);
                if (!typeComparator.compatible(MableAstFactory.newIntType(), sizeType)) {
                    errorReporter.report(0, "Array size must be int. Actual: " + sizeType, null);
                }
            }
        }

        if (node.getExternal() != null && node.getExternal() && node.getInitializer() != null) {
            errorReporter.report(0, "External variable declarations cannot have an initializer", node.getName().getSymbol());

        } else if (node.getInitializer() != null) {
            PType initType = node.getInitializer().apply(this, ctxt);

            BiFunction<PType,PType,Boolean> isDoubleToFloatAssignment = (declType, assignType)->
                    typeComparator.compatible(declType,new AFloatNumericPrimitiveType())&&typeComparator.compatible(assignType,new ARealNumericPrimitiveType());

            if (!typeComparator.compatible(type, initType) && !isDoubleToFloatAssignment.apply(type,initType)) {
                errorReporter.report(0, type + " cannot be initialized with type: " + initType, node.getName().getSymbol());
            }

            //additional check for array initializer, not complete but should do most simple cases
            if (node.getInitializer() instanceof AArrayInitializer) {
                AArrayInitializer initializer = (AArrayInitializer) node.getInitializer();

                Integer declaredSize = null;
                if (!node.getSize().isEmpty() && node.getSize().get(0) instanceof AIntLiteralExp) {
                    declaredSize = ((AIntLiteralExp) node.getSize().get(0)).getValue();
                }

                if (!node.getSize().isEmpty() || !initializer.getExp().isEmpty()) {
                    if (declaredSize == null || declaredSize != initializer.getExp().size()) {
                        if (declaredSize == null) {
                            errorReporter.report(0,
                                    "Array declared without a size. Please fix by declaring with size: '" + +initializer.getExp().size() + "'", null);
                        } else {
                            errorReporter.report(0, "Array declared with different size than initializer. Declared size: '" + declaredSize +
                                    "'. Size of initializer: " + initializer.getExp().size(), null);
                        }

                    }

                }
            }
        }
        return store(node, type);
    }

    private LexIdentifier findShadowingOrigin(AVariableDeclaration node, PStm currentStm, boolean caseSensitive) {

        if (currentStm == null) {
            currentStm = node.getAncestor(PStm.class);
            if (currentStm == null) {
                return null;
            }
        }

        Function<PStm, SBlockStm> findAncestorSBlockStm = (stm) -> {
            //The getAncestor function acts as an identity function when the current node matches the requested type and therefore it is necessary to
            // call parent
            INode stm_ = stm instanceof SBlockStm ? stm.parent() : stm;
            if (stm_ != null) {
                return stm_.getAncestor(SBlockStm.class);
            }
            return null;
        };
        // In a regular simulation specification a block always has a parent. However, in a test specification a block does not necessary have a
        // parent.
        //since the getAncestor function acts as an identity function when the current node matches the requested type it is nessesary to call parent
        SBlockStm block = findAncestorSBlockStm.apply(currentStm);

        if (block == null) {
            return null;
        }

        while (currentStm.parent() != null && currentStm.parent() instanceof PStm && currentStm.parent() != block) {
            currentStm = (PStm) currentStm.parent();
        }

        if (currentStm.parent() == block) {
            //only search from before this statement

            for (int i = block.getBody().indexOf(currentStm) - 1; i >= 0 && i < block.getBody().size(); i--) {
                PStm s = block.getBody().get(i);
                if (s instanceof ALocalVariableStm) {
                    String declName = ((ALocalVariableStm) s).getDeclaration().getName().getText();
                    //match found is does shadow a name
                    if ((caseSensitive && declName.equals(node.getName().getText())) ||
                            (!caseSensitive && declName.equalsIgnoreCase(node.getName().getText()))) {
                        return ((ALocalVariableStm) s).getDeclaration().getName();
                    }
                }
            }
            return findShadowingOrigin(node, block, caseSensitive);
        } else {
            if (currentStm.parent() != null && currentStm.parent() instanceof PStm) {
                return findShadowingOrigin(node, (PStm) currentStm.parent(), caseSensitive);
            }
        }

        return null;
    }

    private <T extends PType> T store(INode node, T type) {
        checkedTypes.put(node, type);
        return type;
    }

    @Override
    public PType defaultPExp(PExp node, Context ctxt) throws AnalysisException {
        throw new InternalException(-5, "Node unknown to typechecker: " + node + " type: " + node.getClass().getSimpleName());
    }

    @Override
    public PType defaultPStm(PStm node, Context ctxt) throws AnalysisException {
        throw new InternalException(-5, "Node unknown to typechecker: " + node + " type: " + node.getClass().getSimpleName());
    }

    @Override
    public PType caseATransferStm(ATransferStm node, Context question) throws AnalysisException {
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseATransferAsStm(ATransferAsStm node, Context question) throws AnalysisException {
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAConfigStm(AConfigStm node, Context question) throws AnalysisException {
        return MableAstFactory.newAVoidType();
    }

    @Override
    public PType caseAConfigFramework(AConfigFramework node, Context question) throws AnalysisException {
        return MableAstFactory.newAUnknownType();
    }

    @Override
    public PType caseAPlusBinaryExp(APlusBinaryExp node, Context ctxt) throws AnalysisException {

        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseAGreaterBinaryExp(AGreaterBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumericComparizon(node, ctxt));
    }

    @Override
    public PType caseAGreaterEqualBinaryExp(AGreaterEqualBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumericComparizon(node, ctxt));
    }

    @Override
    public PType caseAMultiplyBinaryExp(AMultiplyBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseADivideBinaryExp(ADivideBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    public PType checkNumeric(SBinaryExp node, Context ctxt) throws AnalysisException {

        PType left = node.getLeft().apply(this, ctxt);


        if (!typeComparator.compatible(SNumericPrimitiveType.class, left)) {
            errorReporter.report(2, "Type is not numeric: " + node.getLeft() + " - type: " + left, null);
        }

        PType right = node.getRight().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, right)) {
            errorReporter.report(2, "Type is not numeric: " + node + " - type: " + right, null);
        }

        if (left instanceof ARealNumericPrimitiveType) {
            return MableAstFactory.newARealNumericPrimitiveType();
        } else if (right instanceof ARealNumericPrimitiveType) {
            return MableAstFactory.newARealNumericPrimitiveType();
        } else if (left instanceof AFloatNumericPrimitiveType) {
            return MableAstFactory.newARealNumericPrimitiveType();
        } else if (right instanceof AFloatNumericPrimitiveType) {
            return MableAstFactory.newARealNumericPrimitiveType();
        } else if (left instanceof AUIntNumericPrimitiveType) {
            return MableAstFactory.newAUIntNumericPrimitiveType();
        } else if (right instanceof AUIntNumericPrimitiveType) {
            return MableAstFactory.newAUIntNumericPrimitiveType();
        } else {
            return MableAstFactory.newAIntNumericPrimitiveType();
        }
        //            if (left instanceof AIntNumericPrimitiveType) {
        //            return MableAstFactory.newAIntNumericPrimitiveType();
        //        } else if (right instanceof AIntNumericPrimitiveType) {
        //            return MableAstFactory.newAIntNumericPrimitiveType();
        //        } else if (left instanceof AShortNumericPrimitiveType) {
        //            return MableAstFactory.newAIntNumericPrimitiveType();
        //        } else if (right instanceof AShortNumericPrimitiveType) {
        //            return MableAstFactory.newAIntNumericPrimitiveType();
        //        } else if (left instanceof AByteNumericPrimitiveType) {
        //            return MableAstFactory.newAIntNumericPrimitiveType();
        //        } else if (right instanceof AByteNumericPrimitiveType) {
        //            return MableAstFactory.newAIntNumericPrimitiveType();
        //        }
        //        return null;
    }

    public PType checkNumericComparizon(SBinaryExp node, Context ctxt) throws AnalysisException {

        PType left = node.getLeft().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, left)) {
            errorReporter.report(2, "Type is not numeric: " + node.getLeft() + " - type: " + left, null);
        }

        PType right = node.getRight().apply(this, ctxt);

        if (!typeComparator.compatible(SNumericPrimitiveType.class, right)) {
            errorReporter.report(2, "Type is not numeric: " + node + " - type: " + right, null);
        }

        return newBoleanType();
    }

    @Override
    public PType caseAMinusBinaryExp(AMinusBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumeric(node, ctxt));
    }

    @Override
    public PType caseABoolLiteralExp(ABoolLiteralExp node, Context ctxt) throws AnalysisException {
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAStringLiteralExp(AStringLiteralExp node, Context ctxt) throws AnalysisException {
        return store(node, MableAstFactory.newAStringPrimitiveType());
    }

    @Override
    public PType caseARealLiteralExp(ARealLiteralExp node, Context ctxt) throws AnalysisException {
        return store(node, detectType(node.getValue()));
    }

    @Override
    public PType caseAFloatLiteralExp(AFloatLiteralExp node, Context ctxt) throws AnalysisException {
        return store(node, detectType(node.getValue()));
    }

    @Override
    public PType caseAUIntLiteralExp(AUIntLiteralExp node, Context ctxt) throws AnalysisException {
        long value = node.getValue();

        if (value < 0 || value < Integer.MAX_VALUE) {
            return store(node, MableAstFactory.newIntType());
        }
        return store(node, MableAstFactory.newUIntType());

    }

    @Override
    public PType caseAIntLiteralExp(AIntLiteralExp node, Context ctxt) throws AnalysisException {

        return store(node, detectType(node.getValue()));
    }

    @Override
    public PType caseAIdentifierExp(AIdentifierExp node, Context ctxt) throws AnalysisException {
        PDeclaration def = ctxt.findDeclaration(node.getName());

        if (def == null) {
            errorReporter.report(0, "Use of undeclared identifier: '" + node.getName().getText() + "'", node.getName().getSymbol());
            return store(node, newAUnknownType());
        }

        return store(node, checkedTypes.get(def).clone());
    }

    @Override
    public PType caseAFunctionDeclaration(AFunctionDeclaration node, Context info) throws AnalysisException {
        // TODO: Check that function does not already exist
        AFunctionType type = new AFunctionType();
        PType resultType = node.getReturnType().apply(this, info);
        type.setResult(resultType);
        if (node.getFormals() != null && node.getFormals().size() > 0) {
            List<PType> functionParameters = new ArrayList<>();
            for (AFormalParameter formalParameter : node.getFormals()) {
                PType parameterType = formalParameter.getType().apply(this, info);
                functionParameters.add(parameterType);
            }
            type.setParameters(functionParameters);
        }

        return store(node, type);
    }

    @Override
    public PType caseARootDocument(ARootDocument node, Context ctxt) throws AnalysisException {
        for (INode node_ : node.getContent()) {
            node_.apply(this, ctxt);
        }

        return store(node, newAVoidType());
    }

    @Override
    public PType caseASimulationSpecificationCompilationUnit(ASimulationSpecificationCompilationUnit node, Context ctxt) throws AnalysisException {

        ModulesContext visibleModulesContext = new ModulesContext(getAccessibleModulesContext(node.getImports(), ctxt), ctxt);
        node.getBody().apply(this, visibleModulesContext);
        return store(node, newAVoidType());
    }

    @Override
    public PType caseAImportedModuleCompilationUnit(AImportedModuleCompilationUnit node, Context ctxt) throws AnalysisException {
        ModulesContext visibleModulesContext = new ModulesContext(getAccessibleModulesContext(node.getImports(), ctxt), ctxt);
        node.getModule().apply(this, visibleModulesContext);
        return store(node, newAVoidType());
    }

    @Override
    public PType caseAModuleDeclaration(AModuleDeclaration node, Context ctxt) throws AnalysisException {

        PDeclaration decl = ctxt.findDeclaration(node.getName());
        if (decl != null) {
            errorReporter.report(0, "Module dublicates: " + node.getName().getText() + " dublicates: " + decl.getName().getText(),
                    node.getName().getSymbol());
        }

        for (AFunctionDeclaration funDecl : node.getFunctions()) {
            funDecl.apply(this, ctxt);
        }
        return store(node, newAModuleType((LexIdentifier) node.getName().clone()));
    }

    private List<AModuleDeclaration> getAccessibleModulesContext(List<? extends LexIdentifier> imports, Context ctxt) {
        List<AModuleDeclaration> importedModules = new Vector<>();
        for (LexIdentifier importName : imports) {
            PDeclaration decl = ctxt.findGlobalDeclaration(importName);
            if (decl == null) {
                errorReporter.report(0, "Imported decleration: '" + importName.getText() + "' not in scope", importName.getSymbol());
            } else {
                if (decl instanceof AModuleDeclaration) {
                    importedModules.add((AModuleDeclaration) decl);

                    AImportedModuleCompilationUnit importedUnit = decl.getAncestor(AImportedModuleCompilationUnit.class);
                    if (importedUnit != null) {
                        //recursive add imports
                        importedModules.addAll(getAccessibleModulesContext(importedUnit.getImports(), ctxt));
                    } else {
                        errorReporter.report(0, "Module is not in a import unit", decl.getName().getSymbol());
                    }
                } else {
                    errorReporter.report(0, "Imported module is not a module", importName.getSymbol());
                }
            }
        }

        return importedModules;
    }


    @Override
    public PType caseABasicBlockStm(ABasicBlockStm node, Context ctxt) throws AnalysisException {
        return checkBlock(node, node.getBody(), ctxt);
    }

    @Override
    public PType caseAParallelBlockStm(AParallelBlockStm node, Context ctxt) throws AnalysisException {
        return checkBlock(node, node.getBody(), ctxt);
    }

    private AVoidType checkBlock(INode node, List<? extends INode> body, Context ctxt) throws AnalysisException {
        DeclarationList tdm = new DeclarationList();
        Context localCtxt = new LocalContext(tdm, ctxt);
        for (INode bodyNode : body) {
            if (bodyNode instanceof ALocalVariableStm) {
                ALocalVariableStm stm = (ALocalVariableStm) bodyNode;
                if (stm.getDeclaration() != null) {
                    //type is added in the global cache during var decl check
                    /*PType type =*/
                    stm.getDeclaration().apply(this, localCtxt);
                    tdm.add(stm.getDeclaration());
                }
            } else {
                bodyNode.apply(this, localCtxt);
            }

        }

        return store(node, MableAstFactory.newAVoidType());
    }


    @Override
    public PType caseALocalVariableStm(ALocalVariableStm node, Context ctxt) throws AnalysisException {
        PType type = node.getDeclaration().apply(this, ctxt);
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAParExp(AParExp node, Context ctxt) throws AnalysisException {
        PType expType = node.getExp().apply(this, ctxt);

        return store(node, expType);
    }

    @Override
    public PType caseAWhileStm(AWhileStm node, Context ctxt) throws AnalysisException {
        PType testType = node.getTest().apply(this, ctxt);
        if (!(testType instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "While condition is not of type bool: " + node, null);
        }
        node.getBody().apply(this, ctxt);

        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAAssigmentStm(AAssigmentStm node, Context ctxt) throws AnalysisException {
        PType expType = node.getExp().apply(this, ctxt);
        PType type = node.getTarget().apply(this, ctxt);
        if (!typeComparator.compatible(type, expType)) {
            errorReporter.report(-5, "Invalid assignment to cannot assign: '" + expType + "' to '" + type + "' in: " + node, null);

        }
        return store(node, MableAstFactory.newAVoidType());
    }


    @Override
    public PType caseAIdentifierStateDesignator(AIdentifierStateDesignator node, Context ctxt) throws AnalysisException {
        PDeclaration def = ctxt.findDeclaration(node.getName());

        PType type;
        if (def == null) {
            errorReporter.report(0, "Use of undeclared variable: " + node.getName(), node.getName().getSymbol());
            type = newAUnknownType();
        } else {
            type = checkedTypes.get(def).clone();
        }
        return store(node, type);
    }

    @Override
    public PType caseAIfStm(AIfStm node, Context ctxt) throws AnalysisException {
        if (node.getTest() != null) {
            PType testType = node.getTest().apply(this, ctxt);
            if (!(typeComparator.compatible(newBoleanType(), testType))) {
                errorReporter.report(-5, "If condition is not of type bool: " + node, null);
            }
        }
        if (node.getThen() != null) {
            node.getThen().apply(this, ctxt);
        }
        if (node.getElse() != null) {
            node.getElse().apply(this, ctxt);
        }
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAEqualBinaryExp(AEqualBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        PType right = node.getRight().apply(this, ctxt);
        return checkEquality(node, left, right);
    }

    @Override
    public PType caseANotEqualBinaryExp(ANotEqualBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        PType right = node.getRight().apply(this, ctxt);
        return checkEquality(node, left, right);
    }

    private ABooleanPrimitiveType checkEquality(INode node, PType left, PType right) {
        //first check for numbers as we can always compare these
        if (!typeComparator.compatible(SNumericPrimitiveType.class, left) && !typeComparator.compatible(SNumericPrimitiveType.class, right)) {
            //Commutative
            if (!typeComparator.compatible(left, right) && !typeComparator.compatible(right, left)) {
                errorReporter.report(-5, "Left and right part of expression are not compatible: " + node, null);

            }
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAExpressionStm(AExpressionStm node, Context ctxt) throws AnalysisException {
        node.getExp().apply(this, ctxt);
        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseABreakStm(ABreakStm node, Context ctxt) throws AnalysisException {

        if (node.getAncestor(AWhileStm.class) == null) {
            errorReporter.report(0, "Break statement only allowed inside a while", node.getToken());
        }

        return store(node, MableAstFactory.newAVoidType());
    }

    @Override
    public PType caseAArrayStateDesignator(AArrayStateDesignator node, Context ctxt) throws AnalysisException {
        PType indexType = node.getExp().apply(this, ctxt);
        if (!typeComparator.compatible(new AIntNumericPrimitiveType(), indexType)) {
            errorReporter.report(-5, "Index has to be of int type: '" + indexType + "' node " + node, null);
        }
        // Peel of the array type
        PType targetType = node.getTarget().apply(this, ctxt);
        if (targetType instanceof AArrayType) {
            return store(node, ((AArrayType) targetType).getType());
        } else {
            errorReporter.report(-5, "Attempt to index into a variable of non-array type:" + node, null);
            return store(node, targetType);
        }
    }

    public PType checkLogicialBinary(SBinaryExp node, Context ctxt) throws AnalysisException {
        PType left = node.getLeft().apply(this, ctxt);
        if (!(left instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expected lvalue to be bool actual:" + left, null);
        }
        PType right = node.getLeft().apply(this, ctxt);
        if (!(right instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expected rvalue to be bool actual:" + right, null);
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAAndBinaryExp(AAndBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkLogicialBinary(node, ctxt));
    }

    @Override
    public PType caseAOrBinaryExp(AOrBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkLogicialBinary(node, ctxt));
    }

    @Override
    public PType caseALessEqualBinaryExp(ALessEqualBinaryExp node, Context ctxt) throws AnalysisException {

        return store(node, checkNumericComparizon(node, ctxt));
    }

    @Override
    public PType caseALessBinaryExp(ALessBinaryExp node, Context ctxt) throws AnalysisException {
        return store(node, checkNumericComparizon(node, ctxt));
    }


    @Override
    public PType caseANotUnaryExp(ANotUnaryExp node, Context ctxt) throws AnalysisException {
        PType expType = node.getExp().apply(this, ctxt);
        if (!(expType instanceof ABooleanPrimitiveType)) {
            errorReporter.report(-5, "Expression used with ! has to be of type bool: " + node, null);
        }
        return store(node, MableAstFactory.newABoleanPrimitiveType());
    }

    @Override
    public PType caseAUnloadExp(AUnloadExp node, Context ctxt) throws AnalysisException {
        if (node.getArgs() == null || node.getArgs().size() != 1) {
            errorReporter.report(-5, "Wrong number of arguments to Unload. Unload accepts 1 argument: " + node, null);
        } else {
            PType argType = node.getArgs().get(0).apply(this, ctxt);
            if (!(argType instanceof AModuleType)) {
                errorReporter.report(-5, "Argument to unload must be a module type.: " + node, null);

            }
        }
        return store(node, newAVoidType());
    }

    public void typecheck(List<ARootDocument> rootDocuments, List<? extends PDeclaration> globalFunctions) throws AnalysisException {

        if (globalFunctions == null) {
            globalFunctions = new Vector<>();
        }

        // Find all importedModuleCompilationUnits and typecheck these twice.
        List<AImportedModuleCompilationUnit> importedModuleUnits = rootDocuments.stream().flatMap(
                        x -> x.getContent().stream().filter(AImportedModuleCompilationUnit.class::isInstance).map(AImportedModuleCompilationUnit.class::cast))
                .collect(Collectors.toList());


        List<AModuleDeclaration> importedModules =
                importedModuleUnits.stream().map(AImportedModuleCompilationUnit::getModule).collect(Collectors.toList());
        final GlobalContext ctx = new GlobalContext(importedModules, null);

        final QuestionAnswerAdaptor<Context, PType> typeCheckVisitor = this;

        //check all modules imported imported
        //TODO why are we adding this type here?
        for (ARootDocument doc : rootDocuments) {
            doc.apply(new DepthFirstAnalysisAdaptor() {
                @Override
                public void caseAModuleDeclaration(AModuleDeclaration node) throws AnalysisException {
                    checkedTypes.put(node, newAModuleType(node.getName()));
                }
            });
        }

        //check all imported modules
        for (ARootDocument doc : rootDocuments) {
            for (PCompilationUnit unit : doc.getContent()) {
                if (unit instanceof AImportedModuleCompilationUnit) {
                    unit.apply(typeCheckVisitor, ctx);
                }
            }
        }

        ModulesContext allModulesCtxt = new ModulesContext(importedModules, ctx);
        for (PDeclaration func : globalFunctions) {
            func.apply(typeCheckVisitor, allModulesCtxt);
        }

        //add all global functions as local functions
        LocalContext globalContext = new LocalContext(globalFunctions, ctx);

        List<ARootDocument> allSimulationSpecifications =
                rootDocuments.stream().filter(x -> x.getContent().stream().anyMatch(ASimulationSpecificationCompilationUnit.class::isInstance))
                        .collect(Collectors.toList());

        if (allSimulationSpecifications.size() != 1) {
            errorReporter.report(-5, "1 simulation specification is allowed. Found: " + allSimulationSpecifications.size(), null);
        } else {
            allSimulationSpecifications.get(0).apply(this, globalContext);
        }
    }

}
