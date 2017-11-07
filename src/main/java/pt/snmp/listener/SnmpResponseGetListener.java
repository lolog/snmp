package pt.snmp.listener;

import java.util.concurrent.CountDownLatch;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;

public class SnmpResponseGetListener implements ResponseListener {
	CountDownLatch latch;
	
	public SnmpResponseGetListener(CountDownLatch latch) {
		this.latch = latch;
	}
	
	public void onResponse(ResponseEvent event) {
		Snmp snmp = (Snmp) event.getSource();
		PDU request  = event.getRequest();
		PDU response = event.getResponse();
		
		snmp.cancel(event.getRequest(), this);
		
		System.out.println("[request]: --> " + request);
		
		if (response == null) {
			System.out.println("[Error]: response is null");
		}
		else if (response.getErrorStatus() != 0) {
			System.out.println("[Error] {status=" + response.getErrorStatus() + ", text='" + response.getErrorStatusText() + "'}");
		}
		else {
			// size 
			int size  = response.size();
			
			System.out.println("[Info] -------> start response <---------");
			StringBuilder builder = new StringBuilder();
			builder.append("{");
			for (int index=0; index<size; index++) {
				if (index == size - 1 ) {
					builder.append(response.get(index).getOid() + "='" + response.get(index).getVariable()+"'");
				}
				else {
					builder.append(response.get(index).getOid() + "='" + response.get(index).getVariable() + "',");
				}
			}
			builder.append("}");
			
			System.out.println(builder.toString());
			
			latch.countDown();
			
			System.out.println("[Info] -------> end response <---------");
		}
	}

}
