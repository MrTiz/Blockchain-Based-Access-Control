package PAP_PolicyAdministrationPoint;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Statistics {
	private int nRules;
	private JSONArray tests;
	
	public Statistics(int nRules) {
		this.nRules = nRules;
		this.tests = new JSONArray();
	}

	public int getnRules() {
		return this.nRules;
	}
	
	@SuppressWarnings("unchecked")
	public void addTest(Test pdp, Test pip) {
		JSONObject test = new JSONObject();

		test.put("Policy Decision Point (PDP)", pdp.toJsonObject());
		test.put("Policy Information Point (PIP)", pip.toJsonObject());
		
		this.tests.add(test);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJsonObject() {
		JSONObject stats = new JSONObject();
		stats.put("Number of rules", this.nRules);
		stats.put("Tests", this.tests);

		return stats;
	}
	
	public static Statistics fromJSON(JSONObject object) {
		int nRules = Integer.parseInt(object.get("Number of rules").toString());
		JSONArray tests = (JSONArray)object.get("Tests");

		Statistics stat = new Statistics(nRules);
		
		for (Object o : tests)
			if (o instanceof JSONObject) {
				Test pdp = Test.fromJSON((JSONObject)((JSONObject)o).get("Policy Decision Point (PDP)"));
				Test pip = Test.fromJSON((JSONObject)((JSONObject)o).get("Policy Information Point (PIP)"));

				stat.addTest(pdp, pip);
			}

		return stat;
	}
}