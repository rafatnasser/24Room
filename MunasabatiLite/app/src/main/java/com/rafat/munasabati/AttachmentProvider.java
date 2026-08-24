package com.rafat.munasabati;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;

public class AttachmentProvider extends ContentProvider {
    public static final String AUTHORITY="com.rafat.munasabati.attachments";
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String so){return null;}
    @Override public Uri insert(Uri uri,ContentValues v){return null;}
    @Override public int delete(Uri uri,String s,String[] a){return 0;}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){return 0;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(getContext()==null)throw new FileNotFoundException();
        String name=uri.getLastPathSegment();if(name==null)throw new FileNotFoundException();
        File dir=new File(getContext().getFilesDir(),"attachments");File file=new File(dir,name);
        try{
            String base=dir.getCanonicalPath(),path=file.getCanonicalPath();
            if(!path.startsWith(base+File.separator)||!file.exists())throw new FileNotFoundException();
        }catch(Exception ex){throw new FileNotFoundException();}
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
