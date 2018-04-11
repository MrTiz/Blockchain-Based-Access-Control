package PAP_PolicyAdministrationPoint.XacmlToSolidity;

public class Variable {
	private String type;
	private String name;
	private String initialization;

	public Variable(String t, String n, String i) {
		this.type = t;
		this.name = n;
		this.initialization = i;
	}

	public String varToStringWithoutInit() {
		String n = System.lineSeparator();
		String t = "    ";
		
		return t + this.type + " " + this.name + ";"+n;
	}

	public String varToStringWithoutType() {
		String n = System.lineSeparator();
		String t = "    ";
		
		if (this.initialization != null)
			return t+t + this.name + " = " + this.initialization + ";"+n;
		else
			return "";
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInitialization() {
		return this.initialization;
	}

	public void setInitialization(String initialization) {
		this.initialization = initialization + ";";
	}
}