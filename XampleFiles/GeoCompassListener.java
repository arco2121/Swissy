//package Tools.GeoCompass;

public interface GeoCompassListener {
    void onCompassUpdate(float magneticAzimuth, float trueAzimuth);
    void onMagneticInterference(float strength, int level);
    void onCalibrationStart();
    void onCalibrationEnd();
}