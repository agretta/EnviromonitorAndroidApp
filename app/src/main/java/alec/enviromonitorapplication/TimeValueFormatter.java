package alec.enviromonitorapplication;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class TimeValueFormatter implements IAxisValueFormatter {

    private DecimalFormat formatter;
    private SimpleDateFormat dateFormat;
    private long year;

    public TimeValueFormatter(long realTime){
        formatter = new DecimalFormat("00");
        //dateFormat = new SimpleDateFormat("dd/M/Y hh:mm a");
        dateFormat = new SimpleDateFormat("dd:hh:mm a");
        year = realTime;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        /*float dayZoom = ;
        float monthZoom = ;
        float yearZoom = ;
        */
        int time = (int)value;
        int seconds = time%60;
        int minutes = (time/60) % 60;
        int hours = (time/3600) % 60;
        int day = (time/(3600 * 24)) % 365;
        /*String am;
        if (hours > 11) {
            am = "PM";
        }
        else {
            am = "AM";
        }
        hours = hours % 12;
        if (hours == 0) {
            hours = 12;
        }*/
        //return (hours) + ":" + formatter.format(minutes) + " " + am;// + ":" + formatter.format(seconds);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(year);
        cal.set(Calendar.DAY_OF_YEAR, day);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, seconds);


        return dateFormat.format(cal.getTime());

        //return Float.toString(value);
    }
}
