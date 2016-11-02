import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
 
public class ComputerPort {
  private BufferedReader in = null;
  private ServerSocket server = null;
  private Socket client = null;
  private Robot robot;
  private String line = "";
  private boolean running = false;
  private boolean isConnected=false;
  private int serverPort = 9000;
  private int height;
  private int width;
  
  private int count = 0;
  private float extrapolationCount;
  
  private long currTime;
  private long lastMovementTime;
  private long lastSecondTime;
  
  private float nowx;
  private float nowy;
 
  public ComputerPort() {
    selectPort();
    init();
  }
  
  private void selectPort() {
    Scanner scan = new Scanner(System.in);
    System.out.print("Please select a socket (e.g. 9000): ");
    serverPort = Integer.parseInt(scan.next());
    scan.close();
    System.out.println("[Server] Port: " + serverPort);
  }
  
  private void init() {
    try{
      System.out.println("[Server] INIT: Socket: " + serverPort);
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      width = screenSize.width;
      height = screenSize.height;
      robot = new Robot(); //create robot for automating mouse movements/clicks
      server = new ServerSocket(serverPort); //create a server socket on port
      Point point = MouseInfo.getPointerInfo().getLocation(); //Get current mouse position
      nowx = point.x;
      nowy = point.y;
      lastMovementTime = System.nanoTime();
      lastSecondTime = System.nanoTime();
    }catch (IOException e) {
      System.out.println("[Server] Error in opening Socket. Make sure no other program is using this socket!");
      System.exit(-1);
    }catch (AWTException e) {
      System.out.println("[Server] Error in creating robot instance");
      System.exit(-1);
    }
  }
  
  public void updateState() {
    float movex = 0;
    float movey = 0;
    running = true;
    //read input from client while it is connected
    while(running) {
      if(!isConnected) {
        try {
          client = server.accept(); //listens for a connection to be made to this socket and accepts it
          in = new BufferedReader(new InputStreamReader(client.getInputStream())); //the input stream where data will come from client
          isConnected = true;
          Point point = MouseInfo.getPointerInfo().getLocation(); //Get current mouse position
          nowx = point.x;
          nowy = point.y;
          System.out.println("[Server] Client connected.");
        } catch (IOException e) {
          System.out.println("[Server] Error in opening Socket");
          System.exit(-1);
        }
      }
      else {
        try{
          if(in.ready()) {
            line = in.readLine(); //read input from client
            
            if(line == null) {
              System.out.println("[Server] Client disconnected.");
              break;
            } 
            else if(line.contains(",")) {
              count++; //counts mouse commands sent from android device
              
              currTime = System.nanoTime();
              
              if(currTime - lastSecondTime > 1000000000) {
                extrapolationCount = (float) 1000 / (( (float) 1000000000 / (currTime - lastSecondTime)) * count);
                System.out.println("[Info] Count: " + count + "\tExtrapCount: " + extrapolationCount + "\tTime: " + (currTime - lastSecondTime)); //print count since last read
                count = 0;
                lastSecondTime = System.nanoTime();
              }
            }
            else if(line.equals("left_click_down")) {
              System.out.println("[Server] Received left click down.");
              robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            }
            else if(line.equals("left_click_up")) {
              System.out.println("[Server] Received left click up.");
              robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }
            else if(line.equals("right_click_down")) {
              System.out.println("[Server] Received right click down.");
              robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            }
            else if(line.equals("right_click_up")) {
              System.out.println("[Server] Received right click up.");
              robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            }
            //handle disconnections
            else if(line.equalsIgnoreCase("exit")) {
              isConnected = false;
              //Close client socket
              client.close();
              System.out.println("[Server] Client disconnected.");
            }
            else if(line.equalsIgnoreCase("serverExit")) {
              isConnected=false;
              client.close();
              System.out.println("[Server] Shutting down.");
              running = false;
            }
          }
            
          currTime = System.nanoTime();
                    
          //input will come in x,y format if user moves mouse on mousepad
          if(currTime - lastMovementTime > 1000000 && line.contains(",")) {
            Point point = MouseInfo.getPointerInfo().getLocation(); //Get current mouse position
            
            if((int) nowx != point.x)
              nowx = point.x;
            if((int) nowy != point.y)
              nowy = point.y;

            //startTime = System.nanoTime();
            movex = Float.parseFloat(line.split(",")[0]);//extract movement in x direction
            movey = Float.parseFloat(line.split(",")[1]);//extract movement in y direction
            
            nowx += movex / extrapolationCount;
            nowy += movey / extrapolationCount;
            
            if(nowx < 0)
              nowx = 0;
            else if(nowx > width)
              nowx = width;
            
            if(nowy < 0)
              nowy = 0;
            else if(nowy > height)
              nowy = height;
            
            robot.mouseMove((int)(nowx),(int)(nowy));//Move mouse pointer to new location
            
            lastMovementTime = System.nanoTime();
          }
              
          //endTime = System.nanoTime();
          
          //System.out.println("[Info] Time: " + (endTime - startTime));

        } catch (IOException e) {
          System.out.println("[Server] Read failed");
          System.exit(-1);
        }
      }
    }
  }
}