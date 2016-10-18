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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  private Context context;
  private Button  connectButton;
  private Toast   updates;

  private String  ipAddr;
  private int     socketNum;

  private boolean isConnected = false;
  private float   orientation[] = new float[3];

  private int zeroThres = 5;

  private int     sensVal = 500;

  private Socket      socket;
  private PrintWriter out;

  private float xCal;
  private float yCal;

  //Read newest data from sensors continuously
  private SensorEventListener accListener = new SensorEventListener() {
    @Override
    public void onSensorChanged(SensorEvent event) {
      if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
        float norm = (float) Math.sqrt(event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]);

        orientation[0] = event.values[0] / norm;
        orientation[1] = event.values[1] / norm;
        orientation[2] = event.values[2] / norm;

        float xDisp = (orientation[0] - xCal) * -1;
        float yDisp = (orientation[1] - yCal);

        float zeroSpace = (float) zeroThres / 50;
        if(xDisp > -1 * zeroSpace && xDisp < zeroSpace)
          xDisp = 0;
        else if(xDisp >= zeroSpace)
          xDisp -= zeroSpace;
        else
          xDisp += zeroSpace;

        if(yDisp > -1 * zeroSpace && yDisp < zeroSpace)
          yDisp = 0;
        else if(yDisp >= zeroSpace)
          yDisp -= zeroSpace;
        else
          yDisp += zeroSpace;


        if(out != null)
          out.println((float) sensVal / 100 * xDisp + "," + (float) sensVal / 100 * yDisp);
      }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
  };

  /*
   * Runs when app starts
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    context = this;
    updates = new Toast(context);
    updates.cancel();
    updates.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0);

    //initialize accelerometer/sensor manager/listener
    SensorManager sensorM = (SensorManager) getSystemService(SENSOR_SERVICE);
    Sensor accSensor = sensorM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    sensorM.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_FASTEST);

    connectButton = (Button) findViewById(R.id.connectButton);

    //set event listeners
    connectButton.setOnClickListener(this);
    findViewById(R.id.lclickButton).setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if(isConnected && out != null) {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
              findViewById(R.id.lclickButton).setBackgroundColor(0xFF00FFFF);
              out.println("left_click_down");
              break;
            case MotionEvent.ACTION_UP:
              findViewById(R.id.lclickButton).setBackgroundColor(0xFF00CFFF);
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
              findViewById(R.id.rclickButton).setBackgroundColor(0xFF00FFFF);
              out.println("right_click_down");
              break;
            case MotionEvent.ACTION_UP:
              findViewById(R.id.rclickButton).setBackgroundColor(0xFF00CFFF);
              out.println("right_click_up");
              break;
          }
        }

        return false;
      }
    });
    SeekBar sensitivity = (SeekBar) findViewById(R.id.sensitivity);
    sensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        sensVal = progress;
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
    SeekBar zeroThreshold = (SeekBar) findViewById(R.id.zerothreshold);
    zeroThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        zeroThres = progress;
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
    findViewById(R.id.calibrateButton).setOnClickListener(this);
  }

  /*
   * OnClick method is called when any of the onClick buttons are pressed
   */
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.connectButton:
        //if not connected
        if (!isConnected) {
          //make attempt to connect
          updates.cancel();
          updates = Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT);
          updates.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0);
          updates.show();

          ipAddr = ((EditText)findViewById(R.id.ip_address)).getText().toString();
          String socketStr = ((EditText)findViewById(R.id.socket)).getText().toString();
          try {
            socketNum = Integer.parseInt(socketStr);
          } catch (NumberFormatException e){
            updates.cancel();
            updates = Toast.makeText(context, "Invalid socket. Enter a positive integer.", Toast.LENGTH_SHORT);
            updates.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0);
            updates.show();
          }

          setConnection(true);

          //display result of attempt
          updates.cancel();
          updates = Toast.makeText(context,isConnected?"Connected to server!":"Error while connecting. Make sure the IP address is correct and the server is running.",Toast.LENGTH_LONG);
          updates.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0);
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
            connectButton.setText(R.string.disconnect);
            findViewById(R.id.ip_address).setEnabled(false);
            findViewById(R.id.socket).setEnabled(false);
          }catch (IOException e){
            Log.e("remotedroid", "Error while creating OutWriter", e);
            updates.cancel();
            updates = Toast.makeText(context,"Error while connecting",Toast.LENGTH_LONG);
            updates.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0);
            updates.show();
          }
        }
        //connected
        else {
          //make attempt to disconnect
          updates.cancel();
          updates = Toast.makeText(context,"Disconnecting...",Toast.LENGTH_SHORT);
          updates.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 16, 0);
          updates.show();
          try {
            out.println("exit");
            socket.close();

            //update state of variables and views
            isConnected = false;
            connectButton.setText(R.string.connect);
            findViewById(R.id.ip_address).setEnabled(true);
            findViewById(R.id.socket).setEnabled(true);
          } catch (IOException e) {
            Log.e("remotedroid", "Error in closing socket", e);
          }
        }
        break;
      case R.id.calibrateButton:
        xCal = orientation[0];
        yCal = orientation[1];
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

  //TODO load socket and ip from file
  public void setConnection(boolean state) {
    if(state) {
      try {
        InetAddress serverAddr = InetAddress.getByName(ipAddr);
        socket = new Socket(); //Open socket on server IP and port
        socket.connect(new InetSocketAddress(serverAddr, socketNum), 1000);
        isConnected = true;
      } catch (SocketTimeoutException e) {
        try {
          if (socket.isConnected()) {
            socket.close();
          }
        }
        catch (Exception e2) {
          Log.e("remotedroid", "Error while closing connection", e2);
        }
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