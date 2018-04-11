package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.util.HashMap;

public class Function {
	private String funcName;
	private HashMap<String, String> parameters;
	private String returnTypes;
	private String body;
	private String visibility;

	public Function(String funcName, String visibility) {
		this.funcName = funcName;
		this.parameters = new HashMap<String, String>();
		this.returnTypes = null;
		this.body = null;
		this.visibility = visibility;
	}

	public String getFuncName() {
		return this.funcName;
	}

	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}

	public HashMap<String, String> getParameters() {
		return this.parameters;
	}

	public String funcToString() {
		String n = System.lineSeparator();
		String t = "    ";
		
		StringBuilder sb = new StringBuilder();
		sb.append(n+t+"function " + this.funcName + this.paramToStringWithTypes() + " " + this.visibility);
		
		if (this.returnTypes != null)
			sb.append(" returns(" + this.returnTypes + ") {"+n+t+t+ this.body + n+t+"}"+n);
		else
			sb.append(" {"+n+t+t+ this.body + n+t+"}"+n);

		return sb.toString();
	}
	
	public String paramToStringOnlyTypes() {
		StringBuilder par = new StringBuilder("(");

		for (String p : this.parameters.keySet())
			par.append(this.parameters.get(p) + ", ");

		if (!par.toString().equals("("))
			return par.replace(par.length() - 2, par.length(), ")").toString();

		return par.append(")").toString();
	}

	public String paramToStringWithTypes() {
		StringBuilder par = new StringBuilder("(");

		for (String p : this.parameters.keySet())
			par.append(this.parameters.get(p) + " " + p + ", ");

		if (!par.toString().equals("("))
			return par.replace(par.length() - 2, par.length(), ")").toString();

		return par.append(")").toString();
	}

	public String paramToString() {
		StringBuilder par = new StringBuilder("(");

		for (String p : this.parameters.keySet())
			par.append(p + ", ");

		if (this.parameters.size() > 0)
			return par.replace(par.length() - 2, par.length(), ")").toString();

		return par.append(")").toString();
	}

	public void addParameters(String name, String type) {
		for (String n : this.parameters.keySet())
			if (n.equals(name) && type.equals(this.parameters.get(n)))
				return;

		this.parameters.put(name, type);
	}

	public String getReturnTypes() {
		return this.returnTypes;
	}

	public void setReturnTypes(String returnType) {
		this.returnTypes = returnType;
	}

	public String getBody() {
		return this.body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getVisibility() {
		return this.visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}
}