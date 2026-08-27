package com.rafat.munasabati;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class ModernApplication extends Application implements Application.ActivityLifecycleCallbacks {
    @Override public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);}
    @Override public void onActivityResumed(Activity activity){activity.getWindow().getDecorView().post(()->ModernUi.apply(activity));}
    @Override public void onActivityCreated(Activity a,Bundle b){}
    @Override public void onActivityStarted(Activity a){}
    @Override public void onActivityPaused(Activity a){}
    @Override public void onActivityStopped(Activity a){}
    @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
    @Override public void onActivityDestroyed(Activity a){}
}
