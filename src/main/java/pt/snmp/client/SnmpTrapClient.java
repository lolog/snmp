package pt.snmp.client;

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
public class SnmpTrapClient {  
    private Snmp snmp;
    private Address targetAddress;
    
    // initial
    public void init() throws IOException {
        targetAddress = GenericAddress.parse("udp:127.0.0.1/10101");
		TransportMapping<?> transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        snmp.listen();
    }
    
    public void sendPDU() throws IOException {
        CommunityTarget target = new CommunityTarget();
        target.setAddress(targetAddress);
  
        target.setRetries(2);
        target.setTimeout(5 * 1000L);
        target.setVersion(SnmpConstants.version2c);
  
        PDU pdu = new PDU();
        pdu.setType(PDU.TRAP);
        pdu.add(new VariableBinding(new OID(".1.3.6.1.2.3377.10.1.1.1.1"), new OctetString("SnmpTrap")));
        pdu.add(new VariableBinding(new OID(".1.3.6.1.2.3377.10.1.1.1.2"), new OctetString("JavaEE")));
  
        // 向Agent发送PDU，并接收Response
        ResponseEvent respEvnt = snmp.send(pdu, target);
        
        // 解析Response
        if (respEvnt != null && respEvnt.getResponse() != null) {
        	PDU response = respEvnt.getRequest();
            for (int i = 0; i < response.size(); i++) {
                VariableBinding vb = response.get(i);
                System.out.println(vb.getOid() + " : " + vb.getVariable());
            }
        }
    }
  
    public static void main(String[] args) {
        try {
            SnmpTrapClient util = new SnmpTrapClient();
            util.init();
            util.sendPDU();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}  