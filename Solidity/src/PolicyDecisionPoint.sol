pragma solidity ^0.4.9;

contract PIPInterface { function env1date() public pure returns(bytes32) {} 
                        function env2time() public pure returns(bytes32) {} 
                        function env3hot() public pure returns(bool) {} 
                        function env4dateTime() public pure returns(bytes32) {} 
                        function res1resourceId() public pure returns(bytes32) {} 
                        function res2status() public pure returns(bool) {} 
                        function res3maintenance() public pure returns(bool) {} }

contract PolicyDecisionPoint {
    struct User {
        bytes32 userName;
        bool authorized;
    }

    struct Session {
        uint id;
        User users;
    }

    address private admin;
    PIPInterface private PIPcontr;
    uint private id;
    Session[] private register;

    function PolicyDecisionPoint(address pipAddr) public {
        admin = msg.sender;
        PIPcontr = PIPInterface(pipAddr);
        id = 0;
    }

    function getSessionById(uint _id) public view returns(bytes32, bool) {
        if (_id >= register.length) {
            revert();
        }

        return (register[_id].users.userName,
                register[_id].users.authorized);
    }

    function getPermission(bytes32 subject) public returns(uint) {
        if (msg.sender != admin) {
            revert();
        }

        bool outcome = globalRule(subject);

        var u = User(subject, outcome);
        var s = Session(id, u);

        id++;
        register.push(s);
        return (id - 1);
    }

    function rule1() private view returns(bool) {
        return bytes32("26/06/2017") == bytes32(PIPcontr.env1date());
    }

    function rule2() private view returns(bool) {
        return bytes32("17:39") == bytes32(PIPcontr.env2time());
    }

    function rule3() private view returns(bool) {
        return (true == PIPcontr.env3hot());
    }

    function rule4() private view returns(bool) {
        return bytes32("17:39-26/06/2017") == bytes32(PIPcontr.env4dateTime());
    }

    function rule5() private view returns(bool) {
        return bytes32("res.org") >= bytes32(PIPcontr.res1resourceId());
    }

    function rule6() private view returns(bool) {
        return (true == PIPcontr.res2status());
    }

    function rule7() private view returns(bool) {
        return (false == PIPcontr.res3maintenance());
    }

    function globalRule(bytes32 subject) private view returns(bool) {
        return ((rule1() && rule2()) || (rule3())) && ((rule4() && rule5()) || (rule6()) || (rule7()));
    }
}