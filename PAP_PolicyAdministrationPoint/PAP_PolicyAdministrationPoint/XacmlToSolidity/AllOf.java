package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.util.ArrayList;

public class AllOf {
	private ArrayList<Function> allOfList;

	public AllOf() {
		this.allOfList = new ArrayList<Function>();
	}

	public ArrayList<Function> getAllOfList() {
		return this.allOfList;
	}

	public void addFunc(Function function) {
		this.allOfList.add(function);
	}

	public String andBetweenFunc() {
		if (this.allOfList.size() > 1) {
			StringBuilder and = new StringBuilder();

			for (Function f : this.allOfList)
				and.append(f.getFuncName() + f.paramToString() + " && ");

			return and.substring(0, and.length() - 4);
		}

		return this.allOfList.get(0).getFuncName() + this.allOfList.get(0).paramToString();
	}
}