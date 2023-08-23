package eu.domibus.api.util.xml;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.InputStream;

/**
 * Created by Cosmin Baciu on 14-Sep-16.
 */
public interface XMLUtil {

    String BEAN_NAME = "DomibusXMLUtil";

    MessageFactory getMessageFactorySoap12();

    TransformerFactory getTransformerFactory();

    DocumentBuilderFactory getDocumentBuilderFactory();

    DocumentBuilderFactory getDocumentBuilderFactoryNamespaceAware();

    DatatypeFactory getDatatypeFactory();

    UnmarshallerResult unmarshal(boolean ignoreWhitespaces, JAXBContext jaxbContext, InputStream xmlStream, InputStream xsdStream) throws SAXException, JAXBException, ParserConfigurationException, XMLStreamException;

    byte[] marshal(JAXBContext jaxbContext, Object input, InputStream xsdStream) throws SAXException, JAXBException, ParserConfigurationException, XMLStreamException;

    XMLInputFactory getXmlInputFactory();

    XMLStreamReader getXmlStreamReaderFromNode(Node messagingXml) throws TransformerException, XMLStreamException;
}
