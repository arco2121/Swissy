//package Tools.Livella;

public class Livella {
    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private LivellaListener listener;

    private float pitch = 0f;
    private float roll = 0f;
    private float azimuth = 0f;
    public float smoothness = 0.2f;

    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    public void setListener(LivellaListener listener) {
        this.listener = listener;
    }

    public Livella(SensorManager sm) {
        this.sensorManager = sm;
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        SensorManagerListener rotationListener = new SensorManagerListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                getLevel(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        }
    }

    private void getLevel(SensorEvent event) {
        if (event == null) return;
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);
        float newAzimuth = orientation[0] * RAD_TO_DEG;
        float newPitch = orientation[1] * RAD_TO_DEG;
        float newRoll  = orientation[2] * RAD_TO_DEG;
        azimuth = azimuth * (1 - smoothness) + newAzimuth * smoothness;
        pitch   = pitch   * (1 - smoothness) + newPitch   * smoothness;
        roll    = roll    * (1 - smoothness) + newRoll    * smoothness;
        if(listener != null) listener.onLevelChange(azimuth, pitch, roll);
    }
}
