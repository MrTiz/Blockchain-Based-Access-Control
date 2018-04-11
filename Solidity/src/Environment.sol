pragma solidity ^0.4.9;

contract Environment {
    function Environment() public {}
    
    function date() public returns(bytes32) {
        return bytes32("26/06/2017");
    }
    
    function time() public returns(bytes32) {
        return bytes32("17:39");
    }
    
    function hot() public returns(bool) {
        return true;
    }
    
    function dateTime() public returns(bytes32) {
        return bytes32("17:39-26/06/2017");
    }
}
