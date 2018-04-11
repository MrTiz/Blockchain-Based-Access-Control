package PAP_PolicyAdministrationPoint;

import java.math.BigInteger;

import org.json.simple.JSONObject;

public class Test {
	private BigInteger usedGas;
	private BigInteger estimatedGas;
	private long duration;
	
	public Test(BigInteger ug, BigInteger eg, long d) {
		this.usedGas = ug;
		this.estimatedGas = eg;
		this.duration = d;
	}

	public BigInteger getUsedGas() {
		return this.usedGas;
	}

	public BigInteger getEstimatedGas() {
		return this.estimatedGas;
	}

	public long getDuration() {
		return this.duration;
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject toJsonObject() {
		JSONObject stats = new JSONObject();
		
		stats.put("Used gas", this.usedGas.toString());
		stats.put("Estimated gas", this.estimatedGas.toString());
		stats.put("Duration", this.duration);

		return stats;
	}
	
	public static Test fromJSON(JSONObject object) {
		BigInteger ug = new BigInteger(object.get("Used gas").toString());
		BigInteger eg = new BigInteger(object.get("Estimated gas").toString());
		long d = Long.parseLong(object.get("Duration").toString());

		return new Test(ug, eg, d);
	}
}