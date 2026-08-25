package com.rafat.munasabati;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MunasabatiWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context,AppWidgetManager appWidgetManager,int[] appWidgetIds){
        for(int id:appWidgetIds)WidgetUpdater.update(context,appWidgetManager,id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager appWidgetManager,int appWidgetId,Bundle newOptions){
        WidgetUpdater.update(context,appWidgetManager,appWidgetId);
    }

    @Override public void onEnabled(Context context){WidgetUpdater.updateAll(context);}

    @Override public void onReceive(Context context,Intent intent){
        super.onReceive(context,intent);
        String action=intent.getAction();
        if(Intent.ACTION_DATE_CHANGED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action)||Intent.ACTION_LOCALE_CHANGED.equals(action))WidgetUpdater.updateAll(context);
    }
}
