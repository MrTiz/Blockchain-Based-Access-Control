import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Worker implements Runnable {
	private LinkedBlockingQueue<SelectionKey> bfs;
	private ArrayList<pdpInfo> pdpList;
	private ArrayList<Statistics> statistics;
	private ArrayList<Boolean> outcomePDP;
	private Selector selector;
	private String statsFile;
	private Node node;

	public Worker(LinkedBlockingQueue<SelectionKey> bfs, 
			Selector selector,
			Node node, 
			String statsFile)
	{
		this.bfs = bfs;
		this.selector = selector;
		this.pdpList = new ArrayList<pdpInfo>();
		this.outcomePDP = new ArrayList<Boolean>();
		this.statistics = new ArrayList<Statistics>();
		this.statsFile = statsFile;
		this.node = node;
	}

	@Override
	public void run() {
		String n = System.lineSeparator();
		this.readStatistics(statistics);

		try {
			SelectionKey key;
			Attachment att;

			while (true) {
				key = this.bfs.take();
				att = (Attachment) key.attachment();

				String[] read = new String(att.getBuffer().array()).split(n);

				if (read[0].equals("PAP")) {
					int nRules = Integer.parseInt(read[2]);
					System.out.println(n + "Received new PDP Contract address with " + nRules + " rules from PAP: " + read[1]);
					
					this.pdpList.add(new pdpInfo(read[1], nRules));
					
					if (this.getStats(nRules) == null) {
						this.statistics.add(new Statistics(nRules));
					}
					
					att.getChannel().close();
				}
				else if (read[0].equals("PEP")) {
					System.out.println(n + "User '" + read[1] + "' wants to login...");
					System.out.println("Check if he can access...");

					for (pdpInfo p : this.pdpList) {
						System.out.println(n + "Evaluation policy " + (this.pdpList.indexOf(p) + 1) + " ...");
						boolean outcome = this.callPDP(p.getAddress(), read[1], p.getNRules());
						
						System.out.println("Outcome = " + outcome);
						this.outcomePDP.add(outcome);
					}

					boolean outcome = this.combiningAlgorithm();

					if (!outcome)
						System.out.println(n + "User '" + read[1] + "' can not login!");
					else
						System.out.println(n + "User '" + read[1] + "' can access!");
					
					System.out.println("__________________________________");

					String mex = "CTXHND" + n + read[1] + n + Boolean.toString(outcome);

					att.getLength().clear();
					att.setMessage(mex);

					key.channel().keyFor(this.selector).interestOps(SelectionKey.OP_WRITE);					
					this.selector.wakeup();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addTest(int nRules, long duration, BigInteger gasUsed, BigInteger gasEstim) {
		Statistics stat = this.getStats(nRules);
		stat.addTest(duration, gasUsed, gasEstim);
	}
	
	private Statistics getStats(int nRules) {
		for (Statistics s : this.statistics)
			if (s.getNRules() == nRules)
				return s;
		
		return null;
	}
	
	private void readStatistics(ArrayList<Statistics> statistics) {
		Path path = Paths.get(this.statsFile);
		if (!Files.isRegularFile(path)) return;
		
		JSONParser parser = new JSONParser();
		try (FileReader f = new FileReader(this.statsFile);) {
			JSONArray stats = (JSONArray) parser.parse(f);
			
			for (Object s : stats)
				statistics.add(Statistics.fromJSON((JSONObject) s));
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private boolean combiningAlgorithm() {
		for (boolean b : this.outcomePDP)
			if (!b) return false;

		return true;
	}
	
	@SuppressWarnings("unchecked")
	private JSONArray makeJsonArray(ArrayList<Statistics> stats) {
		JSONArray jAr = new JSONArray();
		stats.sort((o1, o2) -> (int)o1.getNRules() - (int)o2.getNRules());
		
		for (Statistics s : stats)
			jAr.add(s.toJsonObject());
		
		return jAr;
	}
	
	private void printJsonArray(ArrayList<Statistics> stats) {
		try(FileWriter stat = new FileWriter(this.statsFile);) {
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

	@SuppressWarnings("rawtypes")
	private boolean callPDP(String address, String subject, int nRules) {
		int l = subject.length();

		if (l > 32)
			subject = subject.substring(0, 32);
		else {
			String filling = String.format("%1$-" + (32 - l) + "s", "");
			subject = subject.concat(filling.replace(' ', '\0'));
		}

		List<Type> inputParameters = Arrays.asList(new Bytes32(subject.getBytes()));
		List<TypeReference<?>> outputParameters =  Arrays.asList(new TypeReference<Uint>() {});
		String funToCall = "getPermission";
		
		EthEstimateGas gasEstim = this.node.estimateGas(null, address, inputParameters, outputParameters, funToCall);
		
		if (gasEstim.hasError()) {
			System.out.println("Error with contract " + address + " with " + nRules + " rules: " + gasEstim.getError().getMessage());
			return false;
		}
		
		BigInteger gasEstimated = new BigInteger(gasEstim.getResult().replace("0x", ""), 16);
		System.out.println("Estimated gas = " + gasEstimated);

		long startTime = System.currentTimeMillis();
		String transactionHash = this.node.sendFunctionTransaction(inputParameters, outputParameters, funToCall, address);
		TransactionReceipt receipt = this.node.getTransactionReceipt(transactionHash);
		long endTime = System.currentTimeMillis();

		System.out.println("Used gas = " + receipt.getGasUsed());

		this.addTest(nRules, endTime - startTime, receipt.getGasUsed(), gasEstimated);
		this.printJsonArray(this.statistics);
		
		String call = this.node.callFunction(inputParameters, outputParameters, funToCall, address);
		BigInteger id = new BigInteger(call, 16);

		inputParameters = Arrays.asList(
				new Uint256(id.subtract(BigInteger.ONE)));

		outputParameters =  Arrays.asList(
				new TypeReference<Bytes32>() {},
				new TypeReference<Bool>() {});

		funToCall = "getSessionById";

		call = this.node.callFunction(inputParameters, outputParameters, funToCall, address);

		if (call.charAt(call.length() - 1) == '1')
			return true;

		return false;
	}
}