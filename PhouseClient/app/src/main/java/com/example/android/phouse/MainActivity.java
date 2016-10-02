package com.example.android.phouse;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.os.StrictMode;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private Context context;
  private Button  connectButton;
  private Toast   updates;

  private boolean isConnected = false;
  private float   orientation[] = new float[3];

  private Socket      socket;
  private PrintWriter out;

  private SensorManager sensorM;
  private Sensor        accSensor;

  //Read newest data from sensors continuously
  private SensorEventListener accListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
      if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        orientation[0] = event.values[0];
        orientation[1] = event.values[1];
        orientation[2] = event.values[2];

        if(out != null)
          out.println(orientation[0] * -1 + "," + orientation[1]);
          //out.println(orientation[0] + " " + orientation[1] + " " + orientation[2]);
      }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    context = this;
    updates = new Toast(context);

    //initialize accelerometer/sensor manager/listener
    sensorM = (SensorManager) getSystemService(SENSOR_SERVICE);
    accSensor = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    sensorM.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);

    connectButton = (Button) findViewById(R.id.connectButton);

    //set event listeners
    connectButton.setOnClickListener(this);
    findViewById(R.id.lclickButton).setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if(isConnected && out != null) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              out.println("left_click_down");
              break;
            case MotionEvent.ACTION_UP:
              out.println("left_click_up");
              break;
          }
        }
        return false;
      }
    });
    findViewById(R.id.rclickButton).setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if(isConnected && out != null) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              out.println("right_click_down");
              break;
            case MotionEvent.ACTION_UP:
              out.println("right_click_up");
              break;
          }
        }

        return false;
      }

    });
  }

  //OnClick method is called when any of the buttons are pressed
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.connectButton:
        //if not connected
        if (!isConnected) {
          //make attempt to connect
          updates.cancel();
          updates = Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT);
          updates.show();

          setConnection(true);

          //display result of attempt
          updates.cancel();
          updates = Toast.makeText(context,isConnected?"Connected to server!":"Error while connecting. Make sure the IP address is correct and the server is running.",Toast.LENGTH_LONG);
          updates.show();

          //return on failure to connect
          if(!isConnected)
            return;

          try {
            //try to create output stream to send data to server
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                      socket.getOutputStream())), true);
            out.println("connected");

            //update state of buttons and editText fields
            connectButton.setText("Disconnect");
            findViewById(R.id.ip_address).setEnabled(false);
            findViewById(R.id.socket).setEnabled(false);
          }catch (IOException e){
            Log.e("remotedroid", "Error while creating OutWriter", e);
            updates.cancel();
            updates = Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG);
            updates.show();
          }
        }
        //connected
        else {
          //make attempt to disconnect
          updates.cancel();
          updates = Toast.makeText(context,"Disconnecting...",Toast.LENGTH_SHORT);
          updates.show();
          try {
            out.println("exit");
            socket.close();

            //update state of variables and views
            isConnected = false;
            connectButton.setText("Connect");
            findViewById(R.id.ip_address).setEnabled(true);
            findViewById(R.id.socket).setEnabled(true);
          } catch (IOException e) {
            Log.e("remotedroid", "Error in closing socket", e);
          }
        }
        break;
    }
  }

  @Override
  public void onDestroy()
  {
    super.onDestroy();

    //disconnect
    if(isConnected && out!=null) {
      setConnection(false);
    }
  }

  //TODO make ip and socket dynamically stated on connect mouseclick
  public void setConnection(boolean state) {
    if(state) {
      try {
        InetAddress serverAddr = InetAddress.getByName("10.20.1.173");
        socket = new Socket(serverAddr, 8998);//Open socket on server IP and port
        isConnected = true;
      } catch (IOException e) {
        Log.e("remotedroid", "Error while connecting", e);
      }
    }
    else {
      try {
        out.println("exit"); //tell server to exit
        socket.close(); //close socket
      } catch (IOException e) {
        Log.e("remotedroid", "Error in closing socket", e);
      }
    }
  }
}