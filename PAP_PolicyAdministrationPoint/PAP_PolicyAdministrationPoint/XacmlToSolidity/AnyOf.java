package PAP_PolicyAdministrationPoint.XacmlToSolidity;

import java.util.ArrayList;

public class AnyOf {
	private ArrayList<AllOf> anyOfList;

	public AnyOf() {
		this.anyOfList = new ArrayList<AllOf>();
	}

	public ArrayList<AllOf> getAnyOfList() {
		return this.anyOfList;
	}

	public void addAllOf(AllOf allof) {
		this.anyOfList.add(allof);
	}

	public String orBetweenFunc() {
		if (this.anyOfList.size() > 1) {
			StringBuilder or = new StringBuilder("(");

			for (AllOf a : this.anyOfList)
				or.append(a.andBetweenFunc() + ") || (");

			return or.replace(or.length() - 6, or.length(), ")").toString();
		}

		return this.anyOfList.get(0).andBetweenFunc();
	}
}