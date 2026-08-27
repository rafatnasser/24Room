package com.rafat.munasabati;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

public class AttachmentProvider extends ContentProvider {
    public static final String AUTHORITY="com.rafat.munasabati.attachments";
    @Override public boolean onCreate(){return true;}

    @Override public String getType(Uri uri){
        String name=uri.getLastPathSegment();
        if(name==null)return "application/octet-stream";
        String lower=name.toLowerCase(Locale.US);
        if(lower.endsWith(".jpg")||lower.endsWith(".jpeg"))return "image/jpeg";
        if(lower.endsWith(".png"))return "image/png";
        if(lower.endsWith(".ics"))return "text/calendar";
        if(lower.endsWith(".pdf"))return "application/pdf";
        int dot=lower.lastIndexOf('.');
        if(dot>=0&&dot<lower.length()-1){
            String mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(lower.substring(dot+1));
            if(mime!=null)return mime;
        }
        return "application/octet-stream";
    }

    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){
        File file=null;
        try{file=resolveFile(uri);}catch(FileNotFoundException ignored){}
        String[] cols=projection!=null?projection:new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE};
        MatrixCursor cursor=new MatrixCursor(cols,1);
        Object[] row=new Object[cols.length];
        for(int i=0;i<cols.length;i++){
            if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=file!=null?file.getName():uri.getLastPathSegment();
            else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=file!=null?file.length():0L;
            else row[i]=null;
        }
        cursor.addRow(row);
        return cursor;
    }

    @Override public Uri insert(Uri uri,ContentValues v){return null;}
    @Override public int delete(Uri uri,String s,String[] a){return 0;}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){return 0;}

    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        return ParcelFileDescriptor.open(resolveFile(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File resolveFile(Uri uri)throws FileNotFoundException{
        if(getContext()==null)throw new FileNotFoundException();
        String name=uri.getLastPathSegment();if(name==null)throw new FileNotFoundException();
        File dir=new File(getContext().getFilesDir(),"attachments");File file=new File(dir,name);
        try{
            String base=dir.getCanonicalPath(),path=file.getCanonicalPath();
            if(!path.startsWith(base+File.separator)||!file.exists())throw new FileNotFoundException();
        }catch(FileNotFoundException ex){throw ex;}
        catch(Exception ex){throw new FileNotFoundException();}
        return file;
    }
}
