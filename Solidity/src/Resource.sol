pragma solidity ^0.4.9;

contract Resource {
    function Resource() public {}
    
    function resourceId() public returns(bytes32) {
        return bytes32("res.org");
    }
    
    function status() public returns(bool) {
        return true;
    }
    
    function maintenance() public returns(bool) {
        return false;
    }
}
