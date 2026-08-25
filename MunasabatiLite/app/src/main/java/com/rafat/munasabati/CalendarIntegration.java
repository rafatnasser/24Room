package com.rafat.munasabati;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import java.util.*;

public final class CalendarIntegration {
    private static final String PREFS="calendar_integration";
    private static final String ENABLED="enabled";
    private static final String CALENDAR_ID="calendar_id";
    private static final String CALENDAR_LABEL="calendar_label";

    public static final class CalendarItem {
        public final long id;
        public final String name;
        public final String account;
        public final String accountType;
        public CalendarItem(long i,String n,String a,String t){id=i;name=n==null?"":n;account=a==null?"":a;accountType=t==null?"":t;}
        public String provider(){String x=accountType.toLowerCase(Locale.ROOT);if(x.contains("google"))return "Google";if(x.contains("microsoft")||x.contains("outlook")||x.contains("exchange"))return "Outlook / Microsoft";return "Android Calendar";}
        @Override public String toString(){String p=provider();String a=account.isEmpty()?"":(" • "+account);return p+" — "+name+a;}
    }

    public static boolean hasPermission(Context c){return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED&&c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    public static boolean enabled(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(ENABLED,false);}
    public static void setEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(ENABLED,v).apply();}
    public static long selectedCalendarId(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong(CALENDAR_ID,-1L);}
    public static String selectedCalendarLabel(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(CALENDAR_LABEL,"");}
    public static void selectCalendar(Context c,CalendarItem item){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(CALENDAR_ID,item==null?-1L:item.id).putString(CALENDAR_LABEL,item==null?"":item.toString()).apply();}

    public static List<CalendarItem> writableCalendars(Context c){
        ArrayList<CalendarItem> out=new ArrayList<>();if(!hasPermission(c))return out;
        String[] projection={CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,CalendarContract.Calendars.ACCOUNT_NAME,CalendarContract.Calendars.ACCOUNT_TYPE,CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL};
        String selection=CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL+">=? AND "+CalendarContract.Calendars.VISIBLE+"=1";
        String[] args={String.valueOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)};
        try(Cursor cur=c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,projection,selection,args,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME+" COLLATE NOCASE")){
            if(cur!=null)while(cur.moveToNext())out.add(new CalendarItem(cur.getLong(0),cur.getString(1),cur.getString(2),cur.getString(3)));
        }catch(Exception ignored){}
        return out;
    }

    public static boolean upsertAndPersist(Context c,EventStore.Event e){
        if(e==null||!enabled(c)||!hasPermission(c))return false;long target=selectedCalendarId(c);if(target<0)return false;
        try{
            if(e.calendarEventId>0&&e.calendarId>0&&e.calendarId!=target){deleteExternal(c,e);e.calendarEventId=-1;e.calendarId=-1;}
            ContentValues v=eventValues(e,target);
            if(e.calendarEventId>0){Uri u=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,e.calendarEventId);int n=c.getContentResolver().update(u,v,null,null);if(n==0)e.calendarEventId=-1;}
            if(e.calendarEventId<=0){Uri u=c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI,v);if(u==null)return false;e.calendarEventId=ContentUris.parseId(u);}
            e.calendarId=target;persistLink(c,e);return true;
        }catch(Exception ex){return false;}
    }

    public static void deleteExternal(Context c,EventStore.Event e){
        if(e==null||e.calendarEventId<=0||!hasPermission(c))return;
        try{c.getContentResolver().delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,e.calendarEventId),null,null);}catch(Exception ignored){}
        e.calendarEventId=-1;e.calendarId=-1;
    }

    public static void deleteLinkedEvent(Context c,long appEventId){
        EventStore.Event e=EventStore.find(c,appEventId);if(e==null)return;deleteExternal(c,e);
        List<EventStore.Event> all=EventStore.load(c);for(EventStore.Event x:all)if(x.id==appEventId){x.calendarId=-1L;x.calendarEventId=-1L;}EventStore.save(c,all);
    }

    public static int syncAll(Context c){
        if(!enabled(c)||!hasPermission(c)||selectedCalendarId(c)<0)return 0;int ok=0;
        for(EventStore.Event e:EventStore.load(c))if(upsertAndPersist(c,e))ok++;return ok;
    }

    private static ContentValues eventValues(EventStore.Event e,long calendarId){
        ContentValues v=new ContentValues();v.put(CalendarContract.Events.CALENDAR_ID,calendarId);v.put(CalendarContract.Events.TITLE,e.title);
        StringBuilder desc=new StringBuilder();if(e.details!=null&&!e.details.isEmpty())desc.append(e.details);if(desc.length()>0)desc.append("\n\n");desc.append("Munasabati • ").append(Categories.englishLabel(e.category));
        v.put(CalendarContract.Events.DESCRIPTION,desc.toString());v.put(CalendarContract.Events.EVENT_LOCATION,e.locationName==null?"":e.locationName);
        v.put(CalendarContract.Events.DTSTART,e.eventTime);v.put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().getID());v.put(CalendarContract.Events.ALL_DAY,0);
        String rule=rrule(e);
        if(rule.isEmpty()){v.put(CalendarContract.Events.DTEND,e.eventTime+60*60*1000L);v.putNull(CalendarContract.Events.RRULE);v.putNull(CalendarContract.Events.DURATION);}
        else{v.put(CalendarContract.Events.DURATION,"PT1H");v.put(CalendarContract.Events.RRULE,rule);v.putNull(CalendarContract.Events.DTEND);}
        return v;
    }

    private static String rrule(EventStore.Event e){if(e==null)return "";if(Recurrence.WEEKLY.equals(e.recurrence))return "FREQ=WEEKLY";if(Recurrence.MONTHLY.equals(e.recurrence))return "FREQ=MONTHLY";if(Recurrence.YEARLY.equals(e.recurrence))return "FREQ=YEARLY";return "";}

    private static void persistLink(Context c,EventStore.Event e){List<EventStore.Event> all=EventStore.load(c);for(EventStore.Event x:all)if(x.id==e.id){x.calendarId=e.calendarId;x.calendarEventId=e.calendarEventId;}EventStore.save(c,all);}
    private CalendarIntegration(){}
}
