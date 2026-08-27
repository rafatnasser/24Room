package com.rafat.munasabati;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import java.util.Locale;

/** Calm four-sound UI palette with adjustable haptics and system Reduce Motion support. */
public final class UiFeedback {
    private static final String PREFS="munasabati_ui";
    private static final String SOUND="sound_enabled",HAPTIC="haptic_enabled",MOTION="motion_enabled",HAPTIC_LEVEL="haptic_level";
    private static ToneGenerator tone;private static long lastToneAt=0L;
    public static boolean soundEnabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(SOUND,true);}public static void setSoundEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(SOUND,v).apply();}
    public static boolean hapticEnabled(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(HAPTIC,true)&&hapticLevel(c)>0;}public static void setHapticEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(HAPTIC,v).apply();}
    public static int hapticLevel(Context c){return c.getSharedPreferences(PREFS,0).getInt(HAPTIC_LEVEL,1);}public static void setHapticLevel(Context c,int v){c.getSharedPreferences(PREFS,0).edit().putInt(HAPTIC_LEVEL,Math.max(0,Math.min(2,v))).putBoolean(HAPTIC,v>0).apply();}
    public static boolean motionEnabled(Context c){if(!c.getSharedPreferences(PREFS,0).getBoolean(MOTION,true))return false;try{return Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0f;}catch(Exception e){return true;}}public static void setMotionEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(MOTION,v).apply();}
    public static void click(View v){if(v==null)return;Context c=v.getContext();haptic(v);if(soundEnabled(c))play(c,toneFor(v));}
    public static void preview(View v){if(v==null)return;haptic(v);play(v.getContext(),ToneGenerator.TONE_PROP_ACK);}
    private static void haptic(View v){Context c=v.getContext();if(!hapticEnabled(c))return;try{v.performHapticFeedback(hapticLevel(c)>=2?HapticFeedbackConstants.LONG_PRESS:HapticFeedbackConstants.VIRTUAL_KEY);}catch(Exception ignored){}}
    private static int toneFor(View v){String s="";if(v instanceof TextView)s=String.valueOf(((TextView)v).getText());if((s==null||s.isEmpty())&&v.getContentDescription()!=null)s=String.valueOf(v.getContentDescription());s=(s==null?"":s).toLowerCase(Locale.ROOT);if(s.contains("حذف")||s.contains("delete")||s.contains("🗑")||s.contains("خطأ")||s.contains("error"))return ToneGenerator.TONE_PROP_NACK;if(s.contains("حفظ")||s.contains("save")||s.contains("✓")||s.contains("تم ")||s.contains("success"))return ToneGenerator.TONE_PROP_ACK;if(s.contains("إضافة")||s.contains("add")||s.contains("＋")||s.contains("+"))return ToneGenerator.TONE_PROP_BEEP2;return ToneGenerator.TONE_PROP_BEEP;}
    private static synchronized void play(Context c,int code){try{AudioManager am=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);if(am!=null&&(am.getRingerMode()!=AudioManager.RINGER_MODE_NORMAL||am.getStreamVolume(AudioManager.STREAM_SYSTEM)<=0))return;long now=System.currentTimeMillis();if(now-lastToneAt<55)return;lastToneAt=now;if(tone==null)tone=new ToneGenerator(AudioManager.STREAM_SYSTEM,26);tone.startTone(code,36);}catch(Exception ignored){}}
    private UiFeedback(){}
}
