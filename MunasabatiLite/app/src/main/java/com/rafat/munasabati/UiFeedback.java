package com.rafat.munasabati;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import java.util.Locale;

/** Lightweight UI audio + haptic feedback that respects device silent mode. */
public final class UiFeedback {
    private static final String PREFS="munasabati_ui";
    private static final String SOUND="sound_enabled",HAPTIC="haptic_enabled",MOTION="motion_enabled";
    private static ToneGenerator tone;
    private static long lastToneAt=0L;

    public static boolean soundEnabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(SOUND,true);}
    public static void setSoundEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(SOUND,v).apply();}
    public static boolean hapticEnabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(HAPTIC,true);}
    public static void setHapticEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(HAPTIC,v).apply();}
    public static boolean motionEnabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(MOTION,true);}
    public static void setMotionEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(MOTION,v).apply();}

    public static void click(View v){
        if(v==null)return;Context c=v.getContext();
        if(hapticEnabled(c))try{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}catch(Exception ignored){}
        if(soundEnabled(c))play(c,toneFor(v));
    }

    public static void preview(View v){
        if(v==null)return;Context c=v.getContext();
        if(hapticEnabled(c))try{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}catch(Exception ignored){}
        play(c,ToneGenerator.TONE_PROP_ACK);
    }

    private static int toneFor(View v){
        String s="";if(v instanceof TextView)s=String.valueOf(((TextView)v).getText());
        if((s==null||s.isEmpty())&&v.getContentDescription()!=null)s=String.valueOf(v.getContentDescription());
        s=(s==null?"":s).toLowerCase(Locale.ROOT);
        if(s.contains("حذف")||s.contains("delete")||s.contains("🗑"))return ToneGenerator.TONE_PROP_NACK;
        if(s.contains("حفظ")||s.contains("save")||s.contains("✓"))return ToneGenerator.TONE_PROP_ACK;
        if(s.contains("إضافة")||s.contains("اضافة")||s.contains("add")||s.contains("＋")||s.contains("+"))return ToneGenerator.TONE_PROP_BEEP2;
        if(s.contains("مزام")||s.contains("sync")||s.contains("↔"))return ToneGenerator.TONE_PROP_PROMPT;
        if(s.contains("رجوع")||s.contains("back")||s.contains("‹")||s.contains("›")||s.contains("⚙"))return ToneGenerator.TONE_PROP_BEEP;
        return ToneGenerator.TONE_PROP_BEEP;
    }

    private static synchronized void play(Context c,int code){
        try{
            AudioManager am=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            if(am!=null){
                if(am.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL)return;
                if(am.getStreamVolume(AudioManager.STREAM_SYSTEM)<=0)return;
            }
            long now=System.currentTimeMillis();if(now-lastToneAt<45)return;lastToneAt=now;
            if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_SYSTEM,32);
            tone.startTone(code,42);
        }catch(Exception ignored){}
    }

    private UiFeedback(){}
}
