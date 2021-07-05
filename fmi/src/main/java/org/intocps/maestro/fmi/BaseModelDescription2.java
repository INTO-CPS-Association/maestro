package org.intocps.maestro.fmi;

import org.apache.commons.io.IOUtils;
import org.intocps.maestro.fmi.xml.NamedNodeMapIterator;
import org.intocps.maestro.fmi.xml.NodeIterator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Defines parsing of model description elements that are common to both fmi2 and fmi3 and also common methods.
 */
public abstract class BaseModelDescription2 {
    private static final boolean DEBUG = false;

    protected final Document doc;
    protected final XPath xpath;

    protected List<ModelDescription.ScalarVariable> scalarVariables = null;
    protected List<ModelDescription.ScalarVariable> outputs = null;
    protected List<ModelDescription.ScalarVariable> derivatives = null;
    protected Map<ModelDescription.ScalarVariable, ModelDescription.ScalarVariable> derivativesMap = new HashMap<>();
    protected List<ModelDescription.ScalarVariable> initialUnknowns = null;

    BaseModelDescription2(InputStream xmlInputStream, Source schemaSource) throws SAXException, IOException, ParserConfigurationException {
        // Document document;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        validateAgainstXSD(new StreamSource(xmlInputStream), schemaSource);

        xmlInputStream.reset();
        doc = docBuilderFactory.newDocumentBuilder().parse(xmlInputStream);

        XPathFactory xPathfactory = XPathFactory.newInstance();
        xpath = xPathfactory.newXPath();
    }

    // Top level attributes
    public String getFmiVersion() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@fmiVersion");
    }

    public String getModelName() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@modelName");
    }

    public String getModelDescription() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@description");
    }

    public String getAuthor() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath, "fmiModelDescription/@author");
    }

    public String getVersion() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath,  "fmiModelDescription/@version");
    }

    public String getCopyright() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath,  "fmiModelDescription/@copyright");
    }

    public String getLicense() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath,  "fmiModelDescription/@license");
    }

    public String getGenerationTool() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath,  "fmiModelDescription/@generationTool");
    }

    public String getGenerationDateAndTime() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath,  "fmiModelDescription/@generationDateAndTime");
    }

    public String getVariableNamingConvention() throws XPathExpressionException {
        return lookupSingleNodeValue(doc, xpath,  "fmiModelDescription/@variableNamingConvention");
    }

    // Capabilities common between the interfaces for CoSimulation, ModelExchange and ScheduledExecution (FMI3)
    public boolean getNeedsExecutionTool() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@needsExecutionTool");
        return name != null && Boolean.parseBoolean(name.getNodeValue());
    }

    public boolean getCanBeInstantiatedOnlyOncePerProcess() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canBeInstantiatedOnlyOncePerProcess");
        return name != null && Boolean.parseBoolean(name.getNodeValue());
    }

    public boolean getCanGetAndSetFmustate() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canGetAndSetFMUstate");
        return name != null && Boolean.parseBoolean(name.getNodeValue());
    }

    public int getMaxOutputDerivativeOrder() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@maxOutputDerivativeOrder");
        return name != null ? Integer.parseInt(name.getNodeValue()) : 0;
    }

    // Attributes specific to the CoSimulation element
    public boolean getCanHandleVariableCommunicationStepSize() throws XPathExpressionException {
        Node name = lookupSingle(doc, xpath, "fmiModelDescription/CoSimulation/@canHandleVariableCommunicationStepSize");
        return name != null && Boolean.parseBoolean(name.getNodeValue());
    }

    // Log categories element
    public List<LogCategory> getLogCategories() throws XPathExpressionException {
        List<LogCategory> categories = new Vector<>();

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/LogCategories/Category"))) {
            NamedNodeMap attributes = n.getAttributes();

            Node descriptionNode = attributes.getNamedItem("description");

            categories.add(new LogCategory(attributes.getNamedItem("name").getNodeValue(),
                    descriptionNode != null ? descriptionNode.getNodeValue() : null));

        }
        return categories;
    }

    // Unit definitions element
    public List<LogCategory> getUnitDefinitions() throws XPathExpressionException {
        List<LogCategory> categories = new Vector<>();

        for (Node n : new NodeIterator(lookup(doc, xpath, "fmiModelDescription/UnitDefinitions/Unit"))) {
            NamedNodeMap attributes = n.getAttributes();
            Node unitName = attributes.getNamedItem("name");

            for(Node cn : new NodeIterator(n.getChildNodes())) {
                switch (cn.getLocalName()){
                    case "BaseUnit":
                        break;
                    case "DisplayUnit":
                        break;
                    case "Annotations":
                        break;
                }
            }
        }
        return categories;
    }

    public static void validateAgainstXSD(Source document, Source schemaSource) throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(new ModelDescription.ResourceResolver());

        Schema schema = factory.newSchema(schemaSource);

        Validator validator = schema.newValidator();
        validator.validate(document);
    }

    public synchronized void parse() throws Exception {}

    protected static Node lookupSingle(Object doc, XPath xpath, String expression) throws XPathExpressionException {
        NodeList list = lookup(doc, xpath, expression);
        if (list != null) {
            return list.item(0);
        }
        return null;
    }

    protected static String lookupSingleNodeValue(Object doc, XPath xpath, String expression) throws XPathExpressionException {
        Node node = lookupSingle(doc, xpath, expression);
        return node != null ? node.getNodeValue() : "";
    }

    protected static InputStream getStream(File file) throws IOException {
        byte[] bytes = IOUtils.toByteArray(new FileInputStream(file));
        return new ByteArrayInputStream(bytes);
    }

    protected static NodeList lookup(Object doc, XPath xpath, String expression) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(expression);

        if (DEBUG) {
            System.out.println("Starting from: " + formatNodeWithAtt(doc));
        }
        final NodeList list = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        if (DEBUG) {
            System.out.print("\tFound: ");
        }
        boolean first = true;
        for (Node n : new NodeIterator(list)) {
            if (DEBUG) {
                System.out.println((!first ? "\t       " : "") + formatNodeWithAtt(n));
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

    private static String formatNodeWithAtt(Object o) {
        if (o instanceof Document) {
            return "Root document";
        } else if (o instanceof Node) {
            Node node = (Node) o;

            StringBuilder tmp = new StringBuilder(node.getLocalName());
            if (node.hasAttributes()) {
                for (Node att : new NamedNodeMapIterator(node.getAttributes())) {
                    tmp.append(" ").append(att).append(", ");
                }
            }
            return tmp.toString();
        }
        return o.toString();
    }

    public static class BaseUnit {
        public final int kg;
        public final int m ;
        public final int s;
        public final int A ;
        public final int K;
        public final int mol;
        public final int cd;
        public final int rad;
        public final double factor;
        public final double offset;

        private BaseUnit(int kg, int m, int s, int A, int K, int mol, int cd, int rad, double factor, double offset) {
            this.kg = kg;
            this.m = m;
            this.s = s;
            this.A = A;
            this.K = K;
            this.mol = mol;
            this.cd = cd;
            this.rad = rad;
            this.factor = factor;
            this.offset = offset;
        }

        public static class Builder {
            private int kg = 0;
            private int m = 0;
            private int s = 0;
            private int A = 0;
            private int K = 0;
            private int mol = 0;
            private int cd = 0;
            private int rad = 0;
            private double factor = 1.0;
            private double offset = 0.0;

            public BaseUnit build(){
                return new BaseUnit(kg, m, s, A, K, mol, cd, rad, factor, offset);
            }
            public Builder setKg(int kg){
                this.kg = kg;
                return this;
            }

            public Builder setM(int m){
                this.m = m;
                return this;
            }

            public Builder setS(int s){
                this.s = s;
                return this;
            }

            public Builder setA(int A){
                this.A = A;
                return this;
            }

            public Builder setK(int K){
                this.K = K;
                return this;
            }

            public Builder setMol(int mol){
                this.mol = mol;
                return this;
            }

            public Builder setCd(int cd){
                this.cd = cd;
                return this;
            }

            public Builder setRad(int rad){
                this.rad = rad;
                return this;
            }

            public Builder setFactor(double factor){
                this.factor = factor;
                return this;
            }

            public Builder setOffset(double offset){
                this.offset = offset;
                return this;
            }
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
}
