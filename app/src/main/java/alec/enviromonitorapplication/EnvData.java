package alec.enviromonitorapplication;

public class EnvData {
    private long time;
    private int temperature;
    private int humidity;
    private int tilt;

    public EnvData(long time, int temp, int humidity) {
        this.time = time;
        this.temperature = temp;
        this.humidity = humidity;
        tilt=0;
    }

    public EnvData(long time, int tilt) {
        this.time = time;
        temperature = 0;
        humidity = 0;
        this.tilt = tilt;
    }

    public long getTime() {
        return time;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getTilt() {
        return tilt;
    }

}
