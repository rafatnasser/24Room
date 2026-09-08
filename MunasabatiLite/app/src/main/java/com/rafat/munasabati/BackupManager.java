package com.rafat.munasabati;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class BackupManager {
    public static int createBackup(Context c,OutputStream out)throws Exception{
        ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(out));
        JSONObject root=EventStore.exportJson(c,false);
        JSONArray events=new JSONArray();
        int copied=0;
        for(EventStore.Event e:EventStore.load(c)){
            JSONObject o=EventStore.toJsonObject(e,false);
            if(e.attachmentUri!=null&&!e.attachmentUri.isEmpty()){
                String safe=safeName(e.attachmentName.isEmpty()?"attachment":e.attachmentName);
                String entry="attachments/"+e.id+"_"+safe;
                try(InputStream in=c.getContentResolver().openInputStream(Uri.parse(e.attachmentUri))){
                    if(in!=null){zip.putNextEntry(new ZipEntry(entry));copy(in,zip);zip.closeEntry();o.put("backupAttachment",entry);copied++;}
                }catch(Exception ignored){}
            }
            events.put(o);
        }
        root.put("events",events);root.put("backupVersion",1);root.put("createdAt",System.currentTimeMillis());
        zip.putNextEntry(new ZipEntry("backup.json"));zip.write(root.toString(2).getBytes("UTF-8"));zip.closeEntry();zip.finish();zip.flush();return copied;
    }

    public static int restoreBackup(Context c,InputStream input)throws Exception{
        File dir=new File(c.getFilesDir(),"attachments");if(!dir.exists())dir.mkdirs();
        HashMap<String,String> extracted=new HashMap<>();byte[] json=null;
        ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input));ZipEntry e;
        while((e=zip.getNextEntry())!=null){
            if(e.isDirectory()){zip.closeEntry();continue;}
            if("backup.json".equals(e.getName())){ByteArrayOutputStream b=new ByteArrayOutputStream();copy(zip,b);json=b.toByteArray();}
            else if(e.getName().startsWith("attachments/")){
                String name=safeName(new File(e.getName()).getName());File target=uniqueFile(dir,name);
                try(OutputStream o=new FileOutputStream(target)){copy(zip,o);}
                extracted.put(e.getName(),target.getName());
            }
            zip.closeEntry();
        }
        if(json==null)throw new IOException("backup.json missing");
        JSONObject root=new JSONObject(new String(json,"UTF-8"));JSONArray a=root.optJSONArray("events");if(a==null)throw new IOException("events missing");
        ArrayList<EventStore.Event> list=new ArrayList<>();
        for(int i=0;i<a.length();i++){
            JSONObject o=a.getJSONObject(i);EventStore.Event ev=EventStore.fromJsonObject(o);
            String path=o.optString("backupAttachment","");
            if(!path.isEmpty()&&extracted.containsKey(path)){
                String file=extracted.get(path);ev.attachmentUri=new Uri.Builder().scheme("content").authority(AttachmentProvider.AUTHORITY).appendPath(file).build().toString();
            }
            list.add(ev);
        }
        EventStore.save(c,list);
        String lang=root.optString("language","");if("ar".equals(lang)||"en".equals(lang))AppSettings.setLanguage(c,lang);
        if(root.has("hijriOffset"))AppSettings.setHijriOffset(c,root.optInt("hijriOffset",0));
        ColorPalette.importSettings(c,root.optJSONObject("categoryColors"));
        for(EventStore.Event ev:list)ReminderScheduler.schedule(c,ev);
        return list.size();
    }

    private static File uniqueFile(File dir,String name){File f=new File(dir,name);if(!f.exists())return f;String base=name,ext="";int d=name.lastIndexOf('.');if(d>0){base=name.substring(0,d);ext=name.substring(d);}int n=2;while(f.exists())f=new File(dir,base+"_"+(n++)+ext);return f;}
    private static String safeName(String s){return s.replaceAll("[^\\p{L}\\p{N}._-]+","_");}
    private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    private BackupManager(){}
}
