package PAP_PolicyAdministrationPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ParserParams {
	private String rpcServer;
	private String address;
	private String password;
	private String privateKeyFile;
	private String solSourceDir;
	private String solBinaryDir;
	private String statsFile;
	private String policyDir;
	private BigInteger gasLimit;
	private ArrayList<String> policies;
	
	public ParserParams(String file) {
		this.policies = new ArrayList<String>();
		this.parseParams(file);
	}

	public String getRpcServer() {
		return this.rpcServer;
	}
	
	public String getSolSourceDir() {
		return this.solSourceDir;
	}

	public String getSolBinaryDir() {
		return this.solBinaryDir;
	}

	public ArrayList<String> getPolicies() {
		return this.policies;
	}
	
	public String getAddress() {
		return this.address;
	}

	public String getPolicyDir() {
		return this.policyDir;
	}

	public String getStatsFile() {
		return this.statsFile;
	}
	
	public BigInteger getGasLimit() {
		return this.gasLimit;
	}

	public String getPassword() {
		return this.password;
	}
	
	public String getPrivateKeyFile() {
		return this.privateKeyFile;
	}

	private void parseParams(String file) {
		try {
			String aux[], par, val;
			List<String> params = Files.readAllLines(Paths.get(file));

			for (String x : params) {
				aux = x.split("=");
				par = aux[0].trim();
				val = aux[1].trim();

				if (par.equals("RpcServer"))
					this.rpcServer = val;
				else if (par.equals("Address"))
					this.address = val;
				else if (par.equals("Password"))
					this.password = val;
				else if (par.equals("PrivateKeyDir"))
					this.privateKeyFile = val;
				else if (par.equals("SoliditySourceDir"))
					this.solSourceDir = val + File.separator;
				else if (par.equals("SolidityBinaryDir"))
					this.solBinaryDir = val + File.separator;
				else if (par.equals("Policy"))
					this.policies.add(val);
				else if (par.equals("PolicyDir"))
					this.policyDir = val + File.separator;
				else if (par.equals("StatsFile"))
					this.statsFile = val;
				else if (par.equals("GasLimit"))
					this.gasLimit = new BigInteger(Integer.toHexString(Integer.parseInt(val)), 16);
			}

			if (this.privateKeyFile != null)
				this.privateKeyFile = this.getPrivateKeyFile(this.privateKeyFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String getPrivateKeyFile(String dir) {
		JSONParser parser = new JSONParser();
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {

				try(FileReader privFile = new FileReader(file);) {
					JSONObject jo = (JSONObject) parser.parse(privFile);

					if (jo.get("address").equals(this.address) || 
							("0x" + jo.get("address")).equals(this.address))
						return file.getAbsolutePath();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		return null;
	}
}