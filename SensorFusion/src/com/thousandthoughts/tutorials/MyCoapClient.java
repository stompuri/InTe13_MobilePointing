package com.thousandthoughts.tutorials;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapEmptyMessage;
import org.ws4d.coap.messages.CoapRequestCode;

import android.os.AsyncTask;

////// COAP CLIENT ///////////
public class MyCoapClient  implements CoapClient {
	private static String SERVER_ADDRESS = "84.248.76.84";
    private static final int PORT = Constants.COAP_DEFAULT_PORT;
    static int counter = 0;
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;
    CoapRequest coapRequest=null;
    
    public MyCoapClient(String IP) throws UnknownHostException
    {
    	if(!IP.equals(" "));
    		{this.SERVER_ADDRESS = IP;}
		channelManager = BasicCoapChannelManager.getInstance();
		
		clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
    }
    
    public void runClient(String payload){
    	coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
		coapRequest.setPayload(payload);
		clientChannel.sendMessage(coapRequest);
		
    }

	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		//System.out.println("Connection Failed");
	}

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		//System.out.println("Received response");
	}
}
