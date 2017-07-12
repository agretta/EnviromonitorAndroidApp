package alec.enviromonitorapplication;

public class EnvData {
    private long time;
    private int temperature;
    private int humidity;

    public EnvData(long time, int temp, int humidity) {
        this.time = time;
        this.temperature = temp;
        this.humidity = humidity;
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


}
