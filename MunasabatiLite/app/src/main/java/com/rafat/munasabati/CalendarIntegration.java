package com.rafat.munasabati;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class CalendarIntegration {
    private static final String PREFS="calendar_integration";
    private static final String ENABLED="enabled";
    private static final String CALENDAR_ID="calendar_id";
    private static final String CALENDAR_LABEL="calendar_label";
    private static final String LAST_SYNC_TIME="last_sync_time";
    private static final String LAST_SYNC_SUMMARY="last_sync_summary";
    private static final String LAST_SYNC_ERRORS="last_sync_errors";
    private static final String LAST_PUSHED="last_pushed";
    private static final String LAST_PULLED="last_pulled";
    private static final String LAST_DELETED="last_deleted";
    private static final String LAST_IMPORTED="last_imported";
    private static final String LAST_CONFLICTS="last_conflicts";
    private static volatile boolean syncing=false;

    public static final class CalendarItem {
        public final long id;
        public final String name;
        public final String account;
        public final String accountType;
        public CalendarItem(long i,String n,String a,String t){id=i;name=n==null?"":n;account=a==null?"":a;accountType=t==null?"":t;}
        public String provider(){String x=accountType.toLowerCase(Locale.ROOT);if(x.contains("google"))return "Google";if(x.contains("microsoft")||x.contains("outlook")||x.contains("exchange"))return "Outlook / Microsoft";return "Android Calendar";}
        @Override public String toString(){String a=account.isEmpty()?"":(" • "+account);return provider()+" — "+name+a;}
    }

    public static final class ExternalEvent {
        public long id,calendarId,start,end;
        public String title="",location="",description="",rrule="";
        public boolean deleted=false;
        public String recurrence(){return recurrenceFromRule(rrule);}
        @Override public String toString(){return title;}
    }

    public static final class SyncResult {
        public int pushed=0,pulled=0,deleted=0,imported=0,conflicts=0,unchanged=0;
        public final ArrayList<String> errors=new ArrayList<>();
        public int totalChanged(){return pushed+pulled+deleted+imported;}
        public boolean hasErrors(){return !errors.isEmpty();}
        public String summary(Context c){
            return AppSettings.tr(c,"رفع ","Pushed ")+pushed+" • "+AppSettings.tr(c,"سحب ","Pulled ")+pulled+" • "+AppSettings.tr(c,"حذف ","Deleted ")+deleted+" • "+AppSettings.tr(c,"استيراد ","Imported ")+imported+(conflicts>0?" • "+AppSettings.tr(c,"تعارض ","Conflicts ")+conflicts:"");
        }
    }

    public static boolean hasPermission(Context c){return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED&&c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    public static boolean enabled(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(ENABLED,false);}
    public static void setEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(ENABLED,v).apply();}
    public static long selectedCalendarId(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong(CALENDAR_ID,-1L);}
    public static String selectedCalendarLabel(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(CALENDAR_LABEL,"");}
    public static void selectCalendar(Context c,CalendarItem item){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(CALENDAR_ID,item==null?-1L:item.id).putString(CALENDAR_LABEL,item==null?"":item.toString()).apply();}
    public static boolean defaultSyncForNewEvent(Context c){return enabled(c)&&hasPermission(c)&&selectedCalendarId(c)>0;}
    public static boolean isSyncing(){return syncing;}

    public static long lastSyncTime(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong(LAST_SYNC_TIME,0L);}
    public static String lastSyncSummary(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(LAST_SYNC_SUMMARY,"");}
    public static String lastSyncErrors(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(LAST_SYNC_ERRORS,"");}
    public static int lastPushed(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(LAST_PUSHED,0);}
    public static int lastPulled(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(LAST_PULLED,0);}
    public static int lastDeleted(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(LAST_DELETED,0);}
    public static int lastImported(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(LAST_IMPORTED,0);}
    public static int lastConflicts(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(LAST_CONFLICTS,0);}

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

    public static synchronized SyncResult syncBidirectional(Context c){
        SyncResult result=new SyncResult();
        if(syncing){result.errors.add(AppSettings.tr(c,"مزامنة أخرى قيد التنفيذ","Another sync is already running"));return result;}
        syncing=true;
        try{
            if(!hasPermission(c)){result.errors.add(AppSettings.tr(c,"صلاحية التقويم غير متاحة","Calendar permission is not available"));recordResult(c,result);return result;}
            List<EventStore.Event> source=new ArrayList<>(EventStore.load(c));ArrayList<EventStore.Event> kept=new ArrayList<>();boolean changed=false;
            long defaultCalendar=selectedCalendarId(c);
            for(EventStore.Event e:source){
                if(!e.calendarSync){kept.add(e);continue;}
                try{
                    if(e.calendarEventId<=0){
                        long target=e.calendarId>0?e.calendarId:defaultCalendar;
                        if(target<=0){result.errors.add(e.title+": "+AppSettings.tr(c,"لم يتم اختيار تقويم","No calendar selected"));kept.add(e);continue;}
                        if(pushEvent(c,e,target)){result.pushed++;changed=true;}else result.errors.add(e.title+": "+AppSettings.tr(c,"تعذر إنشاء الحدث الخارجي","Could not create external event"));
                        kept.add(e);continue;
                    }
                    ExternalEvent ext=queryExternalById(c,e.calendarEventId);
                    if(ext==null||ext.deleted){ReminderScheduler.cancel(c,e.id);result.deleted++;changed=true;continue;}
                    String localFp=localFingerprint(e),externalFp=externalFingerprint(ext),base=e.calendarFingerprint==null?"":e.calendarFingerprint;
                    if(base.isEmpty()){
                        if(localFp.equals(externalFp)){e.calendarFingerprint=localFp;result.unchanged++;changed=true;}
                        else{pullExternal(e,ext);reschedule(c,e);e.calendarFingerprint=localFingerprint(e);result.pulled++;changed=true;}
                    }else if(localFp.equals(base)&&externalFp.equals(base)){result.unchanged++;}
                    else if(localFp.equals(base)&&!externalFp.equals(base)){
                        pullExternal(e,ext);reschedule(c,e);e.calendarFingerprint=localFingerprint(e);result.pulled++;changed=true;
                    }else if(!localFp.equals(base)&&externalFp.equals(base)){
                        long target=e.calendarId>0?e.calendarId:defaultCalendar;if(pushEvent(c,e,target)){result.pushed++;changed=true;}else result.errors.add(e.title+": "+AppSettings.tr(c,"فشل رفع التعديل","Failed to push local change"));
                    }else if(localFp.equals(externalFp)){
                        e.calendarFingerprint=localFp;result.unchanged++;changed=true;
                    }else{
                        result.conflicts++;result.errors.add(e.title+": "+AppSettings.tr(c,"تعارض تعديل — تم اعتماد نسخة التقويم الخارجي","Edit conflict — external calendar version was used"));
                        pullExternal(e,ext);reschedule(c,e);e.calendarFingerprint=localFingerprint(e);result.pulled++;changed=true;
                    }
                    kept.add(e);
                }catch(Exception ex){kept.add(e);result.errors.add(e.title+": "+safeMessage(ex));}
            }
            if(changed||kept.size()!=source.size())EventStore.save(c,kept);
            recordResult(c,result);return result;
        }finally{syncing=false;}
    }

    public static boolean pushLocalChange(Context c,long localEventId){
        if(!hasPermission(c))return false;EventStore.Event e=EventStore.find(c,localEventId);if(e==null||!e.calendarSync)return false;
        long target=e.calendarId>0?e.calendarId:selectedCalendarId(c);if(target<=0)return false;
        boolean ok=pushEvent(c,e,target);if(ok)EventStore.replace(c,e);return ok;
    }

    public static void deleteForLocalDelete(Context c,EventStore.Event e){
        if(e==null||!e.calendarSync||e.calendarEventId<=0||!hasPermission(c))return;
        try{c.getContentResolver().delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,e.calendarEventId),null,null);}catch(Exception ignored){}
    }

    public static List<ExternalEvent> listExternalEvents(Context c,long calendarId,long start,long end){
        ArrayList<ExternalEvent> out=new ArrayList<>();if(!hasPermission(c)||calendarId<=0)return out;
        String[] projection=eventProjection();String selection=CalendarContract.Events.CALENDAR_ID+"=? AND "+CalendarContract.Events.DELETED+"=0 AND ("+CalendarContract.Events.DTSTART+" BETWEEN ? AND ? OR "+CalendarContract.Events.RRULE+" IS NOT NULL)";
        String[] args={String.valueOf(calendarId),String.valueOf(start),String.valueOf(end)};
        try(Cursor cur=c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,projection,selection,args,CalendarContract.Events.DTSTART+" ASC")){
            if(cur!=null)while(cur.moveToNext()&&out.size()<200)out.add(readExternal(cur));
        }catch(Exception ignored){}
        return out;
    }

    public static SyncResult importExternalEvents(Context c,Collection<Long> externalIds,String category){
        SyncResult result=new SyncResult();if(!hasPermission(c)){result.errors.add(AppSettings.tr(c,"صلاحية التقويم غير متاحة","Calendar permission is not available"));recordResult(c,result);return result;}
        List<EventStore.Event> all=new ArrayList<>(EventStore.load(c));HashSet<Long> linked=new HashSet<>();for(EventStore.Event e:all)if(e.calendarEventId>0)linked.add(e.calendarEventId);
        long seed=System.currentTimeMillis();int seq=0;
        for(Long id:externalIds){if(id==null||id<=0||linked.contains(id))continue;ExternalEvent ext=queryExternalById(c,id);if(ext==null||ext.deleted)continue;
            try{
                EventStore.Event e=new EventStore.Event();e.id=seed+(seq++);e.title=ext.title.isEmpty()?AppSettings.tr(c,"مناسبة من التقويم","Calendar event"):ext.title;e.category=category==null?"none":category;
                e.recurrence=ext.recurrence();e.details=cleanDescription(ext.description);e.eventTime=ext.start;e.locationName=ext.location;e.reminderMinutes=30;e.remindersCsv="30";
                e.calendarSync=true;e.calendarId=ext.calendarId;e.calendarEventId=ext.id;e.calendarFingerprint=externalFingerprint(ext);e.updatedAt=System.currentTimeMillis();all.add(e);ReminderScheduler.schedule(c,e);result.imported++;linked.add(id);
            }catch(Exception ex){result.errors.add(ext.title+": "+safeMessage(ex));}
        }
        EventStore.save(c,all);recordResult(c,result);return result;
    }

    public static ExternalEvent queryExternalById(Context c,long id){
        if(!hasPermission(c)||id<=0)return null;Uri uri=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,id);
        try(Cursor cur=c.getContentResolver().query(uri,eventProjection(),null,null,null)){if(cur!=null&&cur.moveToFirst())return readExternal(cur);}catch(Exception ignored){}
        return null;
    }

    private static boolean pushEvent(Context c,EventStore.Event e,long targetCalendar){
        try{
            ContentValues v=eventValues(e,targetCalendar);boolean updated=false;
            if(e.calendarEventId>0){Uri u=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,e.calendarEventId);updated=c.getContentResolver().update(u,v,null,null)>0;}
            if(!updated){Uri u=c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI,v);if(u==null)return false;e.calendarEventId=ContentUris.parseId(u);}
            e.calendarId=targetCalendar;ExternalEvent ext=queryExternalById(c,e.calendarEventId);e.calendarFingerprint=ext==null?localFingerprint(e):externalFingerprint(ext);return true;
        }catch(Exception ex){return false;}
    }

    private static ContentValues eventValues(EventStore.Event e,long calendarId){
        ContentValues v=new ContentValues();v.put(CalendarContract.Events.CALENDAR_ID,calendarId);v.put(CalendarContract.Events.TITLE,e.title);
        String desc=e.details==null?"":e.details.trim();if(!desc.isEmpty())desc+="\n\n";desc+="[Munasabati] "+Categories.englishLabel(e.category);v.put(CalendarContract.Events.DESCRIPTION,desc);
        v.put(CalendarContract.Events.EVENT_LOCATION,e.locationName==null?"":e.locationName);v.put(CalendarContract.Events.DTSTART,e.eventTime);v.put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().getID());v.put(CalendarContract.Events.ALL_DAY,0);
        String rule=rrule(e);if(rule.isEmpty()){v.put(CalendarContract.Events.DTEND,e.eventTime+60*60*1000L);v.putNull(CalendarContract.Events.DURATION);v.putNull(CalendarContract.Events.RRULE);}else{v.putNull(CalendarContract.Events.DTEND);v.put(CalendarContract.Events.DURATION,"PT1H");v.put(CalendarContract.Events.RRULE,rule);}return v;
    }

    private static String[] eventProjection(){return new String[]{CalendarContract.Events._ID,CalendarContract.Events.CALENDAR_ID,CalendarContract.Events.TITLE,CalendarContract.Events.DTSTART,CalendarContract.Events.DTEND,CalendarContract.Events.EVENT_LOCATION,CalendarContract.Events.DESCRIPTION,CalendarContract.Events.RRULE,CalendarContract.Events.DELETED};}
    private static ExternalEvent readExternal(Cursor cur){ExternalEvent x=new ExternalEvent();x.id=cur.getLong(0);x.calendarId=cur.getLong(1);x.title=nvl(cur.getString(2));x.start=cur.getLong(3);x.end=cur.isNull(4)?0L:cur.getLong(4);x.location=nvl(cur.getString(5));x.description=nvl(cur.getString(6));x.rrule=nvl(cur.getString(7));x.deleted=!cur.isNull(8)&&cur.getInt(8)!=0;return x;}

    private static void pullExternal(EventStore.Event e,ExternalEvent x){e.title=x.title;e.eventTime=x.start;e.locationName=x.location;e.details=cleanDescription(x.description);e.recurrence=x.recurrence();e.calendarId=x.calendarId;e.calendarEventId=x.id;e.updatedAt=System.currentTimeMillis();}
    private static void reschedule(Context c,EventStore.Event e){ReminderScheduler.cancel(c,e.id);ReminderScheduler.schedule(c,e);}
    private static String rrule(EventStore.Event e){if(e==null)return "";if(Recurrence.WEEKLY.equals(e.recurrence))return "FREQ=WEEKLY";if(Recurrence.MONTHLY.equals(e.recurrence))return "FREQ=MONTHLY";if(Recurrence.YEARLY.equals(e.recurrence))return "FREQ=YEARLY";return "";}
    private static String recurrenceFromRule(String r){String u=nvl(r).toUpperCase(Locale.ROOT);if(u.contains("FREQ=WEEKLY"))return Recurrence.WEEKLY;if(u.contains("FREQ=MONTHLY"))return Recurrence.MONTHLY;if(u.contains("FREQ=YEARLY"))return Recurrence.YEARLY;return Recurrence.NONE;}

    private static String localFingerprint(EventStore.Event e){return hash(norm(e.title)+"|"+e.eventTime+"|"+norm(e.locationName)+"|"+norm(e.details)+"|"+norm(e.recurrence));}
    private static String externalFingerprint(ExternalEvent x){return hash(norm(x.title)+"|"+x.start+"|"+norm(x.location)+"|"+norm(cleanDescription(x.description))+"|"+norm(x.recurrence()));}
    private static String cleanDescription(String s){String x=nvl(s);int p=x.indexOf("\n\n[Munasabati]");if(p>=0)x=x.substring(0,p);p=x.indexOf("\n\nMunasabati •");if(p>=0)x=x.substring(0,p);return x.trim();}
    private static String norm(String s){return nvl(s).trim().replaceAll("\\s+"," ");}
    private static String nvl(String s){return s==null?"":s;}
    private static String hash(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte x:b)out.append(String.format(Locale.US,"%02x",x));return out.toString();}catch(Exception ex){return Integer.toHexString(s.hashCode());}}
    private static String safeMessage(Exception ex){String m=ex.getMessage();return m==null?ex.getClass().getSimpleName():m;}

    private static void recordResult(Context c,SyncResult r){
        StringBuilder errors=new StringBuilder();for(String e:r.errors){if(errors.length()>0)errors.append("\n");errors.append("• ").append(e);if(errors.length()>3000)break;}
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(LAST_SYNC_TIME,System.currentTimeMillis()).putString(LAST_SYNC_SUMMARY,r.summary(c)).putString(LAST_SYNC_ERRORS,errors.toString()).putInt(LAST_PUSHED,r.pushed).putInt(LAST_PULLED,r.pulled).putInt(LAST_DELETED,r.deleted).putInt(LAST_IMPORTED,r.imported).putInt(LAST_CONFLICTS,r.conflicts).apply();
    }
    private CalendarIntegration(){}
}
