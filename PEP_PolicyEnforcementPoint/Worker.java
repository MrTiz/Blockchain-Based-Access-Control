import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

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

public class Worker implements Runnable {
	private LinkedBlockingQueue<SelectionKey> bfs;
	private Selector selector;
	private ArrayList<User> waitingClients;
	private SocketChannel ch_server;

	public Worker(LinkedBlockingQueue<SelectionKey> bfs, 
			Selector selector, 
			ArrayList<User> waitingClients, 
			SocketChannel ch_server) 
	{
		this.bfs = bfs;
		this.selector = selector;
		this.waitingClients = waitingClients;
		this.ch_server = ch_server;
	}

	@Override
	public void run() {
		try {
			SelectionKey key;
			Attachment att;
			User aux = null;

			while (true) {
				key = this.bfs.take();
				att = (Attachment) key.attachment();

				String message = new String(att.getBuffer().array());
				String issuer = message.substring(0, message.indexOf(System.lineSeparator()));

				if (issuer.equals("CTXHND")) {
					String[] read = new String(att.getBuffer().array()).split(System.lineSeparator());
					
					for (User u : this.waitingClients)
						if (u.getUser().equals(read[1])) {
							String response = this.makeResponse(read[2]);
							
							System.out.println("Response for '" + read[1] + "'");
							System.out.println(System.lineSeparator() + response);
							System.out.println("________________________________" + System.lineSeparator());

							key = u.getKey();

							att = (Attachment) key.attachment();
							att.getLength().clear();
							att.setMessage(response);

							key.channel().keyFor(this.selector).interestOps(SelectionKey.OP_WRITE);					
							this.selector.wakeup();

							aux = u;
							break;
						}

					if (aux != null) {
						this.waitingClients.remove(aux);
						aux = null;
					}
				}
				else if (issuer.equals("USER"))  {
					String read = message.substring(message.indexOf(System.lineSeparator()) + 1, message.length() - 1);
					String request = this.parseRequest(read);
					String mex = "PEP" + System.lineSeparator() + request;
					this.waitingClients.add(new User(request, key));
					
					System.out.println("Request from '" + request + "'");
					System.out.println(System.lineSeparator() + read);
					System.out.println("________________________________" + System.lineSeparator());

					att = (Attachment) this.ch_server.keyFor(this.selector).attachment();
					att.getLength().clear();
					att.setMessage(mex);

					this.ch_server.keyFor(this.selector).interestOps(SelectionKey.OP_WRITE);
					this.selector.wakeup();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private String parseRequest(String request) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			
			Document doc = dBuilder.parse(new InputSource(new StringReader(request)));
			doc.getDocumentElement().normalize();
			
			return doc.getElementsByTagName("AttributeValue").item(0).getTextContent().trim();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private String makeResponse(String outcome) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();

			Element rootElement = doc.createElement("Response");
			doc.appendChild(rootElement);

			Element result = doc.createElement("Result");
			rootElement.appendChild(result);

			Element decision = doc.createElement("Decision");
			result.appendChild(decision);

			if (Boolean.parseBoolean(outcome))		
				decision.appendChild(doc.createTextNode("Permit"));
			else
				decision.appendChild(doc.createTextNode("Deny"));

			Element status = doc.createElement("Status");
			result.appendChild(status);

			Element statusCode = doc.createElement("StatusCode");
			status.appendChild(statusCode);
			statusCode.setAttribute("Value", "urn:oasis:names:tc:xacml:1.0:status:ok");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();	
			DOMSource source = new DOMSource(doc);

			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			StringWriter outWriter = new StringWriter();
			StreamResult _result = new StreamResult(outWriter);
			transformer.transform(source, _result);

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
}