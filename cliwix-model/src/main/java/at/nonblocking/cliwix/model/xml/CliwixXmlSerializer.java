/*
 * Copyright (c) 2014-2016
 * nonblocking.at gmbh [http://www.nonblocking.at]
 *
 * This file is part of Cliwix.
 *
 * Cliwix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.nonblocking.cliwix.model.xml;

import at.nonblocking.cliwix.model.LiferayConfig;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.util.Stack;

/**
 * Utility methods to serialize and de-serialize a Cliwix model
 */
public class CliwixXmlSerializer {

    private static final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
    private static final Schema cliwixSchema;

    static {
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setResourceResolver(new ClasspathResourceResolver());

        try (InputStream cliwixSchemaFileStream = getSchema(getCliwixSchemaName())) {
            cliwixSchema = sf.newSchema(new StreamSource(cliwixSchemaFileStream));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to load Cliwix Schema!", e);
        }
    }

    private CliwixXmlSerializer() {
    }

    public static String getCliwixSchemaName() {
        return "cliwix_" + LiferayConfig.MODEL_VERSION.replace(".", "_") + ".xsd";
    }

    public static InputStream getSchema(String schemaName) throws IOException {
        InputStream schemaFileIS = CliwixXmlSerializer.class.getResourceAsStream("/" + schemaName);
        if (schemaFileIS == null) {
            schemaFileIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(schemaName);
        }

        if (schemaFileIS == null) {
            throw new FileNotFoundException(schemaName + " not found in classpath!");
        }

        return schemaFileIS;
    }

    public static void toXML(LiferayConfig config, OutputStream outputStream) throws JAXBException, XMLStreamException {
        toXML(config, outputStream, null);
    }

    public static void toXML(LiferayConfig config, OutputStream outputStream, Marshaller.Listener listener) throws JAXBException, XMLStreamException {
        Marshaller marshaller = JAXBContext.newInstance(LiferayConfig.class).createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://nonblocking.at/cliwix cliwix_" + LiferayConfig.MODEL_VERSION.replace('.', '_') + ".xsd");
        marshaller.setListener(listener);

        XMLStreamWriter writer = outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
        marshaller.marshal(config, new CliwixIndentingXMLStreamWriter(writer));
    }

    public static LiferayConfig fromXML(File inputFile) throws FileNotFoundException, CliwixSchemaValidationException {
        return fromXML(inputFile, null, null);
    }

    public static LiferayConfig fromXML(File inputFile, Unmarshaller.Listener listener) throws FileNotFoundException, CliwixSchemaValidationException {
        return fromXML(inputFile, listener, null);
    }

    public static LiferayConfig fromXML(File inputFile, Unmarshaller.Listener listener, ValidationEventHandler validationListener)
            throws FileNotFoundException, CliwixSchemaValidationException {

        Unmarshaller unmarshaller;
        try {
            unmarshaller = JAXBContext.newInstance(LiferayConfig.class).createUnmarshaller();
            unmarshaller.setSchema(cliwixSchema);
            unmarshaller.setListener(listener);
            unmarshaller.setEventHandler(validationListener);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setXIncludeAware(true);
        spf.setNamespaceAware(true);

        XMLReader xr;
        try {
            xr = spf.newSAXParser().getXMLReader();
        } catch (Exception e) {
            throw new AssertionError("SAX parser is in classpath");
        }
        SAXSource source = new SAXSource(xr, new InputSource(new FileInputStream(inputFile)));

        //Set the input file directory as CWD to be able to process XInclude relative paths correctly
        String originalCWD = System.getProperty("user.dir");
        System.setProperty("user.dir", inputFile.getParentFile().getAbsolutePath());

        LiferayConfig liferayConfig = null;

        try {
            liferayConfig = (LiferayConfig) unmarshaller.unmarshal(source);
        } catch (JAXBException e) {
            throw new CliwixSchemaValidationException("Invalid XML", e);
        } finally {
            System.setProperty("user.dir", originalCWD);
        }

        liferayConfig.setSourceFile(inputFile);

        return liferayConfig;
    }

    private static class CliwixIndentingXMLStreamWriter implements XMLStreamWriter {

        private enum STATE { SEEN_NOTHING, SEEN_ELEMENT, SEEN_DATA }

        private STATE state = STATE.SEEN_NOTHING;
        private Stack<STATE> stateStack = new Stack<>();

        private String indentStep = "  ";
        private int depth = 0;

        private XMLStreamWriter writer;

        public CliwixIndentingXMLStreamWriter(XMLStreamWriter writer) {
            this.writer = writer;
        }

        private void onStartElement() throws XMLStreamException {
            stateStack.push(STATE.SEEN_ELEMENT);
            state = STATE.SEEN_NOTHING;
            if (depth > 0) {
                this.writer.writeCharacters("\n");
            }
            doIndent();
            depth++;
        }

        private void onEndElement() throws XMLStreamException {
            depth--;
            if (state == STATE.SEEN_ELEMENT) {
                this.writer.writeCharacters("\n");
                doIndent();
            }
            state = stateStack.pop();
        }

        private void onEmptyElement() throws XMLStreamException {
            state = STATE.SEEN_ELEMENT;
            if (depth > 0) {
                this.writer.writeCharacters("\n");
            }
            doIndent();
        }

        private void doIndent() throws XMLStreamException {
            if (depth > 0) {
                for (int i = 0; i < depth; i++) {
                    this.writer.writeCharacters(indentStep);
                }
            }
        }

        @Override
        public void writeStartDocument() throws XMLStreamException {
            this.writer.writeStartDocument();
            this.writer.writeCharacters("\n");
        }

        @Override
        public void writeStartDocument(String version) throws XMLStreamException {
            this.writer.writeStartDocument(version);
            this.writer.writeCharacters("\n");
        }

        @Override
        public void writeStartDocument(String encoding, String version) throws XMLStreamException {
            this.writer.writeStartDocument(encoding, version);
            this.writer.writeCharacters("\n");
        }

        @Override
        public void writeStartElement(String localName) throws XMLStreamException {
            onStartElement();
            this.writer.writeStartElement(localName);
        }

        @Override
        public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
            onStartElement();
            this.writer.writeStartElement(namespaceURI, localName);
        }

        @Override
        public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            onStartElement();
            this.writer.writeStartElement(prefix, localName, namespaceURI);
        }

        @Override
        public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
            onEmptyElement();
            this.writer.writeEmptyElement(namespaceURI, localName);
        }

        @Override
        public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            onEmptyElement();
            this.writer.writeEmptyElement(prefix, localName, namespaceURI);
        }

        @Override
        public void writeEmptyElement(String localName) throws XMLStreamException {
            onEmptyElement();
            this.writer.writeEmptyElement(localName);
        }

        @Override
        public void writeEndElement() throws XMLStreamException {
            onEndElement();
            this.writer.writeEndElement();
        }

        @Override
        public void writeCharacters(String text) throws XMLStreamException {
            state = STATE.SEEN_DATA;
            if (text.startsWith(AdapterCDATA.CDATA_BEGIN)) {
                writeAsCData(text);
            } else {
                this.writer.writeCharacters(text);
            }
        }

        @Override
        public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
            state = STATE.SEEN_DATA;
            this.writer.writeCharacters(text, start, len);
        }

        @Override
        public void writeCData(String data) throws XMLStreamException {
            state = STATE.SEEN_DATA;
            this.writer.writeCData(data);
        }

        private void writeAsCData(String text) throws XMLStreamException {
            writeCData(text.substring(AdapterCDATA.CDATA_BEGIN.length(), text.length() - AdapterCDATA.CDATA_END.length()));
        }

        @Override
        public void writeEndDocument() throws XMLStreamException {
            this.writer.writeEndDocument();
        }

        @Override
        public void close() throws XMLStreamException {
            this.writer.close();
        }

        @Override
        public void flush() throws XMLStreamException {
            this.writer.flush();
        }

        @Override
        public void writeAttribute(String localName, String value) throws XMLStreamException {
            this.writer.writeAttribute(localName, value);
        }

        @Override
        public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
            this.writer.writeAttribute(prefix, namespaceURI, localName, value);
        }

        @Override
        public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
            this.writer.writeAttribute(namespaceURI, localName, value);
        }

        @Override
        public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
            this.writer.writeNamespace(prefix, namespaceURI);
        }

        @Override
        public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
            this.writer.writeDefaultNamespace(namespaceURI);
        }

        @Override
        public void writeComment(String data) throws XMLStreamException {
            this.writer.writeComment(data);
        }

        @Override
        public void writeProcessingInstruction(String target) throws XMLStreamException {
            this.writer.writeProcessingInstruction(target);
        }

        @Override
        public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
            this.writer.writeProcessingInstruction(target, data);
        }

        @Override
        public void writeDTD(String dtd) throws XMLStreamException {
            this.writer.writeDTD(dtd);
        }

        @Override
        public void writeEntityRef(String name) throws XMLStreamException {
            this.writer.writeEntityRef(name);
        }

        @Override
        public String getPrefix(String uri) throws XMLStreamException {
            return this.writer.getPrefix(uri);
        }

        @Override
        public void setPrefix(String prefix, String uri) throws XMLStreamException {
            this.writer.setPrefix(prefix, uri);
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
            this.writer.setDefaultNamespace(uri);
        }

        @Override
        public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
            this.writer.setNamespaceContext(context);
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return this.writer.getNamespaceContext();
        }

        @Override
        public Object getProperty(String name) throws IllegalArgumentException {
            return this.writer.getProperty(name);
        }
    }

    private static class ClasspathResourceResolver implements LSResourceResolver {

        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            LSInput lsInput = new LSInputImpl();

            try {
                InputStream is = getSchema(systemId);
                lsInput.setByteStream(is);
                lsInput.setSystemId(systemId);
                return lsInput;
            } catch (IOException e) {
                return null;
            }
        }

        private static class LSInputImpl implements LSInput {

            private String publicId = null;
            private String systemId = null;
            private String baseSystemId = null;
            private InputStream byteStream = null;
            private Reader charStream = null;
            private String data = null;
            private String encoding = null;
            private boolean certifiedText = false;

            public InputStream getByteStream(){
                return byteStream;
            }

            public void setByteStream(InputStream byteStream){
                this.byteStream = byteStream;
            }

            public Reader getCharacterStream(){
                return charStream;
            }

            public void setCharacterStream(Reader characterStream){
                charStream = characterStream;
            }

            public String getStringData(){
                return data;
            }

            public void setStringData(String stringData){
                data = stringData;
            }

            public String getEncoding(){
                return encoding;
            }

            public void setEncoding(String encoding){
                this.encoding = encoding;
            }

            public String getPublicId(){
                return publicId;
            }

            public void setPublicId(String publicId){
                this.publicId = publicId;
            }

            public String getSystemId(){
                return systemId;
            }

            public void setSystemId(String systemId){
                this.systemId = systemId;
            }

            public String getBaseURI(){
                return baseSystemId;
            }

            public void setBaseURI(String baseURI){
                baseSystemId = baseURI;
            }

            public boolean getCertifiedText(){
                return certifiedText;
            }

            public void setCertifiedText(boolean certifiedText){
                this.certifiedText = certifiedText;
            }
        }
    }
}
