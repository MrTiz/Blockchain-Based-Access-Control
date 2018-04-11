package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import PAP_PolicyAdministrationPoint.NodeInterface;
import PAP_PolicyAdministrationPoint.PAP_PolicyAdministrationPoint;

public class ParserXacmlPolicy implements Runnable {
	private HashMap<String, String> types;		// supported types
	private HashMap<String, String> functions;  // supported XACML functions
	private HashMap<String, Integer> categories;	// supported categories
	
	private Contract pdpContract; 
	private Contract pipContract;
	
	private ArrayList<AnyOf> allRule;
	private Document doc;
	private String srcDir;
	private int nRules;
	private NodeInterface node;

	public ParserXacmlPolicy(Document policy, String solPath, NodeInterface node) {
		this.types = new HashMap<String, String>();
		this.functions = new HashMap<String, String>();
		this.categories = new HashMap<String, Integer>();
		
		this.pdpContract = new Contract("PolicyDecisionPoint");
		this.pipContract = new Contract("PolicyInformationPoint");
		
		this.allRule = new ArrayList<AnyOf>();
		this.doc = policy;
		this.srcDir = solPath;
		this.nRules = 0;
		this.node = node;

		this.initFunctionMap();
		this.initTypesMap();
		this.initCategoriesMap();
	}

	public void run() {
		Node rule = this.doc.getElementsByTagName("Rule").item(0);

		boolean allow = false;			
		if (((Element) rule).getAttribute("Effect").equals("Permit"))
			allow = true;

		this.setAuxFuncAndVar();
		this.parseTree(rule, null, null);
		this.makeGlobalRule(allow);

		this.pdpContract.writeContract(this.srcDir);
		this.pipContract.writeContract(this.srcDir);
		
		PAP_PolicyAdministrationPoint.setNRules(this.nRules);
	}

	private void setAuxFuncAndVar() {
		String n = System.lineSeparator();
		String t = "    ";
		
		ContractInterface pip = new ContractInterface("PIPInterface", null);

		Variable admin = new Variable("address private", "admin", "msg.sender");
		//Variable actions = new Variable("bytes32[] private", "actions", null);
		Variable pip_contr = new Variable("PIPInterface private", 
				"PIPcontr", "PIPInterface(pipAddr)");
		Variable id = new Variable("uint private", "id", "0");
		Variable register = new Variable("Session[] private", "register", null);

		Struct user = new Struct("User");
		user.addVariables(new Variable("bytes32", "userName", null));
		user.addVariables(new Variable("bool", "authorized", null));
		Struct session = new Struct("Session");
		//session.addVariables(new Variable("address", "admin", null));
		session.addVariables(new Variable("uint", "id", null));
		session.addVariables(new Variable("User", "users", null));

		Function gsbi = new Function("getSessionById", "public view");
		gsbi.addParameters("_id", "uint");
		gsbi.setReturnTypes("bytes32, bool");
		String body1 = "if (_id >= register.length) {"+n+t+t+t
				+ "revert();"+n+t+t+"}"+n+n+t+t
				+ "return (register[_id].users.userName,"+n+t+t+t+t
				+ "register[_id].users.authorized);";
		gsbi.setBody(body1);

		Function gp = new Function("getPermission", "public");
		gp.addParameters("subject", "bytes32");
		gp.setReturnTypes("uint");
		String body2 = "if (msg.sender != admin) {"+n
				+ t+t+t+"revert();"+n
				+ t+t+"}"+n
				+ n+t+t+"bool outcome = globalRule(subject);"+n
				+ n+t+t+"var u = User(subject, outcome);"+n
				+ t+t+"var s = Session(id, u);"+n
				+ n+t+t+"id++;"+n
				+ t+t+"register.push(s);"+n				
				+ t+t+"return (id - 1);";
		gp.setBody(body2);

		this.pdpContract.addInterfaces(pip);
		this.pdpContract.addVariables(admin);
		//this.pdpContract.addVariables(actions);
		this.pdpContract.addVariables(pip_contr);
		this.pdpContract.addVariables(id);
		this.pdpContract.addVariables(register);
		this.pdpContract.addStructs(user);
		this.pdpContract.addStructs(session);
		this.pdpContract.addFunctions(gsbi);
		this.pdpContract.addFunctions(gp);
		this.pdpContract.addParameters("pipAddr", "address");
		
		Variable pdp = new Variable("address private", "pdp", "0x0");
		
		Function setPDP = new Function("setPDP", "public");
		setPDP.addParameters("pdpAddr", "address");
		String body3 = "if (msg.sender != admin) {"+n
				+ t+t+t+"revert();"+n
				+ t+t+"}"+n
				+ n+t+t+ "pdp = pdpAddr;";
		setPDP.setBody(body3);
		
		Function checkSender = new Function("checkSender", "private view");
		checkSender.addParameters("sender", "address");
		String body4 = "if (sender != pdp) {"+n
				+ t+t+t+"revert();"+n
				+ t+t+"}";
		checkSender.setBody(body4);
		
		this.pipContract.addVariables(admin);
		this.pipContract.addVariables(pdp);
		this.pipContract.addFunctions(setPDP);
		this.pipContract.addFunctions(checkSender);
	}

	private void makeGlobalRule(boolean b) {
		Function globalRule = new Function("globalRule", "private view");

		globalRule.addParameters("subject", "bytes32");
		globalRule.setReturnTypes("bool");

		String allow = "!";
		if (b) allow = "";

		StringBuilder body = new StringBuilder("return " + allow);

		if (this.allRule.size() > 1) {
			body.append("(");

			for (AnyOf x : this.allRule)
				body.append(x.orBetweenFunc() + ") && (");

			globalRule.setBody(body.replace(body.length() - 6, body.length(), ");").toString());
		}
		else {
			body.append(this.allRule.get(0).orBetweenFunc() + ";");
			globalRule.setBody(body.toString());
		}

		this.pdpContract.addFunctions(globalRule);
	}

	private void parseTree(Node n, AllOf allOF, AnyOf anyOF) {
		String t = "    ";
		String permitOnlyPDP = "checkSender(msg.sender);"+System.lineSeparator()+t+t;

		NodeList nl = n.getChildNodes();

		for (int i = 0; i < nl.getLength(); i++) {
			if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = nl.item(i).getNodeName();

				if (nodeName.equals("AnyOf")) {
					AnyOf anyOfFunc = new AnyOf();
					parseTree(nl.item(i), null, anyOfFunc);

					if (!anyOfFunc.getAnyOfList().isEmpty()) 
						this.allRule.add(anyOfFunc);
				}
				else if (nodeName.equals("AllOf")) {
					AllOf allOfFunc = new AllOf();
					parseTree(nl.item(i), allOfFunc, anyOF);

					if (!allOfFunc.getAllOfList().isEmpty()) 
						anyOF.addAllOf(allOfFunc);
				}
				else if (nodeName.equals("Target")) {
					parseTree(nl.item(i), null, null);
				}
				else if (nodeName.equals("Match")) {					
					Element match = (Element) nl.item(i);
					String matchId = match.getAttribute("MatchId").trim();
					String func = matchId.split("function:")[1];

					String attrVal = match.getElementsByTagName("AttributeValue").item(0).getTextContent().trim();
					Node _node = match.getElementsByTagName("AttributeDesignator").item(0);
					Element attrDes = (Element) _node;

					String cat = attrDes.getAttribute("Category").trim();
					String category = cat.split("category:")[1];

					String type = attrDes.getAttribute("DataType").trim();
					String issuer = attrDes.getAttribute("Issuer").trim();

					String attrId = attrDes.getAttribute("AttributeId").trim();
					String attributeId = this.cleanUnderscore(this.cleanHyphen(attrId.split(category + ":")[1]));

					int counter = this.categories.get(category).intValue() + 1;
					this.categories.put(category, counter);
					this.nRules++;
					
					Function pdpFunc = new Function("rule" + nRules, "private view");
					Function pipFunc = new Function(category.substring(0, 3) + counter + attributeId, "public view");
					Function pipFunc2 = new Function(category.substring(0, 3) + counter + attributeId, "public pure");
					Function catFunc = new Function(attributeId, "public pure");

					if (category.contains("subject")) {
						pdpFunc.addParameters("subject", "bytes32");
						pipFunc.addParameters("subject", "bytes32");
						pipFunc2.addParameters("subject", "bytes32");
						catFunc.addParameters("subject", "bytes32");
					}
					
					if (!this.checkExistenceFunction(issuer, catFunc)) {
						System.out.println("Function '" + catFunc.getFuncName() + "' doesn't exist in contract " + issuer);
						continue;
					}

					type = this.types.get(type.split("XMLSchema#")[1]);

					pdpFunc.setReturnTypes("bool");
					pipFunc.setReturnTypes(type);
					pipFunc2.setReturnTypes(type);
					catFunc.setReturnTypes(type);

					String bodyPIP = permitOnlyPDP + "return TARGET2.TARGET3;";
					String bodyPDP = this.functions.get(func).replace("TARGET1", attrVal).replace("TARGET2", "PIPcontr");

					bodyPIP = bodyPIP.replace("TARGET3", attributeId + pipFunc.paramToString());
					bodyPDP = bodyPDP.replace("TARGET3", category.substring(0, 3) + counter + attributeId + pdpFunc.paramToString());

					/*if (category.contains("action")) {
						bodyPDP = bodyPDP.replace(";", "").replace("return ", "if (");
						bodyPDP = bodyPDP + ") {\n\t\t\tactions.push(\"" + attrVal
								+ "\");\n\t\t\treturn true;\n\t\t}\n\t\treturn false;";
					}*/

					ContractInterface getExistentInterface = this.pipContract.getInterfaceByIssuer(issuer);

					if (getExistentInterface == null) {
						String upperCategory = this.firstCharToUpperCase(category) + "Contract" + counter;
						ContractInterface trustedContr = new ContractInterface(upperCategory, issuer);
						trustedContr.addFunction(catFunc);

						Variable var = new Variable(upperCategory + " private", 
								category + "Contr" + counter,
								upperCategory + "(" + issuer + ")");

						this.pipContract.addVariables(var);
						this.pipContract.addInterfaces(trustedContr);

						bodyPIP = bodyPIP.replace("TARGET2", category + "Contr" + counter);
					}
					else {
						getExistentInterface.addFunction(catFunc);
						bodyPIP = bodyPIP.replace("TARGET2", this.pipContract.getVarByIssuer(issuer).getName());
					}

					pipFunc.setBody(bodyPIP);
					pipFunc2.setBody(bodyPIP);
					pdpFunc.setBody(bodyPDP);

					this.pdpContract.getInterfaces().get(0).addFunction(pipFunc2);
					this.pdpContract.addFunctions(pdpFunc);
					this.pipContract.addFunctions(pipFunc);

					allOF.addFunc(pdpFunc);
				}
			}
		}
	}
	
	private boolean checkExistenceFunction(String address, Function function) {
		String code = this.node.getCode(address);
		
		if (code == null)
			return false;
		
		String param = function.paramToStringOnlyTypes().replaceAll(" ", "");
		String encodedFunction = this.node.sha3HashString(function.getFuncName() + param);
		
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

	private void initTypesMap() {
		this.types.put("integer", "int");
		this.types.put("boolean", "bool");
		this.types.put("string", "bytes32");
		this.types.put("date", "bytes32");
		this.types.put("time", "bytes32");
	}

	private void initFunctionMap() {
		String tmp0 = "return (TARGET1 ";
		String tmp1 = " TARGET2.TARGET3);";
		this.functions.put("integer-equal", tmp0 + "==" + tmp1);
		this.functions.put("integer-greater-than", tmp0 + "<" + tmp1);
		this.functions.put("integer-greater-than-or-equal", tmp0 + "<=" + tmp1);
		this.functions.put("integer-less-than", tmp0 + ">" + tmp1);
		this.functions.put("integer-less-than-or-equal", tmp0 + ">=" + tmp1);

		this.functions.put("boolean-equal", tmp0 + "==" + tmp1);
		this.functions.put("or", tmp0 + "||" + tmp1);
		this.functions.put("and", tmp0 + "&&" + tmp1);

		String tmp2 = "return bytes32(\"TARGET1\") ";
		String tmp3 = " bytes32(TARGET2.TARGET3);";
		this.functions.put("string-equal", tmp2 + "==" + tmp3);
		this.functions.put("string-greater-than", tmp2 + "<" + tmp3);
		this.functions.put("string-greater-than-or-equal", tmp2 + "<=" + tmp3);
		this.functions.put("string-less-than", tmp2 + ">" + tmp3);
		this.functions.put("string-less-than-or-equal", tmp2 + ">=" + tmp3);

		this.functions.put("date-equal", tmp2 + "==" + tmp3);
		this.functions.put("date-greater-than", tmp2 + "<" + tmp3);
		this.functions.put("date-greater-than-or-equal", tmp2 + "<=" + tmp3);
		this.functions.put("date-less-than", tmp2 + ">" + tmp3);
		this.functions.put("date-less-than-or-equal", tmp2 + ">=" + tmp3);

		this.functions.put("time-equal", tmp2 + "==" + tmp3);
		this.functions.put("time-greater-than", tmp2 + "<" + tmp3);
		this.functions.put("time-greater-than-or-equal", tmp2 + "<=" + tmp3);
		this.functions.put("time-less-than", tmp2 + ">" + tmp3);
		this.functions.put("time-less-than-or-equal", tmp2 + ">=" + tmp3);
	}
	
	private void initCategoriesMap() {
		this.categories.put("environment", 0);
		this.categories.put("resource", 0);
		this.categories.put("subject", 0);
	}
}