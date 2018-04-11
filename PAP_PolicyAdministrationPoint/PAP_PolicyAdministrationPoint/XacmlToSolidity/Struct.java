package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.util.ArrayList;

public class Struct {
	private String name;
	private ArrayList<Variable> variables;

	public Struct(String name) {
		this.name = name;
		this.variables = new ArrayList<Variable>();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<Variable> getVariables() {
		return this.variables;
	}

	public void addVariables(Variable var) {
		for (Variable v : this.variables)
			if (v.getName().equals(var.getName()))
				return;

		this.variables.add(var);
	}

	public String structToString() {
		String n = System.lineSeparator();
		String t = "    ";

		StringBuilder tmp = new StringBuilder(t+"struct ");
		tmp.append(this.name + " {"+n+t+t);

		for (Variable v : this.variables)
			tmp.append(v.getType() + " " + v.getName() + ";"+n+t+t);

		tmp.replace(tmp.length() - 4, tmp.length(), "}"+n+n);
		return tmp.toString();
	}
}