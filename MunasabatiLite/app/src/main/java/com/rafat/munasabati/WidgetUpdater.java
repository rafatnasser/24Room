package com.rafat.munasabati;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.util.*;

public final class WidgetUpdater {
    private static final int[] ROW_IDS={R.id.widget_row1,R.id.widget_row2,R.id.widget_row3};
    private static final int[] ICON_IDS={R.id.widget_icon1,R.id.widget_icon2,R.id.widget_icon3};
    private static final int[] TITLE_IDS={R.id.widget_event_title1,R.id.widget_event_title2,R.id.widget_event_title3};
    private static final int[] META_IDS={R.id.widget_event_meta1,R.id.widget_event_meta2,R.id.widget_event_meta3};
    private static final int[] COUNT_IDS={R.id.widget_countdown1,R.id.widget_countdown2,R.id.widget_countdown3};

    public static void updateAll(Context context){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        int[] ids=manager.getAppWidgetIds(new ComponentName(context,MunasabatiWidgetProvider.class));
        for(int id:ids)update(context,manager,id);
    }

    public static void update(Context context,AppWidgetManager manager,int widgetId){
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.widget_munasabati);
        boolean ar=AppSettings.isArabic(context);
        views.setTextViewText(R.id.widget_title,ar?"مناسباتي":"Munasabati");
        views.setTextViewText(R.id.widget_subtitle,ar?"أقرب المناسبات":"Next events");
        views.setTextViewText(R.id.widget_footer,ar?"اضغط على المناسبة لفتحها":"Tap an event to open it");
        views.setTextViewText(R.id.widget_empty,ar?"لا توجد مناسبات قادمة\nاضغط ＋ لإضافة مناسبة":"No upcoming events\nTap ＋ to add one");

        Intent home=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(R.id.widget_header,PendingIntent.getActivity(context,4400,home,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        Intent add=new Intent(context,EditEventActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        views.setOnClickPendingIntent(R.id.widget_add,PendingIntent.getActivity(context,4401,add,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));

        int maxRows=rowsForSize(manager.getAppWidgetOptions(widgetId));
        List<Occurrence> upcoming=upcoming(context,System.currentTimeMillis());
        boolean empty=upcoming.isEmpty();
        views.setViewVisibility(R.id.widget_empty,empty?View.VISIBLE:View.GONE);

        for(int i=0;i<3;i++){
            boolean show=!empty&&i<maxRows&&i<upcoming.size();
            views.setViewVisibility(ROW_IDS[i],show?View.VISIBLE:View.GONE);
            if(!show)continue;
            Occurrence o=upcoming.get(i);EventStore.Event e=o.event;
            String star=e.favorite?" ★":"";
            views.setTextViewText(ICON_IDS[i],Categories.icon(e.category));
            views.setTextViewText(TITLE_IDS[i],e.title+star);
            views.setTextViewText(META_IDS[i],Categories.label(context,e.category)+" • "+DateTools.gregorianShort(context,o.time)+" • "+DateTools.time(context,o.time));
            views.setTextViewText(COUNT_IDS[i],relative(context,o.time));
            int color;
            try{color=Color.parseColor(ColorPalette.eventHex(context,e));}catch(Exception ex){color=Color.rgb(25,91,86);}
            views.setTextColor(COUNT_IDS[i],color);

            Intent open=new Intent(context,EditEventActivity.class).putExtra("event_id",e.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            views.setOnClickPendingIntent(ROW_IDS[i],PendingIntent.getActivity(context,4500+i+(int)(e.id&0x3fffffff),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        }
        manager.updateAppWidget(widgetId,views);
    }

    private static int rowsForSize(Bundle options){
        int h=options==null?220:options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,220);
        return h<175?1:3;
    }

    private static List<Occurrence> upcoming(Context context,long now){
        ArrayList<Occurrence> out=new ArrayList<>();
        for(EventStore.Event e:EventStore.load(context)){
            long t=Recurrence.isRecurring(e)?Recurrence.nextOccurrence(e,now):e.eventTime;
            if(t>=now)out.add(new Occurrence(e,t));
        }
        out.sort((a,b)->{
            int time=Long.compare(a.time,b.time);if(time!=0)return time;
            if(a.event.pinned!=b.event.pinned)return a.event.pinned?-1:1;
            if(a.event.favorite!=b.event.favorite)return a.event.favorite?-1:1;
            return a.event.title.compareToIgnoreCase(b.event.title);
        });
        return out.size()>3?new ArrayList<>(out.subList(0,3)):out;
    }

    private static String relative(Context c,long when){
        boolean ar=AppSettings.isArabic(c);long diff=when-System.currentTimeMillis();
        if(diff<=60000L)return ar?"الآن":"Now";
        if(diff<3600000L){long n=Math.max(1,Math.round(diff/60000.0));
            if(ar){if(n==1)return "بعد دقيقة";if(n==2)return "بعد دقيقتين";return "بعد "+n+" دقيقة";}
            return "in "+n+" min";
        }
        if(diff<86400000L){long n=Math.max(1,Math.round(diff/3600000.0));
            if(ar){if(n==1)return "بعد ساعة";if(n==2)return "بعد ساعتين";return "بعد "+n+" ساعات";}
            return n==1?"in 1 hour":"in "+n+" hours";
        }
        long n=Math.max(1,(long)Math.ceil(diff/86400000.0));
        if(ar){if(n==1)return "متبقي يوم";if(n==2)return "متبقي يومان";if(n>=3&&n<=10)return "متبقي "+n+" أيام";return "متبقي "+n+" يومًا";}
        return n==1?"1 day left":n+" days left";
    }

    private static final class Occurrence{
        final EventStore.Event event;final long time;
        Occurrence(EventStore.Event e,long t){event=e;time=t;}
    }
    private WidgetUpdater(){}
}
