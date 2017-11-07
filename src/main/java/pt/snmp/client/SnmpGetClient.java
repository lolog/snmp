package pt.snmp.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import pt.snmp.listener.SnmpResponseGetListener;

public class SnmpGetClient {
	public static final int DEFAULT_VERSION = SnmpConstants.version2c;
	public static final String DEFAULT_PROTOCOL = "udp";
	public static final int DEFAULT_PORT = 161;
	public static final long DEFAULT_TIMEOUT = 3 * 1000L;
	public static final int DEFAULT_RETRY = 3;
	
	public CommunityTarget createDefault (String ip, String community) {
		Address address = GenericAddress.parse(DEFAULT_PROTOCOL + ":" + ip + "/" + DEFAULT_PORT);
		
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(community));
		target.setAddress(address);
		target.setVersion(DEFAULT_VERSION);
		target.setTimeout(DEFAULT_TIMEOUT);
		target.setRetries(DEFAULT_RETRY);
		
		return target;
	}
	
	public void snmpGet(String ip, String community, String... oid) {
		CommunityTarget target = createDefault(ip, community);
		
		Snmp snmp = null;
		
		try {
			PDU pdu = new PDU();
			for (int index=0; index < oid.length; index++) {
				VariableBinding vb = new VariableBinding(new OID(oid[index]), new OctetString("Lolog"));
				pdu.add(vb);
			}
			
			DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			snmp.listen();
			
			System.out.println(" ----------> Send PDU <---------- ");
			pdu.setType(PDU.GET);
			ResponseEvent responseEvent = snmp.send(pdu, target);
			
			System.out.println("--> peerAddress = [" + responseEvent.getPeerAddress() + "]");
			
			PDU response = responseEvent.getResponse();
			if (response == null) {
				System.out.println("[Error]: response is null...");
			}
			else if (response.getErrorStatus() != 0) {
				System.out.println("[Error]: {status=" + response.getErrorStatus() + ", text='"+response.getErrorStatusText()+"'}");
			}
			else {
				System.out.println("--> response pdu size is = [" + response.size() + "]");
				for (int index = 0; index < response.size(); index++) {
					VariableBinding vb = response.get(index);
					System.out.println(vb.getOid() + "=" + vb.getVariable().getSyntaxString());
				}
			}
			System.out.println(" -------> Snmp Get OID finished <------- ");
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void asyncSnmpGet(String ip, String community, String... oid) {
		CommunityTarget target = createDefault(ip, community);
		
		CountDownLatch latch = new CountDownLatch(1);
		Snmp snmp = null;
		
		try {
			PDU pdu = new PDU();
			for (int index=0; index < oid.length; index++) {
				VariableBinding vb = new VariableBinding(new OID(oid[index]));
				pdu.add(vb);
			}
			
			DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			snmp.listen();
			
			pdu.setType(PDU.GET);
			snmp.send(pdu, target, null, new SnmpResponseGetListener(latch));
	
			// wait
			latch.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void snmpTree (String ip, String community, String oid) {
		CommunityTarget target = createDefault(ip, community);
		
		Snmp snmp = null;
		
		try {
			TransportMapping<?> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();
			
			PDU pdu = new PDU();
			OID Oid = new OID(oid);
			pdu.add(new VariableBinding(Oid));
			
			boolean finished = false;
			
			while(!finished) {
				ResponseEvent event = snmp.getNext(pdu, target);
				PDU response = event.getResponse();
				
				if (response == null) {
					System.out.println("-----> finished...");
					finished = true;
					break;
				}
				
				VariableBinding vb = response.get(0);
				
				// checked finished
				finished = checkedTreeFinished(Oid, pdu, vb);
				
				if(!finished) {
					System.out.println("=== each:");
					System.out.println(vb.getOid() + " = " + vb.getVariable());
					
					// Set up the variable binding for the next entry.  
					pdu.setRequestID(new Integer32(0));  
					pdu.set(0, vb);
				}
				else {
					System.out.println("SNMP Tree OID has finished.");
					snmp.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				snmp.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean checkedTreeFinished(OID oid, PDU pdu, VariableBinding vb) {
		if (pdu.getErrorStatus() != 0) {
			System.out.println("[Error] {error=true, status="+pdu.getErrorStatus()+", text='"+pdu.getErrorStatusText()+"'}");
			return true;
		}
		if (vb.getOid() == null) {
			System.out.println("[Error] {error=true, oid=null}");
			return true;
		}
		if(vb.getOid().size() < oid.size()) {
			System.out.println("[Error] {error=true, oid='Oid length not equal'}");
			return true;
		}
		if(oid.leftMostCompare(oid.size(), vb.getOid()) != 0) {
			System.out.println("[Error] {error=true, oid='leftMostCompare not 0'}");
			return true;
		}
		if(Null.isExceptionSyntax(vb.getVariable().getSyntax())) {
			System.out.println("[Error] {error=true, oid='Null is Exception'}");
			return true;
		}
		
		if(vb.getOid().compareTo(oid) <= 0) {
			System.out.println("[Error] {error=true, oid='Receive is Error'}");
			return true;
		}
		
		return false;
	}
}
