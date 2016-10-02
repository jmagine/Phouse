import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.MouseInfo;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
 
public class ComputerPort {
	private BufferedReader in = null;
	private ServerSocket server = null;
	private Socket client = null;
	private Robot robot;
	private String line;
	private boolean running = false;
	private boolean isConnected=false;
	private int serverPort = 8998;
	
	private float nowx;
	private float nowy;
 
	public ComputerPort() {
		init();
	}
	
	private void init() {
		try{
			System.out.println("[Server] INIT: Socket: " + serverPort);
	    	robot = new Robot(); //create robot for automating mouse movements/clicks
			server = new ServerSocket(serverPort); //create a server socket on port
			Point point = MouseInfo.getPointerInfo().getLocation(); //Get current mouse position
			nowx=point.x;
			nowy=point.y;
		}catch (IOException e) {
			System.out.println("[Server] Error in opening Socket");
			System.exit(-1);
		}catch (AWTException e) {
			System.out.println("[Server] Error in creating robot instance");
			System.exit(-1);
		}
	}
	
	public void updateState() {
		running = true;
		//read input from client while it is connected
	    while(running) {
	    	if(!isConnected) {
	    		try {
	    			client = server.accept(); //listens for a connection to be made to this socket and accepts it
	    			in = new BufferedReader(new InputStreamReader(client.getInputStream())); //the input stream where data will come from client
	    		    isConnected = true;
	    		} catch (IOException e) {
	    			System.out.println("[Server] Error in opening Socket");
	    			System.exit(-1);
	    		}
	    	}
	    	else {
		        try{
					line = in.readLine(); //read input from client
					if(line == null) {
						System.out.println("[Server] Client disconnected.");
						break;
					}
					System.out.println("[Client] " + line); //print whatever we get from client
					
					//input will come in x,y format if user moves mouse on mousepad
					if(line.contains(",")) {
						float movex = Float.parseFloat(line.split(",")[0]);//extract movement in x direction
						float movey = Float.parseFloat(line.split(",")[1]);//extract movement in y direction
						nowx += movex;
						nowy += movey;
						robot.mouseMove((int)(nowx),(int)(nowy));//Move mouse pointer to new location
					}
					//handle clicks
					else if(line.contains("left_click_down")) {
						System.out.println("[Server] Received left click down.");
						robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					}
					else if(line.contains("left_click_up")) {
						System.out.println("[Server] Received left click up.");
						robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					}
					else if(line.contains("right_click_down")) {
						System.out.println("[Server] Received right click down.");
						robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
					}
					else if(line.contains("right_click_up")) {
						System.out.println("[Server] Received right click up.");
						robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					}
					//handle disconnections
					else if(line.equalsIgnoreCase("exit")) {
						isConnected=false;
						//Close client socket
						client.close();
					}
					else if(line.equalsIgnoreCase("serverExit")) {
						isConnected=false;
						client.close();
						System.out.println("[Server] Shutting down.");
						running = false;
					}
		        } catch (IOException e) {
					System.out.println("[Server] Read failed");
					System.exit(-1);
		        }
	      	}
	    }
	}
}