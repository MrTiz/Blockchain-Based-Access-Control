package PAP_PolicyAdministrationPoint;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import PAP_PolicyAdministrationPoint.XacmlToSolidity.ParserXacmlPolicy;

public class PAP_PolicyAdministrationPoint {
	private static int nRules = 0;
	private static ArrayList<String> policies = null;

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		ArrayList<Statistics> statistics = new ArrayList<Statistics>();

		ParserParams parserP = new ParserParams(args[0]);
		Node node = new Node(parserP); 

		String rpcServer = parserP.getRpcServer();
		String solPath = parserP.getSolSourceDir();
		String policyDir = parserP.getPolicyDir();
		String statsFile = parserP.getStatsFile();
		policies = parserP.getPolicies();

		readStatistics(statistics, statsFile);

		if (policies == null) 
			policies = new ArrayList<String>();

		String pipContract = "PolicyInformationPoint";
		String pdpContract = "PolicyDecisionPoint";

		String HOST = "localhost";
		int PORT = 55555;

		String n = System.lineSeparator();

		try (Scanner scanIn = new Scanner(System.in);) {
			System.out.println(n + "If you want to write new policy write 'ok' and press 'Enter'"
					+ n + "otherwise press only 'Enter'.");
			String read = scanIn.nextLine();

			if (read.equals("ok"))
				makePolicy(policyDir, scanIn, rpcServer, node);

			for (String policy : policies) {
				System.out.println(n + "Parsing policy " + policy + " ...");
				Document doc = parseXACML(node.readFile(policy));

				System.out.println("Making solidity contracts...");
				makeSolidityContracts(doc, solPath, node);

				System.out.println("Compiling contracts...");
				String compPip = node.compileContract(pipContract + ".sol");
				String compPdp = node.compileContract(pdpContract + ".sol");

				System.out.println("Checking balance...");
				BigInteger balance = node.getBalance();
				System.out.println("Current balance = " + balance.toString());

				EthEstimateGas pipEstim = node.estimateGas(compPip, null, null, null, null);

				if (pipEstim.hasError()) {
					System.out.println("PIP Contract: " + pipEstim.getError().getMessage());
					System.exit(-1);
				}

				BigInteger pipGasEstim = new BigInteger(pipEstim.getResult().replace("0x", ""), 16);
				System.out.println(n + "PIP Contract: estimated gas = " + pipGasEstim);

				if (balance.compareTo(BigInteger.ZERO) < 1 ||
						balance.compareTo(pipGasEstim) < 1 ) 
				{
					System.out.println("Insufficient balance");
					System.exit(-1);
				}

				System.out.println("PIP Contract: deploying...");
				String pipHash = node.deployContract(compPip, null);

				System.out.println("PIP Contract: mining in progress...");
				long startTime = System.currentTimeMillis();
				TransactionReceipt pip = node.getTransactionReceipt(pipHash);
				long endTime = System.currentTimeMillis();

				System.out.println(n + "PIP Contract: mined!" + n
						+ "    Address: " + pip.getContractAddress() + n
						+ "    Gas used: " + pip.getGasUsed().toString() + n
						+ "    Mining duration: " + (endTime - startTime) + " milliseconds" + n
						+ "    Transaction block number: " + pip.getBlockNumber() + n);

				Test pipTest = new Test(pip.getGasUsed(), pipGasEstim, endTime - startTime);
				
				List<Type> inputParameters = Arrays.asList(new Address(pip.getContractAddress()));
				EthEstimateGas pdpEstim = node.estimateGas(compPdp, null, inputParameters, null, null);

				if (pdpEstim.hasError()) {
					System.out.println("PDP Contract: " + pdpEstim.getError().getMessage());
					System.exit(-1);
				}

				BigInteger pdpGasEstim = new BigInteger(pdpEstim.getResult().replace("0x", ""), 16);
				System.out.println("PDP Contract: estimated gas = " + pdpGasEstim);
				
				if (balance.compareTo(BigInteger.ZERO) < 1 ||
						balance.compareTo(pdpGasEstim) < 1 ) 
				{
					System.out.println("Insufficient balance");
					System.exit(-1);
				}

				System.out.println("PDP Contract: deploying...");
				String pdpHash = node.deployContract(compPdp, new Address(pip.getContractAddress()));

				System.out.println("PDP Contract: mining in progress...");
				startTime = System.currentTimeMillis();
				TransactionReceipt pdp = node.getTransactionReceipt(pdpHash);
				endTime = System.currentTimeMillis();

				System.out.println(n + "PDP Contract: mined!" + n
						+ "    Address: " + pdp.getContractAddress() + n
						+ "    Gas used: " + pdp.getGasUsed().toString() + n
						+ "    Mining duration: " + (endTime - startTime) + " milliseconds" + n
						+ "    Transaction block number: " + pdp.getBlockNumber() + n);

				Test pdpTest = new Test(pdp.getGasUsed(), pdpGasEstim, endTime - startTime);

				System.out.println("Set PDP as owner...");
				String setOwner = callPIP(node, pip.getContractAddress(), pdp.getContractAddress());
				System.out.println("Complete, gas used for setting owner: " + setOwner);

				addTest(statistics, pdpTest, pipTest);
				printJsonArray(statistics, statsFile);

				informContextHandler(HOST, PORT, pdp.getContractAddress(), nRules);
			}
		}
	}

	public static void setNRules(int n) {
		nRules = n;
	}

	public static void addPolicy(String p) {
		policies.add(0, p);
	}

	private static void addTest(ArrayList<Statistics> statistics, Test pdp, Test pip) {
		Statistics stat = getStats(nRules, statistics);

		if (stat == null) {
			Statistics stats = new Statistics(nRules);
			stats.addTest(pdp, pip);
			statistics.add(stats);
		}
		else {
			stat.addTest(pdp, pip);
		}
	}

	@SuppressWarnings("unchecked")
	private static JSONArray makeJsonArray(ArrayList<Statistics> stats) {
		JSONArray jAr = new JSONArray();
		stats.sort((o1, o2) -> (int)o1.getnRules() - (int)o2.getnRules());

		for (Statistics s : stats)
			jAr.add(s.toJsonObject());

		return jAr;
	}

	private static void printJsonArray(ArrayList<Statistics> stats, String file) {
		try(FileWriter stat = new FileWriter(file);) {
			JSONArray ja = makeJsonArray(stats);//.writeJSONString(stat);
			
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(ja.toJSONString());
			
			String prettyJsonString = gson.toJson(je);
			stat.write(prettyJsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readStatistics(ArrayList<Statistics> statistics, String file) {
		Path path = Paths.get(file);
		if (!Files.isRegularFile(path)) return;

		JSONParser parser = new JSONParser();
		try (FileReader f = new FileReader(file);) {
			JSONArray stats = (JSONArray) parser.parse(f);

			for (Object s : stats)
				statistics.add(Statistics.fromJSON((JSONObject) s));

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private static Statistics getStats(int nRules, ArrayList<Statistics> stats) {
		for (Statistics s : stats)
			if (s.getnRules() == nRules)
				return s;

		return null;
	}

	@SuppressWarnings("rawtypes")
	private static String callPIP(Node node, String pipAddr, String pdpAddr) {
		List<Type> inputParameters = Arrays.asList(new Address(pdpAddr));
		List<TypeReference<?>> outputParameters = Arrays.asList();

		String funToCall = "setPDP";

		EthEstimateGas gasEstim = node.estimateGas(null, pipAddr, inputParameters, outputParameters, funToCall);

		if (gasEstim.hasError()) {
			System.out.println(gasEstim.getError().getMessage());
			return gasEstim.getError().getMessage();
		}

		BigInteger gasEstimated = new BigInteger(gasEstim.getResult().replace("0x", ""), 16);
		System.out.println("Estimated gas = " + gasEstimated);

		String transactionHash = node.sendFunctionTransaction(inputParameters, outputParameters, funToCall, pipAddr);
		return node.getTransactionReceipt(transactionHash).getGasUsed().toString();
	}

	/*
	 * Sends the address of the contract deployed into the ethereum 
	 * blockchain to the context handler
	 * 
	 * @param host		a string that identifies the context handler's hostname
	 * @param port		an integer indicating the remote port to connect to
	 * @param address	the address of the contract deployed into the ethereum 
	 * 					blockchain
	 * @exception		IOException if <code>SocketChannel.open</code> or
	 * 									<code>SocketChannel.write</code> fail
	 */
	private static void informContextHandler(String host, int port, String address, int nRules) {
		InetSocketAddress socketAddress = new InetSocketAddress(host, port);
		String mex = "PAP" + System.lineSeparator() + address + System.lineSeparator() + nRules;

		try {
			SocketChannel server = SocketChannel.open(socketAddress);

			ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
			ByteBuffer message = ByteBuffer.allocate(mex.length());

			length.putInt(mex.length());
			message.put(mex.getBytes());

			length.flip();
			message.flip();

			server.write(length);
			server.write(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Creates a solidity contract starting from a xacml policy
	 * 
	 * @param doc		a document which represents the root of the document tree
	 * @param solPath	a string that indicates the solidity contract directory
	 * @exception		InterruptedException if <code>.join()</code> fails
	 */
	private static void makeSolidityContracts(Document doc, String solPath, NodeInterface node) {
		try {
			ParserXacmlPolicy pxp = new ParserXacmlPolicy(doc, solPath, node);
			Thread parse = new Thread(pxp);
			parse.start();
			parse.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Generate a xacml policy using an interactive shell
	 * 
	 * @param policy	a string that indicates the file in which to save the generated policy
	 * @param scanIn	a scanner to read keyboard input
	 * @exception		InterruptedException if <code>.join()</code> fails
	 */
	private static void makePolicy(String policyDir, Scanner scanIn, String rpcServer, Node node) {
		try {
			XacmlPolicyGenerator xpg = new XacmlPolicyGenerator(policyDir, scanIn, rpcServer, node);
			Thread XaPoGe = new Thread(xpg);
			XaPoGe.start();
			XaPoGe.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Parse a XACML file as an XML document and return a new DOM document object
	 * 
	 * @param text	a string containing the text to be parsed
	 * @return		a document containing new DOM document object
	 * @exception	SAXException if any parse errors occur
	 * @exception	IOException if any IO errors occur
	 * @exception	ParserConfigurationException if a DocumentBuilder cannot be 
	 * 												created which satisfies the 
	 * 												configuration requested
	 */
	private static Document parseXACML(String text) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document doc = dBuilder.parse(new InputSource(new StringReader(text)));
			doc.getDocumentElement().normalize();

			return doc;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return null;
	}
}