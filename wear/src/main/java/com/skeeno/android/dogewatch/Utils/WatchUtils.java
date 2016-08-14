package com.skeeno.android.dogewatch.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.Time;

import com.skeeno.android.dogewatch.R;

/**
 * Created by Johnry on 8/14/2016.
 */
public class WatchUtils {

    public String getFuzzTimeString(Context context, Time time) {
        String timeRange;

        if(time.hour >= 1 && time.hour < 12) {
            //morning
            return context.getString(R.string.morning_string);
        } else if(time.hour >= 12 && time.hour < 13) {
            //midday
            return context.getString(R.string.noon_string);
        } else if (time.hour >= 13 && time.hour < 18) {
            //afternoon
            return context.getString(R.string.afternoon_string);
        } else if (time.hour >= 18 && time.hour <= 23) {
            //evening
            return context.getString(R.string.evening_string);
        } else if (time.hour == 0) {
            //midnight
            return context.getString(R.string.evening_string);
        } else {
            return context.getString(R.string.time_default_string);
        }
    }

    public String getFormattedDigitalTime(Time time) {
        return String.format("%02d:%02d", time.hour, time.minute);
    }

    public String getFormattedDate(Time time) {
        return String.format("%02d/%02d/%04d", time.month + 1, time.monthDay, time.year);
    }
}
