package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Contract {
	private String contractName;
	private HashMap<String, String> parameters;
	private ArrayList<ContractInterface> interfaces;
	private ArrayList<Variable> variables;
	private ArrayList<Struct> structs;
	private ArrayList<Function> functions;
	private ArrayList<String> text;

	public Contract(String contractName) {
		this.contractName = contractName;
		this.parameters = new HashMap<String, String>();
		this.interfaces = new ArrayList<ContractInterface>();
		this.variables = new ArrayList<Variable>();
		this.structs = new ArrayList<Struct>();
		this.functions = new ArrayList<Function>();
		this.text = new ArrayList<String>();
	}

	public ContractInterface getInterfaceByIssuer(String issuer) {
		for (ContractInterface ci : this.interfaces)
			if (ci.getIssuer().equals(issuer))
				return ci;

		return null;
	}

	public Variable getVarByIssuer(String issuer) {
		for (Variable v : this.variables)
			if (v.getInitialization().contains(issuer))
				return v;

		return null;
	}

	public HashMap<String, String> getParameters() {
		return this.parameters;
	}

	private String paramToString() {
		StringBuilder par = new StringBuilder("(");

		for (String k : this.parameters.keySet())
			par.append(this.parameters.get(k) + " " + k + ", ");

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

	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public ArrayList<ContractInterface> getInterfaces() {
		return this.interfaces;
	}

	public void addInterfaces(ContractInterface interfaces) {
		if (this.getInterfaceByIssuer(interfaces.getIssuer()) != null)
			return;

		this.interfaces.add(interfaces);
	}

	public String interfacesToString() {
		StringBuilder its = new StringBuilder();

		for (ContractInterface ci : this.interfaces)
			its.append("contract " + ci.getContractName() + ci.funcToString());

		return its.toString();
	}

	public ArrayList<Variable> getVariables() {
		return this.variables;
	}

	public void addVariables(Variable variables) {
		for (Variable v : this.variables)
			if (v.getName().equals(variables.getName()))
				return;

		this.variables.add(variables);
	}

	public ArrayList<Struct> getStructs() {
		return this.structs;
	}

	public void addStructs(Struct struct) {
		for (Struct s : this.structs)
			if (s.getName().equals(struct.getName()))
				return;

		this.structs.add(struct);
	}

	public ArrayList<Function> getFunctions() {
		return this.functions;
	}

	public void addFunctions(Function functions) {
		for (Function f : this.functions)
			if (f.getFuncName().equals(functions.getFuncName()))
				return;

		this.functions.add(functions);
	}

	private void setText() {
		String n = System.lineSeparator();
		String t = "    ";
		
		this.text.add("pragma solidity ^0.4.9;"+n+n);

		this.text.add(this.interfacesToString());

		this.text.add(n+"contract " + this.contractName + " {"+n);

		for (Struct s : this.structs)
			this.text.add(s.structToString());

		for (Variable v : this.variables)
			this.text.add(v.varToStringWithoutInit());

		this.text.add(n+t+"function " + this.contractName + this.paramToString() + " public {"+n);

		for (Variable v : this.variables)
			this.text.add(v.varToStringWithoutType());

		this.text.add(t+"}"+n);

		for (Function f : this.functions)
			this.text.add(f.funcToString());

		this.text.add("}");
	}

	public void writeContract(String path) {
		try {
			this.setText();

			File f = new File(path + this.contractName + ".sol");
			FileWriter fw = new FileWriter(f, false);

			for (String x : this.text)
				fw.write(x);

			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}