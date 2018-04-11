pragma solidity ^0.4.9;

contract Subject {
    function Subject() public {}
    
    function subjectId(bytes32 subject) public returns(bytes32) {
        return bytes32("prova.org");
    }
    
    function reputation(bytes32 subject) public returns(int) {
        return 10;
    }
    
    function role(bytes32 subject) public returns(bytes32) {
        return bytes32("administrator");
    }
}
