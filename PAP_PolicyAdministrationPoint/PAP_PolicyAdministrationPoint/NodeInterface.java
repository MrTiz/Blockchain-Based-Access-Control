package PAP_PolicyAdministrationPoint;

import java.util.List;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;

public interface NodeInterface {
	public String getCode(String address);
	
	@SuppressWarnings("rawtypes")
	public String encodeFunction(String functionName, 
			List<Type> inputParameters, 
			List<TypeReference<?>> outputParameters);
	
	public String sha3HashString(String s);
}