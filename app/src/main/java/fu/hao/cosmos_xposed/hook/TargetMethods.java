package fu.hao.cosmos_xposed.hook;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/15/2016
 */
public class TargetMethods {
    public static Set<String> TARGET_METHODS = new HashSet<>();

    static {
        Set<String> aHashSet = new HashSet<>();
        aHashSet.addAll(Arrays.asList(new String[]{
                //"<android.widget.TextView: void setText(java.lang.CharSequence)>",
                "<android.telephony.gsm.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
                "<android.telephony.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
                "<android.telephony.gsm.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>",
                "<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>",
                "<android.telephony.gsm.SmsManager: void sendMultipartTextMessage(java.lang.String,java.lang.String,java.util.ArrayList,java.util.ArrayList,java.util.ArrayList)>",
                "<android.telephony.SmsManager: void sendDataMessage(java.lang.String,java.lang.String,short,byte[],android.app.PendingIntent,android.app.PendingIntent)>",

                "<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.location.LocationListener,android.os.Looper)>",
                "<android.location.LocationManager: java.util.List getProviders(android.location.Criteria,boolean)>",
                "<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.app.PendingIntent)>",
                "<android.location.LocationManager: android.location.LocationProvider getProvider(java.lang.String)>",
                "<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)>",
                "<android.location.LocationManager: boolean isProviderEnabled(java.lang.String)>",
                "<android.location.LocationManager: void addProximityAlert(double,double,float,long,android.app.PendingIntent)>",
                "<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener)>",
                "<android.location.LocationManager: java.lang.String getBestProvider(android.location.Criteria,boolean)>",
                "<android.telephony.TelephonyManager: java.util.List getNeighboringCellInfo()>",
                "<android.telephony.TelephonyManager: android.telephony.CellLocation getCellLocation()>",
                "<android.telephony.TelephonyManager: void listen(android.telephony.PhoneStateListener,int)>",
                "<android.location.LocationManager: java.util.List getProviders(boolean)>",
                "<android.location.LocationManager: void requestLocationUpdates(long,float,android.location.Criteria,android.app.PendingIntent)>",
                "<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.app.PendingIntent)>",
                "<android.location.LocationManager: boolean sendExtraCommand(java.lang.String,java.lang.String,android.os.Bundle)>",
                "<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.location.LocationListener,android.os.Looper)>",
                "<android.location.LocationManager: void requestSingleUpdate(android.location.Criteria,android.location.LocationListener,android.os.Looper)>",
                "<android.location.LocationManager: void requestSingleUpdate(java.lang.String,android.app.PendingIntent)>",
                "<android.location.LocationManager: void requestLocationUpdates(java.lang.String,long,float,android.location.LocationListener,android.os.Looper)>",

                "<android.location.LocationManager: boolean addNmeaListener(android.location.GpsStatus$NmeaListener)>",
                "<android.location.LocationManager: boolean addGpsStatusListener(android.location.GpsStatus$Listener)>",

                "<android.location.Location: double getLatitude()>",
                "<android.location.Location: double getLongitude()>"
        }));
        TARGET_METHODS = aHashSet;
    }
}
