package alec.enviromonitorapplication;

public class EnvData {
    private long time;
    private byte temperature;
    private byte humidity;

    public EnvData(long time, byte temp, byte humidity) {
        this.time = time;
        this.temperature = temp;
        this.humidity = humidity;
    }

    public long getTime() {
        return time;
    }

    public byte getTemperature() {
        return temperature;
    }

    public byte getHumidity() {
        return humidity;
    }


}
