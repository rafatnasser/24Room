package com.rafat.munasabati;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public final class AutoBackupManager {
    private static final String PREFIX="Munasabati_Auto_";

    public static int run(Context c)throws Exception{
        String raw=AppSettings.autoBackupTree(c);if(raw.isEmpty())throw new IllegalStateException("No backup folder");
        Uri tree=Uri.parse(raw);ContentResolver r=c.getContentResolver();
        String treeId=DocumentsContract.getTreeDocumentId(tree);
        Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,treeId);
        String name=PREFIX+new SimpleDateFormat("yyyy-MM-dd_HH-mm",Locale.US).format(new Date())+".munasabati";
        Uri file=DocumentsContract.createDocument(r,parent,"application/zip",name);
        if(file==null)throw new IllegalStateException("Unable to create backup");
        int attachments;
        try(OutputStream out=r.openOutputStream(file)){if(out==null)throw new IllegalStateException("Unable to open backup");attachments=BackupManager.createBackup(c,out);}
        prune(c,tree,AppSettings.autoBackupKeep(c));return attachments;
    }

    private static void prune(Context c,Uri tree,int keep)throws Exception{
        ContentResolver r=c.getContentResolver();String id=DocumentsContract.getTreeDocumentId(tree);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);
        ArrayList<Item> items=new ArrayList<>();
        String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        try(Cursor cur=r.query(children,cols,null,null,null)){
            if(cur!=null)while(cur.moveToNext()){
                String docId=cur.getString(0),name=cur.getString(1);long modified=cur.getLong(2);
                if(name!=null&&name.startsWith(PREFIX))items.add(new Item(docId,modified));
            }
        }
        items.sort((a,b)->Long.compare(b.modified,a.modified));
        for(int i=Math.max(keep,1);i<items.size();i++){
            Uri doc=DocumentsContract.buildDocumentUriUsingTree(tree,items.get(i).id);
            try{DocumentsContract.deleteDocument(r,doc);}catch(Exception ignored){}
        }
    }
    private static final class Item{String id;long modified;Item(String i,long m){id=i;modified=m;}}
    private AutoBackupManager(){}
}
