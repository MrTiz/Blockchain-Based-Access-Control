import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

public class Node {
	private Web3j web3;
	private String rpcServer;
	private String address;
	private String password;
	private String privateKeyFile;
	private String solSourceDir;
	private String solBinaryDir;
	private BigInteger gasLimit;

	public Node(ParserParams parserP) {
		this.rpcServer = parserP.getRpcServer();
		this.address = parserP.getAddress();
		this.password = parserP.getPassword();
		this.privateKeyFile = parserP.getPrivateKeyFile();
		this.gasLimit = parserP.getGasLimit();
		this.solSourceDir = parserP.getSolSourceDir();
		this.solBinaryDir = parserP.getSolBinaryDir();
		this.web3 = Web3j.build(new HttpService(this.rpcServer));
	}

	public Web3j getWeb3j() {
		return this.web3;
	}

	public String getCode(String address) {
		try {
			EthGetCode code = web3.ethGetCode(address, DefaultBlockParameterName.LATEST)
					.sendAsync().get();

			return code.getResult();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String sha3HashString(String s) {
		try {
			String str = "0x" + String.format("%x", new BigInteger(1, s.getBytes()));
			return this.web3.web3Sha3(str).sendAsync().get().getResult();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	public String encodeFunction(String functionName, 
			List<Type> inputParameters, 
			List<TypeReference<?>> outputParameters) 
	{
		return FunctionEncoder.encode(
				new org.web3j.abi.datatypes.Function(
						functionName, 
						inputParameters, 
						outputParameters));
	}

	/*
	 * Compile a solidity contract using the solc compiler
	 * 
	 * @param contract	a string that indicates the name of the solidity contract file
	 * @return 			a string that indicates the compiled solidity contract file
	 * @exception		IOException if <code>p.start</code> fails
	 */
	public String compileContract(String contract) {
		try {
			ProcessBuilder p = new ProcessBuilder(
					"solc",
					"--optimize",
					"--bin",
					"--abi",
					"--overwrite",
					this.solSourceDir + contract,
					"-o",
					this.solBinaryDir);

			Process compile = p.start();
			compile.waitFor();

			return this.solBinaryDir + contract.substring(0, contract.lastIndexOf(".")) + ".bin";
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Get the balance of the account of given address
	 * 
	 * @return			a biginteger that indicates the current balance of the account
	 * @exception		InterruptedException if <code>.sendAsync().get()</code> fails
	 * @exception		ExecutionException if <code>.sendAsync().get()</code> fails
	 */
	public BigInteger getBalance() {
		try {
			EthGetBalance ethGetBalance = this.web3
					.ethGetBalance(this.address, DefaultBlockParameterName.LATEST)
					.sendAsync().get();

			return ethGetBalance.getBalance();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Get the current gas price
	 *
	 * @return			a biginteger that indicates the current gas price
	 * @exception		InterruptedException if <code>.sendAsync().get()</code> fails
	 * @exception		ExecutionException if <code>.sendAsync().get()</code> fails
	 */
	private	 BigInteger getGasPrice() {
		try {
			return this.web3.ethGasPrice().sendAsync().get().getGasPrice();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Get current nonce
	 * 
	 * @return			a BigInteger that indicates the current nonce
	 * @exception		InterruptedException if <code>.sendAsync().get()</code> fails
	 * @exception		ExecutionException if <code>.sendAsync().get()</code> fails
	 */
	private BigInteger getNonce() {
		try {
			EthGetTransactionCount ethGetTransactionCount = this.web3
					.ethGetTransactionCount(this.address, DefaultBlockParameterName.LATEST)
					.sendAsync().get();

			return ethGetTransactionCount.getTransactionCount();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Estimates the gas that would be used if a contract was deployed in the blockchain
	 * 
	 * @param parameter		parameter to be passed to the contract costructor
	 * @param contractPath	a string that indicates the path of the solidity contract file
	 * @return				a biginteger that indicates the estimated gas
	 * @exception			InterruptedException if <code>.sendAsync().get()</code> fails
	 * @exception			ExecutionException if <code>.sendAsync().get()</code> fails
	 */
	@SuppressWarnings("rawtypes")
	public EthEstimateGas estimateGas(
			String contractPath, 
			String to, 
			List<Type> inputParameters, 
			List<TypeReference<?>> outputParameters, 
			String funToCall) 
	{
		try {			
			Transaction transaction;
			
			if (contractPath != null) {
				String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList());

				if (inputParameters != null)
					encodedConstructor = FunctionEncoder.encodeConstructor(inputParameters);
				
				transaction = Transaction.createContractTransaction(
						this.address, 
						this.getNonce(), 
						this.getGasPrice(), 
						this.gasLimit,
						BigInteger.ZERO,
						"0x" + this.readFile(contractPath) + encodedConstructor);
			}
			else {
				String encodedFunction = this.encodeFunction(funToCall, inputParameters, outputParameters);

				transaction = Transaction.createFunctionCallTransaction(
						this.address, 
						this.getNonce(), 
						this.getGasPrice(), 
						this.gasLimit, 
						to, 
						BigInteger.ZERO, 
						encodedFunction);
			}

			return this.web3.ethEstimateGas(transaction).sendAsync().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Deploy contract in Ethereum Blockchain by sending a raw transaction with 
	 * signed data
	 * 
	 * @param contractBin	a string that indicates the compiled solidity contract file
	 * @param parameter		parameter to be passed to the contract costructor
	 * @return				a string that indicates the transaction hash
	 * @exception			InterruptedException if <code>.sendAsync().get()</code> or 
	 * 												<code>Thread.sleep()</code> fails
	 * @exception			ExecutionException if <code>.sendAsync().get()</code> fails
	 * @exception			IOException if <code>WalletUtils.loadCredentials()</code> fails
	 * @exception			CipherException if <code>TransactionEncoder.signMessage()</code> fails
	 */
	public String deployContract(String contractBin, Type<?> parameter) {
		try {
			String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.asList());
			
			Credentials credentials = WalletUtils.loadCredentials(this.password, this.privateKeyFile);

			if (parameter != null)
				encodedConstructor = FunctionEncoder.encodeConstructor(
						Arrays.asList(parameter));

			org.web3j.crypto.RawTransaction rawTransaction = org.web3j.crypto.RawTransaction.createContractTransaction(
					this.getNonce(),
					this.getGasPrice(),
					this.gasLimit,
					BigInteger.ZERO,
					"0x" + this.readFile(contractBin) + encodedConstructor);

			byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
			String hexValue = Numeric.toHexString(signedMessage);

			EthSendTransaction ethSendTransaction = this.web3
					.ethSendRawTransaction(hexValue)
					.sendAsync().get();
			String transactionHash = ethSendTransaction.getTransactionHash();

			while (transactionHash == null || transactionHash.equals("0x0")) {
				Thread.sleep(1000);
				transactionHash = ethSendTransaction.getTransactionHash();
			}

			return transactionHash;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CipherException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Get the receipt of the transaction
	 * 
	 * @param transactionHash	a string that indicates the transaction hash
	 * @return					a TransactionReceipt
	 * @exception				InterruptedException if <code>.sendAsync().get()</code> fails
	 * @exception				ExecutionException if <code>.sendAsync().get()</code> fails
	 */
	public TransactionReceipt getTransactionReceipt(String transactionHash) {
		try {
			EthGetTransactionReceipt receipt = this.web3
					.ethGetTransactionReceipt(transactionHash)
					.sendAsync().get();

			while (receipt.getResult() == null) {
				Thread.sleep(1000);
				receipt = this.web3.ethGetTransactionReceipt(transactionHash)
						.sendAsync().get();
			}

			return receipt.getResult();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	public String sendFunctionTransaction(
			List<Type> inputParameters, 
			List<TypeReference<?>> outputParameters, 
			String funToCall, 
			String pdpAddr) 
	{
		try {
			String encodedFunction = this.encodeFunction(funToCall, inputParameters, outputParameters);
			org.web3j.crypto.RawTransaction rawTransaction = org.web3j.crypto.RawTransaction.createTransaction(
					this.getNonce(), 
					this.getGasPrice(), 
					this.gasLimit, 
					pdpAddr, 
					encodedFunction);

			Credentials credentials = WalletUtils.loadCredentials(
					this.password,
					this.privateKeyFile);

			byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
			String hexValue = Numeric.toHexString(signedMessage);

			EthSendTransaction sendRawTransaction = this.web3.ethSendRawTransaction(hexValue).sendAsync().get();
			String transactionHash = sendRawTransaction.getTransactionHash();

			while (transactionHash == null || transactionHash.equals("0x0")) {
				Thread.sleep(1000);
				transactionHash = sendRawTransaction.getTransactionHash();
			}

			return transactionHash;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CipherException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("rawtypes")
	public String callFunction(
			List<Type> inputParameters, 
			List<TypeReference<?>> outputParameters, 
			String funToCall,
			String pdpAddr)
	{
		try {
			String encodedFunction = this.encodeFunction(funToCall, inputParameters, outputParameters);
			Transaction transaction = Transaction.createEthCallTransaction(
					this.address, 
					pdpAddr, 
					encodedFunction);

			EthCall call = this.web3.ethCall(transaction, DefaultBlockParameterName.LATEST)
					.sendAsync()
					.get();

			return call.getResult().replace("0x", "");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	/*
	 * Read all bytes from a file
	 * 
	 * @param policy	a string indicating the path to the file to be read
	 * @return 			a string containing all readed bytes
	 * @exception		IOException if <code>Files.readAllBytes()</code> fails
	 */
	public String readFile(String policy) {
		try {
			byte[] read = Files.readAllBytes(Paths.get(policy));
			return new String(read, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}