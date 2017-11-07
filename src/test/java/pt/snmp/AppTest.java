package pt.snmp;

import org.junit.Test;

import pt.snmp.client.SnmpGetClient;

public class AppTest {
	SnmpGetClient snmpData = new SnmpGetClient();
	String ip = "127.0.0.1";
	String community = "public";
	
	@Test
	public void snmpGet() {
		String oid1 = "1.3.6.1.2.1.1.6.0";
		String oid2 = "1.3.6.1.2.1.1.7.0";
		String oid3 = "1.3.6.1.2.1";
		snmpData.snmpGet(ip, community, oid1, oid2, oid3);
	}
	
	@Test
	public void asyncSnmpGet() {
		String oid1 = "1.3.6.1.2.1.1.6.0";
		String oid2 = "1.3.6.1.2.1.1.7.0";
		String oid3 = "1.3.6.1.2.12";
		snmpData.asyncSnmpGet(ip, community, oid1, oid2, oid3);
	}
}