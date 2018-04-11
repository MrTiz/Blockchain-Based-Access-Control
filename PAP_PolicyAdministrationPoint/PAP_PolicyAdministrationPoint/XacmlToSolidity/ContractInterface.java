package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.util.ArrayList;

public class ContractInterface {
	private String contractName;
	private String issuer;
	private ArrayList<Function> functionsList;

	public ContractInterface(String contactName, String issuer) {
		this.setContractName(contactName);
		this.issuer = issuer;
		this.functionsList = new ArrayList<Function>();
	}

	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public ArrayList<Function> getFunctionsList() {
		return this.functionsList;
	}

	private String whiteSpace() {
		int n = ("contract " + this.contractName + " { ").length();
		return new String(new char[n]).replace('\0', ' ');
	}

	public String funcToString() {
		String n = System.lineSeparator();
		StringBuilder fun = new StringBuilder(" { ");

		for (Function f : this.functionsList) {
			fun.append("function " + f.getFuncName());
			fun.append(f.paramToStringOnlyTypes());
			fun.append(" " + f.getVisibility());
			fun.append(" returns(" + f.getReturnTypes());
			fun.append(") {} "+n + this.whiteSpace());
		}

		int k = (n + this.whiteSpace()).length();
		fun.replace(fun.length() - k, fun.length(), "}"+n);
		return fun.toString();
	}

	public void addFunction(Function function) {		
		for (Function f : this.functionsList)
			if (f.getFuncName().equals(function.getFuncName()))
				return;

		this.functionsList.add(function);
	}
}