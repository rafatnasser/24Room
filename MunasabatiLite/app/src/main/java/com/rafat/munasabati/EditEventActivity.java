package com.rafat.munasabati;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditEventActivity extends Activity {
    private static final int PICK_FILE=41, LOC=42;
    private EditText title,details,locationName,locationUrl; private Button dateBtn,timeBtn,attachBtn; private Spinner reminder;
    private long eventTime,id; private String attachmentUri="",attachmentName="",attachmentType="";
    private final int[] reminderValues={0,10,30,60,180,1440,2880};
    @Override public void onCreate(Bundle b){super.onCreate(b); setTitle("مناسباتي"); eventTime=System.currentTimeMillis()+3600000L; id=getIntent().getLongExtra("event_id",0L); buildUi(); loadExisting(); refreshDateTime();}
    private void buildUi(){
        ScrollView s=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(14),dp(16),dp(20)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); s.addView(root);
        root.addView(label(id==0?"إضافة مناسبة":"تعديل المناسبة",26,true)); title=input("اسم المناسبة *"); root.addView(title); details=input("ملاحظات"); details.setMinLines(3); root.addView(details);
        root.addView(label("التاريخ والوقت",17,true)); LinearLayout row=new LinearLayout(this); dateBtn=new Button(this); timeBtn=new Button(this); row.addView(dateBtn,new LinearLayout.LayoutParams(0,dp(50),1)); row.addView(timeBtn,new LinearLayout.LayoutParams(0,dp(50),1)); root.addView(row); dateBtn.setOnClickListener(v->pickDate()); timeBtn.setOnClickListener(v->pickTime());
        root.addView(label("التذكير قبل المناسبة",17,true)); reminder=new Spinner(this); String[] labels={"وقت المناسبة","قبل 10 دقائق","قبل 30 دقيقة","قبل ساعة","قبل 3 ساعات","قبل يوم","قبل يومين"}; reminder.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels)); reminder.setSelection(2); root.addView(reminder);
        root.addView(label("المرفق",17,true)); attachBtn=new Button(this); attachBtn.setText("إضافة صورة أو PDF"); root.addView(attachBtn); attachBtn.setOnClickListener(v->pickFile());
        root.addView(label("موقع الحدث",17,true)); locationName=input("اسم المكان"); locationUrl=input("رابط Google Maps أو رابط الموقع"); root.addView(locationName); root.addView(locationUrl); Button current=new Button(this); current.setText("استخدام موقعي الحالي"); root.addView(current); current.setOnClickListener(v->useLocation());
        Button save=new Button(this); save.setText("حفظ المناسبة"); root.addView(save,new LinearLayout.LayoutParams(-1,dp(56))); save.setOnClickListener(v->save()); Button cancel=new Button(this); cancel.setText("إلغاء"); root.addView(cancel); cancel.setOnClickListener(v->finish()); setContentView(s);
    }
    private EditText input(String hint){EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); e.setPadding(dp(10),dp(10),dp(10),dp(10)); e.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); return e;}
    private TextView label(String x,int z,boolean b){TextView t=new TextView(this); t.setText(x); t.setTextSize(z); t.setPadding(0,dp(10),0,dp(4)); if(b)t.setTypeface(null,1); return t;}
    private void loadExisting(){if(id==0)return; EventStore.Event e=EventStore.find(this,id); if(e==null)return; title.setText(e.title); details.setText(e.details); eventTime=e.eventTime; attachmentUri=e.attachmentUri; attachmentName=e.attachmentName; attachmentType=e.attachmentType; attachBtn.setText(attachmentName.isEmpty()?"إضافة صورة أو PDF":attachmentName); locationName.setText(e.locationName); locationUrl.setText(e.locationUrl); for(int i=0;i<reminderValues.length;i++)if(reminderValues[i]==e.reminderMinutes)reminder.setSelection(i);}
    private void pickDate(){Calendar c=Calendar.getInstance();c.setTimeInMillis(eventTime);new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.setTimeInMillis(eventTime);n.set(y,m,d);eventTime=n.getTimeInMillis();refreshDateTime();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private void pickTime(){Calendar c=Calendar.getInstance();c.setTimeInMillis(eventTime);new TimePickerDialog(this,(v,h,m)->{Calendar n=Calendar.getInstance();n.setTimeInMillis(eventTime);n.set(Calendar.HOUR_OF_DAY,h);n.set(Calendar.MINUTE,m);n.set(Calendar.SECOND,0);eventTime=n.getTimeInMillis();refreshDateTime();},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();}
    private void refreshDateTime(){dateBtn.setText(new SimpleDateFormat("d MMM yyyy",new Locale("ar")).format(new Date(eventTime)));timeBtn.setText(new SimpleDateFormat("h:mm a",new Locale("ar")).format(new Date(eventTime)));}
    private void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"}); startActivityForResult(i,PICK_FILE);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data); if(r==PICK_FILE&&c==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);attachmentUri=u.toString();attachmentType=getContentResolver().getType(u);attachmentName=queryName(u);attachBtn.setText(attachmentName);}}
    private String queryName(Uri u){try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}return "مرفق";}
    private void useLocation(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOC);return;} fillLocation();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g); if(r==LOC&&(g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED))fillLocation();}
    @SuppressWarnings("MissingPermission") private void fillLocation(){LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE); Location best=null; for(String p:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){try{Location x=lm.getLastKnownLocation(p);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}catch(Exception ignored){}} if(best==null){Toast.makeText(this,"تعذر تحديد الموقع الحالي. فعّل الموقع وحاول مرة أخرى.",Toast.LENGTH_LONG).show();return;} if(locationName.getText().toString().trim().isEmpty())locationName.setText("الموقع الحالي");locationUrl.setText("geo:"+best.getLatitude()+","+best.getLongitude()+"?q="+best.getLatitude()+","+best.getLongitude());}
    private void save(){String n=title.getText().toString().trim();if(n.isEmpty()){title.setError("مطلوب");return;}if(eventTime<=System.currentTimeMillis()){Toast.makeText(this,"اختر وقتًا مستقبليًا للمناسبة",Toast.LENGTH_SHORT).show();return;}List<EventStore.Event> all=new ArrayList<>(EventStore.load(this));EventStore.Event e=id==0?new EventStore.Event():EventStore.find(this,id);if(e==null)e=new EventStore.Event();if(id==0)e.id=System.currentTimeMillis();else ReminderScheduler.cancel(this,id);e.title=n;e.details=details.getText().toString().trim();e.eventTime=eventTime;e.reminderMinutes=reminderValues[reminder.getSelectedItemPosition()];e.attachmentUri=attachmentUri;e.attachmentName=attachmentName;e.attachmentType=attachmentType==null?"":attachmentType;e.locationName=locationName.getText().toString().trim();e.locationUrl=locationUrl.getText().toString().trim();long finalId=e.id;all.removeIf(x->x.id==finalId);all.add(e);EventStore.save(this,all);ReminderScheduler.schedule(this,e);Toast.makeText(this,"تم حفظ المناسبة",Toast.LENGTH_SHORT).show();finish();}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
