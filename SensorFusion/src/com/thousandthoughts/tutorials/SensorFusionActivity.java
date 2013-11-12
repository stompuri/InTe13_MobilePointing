/************************************************************************************
 * Copyright (c) 2012 Paul Lawitzki
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 ************************************************************************************/

package com.thousandthoughts.tutorials;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.math.RoundingMode;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import org.json.*;

import com.thousandthoughts.tutorials.R;

public class SensorFusionActivity extends Activity
implements SensorEventListener{
    
	private SensorManager mSensorManager = null;
	
    // angular speeds from gyro
    private float[] gyro = new float[3];
 
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
 
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
 
    // magnetic field vector
    private float[] magnet = new float[3];
 
    // accelerometer vector
    private float[] accel = new float[3];
 
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
 
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];
 
    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
	private float timestamp;
	private boolean initState = true;
    
	public static final int TIME_CONSTANT = 30;
	public static final float FILTER_COEFFICIENT = 0.98f;
	private Timer fuseTimer = new Timer();
	
	// The following members are only for APP GUI and sending the sensor output.
	public Handler mHandler;
	private RadioGroup mRadioGroup;
	private TextView mAzimuthView;
	private TextView mPitchView;
	private TextView mRollView;
	private int radioSelection;
	private MyCoapClient client1;
	private MyCoapClient client2;
	private String IP1;
	private String IP2;
	private JSONObject json;
	private Runnable runnabletask = null;
	private RelativeLayout layout;
	private double angle1;
	private double angle2;
	DecimalFormat d = new DecimalFormat("#.##");
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        layout = (RelativeLayout)findViewById(R.id.mainlayout);
        IP1="";
        IP2="";
        angle1 = 0.0f;
        angle2 = 0.0f;
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;
 
        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f; gyroMatrix[1] = 0.0f; gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f; gyroMatrix[4] = 1.0f; gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f; gyroMatrix[7] = 0.0f; gyroMatrix[8] = 1.0f;
 
        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();
        
        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
                                      1000, TIME_CONSTANT);
        
        // GUI stuff
        try {
			client1  = new MyCoapClient(this.IP1);
			client2 = new MyCoapClient(this.IP2);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        json = new JSONObject();
        mHandler = new Handler();
        radioSelection = 0;
        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);
        
        /// Application layout here only
        
        RelativeLayout.LayoutParams left = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams right = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams up = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams down = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams button1 = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams button2 = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams text1 = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	RelativeLayout.LayoutParams text2 = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    	ImageView img_left = new ImageView(this);
    	ImageView img_right = new ImageView(this);
    	ImageView img_up = new ImageView(this);
    	ImageView img_down = new ImageView(this);
    	ImageView img_button1 = new ImageView(this);
    	ImageView img_button2 = new ImageView(this);
    	final EditText edittext1 = new EditText(this);
    	final EditText edittext2 = new EditText(this);
    	img_left.setImageResource(R.drawable.left_button);
    	img_right.setImageResource(R.drawable.right_button);
    	img_up.setImageResource(R.drawable.up);
    	img_down.setImageResource(R.drawable.down);
    	img_button1.setImageResource(R.drawable.button);
    	img_button2.setImageResource(R.drawable.button);
    	
    	
    	left.setMargins(0, 66, 0, 0);
    	right.setMargins(360, 66, 0, 0);
    	up.setMargins(240, 90, 0, 0);
    	down.setMargins(240, 240, 0, 0);
    	button1.setMargins(440, 800, 0, 0);
    	button2.setMargins(440, 900, 0, 0);
    	text1.setMargins(5, 800, 0, 0);
    	text2.setMargins(5, 900, 0, 0);
    	
    	layout.addView(img_left, left);
    	layout.addView(img_right, right);
    	layout.addView(img_up, up);
    	layout.addView(img_down, down);
    	layout.addView(img_button1, button1);
    	layout.addView(edittext1, text1);
    	layout.addView(img_button2, button2);
    	layout.addView(edittext2, text2);
    	
    	img_left.getLayoutParams().height = 560;
    	img_left.getLayoutParams().width = 280;
    	img_right.getLayoutParams().height = 560;
    	img_right.getLayoutParams().width = 280;
    	img_up.getLayoutParams().height = 150;
    	img_up.getLayoutParams().width = 150;
    	img_down.getLayoutParams().height = 150;
    	img_down.getLayoutParams().width = 150;
    	img_button1.getLayoutParams().height = 100;
    	img_button1.getLayoutParams().width = 200;
    	edittext1.getLayoutParams().width = 400;
    	edittext1.setText("84.248.76.84");
    	edittext2.setText("84.248.76.84");
    	img_button2.getLayoutParams().height = 100;
    	img_button2.getLayoutParams().width = 200;
    	edittext2.getLayoutParams().width = 400;
    	
/////////// Define and Remember Position for EACH PHYSICAL OBJECTS (LAPTOPS) /////////////////////
    	img_button1.setOnTouchListener(new View.OnTouchListener(){

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
    			case MotionEvent.ACTION_DOWN:
    				// POSITION OF LAPTOP 1 IS REMEMBERED
    				angle1=fusedOrientation[0];
    				IP1 = edittext1.getText().toString();
    				try {
    					// CREATE CLIENT CONNECT TO FIRST LAPTOP
						client1 = new MyCoapClient(IP1);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				break;
    			case MotionEvent.ACTION_MOVE:
    				break;
    			case MotionEvent.ACTION_UP:
    				//view.setImageResource(R.drawable.left_button);
    				break;
    			}
				return true;
			}
    		
    	});
    	
    	img_button2.setOnTouchListener(new View.OnTouchListener(){

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
    			case MotionEvent.ACTION_DOWN:
    				// POSITION OF LAPTOP 2 IS REMEMBERED
    				angle2=fusedOrientation[0];
    				IP2 = edittext2.getText().toString();
    				try {
    					// CREATE CLIENT CONNECT TO SECOND LAPTOP
						client2 = new MyCoapClient(IP2);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				break;
    			case MotionEvent.ACTION_MOVE:
    				break;
    			case MotionEvent.ACTION_UP:
    				break;
    			}
				return true;
			}
    		
    	});
    	
    	img_left.setOnTouchListener(new View.OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View v, MotionEvent event) {
    	    	ImageView view = (ImageView)v;
    			switch(event.getAction()){
    			case MotionEvent.ACTION_DOWN:
    				view.setImageResource(R.drawable.left_button_press);
    				
///////////////////// PERFORM CLICK ACTION BASED ON POSITION OF 2 PHYSICAL OBJECTS (LAPTOPs) HERE/////////////////////////
    				
    				// PHONE's ANGLE WITHIN FIRST LAPTOP//
    				if(angle1!=0.0f)
    				{
    					if(angle1*180/Math.PI-40.0 < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle1*180/Math.PI+40.0)
    					{
    						client1.runClient("left pressed");
    					}
    				}
    				//PHONE's ANGLE WITHIN SECOND LAPTOP//
    				if(angle2!=0.0f)
    				{
    					if(angle2*180/Math.PI-40.0 < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle2*180/Math.PI+40.0)
    					{
    						client2.runClient("left pressed");
    					}
    				}			
    				break;
    			case MotionEvent.ACTION_MOVE:
    				break;
    			case MotionEvent.ACTION_UP:
    				view.setImageResource(R.drawable.left_button);
    				// PHONE's ANGLE WITHIN FIRST LAPTOP//
    				if(angle1!=0.0f)
    				{
    					if(angle1*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle1*180/Math.PI+40.0f)
    					{
    						client1.runClient("left released");
    					}
    				}
    				//PHONE's ANGLE WITHIN SECOND LAPTOP//
    				if(angle2!=0.0f)
    				{
    					if(angle2*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle2*180/Math.PI+40.0f)
    					{
    						client2.runClient("left released");
    					}
    				}
    				break;
    			}
    	        return true;
    	    }
    	});
    	
    	img_right.setOnTouchListener(new View.OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View v, MotionEvent event) {
    	    	ImageView view = (ImageView)v;
    			switch(event.getAction()){
    			case MotionEvent.ACTION_DOWN:
    				view.setImageResource(R.drawable.right_button_press);
    				// PHONE's ANGLE WITHIN FIRST LAPTOP//
    				if(angle1!=0.0f)
    				{
    					if(angle1*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle1*180/Math.PI+40.0f)
    					{
    						client1.runClient("right pressed");
    					}
    				}
    				//PHONE's ANGLE WITHIN SECOND LAPTOP//
    				if(angle2!=0.0f)
    				{
    					if(angle2*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle2*180/Math.PI+40.0f)
    					{
    						client2.runClient("right pressed");
    					}
    				}
    				break;
    			case MotionEvent.ACTION_MOVE:
    				break;
    			case MotionEvent.ACTION_UP:
    				view.setImageResource(R.drawable.right_button);
    				break;
    			}
    	        return true;
    	    }
    	});
    	
    	
    	img_up.setOnTouchListener(new View.OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View v, MotionEvent event) {
    	    	ImageView view = (ImageView)v;
    			switch(event.getAction()){
    			case MotionEvent.ACTION_DOWN:
    				view.setImageResource(R.drawable.up_press);
    				// PHONE's ANGLE WITHIN FIRST LAPTOP//
    				if(angle1!=0.0f)
    				{
    					if(angle1*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle1*180/Math.PI+40.0f)
    					{
    						client1.runClient("up pressed");
    					}
    				}
    				//PHONE's ANGLE WITHIN SECOND LAPTOP//
    				if(angle2!=0.0f)
    				{
    					if(angle2*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle2*180/Math.PI+40.0f)
    					{
    						client2.runClient("up pressed");
    					}
    				}
    				break;
    			case MotionEvent.ACTION_MOVE:
    				break;
    			case MotionEvent.ACTION_UP:
    				view.setImageResource(R.drawable.up);
    				break;
    			}
    	        return true;
    	    }
    	});
    	
    	img_down.setOnTouchListener(new View.OnTouchListener() {
    	    @Override
    	    public boolean onTouch(View v, MotionEvent event) {
    	    	ImageView view = (ImageView)v;
    			switch(event.getAction()){
    			case MotionEvent.ACTION_DOWN:
    				view.setImageResource(R.drawable.down_press);
    				// PHONE's ANGLE WITHIN FIRST LAPTOP//
    				if(angle1!=0.0f)
    				{
    					if(angle1*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle1*180/Math.PI+40.0f)
    					{
    						client1.runClient("down pressed");
    					}
    				}
    				//PHONE's ANGLE WITHIN SECOND LAPTOP//
    				if(angle2!=0.0f)
    				{
    					if(angle2*180/Math.PI-40.0f < fusedOrientation[0]*180/Math.PI
    							&& fusedOrientation[0]*180/Math.PI < angle2*180/Math.PI+40.0f)
    					{
    						client2.runClient("down pressed");
    					}
    				}
    				break;
    			case MotionEvent.ACTION_MOVE:
    				break;
    			case MotionEvent.ACTION_UP:
    				view.setImageResource(R.drawable.down);
    				break;
    			}
    	        return true;
    	    }
    	});
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	// unregister sensor listeners to prevent the activity from draining the device's battery.
    	mSensorManager.unregisterListener(this);
    	mHandler.removeCallbacks(runnabletask);
    	fuseTimer.cancel();
    	fuseTimer.purge();
    }
    
    
	@Override
	public void onDestroy(){
		super.onDestroy();
		//mSensorManager.unregisterListener(this);
		//mHandler.removeCallbacksAndMessages(runnabletask);
	}
    
    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the device's battery.
        mSensorManager.unregisterListener(this);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	// restore the sensor listeners when user resumes the application.
    	initListeners();
    }
    
    // This function registers sensor listeners for the accelerometer, magnetometer and gyroscope.
    public void initListeners(){
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST);
     
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST);
     
        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_FASTEST);
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
	    case Sensor.TYPE_ACCELEROMETER:
	        // copy new accelerometer data into accel array and calculate orientation
	        System.arraycopy(event.values, 0, accel, 0, 3);
	        calculateAccMagOrientation();
	        break;
	 
	    case Sensor.TYPE_GYROSCOPE:
	        // process gyro data
	        gyroFunction(event);
	        break;
	 
	    case Sensor.TYPE_MAGNETIC_FIELD:
	        // copy new magnetometer data into magnet array
	        System.arraycopy(event.values, 0, magnet, 0, 3);
	        break;
	    }
	}
	
	// calculates orientation angles from accelerometer and magnetometer output
	public void calculateAccMagOrientation() {
	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
	        SensorManager.getOrientation(rotationMatrix, accMagOrientation);
	    }
	}
	
	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
            float[] deltaRotationVector,
            float timeFactor)
	{
		float[] normValues = new float[3];
		
		// Calculate the angular speed of the sample
		float omegaMagnitude =
		(float)Math.sqrt(gyroValues[0] * gyroValues[0] +
		gyroValues[1] * gyroValues[1] +
		gyroValues[2] * gyroValues[2]);
		
		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > EPSILON) {
		normValues[0] = gyroValues[0] / omegaMagnitude;
		normValues[1] = gyroValues[1] / omegaMagnitude;
		normValues[2] = gyroValues[2] / omegaMagnitude;
		}
		
		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
		float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
	
    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;
     
        // initialisation of the gyroscope based rotation matrix
        if(initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }
     
        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
        System.arraycopy(event.values, 0, gyro, 0, 3);
        getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }
     
        // measurement done, save current time for next interval
        timestamp = event.timestamp;
     
        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
     
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);
     
        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }
    
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];
     
        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);
     
        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
     
        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
     
        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
     
        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
     
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
     
        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
     
        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return result;
    }
    
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            
            /*
             * Fix for 179° <--> -179° transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
             */
            
            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[0]);
        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            	fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }
            
            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[1]);
        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            	fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }
            
            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * accMagOrientation[2]);
        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            }
            else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            	fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
            }
            else {
            	fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }
     
            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
            
            
            // update sensor output
            runnabletask = updateOreintationDisplayTask;
            mHandler.post(runnabletask);
            
        }
    }
    
    
    
    // CREATE A THREAD FOR EACH SENSOR FUNSION DATA AND SEND TO SERVER SIDE
    private Runnable updateOreintationDisplayTask = new Runnable() {
		public void run() {
			// PHONE's ANGLE WITHIN FIRST LAPTOP//
			if(angle1!=0.0f)
			{
				if((angle1*180/Math.PI-40.0) < (fusedOrientation[0]*180/Math.PI)
						&& (fusedOrientation[0]*180/Math.PI) < (angle1*180/Math.PI+40.0))
				{
					try {
						json.put("x", fusedOrientation[1]* 180/Math.PI);
						json.put("z", (fusedOrientation[0]-angle1)* 180/Math.PI);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String payload = json.toString();
					client1.runClient(payload);
					//updateOreintationDisplay();
				}
			}
			// PHONE's ANGLE WITHIN FIRST LAPTOP//
			if(angle2!=0.0f)
			{
				if((angle2*180/Math.PI-40.0) < (fusedOrientation[0]*180/Math.PI)
						&& (fusedOrientation[0]*180/Math.PI) < (angle2*180/Math.PI+40.0))
				{
					try {
						json.put("x", fusedOrientation[1]* 180/Math.PI);
						json.put("z", (fusedOrientation[0]-angle2)* 180/Math.PI);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String payload = json.toString();
					client2.runClient(payload);
					//updateOreintationDisplay();
				}
			}
				
		}
	};
}