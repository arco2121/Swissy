public interface ToolStructure {
    void startSensors();
    void stopSensors();
    void setListener(ToolListenerStructure listener);
}