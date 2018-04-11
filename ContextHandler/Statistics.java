import java.math.BigInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Statistics {
	private int nRules;
	private JSONArray tests;
	private long average;

	public Statistics(int nRules) {
		this.nRules = nRules;
		this.tests = new JSONArray();
		this.average = 0;
	}

	public int getNRules() {
		return this.nRules;
	}

	@SuppressWarnings("unchecked")
	public void addTest(long duration, BigInteger gasUsed, BigInteger gasEstimated) {
		JSONObject test = new JSONObject();
		
		test.put("Test duration", duration);
		test.put("Used gas", gasUsed);
		test.put("Estimated gas", gasEstimated);

		this.tests.add(test);
		this.setAverage();
	}

	private void setAverage() {
		long sum = 0;

		for (Object o : this.tests)
			if (o instanceof JSONObject)
				sum += (long)((JSONObject)o).get("Test duration");

		this.average = sum / this.tests.size();
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJsonObject() {
		JSONObject stats = new JSONObject();
		
		stats.put("Number of rules", this.nRules);
		stats.put("Statistics", this.tests);
		stats.put("Average", this.average);

		return stats;
	}

	public static Statistics fromJSON(JSONObject object) {
		int nRules = Integer.parseInt(object.get("Number of rules").toString());
		JSONArray tests = (JSONArray)object.get("Statistics");

		Statistics stat = new Statistics(nRules);
		
		for (Object o : tests)
			if (o instanceof JSONObject) {
				long d = Long.parseLong(((JSONObject)o).get("Test duration").toString());
				BigInteger u = new BigInteger(((JSONObject)o).get("Used gas").toString());
				BigInteger e = new BigInteger(((JSONObject)o).get("Estimated gas").toString());

				stat.addTest(d, u, e);
			}

		return stat;
	}
}