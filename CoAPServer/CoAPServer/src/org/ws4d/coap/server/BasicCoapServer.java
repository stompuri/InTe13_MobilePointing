/* Copyright [2011] [University of Rostock]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

package org.ws4d.coap.server;

import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapMessage;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapServer;
import org.ws4d.coap.interfaces.CoapServerChannel;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapResponseCode;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

import org.json.*;

/**
 * @author Christian Lerche <christian.lerche@uni-rostock.de>
 */

public class BasicCoapServer implements CoapServer {
    private static final int PORT = 5683;
    private static double width;
    private static double height;
    static int counter = 0;

    public static void main(String[] args) {
    	Dimension resolution = Toolkit.getDefaultToolkit().getScreenSize();
    	width = resolution.getWidth();
    	height = resolution.getHeight();
        System.out.println("Start CoAP Server on port " + PORT);
        
        // Start CoAP Server on PORT 5683
        BasicCoapServer server = new BasicCoapServer();
        CoapChannelManager channelManager = BasicCoapChannelManager.getInstance();
        channelManager.createServerListener(server, PORT);
        
    }

	@Override
	public CoapServer onAccept(CoapRequest request) {
		System.out.println("Accept connection...");
		return this;
	}

	@Override
	public void onRequest(CoapServerChannel channel, CoapRequest request) {
		System.out.println("Received message: " + request.toString()+ " Payload: " + new String(request.getPayload()));
		//Robot class for control computer's mouse
		Robot robot;
		JSONObject json;
		//Convert Request from client (bytes type) to String
		String client_request = new String(request.getPayload());
		
		//Predefine x and y coordinates on the screen
		double x=0;
		double y=0;
		
		
		//When receive mouse left pressed action from client
		if(client_request.equals("left pressed"))
		{
			try {
				robot = new Robot();
				robot.mousePress(InputEvent.BUTTON1_MASK);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//When receive mouse left released action from client
		else if(client_request.equals("left released"))
		{
			try {
				robot = new Robot();
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//When receive mouse right pressed action from client
		else if(client_request.equals("right pressed"))
		{
			try {
				robot = new Robot();
	            robot.mousePress(InputEvent.BUTTON3_MASK);
	            robot.mouseRelease(InputEvent.BUTTON3_MASK);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//When receive mouse scroll up action from client		
		else if(client_request.equals("up pressed"))
		{
			try {
				robot = new Robot();
				robot.mouseWheel(-1);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//When receive mouse scroll down action from client
		else if(client_request.equals("down pressed"))
		{
			try {
				robot = new Robot();
				robot.mouseWheel(1);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//When receive sensor fusion data from client
		else
		{
			try {
				json = new JSONObject(new String(request.getPayload()));
				double ori_x = json.getDouble("x");
				double ori_y = -1000.0;
				// make Y coordinate more stable using Low Pass method
				ori_y = lowPass(json.getDouble("z"), ori_y);
				
				// modify x and y to match computer's screen resolution (within 60 degree angle)
				x=(ori_x+30.0f) * (height/60.0f);
				y=(ori_y+30.0f) * (width/60.0f);
				
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				//Start moving cursor based on sensor fusion data
				robot = new Robot();
		        robot.mouseMove((int)y,(int)x);
			} catch (AWTException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Just CoAP stuffs here for connection
		CoapMessage response = channel.createResponse(request,
				CoapResponseCode.Content_205);
		response.setContentType(CoapMediaType.text_plain);
		
		response.setPayload("payload...".getBytes());
		
		if (request.getObserveOption() != null){
			System.out.println("Client wants to observe this resource.");
		}
		
		response.setObserveOption(1);
		
		channel.sendMessage(response);
	}
	
	//Low Pass method here
	protected double lowPass( double input, double output ) {
	    if ( output == -1000.0 ) return input;
	     	output = output + 0.2f * (input - output);
	    return output;
	}
	@Override
	public void onSeparateResponseFailed(CoapServerChannel channel) {
		System.out.println("Separate response transmission failed.");
		
	}
}
