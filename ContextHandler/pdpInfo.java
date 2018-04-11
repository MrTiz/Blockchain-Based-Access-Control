public class pdpInfo {
	private String address;
	private int nRules;
	
	public pdpInfo(String address, int nRules) {
		this.address = address;
		this.nRules = nRules;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public int getNRules() {
		return this.nRules;
	}
}