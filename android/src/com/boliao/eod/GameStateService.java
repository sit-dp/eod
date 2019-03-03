package com.boliao.eod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
//import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.util.List;

import static android.app.Notification.VISIBILITY_PUBLIC;

/**
 * TODO SERVICES 5: a background service to manage game state
 * - collect sensor data and send these updates to GameState in game core component
 * - determine time to spawn bugs
 * - both a Started (collect sensor data) and Bound Service (update UI continuously)
 * - this background Service will try to persist until the app is explicitly closed
 * - Q1: when will it be killed?
 * - Q2: what happens when it is killed?
 */
public class GameStateService extends Service implements SensorEventListener {
    private static final String TAG = "GameStateService";

    Thread bgThread;

    // TODO NOTIFICATIONS
    // - add var for NotificationManager
    // - add ID vars for notifications
    NotificationManager notificationManager;
    private static final String NOTIFICATION_CHANNEL_ID = "EOD CHANNEL";
    private static final int NOTIFY_ID = 0;
    private static final int PENDINGINTENT_ID = 1;


    // TODO SENSORS
    // create SensorManager and Sensor vars to interface with phone sensors
    private SensorManager sensorManager;
    private Sensor stepDetector;

    /**
     * Empty Ctor
     */
    public GameStateService() {}


    // TODO SERVICES 6: create GameStateBinder class that extends Binder
    // - boilerplate for Bound Service
    // - init an IBinder interface to offer a handle to this class
    public class GameStateBinder extends Binder {
        public GameStateService getService() {
            return GameStateService.this;
        }
    }
    private final IBinder binder = new GameStateBinder();

    /**
     * TODO SERVICES 7:implement onBind to return the binder interface
     * - boilerplate for Bound Service
     * @param intent to hold any info from caller
     * @return IBinder to obtain a handle to the service class
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * TODO SERVICES 8: override onCreate Service lifecycle to initialize various things
     * - get handle to SensorManager from a System Service
     * - get list of available sensors from the sensorManager
     * - get handle to step detector from sensorManager
     * - init NotificationManager and NotificationChannel
     */
    @Override
    public void onCreate() {
        super.onCreate();

        // TODO SENSORS 1: get handle to sensor device
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // get list of all available sensors, along with some capability data
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String sensorsStr = "available sensors:";
        for (Sensor sensor: sensors) {
            sensorsStr += "\n" + sensor.getName() +
                    " madeBy=" + sensor.getVendor() +
                    " v" + sensor.getVersion() +
                    " minDelay=" + sensor.getMinDelay() +
                    " maxRange=" + sensor.getMaximumRange() +
                    " power=" + sensor.getPower();
        }
        Log.i(TAG, sensorsStr);

        // TODO SENSORS 2: get handles only for required sensors
        // - if you want to show app only if user has the sensor, then do <uses-feature> in manifest
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (stepDetector == null) {
            Log.e(TAG, "No step sensors on device!");
        }

        // TODO SERVICES 9: obtain and init notification manager with a channel
        // - notification channels introduced in Android Oreo
        // - need to initialize a channel before creating actual notifications
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_HIGH));
    }

    /**
     * TODO SERVICES 10: implement onStartCommand to define what the service will actually do
     * - register this class as a SensorListener (extend this Service) using sensorManager
     * - add a thread to manage spawning of bugs based on a countdown
     *   - spawn bug when GameState.i().isCanNotify() && !GameState.i().isAppActive()
     *   - create pending intent to launch AndroidLauncher
     *   - use NotificationCompat.Builder to make notification
     * @param intent to hold any info from caller
     * @param flags to show more data about how this was started (e.g., REDELIVERY)
     * @param startId id of this started instance
     * @return an int that controls what happens when this service is auto killed
     *         , e.g., sticky or not (see https://goo.gl/shXLoy)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO SENSORS 3: Registering listener to listen for sensor events.
        // - note that the DELAY is the max, and system normally lower
        // - don't just use SENSOR_DELAY_FASTEST (0us) as it uses max power
        sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_GAME);

        // control the spawn timer in a thread
        // O.M.G. a raw java thread
        bgThread = new Thread()  {
            @Override
            public void run() {
                super.run();
                try {
                    while (true) {
                        // decrement countdown every sec
                        sleep(1000);
                        GameState.i().decTimer();

                        // notify user when bug is spawning
                        if (GameState.i().isCanNotify() && !GameState.i().isAppActive()) {
                            Log.i(TAG, "The NIGHT has come: a bug will spawn...");

                            // TODO SERVICES 11: create pending intent to open app from notification
                            Intent intent = new Intent(GameStateService.this, AndroidLauncher.class);
                            PendingIntent pi = PendingIntent.getActivity(GameStateService.this, PENDINGINTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                            // build the notification
                            Notification noti =new NotificationCompat.Builder(GameStateService.this, NOTIFICATION_CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_stat_name)
                                    .setContentTitle("Exercise Or Die")
                                    .setColor(Color.RED)
                                    .setVisibility(VISIBILITY_PUBLIC)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH) // for android 7.1 and below
                                    .setContentText("OMG NIGHT TIME lai liao, BUGs will spawn")
                                    .setAutoCancel(true)
                                    .setVibrate(new long[] {1000, 1000, 1000, 1000, 1000})
                                    .setLights(Color.RED, 3000, 3000)
                                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                    .setContentIntent(pi)
                                    .build();

                            // activate the notification
                            notificationManager.notify(NOTIFY_ID, noti);
                        }
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        bgThread.start();

        // TODO SERVICE 11: return appropriate flag to indicate what happens when killed
        return START_STICKY;
    }

    // TODO SERVICE 12: override Service's onDestroy to destroy any background activity if desired
    // TODO SENSORS 4: override Service's onDestroy to unregister listeners from the sensorManager
    // - also destroy any manual threads
    @Override
    public void onDestroy() {
        super.onDestroy();

        sensorManager.unregisterListener(this, stepDetector);

        // here's an example of the iffiniess of using raw threads: no good way to stop it
        // bgThread.stop(); // has been deprecated
        // bgThread.interrupt();
    }


    // TODO SENSORS 5: implement onSensorChanged callback
    // - system will call this back when sensor has new vals
    // - simply call GameState.i().incSteps(event.values[0])
    //   if event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR
    // - log value for debugging
    // - do as minimal as possible (this is called VERY frequently)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values.length > 0) {
            int val = (int) event.values[0];

            // update game state based on sensor vals
            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                Log.d(TAG, "Step detector:" + val);
                GameState.i().incSteps(val);
            }
        }
    }

    // TODO SENSORS: implement onSensorChanged callback
    // - system will call this back when sensor accuracy changed
    // - just show a log msg
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Sensor accuracy changed to " + accuracy);
    }
}
