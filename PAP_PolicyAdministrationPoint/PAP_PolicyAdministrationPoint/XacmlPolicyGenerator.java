package PAP_PolicyAdministrationPoint;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.Random;

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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.http.HttpService;

public class XacmlPolicyGenerator implements Runnable {
	private HashMap<String, String> supportedFunctions;
	private ArrayList<String> supportedCategory;
	private StringBuilder allFunc;
	private StringBuilder allCate;
	private String policyDir;
	private String file;
	private Scanner scanIn;
	private String rpcServer;
	private int nRules;
	private Node node;

	public XacmlPolicyGenerator(String polDir, Scanner scanIn, String rpcServer, Node node) {
		this.policyDir = polDir;
		this.file = null;
		this.scanIn = scanIn;
		this.rpcServer = rpcServer;
		this.nRules = 0;
		this.node = node;

		this.supportedFunctions = new HashMap<String, String>();
		this.supportedCategory = new ArrayList<>();

		this.addSupportedFunctions(this.supportedFunctions);
		this.addSupportedCategory(this.supportedCategory);

		this.allFunc = new StringBuilder();
		this.allCate = new StringBuilder();

		for (String x : this.supportedFunctions.keySet())
			this.allFunc.append(x + " | ");

		for (String x : this.supportedCategory)
			this.allCate.append(x + " | ");
	}

	@Override
	public void run() {
		try {
			String read = null;

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = this.setRuleID(doc);

			this.setEffect(rootElement);
			this.setDescription(doc, rootElement);

			Element target = doc.createElement("Target");
			rootElement.appendChild(target);

			while (true) {
				Element anyOf = doc.createElement("AnyOf");
				target.appendChild(anyOf);

				while (true) {
					Element allOf = doc.createElement("AllOf");
					anyOf.appendChild(allOf);

					while (true) {
						this.nRules++;
						String[] type = new String[1];

						Element match = this.setFunction(doc, allOf, type);
						Element attributeValue = this.setAttributeValue(doc, match, type[0]);
						Element attributeDesignator = this.setAttributeDesignator(doc, match, type[0]);

						this.setData(doc, attributeValue, type[0]);
						String categ = this.setCategory(attributeDesignator);
						
						String param = "()";
						if (categ.contains("subject")) {
							param = "(bytes32)";
						}
						
						while(true) {
							String function = this.setAttributeID(attributeDesignator, categ);
							String issuer = this.setIssuer(attributeDesignator);
							
							if (!this.checkExistenceFunction(issuer, function + param)) {
								System.out.println("Function '" + function + "' doesn't exist in contract " + issuer + "; try again!");
								continue;
							}
							
							break;
						}

						System.out.println("Do you want to add another 'Match' element ? : yes | no");
						read = this.scanIn.nextLine();

						if (read.equals("yes")) continue;
						else break;
					}

					System.out.println("Do you want to add another 'AllOf' element ? : yes | no");
					read = this.scanIn.nextLine();

					if (read.equals("yes")) continue;
					else break;
				}

				System.out.println("Do you want to add another 'AnyOf' element ? : yes | no");
				read = this.scanIn.nextLine();

				if (read.equals("yes")) continue;
				else break;
			}
			
			System.out.println("Choose a name for this policy (default: \"" + this.nRules + ".xml\")");
			read = this.scanIn.nextLine();
			
			if (read.trim().isEmpty()) 
				this.file = this.policyDir + this.nRules + ".xml";
			else
				this.file = this.policyDir + read + ".xml";

			this.buildPolicy(doc);
			PAP_PolicyAdministrationPoint.setNRules(this.nRules);
			PAP_PolicyAdministrationPoint.addPolicy(this.file);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private Element setRuleID(Document doc) {
		Element rootElement = doc.createElement("Rule");
		doc.appendChild(rootElement);

		Random rand = new Random();
		int randomRuleID = rand.nextInt(100000);
		rootElement.setAttribute("RuleId", String.valueOf(randomRuleID));

		return rootElement;
	}

	private void setEffect(Element e) {
		String read;

		do {
			System.out.println("Effect ? : Permit | Deny");
			read = this.scanIn.nextLine();
		}
		while (!read.equals("Permit") && !read.equals("Deny"));

		e.setAttribute("Effect", read);
	}

	private void setDescription(Document doc, Element e) {
		Element description = doc.createElement("Description");

		System.out.println("Description ?");
		String read = this.scanIn.nextLine();

		description.appendChild(doc.createTextNode(read));
		e.appendChild(description);
	}

	private Element setFunction(Document doc, Element e, String[] type) {
		String n = System.lineSeparator();
		String read;
		Element match = doc.createElement("Match");

		do {
			System.out.println("Function name ?"+n + this.allFunc.toString());
			read = this.scanIn.nextLine();
		}
		while (!this.supportedFunctions.containsKey(read));

		type[0] = this.supportedFunctions.get(read);

		e.appendChild(match);
		match.setAttribute("MatchId", "urn:oasis:names:tc:xacml:3.0:function:" + read);

		return match;
	}

	private Element setAttributeValue(Document doc, Element match, String type) {
		Element attributeValue = doc.createElement("AttributeValue");
		match.appendChild(attributeValue);
		attributeValue.setAttribute("DataType", "http://www.w3.org/2001/XMLSchema#" + type);

		return attributeValue;
	}

	private Element setAttributeDesignator(Document doc, Element match, String type) {
		Element attributeDesignator = doc.createElement("AttributeDesignator");
		match.appendChild(attributeDesignator);
		attributeDesignator.setAttribute("DataType", "http://www.w3.org/2001/XMLSchema#" + type);

		return attributeDesignator;
	}

	private void setData(Document doc, Element attributeValue, String type) {
		String read;

		while (true) {
			System.out.println("Data ?");
			read = this.scanIn.nextLine();

			try {
				if (type.equals("integer")) {
					Integer.parseInt(read);
				}
			} catch(NumberFormatException e) {
				System.out.println("Invalid value!");
				continue;
			}

			if (type.equals("boolean")) {
				read = String.valueOf(Boolean.parseBoolean(read));
			}

			// read = string || date || time
			if (read.length() > 32) {
				StringBuilder substr = new StringBuilder();
				byte[] byt = read.getBytes();

				for (int i = 0; i < 32; i++) {
					substr.append((char)byt[i]);
				}

				read = substr.toString();								
			}

			break;
		}

		attributeValue.appendChild(doc.createTextNode(read));
	}

	private String setCategory(Element attributeDesignator) {
		String n = System.lineSeparator();
		String read;

		do {
			System.out.println("Category ?"+n + this.allCate.toString());
			read = this.scanIn.nextLine();
		}
		while (!this.supportedCategory.contains(read));

		attributeDesignator.setAttribute("Category", "urn:oasis:names:tc:xacml:3.0:attribute-category:" + read);
		return read;
	}

	private String setAttributeID(Element attributeDesignator, String categ) {
		System.out.println("AttributeId ?");
		String read = this.scanIn.nextLine();

		attributeDesignator.setAttribute("AttributeId", "urn:oasis:names:tc:xacml:3.0:" + categ + ":" + read);
		attributeDesignator.setAttribute("MustBePresent", "true");

		return this.cleanUnderscore(this.cleanHyphen(read));
	}
	
	private boolean checkExistenceFunction(String address, String function) {
		String code = this.node.getCode(address);
		
		if (code == null)
			return false;

		String encodedFunction = this.node.sha3HashString(function);
		
		if (code.contains(encodedFunction.substring(2, 9))) {
			return true;
		}
		
		return false;
	}

	private String cleanHyphen(String s) {
		if (!s.contains("-")) 
			return s;

		String[] truncated = s.split("-");
		StringBuilder cleaned = new StringBuilder(truncated[0]);

		for(int i = 1; i < truncated.length; i++) {
			cleaned.append(firstCharToUpperCase(truncated[i]));
		}

		return cleaned.toString();
	}

	private String cleanUnderscore(String s) {
		if (!s.contains("_")) 
			return s;

		String[] truncated = s.split("_");
		StringBuilder cleaned = new StringBuilder(truncated[0]);

		for(int i = 1; i < truncated.length; i++) {
			cleaned.append(firstCharToUpperCase(truncated[i]));
		}

		return cleaned.toString();
	}

	private String firstCharToUpperCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private String setIssuer(Element attributeDesignator) {
		try {
			Web3j web3 = Web3j.build(new HttpService(this.rpcServer));
			String read;
			EthGetCode code;

			do {
				System.out.println("Issuer ?  -  If it does not exist you will need to re-insert it");
				read = this.scanIn.nextLine();
				code = web3.ethGetCode(read, DefaultBlockParameterName.LATEST)
						.sendAsync().get();
			} while (code.getResult() == null || code.getResult().equals("0x"));

			attributeDesignator.setAttribute("Issuer", read);
			
			return read;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private void buildPolicy(Document doc) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();	
			DOMSource source = new DOMSource(doc);
			
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			StreamResult result = new StreamResult(new File(this.file));
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	private void addSupportedCategory(ArrayList<String> a) {
		a.add("environment");
		a.add("resource");
		a.add("subject");
		//a.add("action");
	}

	private void addSupportedFunctions(HashMap<String, String> m) {
		m.put("integer-equal", "integer");
		m.put("integer-greater-than", "integer");
		m.put("integer-greater-than-or-equal", "integer");
		m.put("integer-less-than", "integer");
		m.put("integer-less-than-or-equal", "integer");

		m.put("boolean-equal", "boolean");
		m.put("or", "boolean");
		m.put("and", "boolean");

		m.put("string-equal", "string");
		m.put("string-greater-than", "string");
		m.put("string-greater-than-or-equal", "string");
		m.put("string-less-than", "string");
		m.put("string-less-than-or-equal", "string");

		m.put("date-equal", "date");
		m.put("date-greater-than", "date");
		m.put("date-greater-than-or-equal", "date");
		m.put("date-less-than", "date");
		m.put("date-less-than-or-equal", "date");

		m.put("time-equal", "time");
		m.put("time-greater-than", "time");
		m.put("time-greater-than-or-equal", "time");
		m.put("time-less-than", "time");
		m.put("time-less-than-or-equal", "time");
	}
}