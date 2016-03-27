package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;
import java.text.*;

import android.hardware.*;
import android.util.*;

/**
 * Class containing the activity detection algorithm.
 * 
 * <p>
 * You can code your activity detection algorithm in this class. (You may add
 * more Java class files or add libraries in the 'libs' folder if you need). The
 * different callbacks are invoked as per the sensor log files, in the
 * increasing order of timestamps. In the best case, you will simply need to
 * copy paste this class file (and any supporting class files and libraries) to
 * the Android app without modification (in stage 2 of the project).
 * 
 * <p>
 * Remember that your detection algorithm executes as the sensor data arrives
 * one by one. Once you have detected the user's current activity, output it
 * using the {@link ActivitySimulator.outputDetectedActivity(UserActivities)}
 * method. If the detected activity changes later on, then you need to output
 * the newly detected activity using the same method, and so on. The detected
 * activities are logged to the file "DetectedActivities.txt", in the same
 * folder as your sensor logs.
 * 
 * <p>
 * To get the current simulator time, use the method
 * {@link ActivitySimulator.currentTimeMillis()}. You can set timers using the
 * {@link SimulatorTimer} class if you require. You can log to the console/DDMS
 * using either {@code System.out.println()} or using the
 * {@link android.util.Log} class. You can use the
 * {@code SensorManager.getRotationMatrix()} method (and any other helpful
 * methods) as you would normally do on Android.
 * 
 * <p>
 * Note: Since this is a simulator, DO NOT create threads, DO NOT sleep(), or do
 * anything that can cause the simulator to stall/pause. You can however use
 * timers if you require, see the documentation of the {@link SimulatorTimer}
 * class. In the simulator, the timers are faked. When you copy the code into an
 * actual Android app, the timers are real, but the code of this class does not
 * need not be modified.
 */
public class ActivityDetection {

  /** To format the UNIX millis time as a human-readable string. */
  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-h-mm-ssa");
  private boolean isFirstAcclReading = true;
  private int numberTimers = 1;
  private int currentActivity = 0;

  // ACCELEROMETER
  private int BUFFER_SIZE = 30;
  private float acclBuffer[][] = new float[3][BUFFER_SIZE];
  private int accIndex = 0;
  private float WALK_THRESHOLD = 20;

  // LIGHT
  private float LIGHT_THRESHOLD_INDOOR = 100;
  private float LIGHT_THRESHOLD_OUTDOOR = 500;
  private int LIGHT_BUFF_SIZE = 10;
  private float lightSensorFilter[] = new float[LIGHT_BUFF_SIZE];
  private int lightIndex = 0;

  private float lightSensorClean = 0;
  private boolean isUserOutside = false;

  /**
   * Called when the accelerometer sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param x
   *            Accl x value (m/sec^2)
   * @param y
   *            Accl y value (m/sec^2)
   * @param z
   *            Accl z value (m/sec^2)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onAcclSensorChanged(long timestamp, float x, float y, float z, int accuracy) {

    // Process the sensor data as they arrive in each callback,
    // with all the processing in the callback itself (don't create
    // threads).

    // You will most likely not need to use Timers at all, it is just
    // provided for convenience if you require.

    // Here, we just show a dummy example of creating a timer
    // to execute a task 10 minutes later.
    // Be careful not to create too many timers!
    if (isFirstAcclReading) {
      isFirstAcclReading = false;
      SimulatorTimer timer = new SimulatorTimer();
      timer.schedule(this.task, // Task to be executed
          1 * 1000); // Delay in millisec (10 min)
    }
  }

  /**
   * Called when the gravity sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param x
   *            Gravity x value (m/sec^2)
   * @param y
   *            Gravity y value (m/sec^2)
   * @param z
   *            Gravity z value (m/sec^2)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onGravitySensorChanged(long timestamp, float x, float y, float z, int accuracy) {
  }

  /**
   * Called when the linear accelerometer sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param x
   *            Linear Accl x value (m/sec^2)
   * @param y
   *            Linear Accl y value (m/sec^2)
   * @param z
   *            Linear Accl z value (m/sec^2)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onLinearAcclSensorChanged(long timestamp, float x, float y, float z, int accuracy) {

    accIndex = accIndex % BUFFER_SIZE;

    acclBuffer[0][accIndex] = x;
    acclBuffer[1][accIndex] = y;
    acclBuffer[2][accIndex] = z;
    accIndex++;

    if (isFirstAcclReading) {
      isFirstAcclReading = false;
      SimulatorTimer timer = new SimulatorTimer();
      timer.schedule(this.task, // Task to be executed
          10 * 60 * 1000); // Delay in millisec (10 min)
    }

  }

  double getStandardDeviation() {
    double mean = getMean();
    double sum = 0.0;
    for (int i = 0; i < BUFFER_SIZE; i++) {
      sum += Math.pow((getMagnitude(i) - mean), 2);
    }

    double stdDev = Math.sqrt((1 / BUFFER_SIZE) * sum);
    return stdDev;
  }

  double getMean() {
    double sum = 0.0;
    for (int j = 0; j < BUFFER_SIZE; j++) {
      sum += getMagnitude(j);
    }
    return sum / (float) BUFFER_SIZE;

  }

  double getMagnitude(int j) {
    return Math.sqrt(Math.pow(acclBuffer[0][j], 2) + Math.pow(acclBuffer[1][j], 2) + Math.pow(acclBuffer[2][j], 2));
  }

  /**
   * Called when the magnetic sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param x
   *            Magnetic x value (microTesla)
   * @param y
   *            Magnetic y value (microTesla)
   * @param z
   *            Magnetic z value (microTesla)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onMagneticSensorChanged(long timestamp, float x, float y, float z, int accuracy) {
  }

  /**
   * Called when the gyroscope sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param x
   *            Gyroscope x value (rad/sec)
   * @param y
   *            Gyroscope y value (rad/sec)
   * @param z
   *            Gyroscope z value (rad/sec)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onGyroscopeSensorChanged(long timestamp, float x, float y, float z, int accuracy) {
  }

  /**
   * Called when the rotation vector sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param x
   *            Rotation vector x value (unitless)
   * @param y
   *            Rotation vector y value (unitless)
   * @param z
   *            Rotation vector z value (unitless)
   * @param scalar
   *            Rotation vector scalar value (unitless)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onRotationVectorSensorChanged(long timestamp, float x, float y, float z, float scalar, int accuracy) {
  }

  /**
   * Called when the barometer sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param pressure
   *            Barometer pressure value (millibar)
   * @param altitude
   *            Barometer altitude value w.r.t. standard sea level reference
   *            (meters)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onBarometerSensorChanged(long timestamp, float pressure, float altitude, int accuracy) {
  }

  /**
   * Called when the light sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param light
   *            Light value (lux)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onLightSensorChanged(long timestamp, float light, int accuracy) {
    lightIndex = lightIndex % LIGHT_BUFF_SIZE;
    lightSensorFilter[lightIndex] = light;
    lightSensorClean = getMedian(lightSensorFilter);
  }

  /**
   * Called when the proximity sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this sensor event
   * @param proximity
   *            Proximity value (cm)
   * @param accuracy
   *            Accuracy of the sensor data (you can ignore this)
   */
  public void onProximitySensorChanged(long timestamp, float proximity, int accuracy) {
  }

  /**
   * Called when the location sensor has changed.
   * 
   * @param timestamp
   *            Timestamp of this location event
   * @param provider
   *            "gps" or "network"
   * @param latitude
   *            Latitude (deg)
   * @param longitude
   *            Longitude (deg)
   * @param accuracy
   *            Accuracy of the location data (you may use this) (meters)
   * @param altitude
   *            Altitude (meters) (may be -1 if unavailable)
   * @param bearing
   *            Bearing (deg) (may be -1 if unavailable)
   * @param speed
   *            Speed (m/sec) (may be -1 if unavailable)
   */
  public void onLocationSensorChanged(long timestamp, String provider, double latitude, double longitude,
      float accuracy, double altitude, float bearing, float speed) {
  }

  private float getMedian(float in_array[]) {
    Arrays.sort(in_array);
    int size = in_array.length;
    if (size % 2 == 0) {
      return (in_array[size / 2] + in_array[(size / 2) + 1]) / 2;
    } else {
      return in_array[(size + 1) / 2];
    }
  }

  /**
   * Helper method to convert UNIX millis time into a human-readable string.
   */
  private static String convertUnixTimeToReadableString(long millisec) {
    return sdf.format(new Date(millisec));
  }

  private Runnable task = new Runnable() {
    public void run() {

      // Logging to the DDMS (in the simulator, the DDMS log is to the
      // console)
      System.out.println();
      Log.i("ActivitySim", "Timer " + numberTimers + ": Current simulator time: "
          + convertUnixTimeToReadableString(ActivitySimulator.currentTimeMillis()));
      System.out.println("Timer " + numberTimers + ": Current simulator time: "
          + convertUnixTimeToReadableString(ActivitySimulator.currentTimeMillis()));

      // Dummy example of outputting a detected activity
      // (to the file "DetectedActivities.txt" in the trace folder).
      // (here we just alternate between indoor and walking every 10 min)
      if (isUserOutside && (lightSensorClean < LIGHT_THRESHOLD_INDOOR)) {
        isUserOutside = false;
        currentActivity = UserActivities.IDLE_INDOOR;
      } else if (!isUserOutside && (lightSensorClean > LIGHT_THRESHOLD_OUTDOOR)) {
        isUserOutside = true;
        // currentActivity = UserActivities.IDLE_OUTDOOR;
      }

      if (isUserOutside) {
        switch (currentActivity) {
        case UserActivities.WALKING:

          break;
        case UserActivities.VEHICLE:
          if (getStandardDeviation() > WALK_THRESHOLD) {
            currentActivity = UserActivities.WALKING;
          }
          break;
        case UserActivities.IDLE_OUTDOOR:
          if (getStandardDeviation() > WALK_THRESHOLD) {
            currentActivity = UserActivities.WALKING;
          }
          break;
        default:
          break;

        }
      }

      ActivitySimulator.outputDetectedActivity(currentActivity);

      // Set a second timer to execute the same task 10 min later
      ++numberTimers;
      if (numberTimers <= 2) {
        SimulatorTimer timer = new SimulatorTimer();
        timer.schedule(task, // Task to be executed
            1 * 1000); // Delay in millisec (10 min)
      }
    }
  };
}