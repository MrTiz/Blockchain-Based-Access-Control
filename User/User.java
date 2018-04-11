import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class User {
	public static void main(String[] args) {
		String HOST = "localhost";
		int PORT = 44444;

		InetSocketAddress address = new InetSocketAddress(HOST, PORT);		
		String request = "USER" + System.lineSeparator() + makeRequest("prova prova");
		
		try (SocketChannel server = SocketChannel.open(address);) {
			ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
			ByteBuffer message = ByteBuffer.allocate(request.length());

			length.putInt(request.length());
			message.put(request.getBytes());

			length.flip();
			message.flip();

			server.write(length);
			server.write(message);

			length.clear();

			while(server.read(length) != -1) {
				if(!length.hasRemaining()) {
					length.flip();

					int l = length.getInt();
					message = ByteBuffer.allocate(l);

					if(server.read(message) == -1) 
						break;

					if(message.position() == l) {
						message.flip();

						System.out.println(parseResponse(new String(message.array())));
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String makeRequest(String userName) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			
			Element rootElement = doc.createElement("Request");
			doc.appendChild(rootElement);

			Element attributes = doc.createElement("Attributes");
			rootElement.appendChild(attributes);
			attributes.setAttribute("Category", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
			
			Element attribute = doc.createElement("Attribute");
			attributes.appendChild(attribute);
			attribute.setAttribute("AttributeId", "user.identifier");
			attribute.setAttribute("IncludeInResult", "true");
			
			Element attributeValue = doc.createElement("AttributeValue");
			attribute.appendChild(attributeValue);
			attributeValue.setAttribute("DataType", "http://www.w3.org/2001/XMLSchema#string");
			attributeValue.appendChild(doc.createTextNode(userName));
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();	
			DOMSource source = new DOMSource(doc);
			
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			
			StringWriter outWriter = new StringWriter();
			StreamResult result = new StreamResult(outWriter);
			transformer.transform(source, result);
			
			return outWriter.getBuffer().toString(); 
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static boolean parseResponse(String response) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			Document doc = dBuilder.parse(new InputSource(new StringReader(response)));
			doc.getDocumentElement().normalize();
			
			String outcome = doc.getElementsByTagName("Decision").item(0).getTextContent().trim();
			
			if (outcome.equals("Permit"))
				return true;
			
			return false;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return false;
	}
}