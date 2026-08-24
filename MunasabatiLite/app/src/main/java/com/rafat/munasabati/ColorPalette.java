package com.rafat.munasabati;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import org.json.JSONObject;

public final class ColorPalette {
    public static final String AUTO="";
    public static final String[] HEX={"#195B56","#D09738","#3B82F6","#8B5CF6","#E2557A","#F97316","#0891B2","#64748B","#16A34A","#B45309"};
    private static final String[] AR={"أخضر داكن","ذهبي","أزرق","بنفسجي","وردي","برتقالي","سماوي","رمادي","أخضر","بني"};
    private static final String[] EN={"Deep green","Gold","Blue","Purple","Rose","Orange","Cyan","Slate","Green","Brown"};
    private static final String PREFS="category_colors";

    public static String[] labels(Context c,boolean includeAuto){
        String[] names=AppSettings.isArabic(c)?AR:EN;
        if(!includeAuto)return names.clone();
        String[] out=new String[names.length+1];out[0]=AppSettings.tr(c,"تلقائي حسب الفئة","Automatic by category");
        System.arraycopy(names,0,out,1,names.length);return out;
    }
    public static String defaultHex(String category){
        switch(category==null?"":category){
            case "birthday": return "#E2557A";
            case "wedding_anniversary": case "marriage": case "engagement": return "#8B5CF6";
            case "eid_fitr": case "eid_adha": case "mawlid": return "#D09738";
            case "travel": return "#3B82F6";
            case "lunch": case "dinner": case "breakfast": return "#F97316";
            case "death": case "martyrdom": case "fatiha": case "fortieth": return "#64748B";
            case "entertainment": return "#0891B2";
            case "weekly_habit": return "#16A34A";
            default:return "#195B56";
        }
    }
    public static String categoryHex(Context c,String category){
        return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(category,defaultHex(category));
    }
    public static void setCategoryHex(Context c,String category,String hex){
        c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(category,hex).apply();
    }
    public static String eventHex(Context c,EventStore.Event e){
        return e!=null&&e.color!=null&&!e.color.isEmpty()?e.color:categoryHex(c,e==null?"none":e.category);
    }
    public static int color(Context c,EventStore.Event e){try{return Color.parseColor(eventHex(c,e));}catch(Exception ex){return Color.rgb(25,91,86);}}
    public static int soft(int base){
        int r=(int)(Color.red(base)*0.14+255*0.86),g=(int)(Color.green(base)*0.14+255*0.86),b=(int)(Color.blue(base)*0.14+255*0.86);
        return Color.rgb(r,g,b);
    }
    public static int indexOf(String hex){if(hex!=null)for(int i=0;i<HEX.length;i++)if(HEX[i].equalsIgnoreCase(hex))return i;return 0;}
    public static JSONObject exportSettings(Context c){
        JSONObject o=new JSONObject();SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        for(String code:Categories.CODES)try{o.put(code,p.getString(code,defaultHex(code)));}catch(Exception ignored){}
        return o;
    }
    public static void importSettings(Context c,JSONObject o){
        if(o==null)return;SharedPreferences.Editor ed=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        for(String code:Categories.CODES)if(o.has(code))ed.putString(code,o.optString(code,defaultHex(code)));ed.apply();
    }
    private ColorPalette(){}
}
