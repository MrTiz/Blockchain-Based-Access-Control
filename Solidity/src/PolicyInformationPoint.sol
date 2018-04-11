pragma solidity ^0.4.9;

contract EnvironmentContract1 { function date() public pure returns(bytes32) {} 
                                function time() public pure returns(bytes32) {} 
                                function hot() public pure returns(bool) {} 
                                function dateTime() public pure returns(bytes32) {} }
contract ResourceContract1 { function resourceId() public pure returns(bytes32) {} 
                             function status() public pure returns(bool) {} 
                             function maintenance() public pure returns(bool) {} }

contract PolicyInformationPoint {
    address private admin;
    address private pdp;
    EnvironmentContract1 private environmentContr1;
    ResourceContract1 private resourceContr1;

    function PolicyInformationPoint() public {
        admin = msg.sender;
        pdp = 0x0;
        environmentContr1 = EnvironmentContract1(0xff60e19495aa6a93f6e0ca3c02e19666899ce141);
        resourceContr1 = ResourceContract1(0x55ac28eb8040399e2e971b6be679b1318e9956e1);
    }

    function setPDP(address pdpAddr) public {
        if (msg.sender != admin) {
            revert();
        }

        pdp = pdpAddr;
    }

    function checkSender(address sender) private view {
        if (sender != pdp) {
            revert();
        }
    }

    function env1date() public view returns(bytes32) {
        checkSender(msg.sender);
        return environmentContr1.date();
    }

    function env2time() public view returns(bytes32) {
        checkSender(msg.sender);
        return environmentContr1.time();
    }

    function env3hot() public view returns(bool) {
        checkSender(msg.sender);
        return environmentContr1.hot();
    }

    function env4dateTime() public view returns(bytes32) {
        checkSender(msg.sender);
        return environmentContr1.dateTime();
    }

    function res1resourceId() public view returns(bytes32) {
        checkSender(msg.sender);
        return resourceContr1.resourceId();
    }

    function res2status() public view returns(bool) {
        checkSender(msg.sender);
        return resourceContr1.status();
    }

    function res3maintenance() public view returns(bool) {
        checkSender(msg.sender);
        return resourceContr1.maintenance();
    }
}