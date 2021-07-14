/*
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-CurrentYear, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License
 * are obtained from the INTO-CPS Association, either from the above address,
 * from the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */

/*
 * Author:
 *		Kenneth Lausdahl
 *		Casper Thule
 */
package org.intocps.maestro.fmi;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.intocps.maestro.fmi.xml.NamedNodeMapIterator;
import org.intocps.maestro.fmi.xml.NodeIterator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class ModelDescription {
    private static final boolean DEBUG = false;
    // final private File file;
    final Document doc;
    final XPath xpath;
    List<ScalarVariable> scalarVariables = null;
    List<ScalarVariable> outputs = null;
    List<ScalarVariable> derivatives = null;
    Map<ScalarVariable, ScalarVariable> derivativesMap = new HashMap<>();
    List<ScalarVariable> initialUnknowns = null;

    public ModelDescription(File file) throws ParserConfigurationException, SAXException, IOException {
        this(getStream(file), new StreamSource(ModelDescription.class.getClassLoader().getResourceAsStream("fmi2ModelDescription.xsd")));
    }

    public ModelDescription(InputStream file) throws ParserConfigurationException, SAXException, IOException {
        this(file, new StreamSource(ModelDescription.class.getClassLoader().getResourceAsStream("fmi2ModelDescription.xsd")));
    }

    ModelDescription(InputStream xmlInputStream, Source schemaSource) throws SAXException, IOException, ParserConfigurationException {
        // Document document;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        validateAgainstXSD(new StreamSource(xmlInputStream), schemaSource);

        xmlInputStream.reset();
        doc = docBuilderFactory.newDocumentBuilder().parse(xmlInputStream);

        XPathFactory xPathfactory = XPathFactory.newInstance();
        xpath = xPathfactory.newXPath();

    }

    static InputStream getStream(File file) throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(file));
        return new ByteArrayInputStream(bytes);
    }

    static void validateAgainstXSD(Source document, Source schemaSource) throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(new ResourceResolver());

        Schema schema = factory.newSchema(schemaSource);

        Validator validator = schema.newValidator();
        validator.validate(document);

    }

    public static Node lookupSingle(Object doc, XPath xpath, String expression) throws XPathExpressionException {
        NodeList list = lookup(doc, xpath, expression);
        if (list != null) {
            return list.item(0);
        }
        return null;
    }

    public static NodeList lookup(Object doc, XPath xpath, String expression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(expression);

        if (DEBUG) {
            System.out.println("Starting from: " + formateNodeWithAtt(doc));
        }
        final NodeList list = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        if (DEBUG) {
            System.out.print("\tFound: ");
        }
        boolean first = true;
        for (Node n : new NodeIterator(list)) {
            if (DEBUG) {
                System.out.println((!first ? "\t       " : "") + formateNodeWithAtt(n));
            }
            first = false;
        }
        if (first) {
            if (DEBUG) {
                System.out.println("none");
            }
        }
        return list;

    }

    public static String formateNodeWithAtt(Object o) {
        if (o instanceof Document) {
            return "Root document";
        } else if (o instanceof Node) {
            Node node = (Node) o;

            String tmp = "";
            tmp = node.getLocalName();
            if (node.hasAttributes()) {
                for (Node att : new NamedNodeMapIterator(node.getAttributes())) {
                    tmp += " " + att + ", ";
                }
            }
            return tmp;
        }
        return o.toString();
    }

    public String getModelId() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@modelName");
        return name.getNodeValue();
    }

    public String getGuid() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@guid");
        return name.getNodeValue();
    }

    public String getFmiVersion() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@fmiVersion");
        return name.getNodeValue();
    }

    public String getModelDescription() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@description");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getAuthor() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@author");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getModelVersion() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@version");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getCopyright() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@copyright");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getLicense() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@license");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getGenerationTool() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@generationTool");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getGenerationDateAndTime() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@generationDateAndTime");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getVariableNamingConvention() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/@variableNamingConvention");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public String getVendorToolName() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/VendorAnnotations/Tool/@name");
        if (name == null) {
            return "";
        }
        return name.getNodeValue();
    }

    public boolean getNeedsExecutionTool() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@needsExecutionTool");
        return name != null && Boolean.parseBoolean(name.getNodeValue());
    }

    public boolean getCanGetAndSetFmustate() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canGetAndSetFMUstate");
        if (name == null) {
            return false;
        }
        return Boolean.valueOf(name.getNodeValue());
    }

    public boolean getCanBeInstantiatedOnlyOncePerProcess() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canBeInstantiatedOnlyOncePerProcess");
        if (name == null) {
            return false;
        }
        return Boolean.valueOf(name.getNodeValue());
    }

    public boolean getCanInterpolateInputs() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canInterpolateInputs");
        if (name == null) {
            return false;
        }
        return Boolean.valueOf(name.getNodeValue());
    }

    public boolean getCanHandleVariableCommunicationStepSize() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canHandleVariableCommunicationStepSize");
        if (name == null) {
            return false;
        }
        return Boolean.valueOf(name.getNodeValue());
    }

    public List<LogCategory> getLogCategories() throws XPathExpressionException {
        List<LogCategory> categories = new Vector<>();

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/LogCategories/Category"))) {
            NamedNodeMap attributes = n.getAttributes();

            Node descritpionNode = attributes.getNamedItem("description");

            categories.add(new LogCategory(attributes.getNamedItem("name").getNodeValue(),
                    descritpionNode != null ? descritpionNode.getNodeValue() : null));

        }
        return categories;
    }

    public List<ScalarVariable> getScalarVariables() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        if (scalarVariables == null) {
            parse();
        }
        return scalarVariables;
    }

    public List<ScalarVariable> getOutputs() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        if (outputs == null) {
            parse();
        }
        return outputs;
    }

    /**
     * @return Map of ports to derivative ports.
     * @throws XPathExpressionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Map<ScalarVariable, ScalarVariable> getDerivativesMap() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        if (derivativesMap == null) {
            parse();
        }
        return derivativesMap;
    }

    public List<ScalarVariable> getDerivatives() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        if (derivatives == null) {
            parse();
        }
        return derivatives;
    }

    public List<ScalarVariable> getInitialUnknowns() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        if (initialUnknowns == null) {
            parse();
        }
        return initialUnknowns;
    }

    public synchronized void parse() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        Map<String, SimbpleTypeDefinition> typeDefinitions = parseTypeDefinitions();

        List<ScalarVariable> vars = new Vector<>();
        Map<Integer, ScalarVariable> indexMap = new HashMap<>();

        int index = 0;

        List<ScalarVariable> ders = new Vector<>();

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/ModelVariables/ScalarVariable"))) {
            ScalarVariable sc = new ScalarVariable();
            indexMap.put(++index, sc);

            NamedNodeMap attributes = n.getAttributes();
            sc.name = attributes.getNamedItem("name").getNodeValue();
            sc.valueReference = Long.parseLong(attributes.getNamedItem("valueReference").getNodeValue());

            // optional
            sc.causality = getAttribute(Causality.class, attributes, "causality");
            if (sc.causality == null) {
                sc.causality = Causality.Local;
            }

            sc.variability = getAttribute(Variability.class, attributes, "variability");
            sc.initial = getAttribute(Initial.class, attributes, "initial");
            sc.description = getNodeValue(attributes, "description", "");

            Node child = lookupSingle(n, xpath, "Real[1] | Boolean[1] | String[1] | Integer[1] | Enumeration[1]");

            sc.type = parseType(child, typeDefinitions);
            if (sc.type.type == Types.Real && ((RealType) sc.type).derivative != null) {
                ders.add(sc);
            }

            vars.add(sc);
        }

        ders.forEach(der -> {
            ScalarVariable derSource = indexMap.get(Integer.parseInt((String) ((RealType) der.type).derivative));
            if (derSource.causality == Causality.Output) {
                derivativesMap.put(derSource, der);
            }
        });

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/ModelStructure/Outputs/Unknown"))) {
            decodeUnknownElement(indexMap, n, new IOptainUnknownDestination() {

                @Override
                public Map<ScalarVariable, ScalarVariable.DependencyKind> get(ScalarVariable sc) {
                    return sc.outputDependencies;
                }

                @Override
                public List<ScalarVariable> getList() {
                    if (outputs == null) {
                        outputs = new Vector<>();
                    }
                    return outputs;
                }
            }, ModelStructureElementType.Outputs);
        }

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/ModelStructure/Derivatives/Unknown"))) {
            decodeUnknownElement(indexMap, n, new IOptainUnknownDestination() {

                @Override
                public Map<ScalarVariable, ScalarVariable.DependencyKind> get(ScalarVariable sc) {
                    return sc.derivativesDependencies;
                }

                @Override
                public List<ScalarVariable> getList() {
                    if (derivatives == null) {
                        derivatives = new Vector<>();
                    }
                    return derivatives;
                }

            }, ModelStructureElementType.Derivatives);
        }

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/ModelStructure/InitialUnknowns/Unknown"))) {
            decodeUnknownElement(indexMap, n, new IOptainUnknownDestination() {

                @Override
                public Map<ScalarVariable, ScalarVariable.DependencyKind> get(ScalarVariable sc) {
                    return sc.initialUnknownsDependencies;
                }

                @Override
                public List<ScalarVariable> getList() {
                    if (initialUnknowns == null) {
                        initialUnknowns = new Vector<>();
                    }
                    return initialUnknowns;
                }
            }, ModelStructureElementType.InitialUnknown);
        }

        scalarVariables = vars;

        if (outputs == null) {
            outputs = new Vector<>();
        }
        if (derivatives == null) {
            derivatives = new Vector<>();
        }
        if (initialUnknowns == null) {
            initialUnknowns = new Vector<>();
        }

    }

    private Map<String, SimbpleTypeDefinition> parseTypeDefinitions() throws XPathExpressionException, InvocationTargetException, IllegalAccessException {
        Map<String, SimbpleTypeDefinition> typeDefinitions = new HashMap<>();

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/TypeDefinitions/SimpleType"))) {
            SimbpleTypeDefinition def = new SimbpleTypeDefinition();

            Node attribute = n.getAttributes().getNamedItem("name");
            if (attribute != null) {
                def.name = attribute.getNodeValue();
            }
            attribute = n.getAttributes().getNamedItem("description");
            if (attribute != null) {
                def.description = attribute.getNodeValue();
            }

            Node child = lookupSingle(n, xpath, "Real[1] | Boolean[1] | String[1] | Integer[1] | Enumeration[1]");

            def.type = parseType(child, typeDefinitions);

            typeDefinitions.put(def.name, def);
        }

        return typeDefinitions;
    }

    private void copyDefaults(Type type, Node node,
            Map<String, SimbpleTypeDefinition> typeDefinitions) throws InvocationTargetException, IllegalAccessException {
        Node attribute = node.getAttributes().getNamedItem("declaredType");
        if (attribute != null) {
            String declaredType = attribute.getNodeValue();
            if (typeDefinitions.containsKey(declaredType)) {
                typeDefinitions.get(declaredType).setDefaults(type);
            }
        }
    }

    private String parseTypeStart(Node node) {
        Node startAtt = node.getAttributes().getNamedItem("start");
        if (startAtt != null) {
            return startAtt.getNodeValue();
        }
        return null;
    }

    private void parseBooleanType(BooleanType type, Node node) {
        String startValue = parseTypeStart(node);
        if (startValue != null) {
            type.start = Boolean.valueOf(startValue);
        }
    }

    private void parseStringType(StringType type, Node node) {
        String startValue = parseTypeStart(node);
        if (startValue != null) {
            type.start = startValue;
        }
    }

    private void parseIntegerType(IntegerType type, Node node, boolean realMode) {
        Node attribute;

        String startValue = parseTypeStart(node);
        if (startValue != null) {
            if (!realMode) {
                type.start = Integer.valueOf(startValue);
            } else {
                type.start = Double.valueOf(startValue);
            }
        }

        attribute = node.getAttributes().getNamedItem("min");
        if (attribute != null) {
            if (!realMode) {
                type.min = Integer.parseInt(attribute.getNodeValue());
            } else {
                ((RealType) type).min = Double.parseDouble(attribute.getNodeValue());
            }
        }

        attribute = node.getAttributes().getNamedItem("max");
        if (attribute != null) {
            if (!realMode) {
                type.max = Integer.parseInt(attribute.getNodeValue());
            } else {
                ((RealType) type).max = Double.parseDouble(attribute.getNodeValue());
            }
        }

        attribute = node.getAttributes().getNamedItem("quantity");
        if (attribute != null) {
            type.quantity = attribute.getNodeValue();
        }
    }

    private void parseRealType(RealType type, Node node) {
        parseIntegerType(type, node, true);
        String startValue = parseTypeStart(node);
        if (startValue != null) {
            type.start = Double.valueOf(startValue);
        }

        Node attribute = node.getAttributes().getNamedItem("unit");
        if (attribute != null) {
            type.unit = attribute.getNodeValue();
        }

        attribute = node.getAttributes().getNamedItem("displayUnit");
        if (attribute != null) {
            type.displayUnit = attribute.getNodeValue();
        }

        attribute = node.getAttributes().getNamedItem("relativeQuantity");
        if (attribute != null) {
            type.relativeQuantity = Boolean.parseBoolean(attribute.getNodeValue());
        }

        attribute = node.getAttributes().getNamedItem("nominal");
        if (attribute != null) {
            type.nominal = Double.parseDouble(attribute.getNodeValue());
        }

        attribute = node.getAttributes().getNamedItem("unbound");
        if (attribute != null) {
            type.unbound = Boolean.parseBoolean(attribute.getNodeValue());
        }

        attribute = node.getAttributes().getNamedItem("reinit");
        if (attribute != null) {
            type.reinit = Boolean.parseBoolean(attribute.getNodeValue());
        }
    }

    private Type parseType(Node child, Map<String, SimbpleTypeDefinition> typeDefinitions) throws InvocationTargetException, IllegalAccessException {
        Types typeId = Types.valueOfIgnorecase(child.getNodeName());

        Type type = null;
        switch (typeId) {
            case Boolean:
                type = new BooleanType();
                copyDefaults(type, child, typeDefinitions);
                parseBooleanType((BooleanType) type, child);
                break;
            case Enumeration:
                type = new EnumerationType();
            case Integer:
                if (type == null) {
                    type = new IntegerType();
                }
                copyDefaults(type, child, typeDefinitions);
                parseIntegerType((IntegerType) type, child, false);
                break;
            case Real:
                type = new RealType();
                copyDefaults(type, child, typeDefinitions);
                parseRealType((RealType) type, child);

                Node derivative = child.getAttributes().getNamedItem("derivative");
                if (derivative != null) {
                    ((RealType) type).derivative = derivative.getNodeValue();
                }

                break;
            case String:
                type = new StringType();
                copyDefaults(type, child, typeDefinitions);
                parseStringType((StringType) type, child);
                break;
            default:
                break;
        }

        return type;
    }

    private void decodeUnknownElement(Map<Integer, ScalarVariable> indexMap, Node n, IOptainUnknownDestination handler,
            ModelStructureElementType type) throws ModelDescriptionParseException {
        int index;
        NamedNodeMap attributes = n.getAttributes();
        index = Integer.valueOf(attributes.getNamedItem("index").getNodeValue());

        ScalarVariable sc = indexMap.get(index);

        if (sc == null) {
            throw new ModelDescriptionParseException("Invalid index attribut value in Unknown: //Unknown[@index='" + index + "']");
        }
        // sc.outputDependencies = new HashMap<ModelDescription.ScalarVariable,
        // ModelDescription.ScalarVariable.DependencyKind>();
        if (handler.getList() != null) {
            handler.getList().add(sc);
        }

        Node dependenciesNode = attributes.getNamedItem("dependencies");

        if (dependenciesNode != null) {
            String dependencies = dependenciesNode.getNodeValue();
            if (dependencies != null && !dependencies.isEmpty()) {
                String[] dependencyArr = dependencies.split(" ");
                List<ScalarVariable.DependencyKind> dependencyKinds = new Vector<>();

                Node dependencyKindsNode = attributes.getNamedItem("dependenciesKind");
                if (dependencyKindsNode != null) {
                    dependencyKinds = getAttribute(ScalarVariable.DependencyKind.class, (dependencyKindsNode.getNodeValue() + "").split(" "));

                }

                if (dependencyKinds.size() > dependencyArr.length) {
                    throw new ModelDescriptionParseException(
                            "dependencies and dependenciesKind does not match missing dependency for kind //Unknown[@index='" + index + "']");
                }

                for (int i = 0; i < dependencyArr.length; i++) {

                    ScalarVariable.DependencyKind kind = ScalarVariable.DependencyKind.Dependent;
                    if (dependencyKinds.size() > i) {
                        kind = dependencyKinds.get(i);
                    }

                    ScalarVariable key = indexMap.get(Integer.valueOf(dependencyArr[i]));
                    if (key == null) {
                        throw new ModelDescriptionParseException("Invalid index attribut value in Unknown: //Unknown[@index='" + index + "']");
                    }
                    handler.get(sc).put(key, kind);
                }

            }
        } else {
            switch (type) {
                case Derivatives:
                case Outputs: {
                    for (ScalarVariable other : indexMap.values()) {
                        switch (other.causality) {
                            case CalculatedParameter:
                                break;
                            case Independent:
                            case Input:
                                handler.get(sc).put(other, ScalarVariable.DependencyKind.Dependent);
                                break;
                            case Local:
                                break;
                            case Output:
                                break;
                            case Parameter:
                            default:
                                break;
                        }

                    }
                }
                break;
                case InitialUnknown: {
                    for (ScalarVariable other : indexMap.values()) {
                        switch (other.causality) {
                            case CalculatedParameter:
                                break;
                            case Independent:
                            case Input:
                                handler.get(sc).put(other, ScalarVariable.DependencyKind.Dependent);
                                break;
                            case Local:
                                break;
                            case Output:
                                break;
                            case Parameter:
                            default:
                                break;
                        }

                        if (other.initial != null) {
                            switch (other.initial) {
                                case Approx:
                                    break;
                                case Calculated:
                                    break;
                                case Exact:
                                    handler.get(sc).put(other, ScalarVariable.DependencyKind.Dependent);
                                    break;
                                default:
                                    break;
                            }
                        }

                    }
                }
                break;
                default:
                    break;

            }
        }

    }

    public int getMaxOutputDerivativeOrder() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@maxOutputDerivativeOrder");
        if (name == null) {
            return 0;
        }
        return Integer.parseInt(name.getNodeValue());
    }

    @SuppressWarnings("unchecked")
    private <T> T getNodeValue(NamedNodeMap attributes, String name, T defaultValue) {
        Node att = attributes.getNamedItem(name);
        if (att != null) {
            return (T) att.getNodeValue();
        }
        return defaultValue;
    }

    private <T extends Enum<T>> T getAttribute(Class<T> en, NamedNodeMap attributes, String name) {
        Node att = attributes.getNamedItem(name);
        if (att != null) {
            return (T) Enum.valueOf(en, StringUtils.capitalize(att.getNodeValue()));
        }
        return null;
    }

    private <T extends Enum<T>> List<T> getAttribute(Class<T> en, String[] name) {
        List<T> list = new Vector<>();
        for (String n : name) {
            list.add((T) Enum.valueOf(en, StringUtils.capitalize(n)));
        }
        return list;
    }

    public enum Types {
        Boolean,
        Real,
        Integer,
        String,
        Enumeration;

        public static Types valueOfIgnorecase(String value) {
            for (Types t : values()) {
                if (t.name().equalsIgnoreCase(value)) {
                    return t;
                }
            }
            return null;
        }
    }

    public enum Causality {
        Parameter,
        CalculatedParameter,
        Input,
        Output,
        Local,
        Independent;

        public static Causality valueOfIgnorecase(String value) {
            for (Causality c : values()) {
                if (c.name().equalsIgnoreCase(value)) {
                    return c;
                }
            }
            return null;
        }
    }

    public enum Variability {
        Constant,
        Fixed,
        Tunable,
        Discrete,
        Continuous;

        public static Variability valueOfIgnorecase(String value) {
            for (Variability v : values()) {
                if (v.name().equalsIgnoreCase(value)) {
                    return v;
                }
            }
            return null;
        }
    }

    public enum Initial {
        Exact,
        Approx,
        Calculated;

        public static Initial valueOfIgnorecase(String value) {
            for (Initial i : values()) {
                if (i.name().equalsIgnoreCase(value)) {
                    return i;
                }
            }
            return null;
        }
    }

    enum ModelStructureElementType {
        InitialUnknown,
        Outputs,
        Derivatives
    }

    private interface IOptainUnknownDestination {
        Map<ScalarVariable, ScalarVariable.DependencyKind> get(ScalarVariable sc);

        List<ScalarVariable> getList();
    }

    public static class ModelDescriptionParseException extends XPathExpressionException {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public ModelDescriptionParseException(String message) {
            super(message);
        }

    }

    public static class Type {
        public Types type;
        public Object start;

        @Override
        public String toString() {
            return type + (start != null ? " " + start : "");
        }

    }

    public static class BooleanType extends Type {

        public BooleanType() {
            super.type = Types.Boolean;
        }
    }

    public static class StringType extends Type {

        public StringType() {
            super.type = Types.String;
        }
    }

    public static class IntegerType extends Type {
        public String quantity;
        public Integer min;
        public Integer max;

        public IntegerType() {
            super.type = Types.Integer;
        }
    }

    public static class EnumerationType extends IntegerType {

        public EnumerationType() {
            super.type = Types.Enumeration;
        }
    }

    public static class RealType extends IntegerType {
        public Object derivative;
        public String unit;
        public String displayUnit;
        public boolean relativeQuantity = false;
        public double nominal;
        public boolean unbound = false;
        public boolean reinit = false;
        public Double min;
        public Double max;

        public RealType() {
            super.type = Types.Real;
        }
    }

    // public boolean validate() throws FileNotFoundException
    // {
    // return validateAgainstXSD(new FileInputStream(file), new
    // File("modeldescription").listFiles());
    // }

    public static class SimbpleTypeDefinition {
        public Type type;
        public String name;
        public String description;

        public void setDefaults(Type destination) throws InvocationTargetException, IllegalAccessException {
            BeanUtils.copyProperties(type, destination);
        }

    }

    public static class ScalarVariable {
        public final Map<ScalarVariable, DependencyKind> outputDependencies = new HashMap<>();
        public final Map<ScalarVariable, DependencyKind> derivativesDependencies = new HashMap<>();
        public final Map<ScalarVariable, DependencyKind> initialUnknownsDependencies = new HashMap<>();
        public String name;
        public long valueReference;
        public String description;
        public Causality causality;
        public Variability variability;
        public Initial initial;
        public Type type;

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public Long getValueReference() {
            return new Long(valueReference);
        }

        @Override
        public String toString() {
            // return
            // String.format("name: %s, ref: %s, causality: %s, Variability: %s, Initial: %s, "
            // + "Type: %s, Description: %s", name, valueReference, causality,
            // variability, initial, type, description);
            return getName();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ScalarVariable) {
                return this.name.equals(((ScalarVariable) obj).getName());
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        public enum DependencyKind {
            Dependent,
            Constant,
            Fixed,
            Tunable,
            Discrete
        }
    }

    public static class LogCategory {
        public final String name;
        public final String description;

        public LogCategory(String name, String description) {
            this.name = name;
            this.description = description;
        }

        protected LogCategory() {
            name = null;
            description = null;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class ResourceResolver implements LSResourceResolver {

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {

            // note: in this sample, the XSD's are expected to be in the root of the classpath
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(systemId);
            return new Input(publicId, systemId, resourceAsStream);
        }

    }

    public static class Input implements LSInput {

        private String publicId;

        private String systemId;
        private BufferedInputStream inputStream;

        public Input(String publicId, String sysId, InputStream input) {
            this.publicId = publicId;
            this.systemId = sysId;
            this.inputStream = new BufferedInputStream(input);
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String baseURI) {
        }

        @Override
        public InputStream getByteStream() {
            return null;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public String getStringData() {
            synchronized (inputStream) {
                try {
                    byte[] input = new byte[inputStream.available()];
                    inputStream.read(input);
                    String contents = new String(input);
                    return contents;
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e);
                    return null;
                }
            }
        }

        @Override
        public void setStringData(String stringData) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        public BufferedInputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(BufferedInputStream inputStream) {
            this.inputStream = inputStream;
        }
    }

}
