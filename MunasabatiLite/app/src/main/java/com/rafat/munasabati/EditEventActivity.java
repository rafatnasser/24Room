package com.rafat.munasabati;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditEventActivity extends Activity {
    private static final int PICK_FILE=41, LOC=42;
    private EditText title, details, locationName, locationUrl;
    private Button dateBtn, timeBtn, attachBtn;
    private TextView hijriPreview;
    private Spinner reminder, category, recurrence;
    private long eventTime, id;
    private String attachmentUri="", attachmentName="", attachmentType="";
    private final int[] reminderValues={0,10,30,60,180,1440,2880};
    private final int primary=Color.rgb(25,91,86), accent=Color.rgb(208,151,56), bg=Color.rgb(246,248,251), muted=Color.rgb(91,101,115);

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(primary);eventTime=System.currentTimeMillis()+3600000L;id=getIntent().getLongExtra("event_id",0L);buildUi();loadExisting();refreshDateTime();}
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);} private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(bg);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(24));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(root);
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(14),dp(18),dp(14));hero.setBackground(round(primary,22));root.addView(hero,withBottom(-1,-2,dp(12)));
        TextView head=label(id==0?tr("إضافة مناسبة","Add event"):tr("تعديل المناسبة","Edit event"),25,true);head.setTextColor(Color.WHITE);head.setPadding(0,0,0,0);hero.addView(head);
        TextView sub=label(tr("أضف التفاصيل والتاريخ والموقع والتذكير","Add details, dates, location and reminder"),13,false);sub.setTextColor(Color.rgb(224,239,237));sub.setPadding(0,dp(3),0,0);hero.addView(sub);

        LinearLayout basic=section(root,tr("تفاصيل المناسبة","Event details"));
        title=input(tr("اسم المناسبة *","Event name *"));basic.addView(title);
        basic.addView(fieldLabel(tr("الفئة","Category")));category=new Spinner(this);category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));basic.addView(category,new LinearLayout.LayoutParams(-1,dp(50)));
        details=input(tr("ملاحظات إضافية","Additional notes"));details.setMinLines(3);basic.addView(details);

        LinearLayout when=section(root,tr("التاريخ والوقت","Date & time"));
        LinearLayout row=new LinearLayout(this);dateBtn=secondaryButton("");timeBtn=secondaryButton("");row.addView(dateBtn,new LinearLayout.LayoutParams(0,dp(52),1));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(52),1);tp.setMargins(dp(6),0,0,0);row.addView(timeBtn,tp);when.addView(row);
        dateBtn.setOnClickListener(v->pickDate());timeBtn.setOnClickListener(v->pickTime());
        hijriPreview=label("",15,true);hijriPreview.setTextColor(accent);hijriPreview.setBackground(round(Color.rgb(255,249,235),14));hijriPreview.setPadding(dp(12),dp(10),dp(12),dp(10));LinearLayout.LayoutParams hpp=new LinearLayout.LayoutParams(-1,-2);hpp.setMargins(0,dp(8),0,0);when.addView(hijriPreview,hpp);
        TextView hint=label(tr("يعتمد التاريخ الهجري على تعديل الأيام المحفوظ في الإعدادات.","Hijri date uses the day adjustment saved in Settings."),12,false);hint.setTextColor(muted);when.addView(hint);
        when.addView(fieldLabel(tr("التذكير قبل المناسبة","Reminder before event")));reminder=new Spinner(this);reminder.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,reminderLabels()));reminder.setSelection(2);when.addView(reminder,new LinearLayout.LayoutParams(-1,dp(50)));
        when.addView(fieldLabel(tr("تكرار المناسبة","Repeat event")));recurrence=new Spinner(this);recurrence.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Recurrence.labels(this)));when.addView(recurrence,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView repeatHint=label(tr("«عادة أسبوعية» تضبط أسبوعيًا و«سنوية» تضبط سنويًا تلقائيًا، ويمكنك تغيير الاختيار.","“Weekly habit” defaults to weekly and “Anniversary” defaults to yearly; you can change it."),12,false);repeatHint.setTextColor(muted);when.addView(repeatHint);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long rowId){String auto=Recurrence.autoForCategory(Categories.CODES[pos]);if(!Recurrence.NONE.equals(auto))recurrence.setSelection(Recurrence.indexOf(auto));}public void onNothingSelected(android.widget.AdapterView<?> p){}});

        LinearLayout attachment=section(root,tr("المرفق","Attachment"));attachBtn=secondaryButton(tr("📎  إضافة صورة أو PDF","📎  Add image or PDF"));attachment.addView(attachBtn,new LinearLayout.LayoutParams(-1,dp(50)));attachBtn.setOnClickListener(v->pickFile());
        Button removeAttachment=linkButton(tr("إزالة المرفق","Remove attachment"));attachment.addView(removeAttachment);removeAttachment.setOnClickListener(v->{attachmentUri="";attachmentName="";attachmentType="";attachBtn.setText(tr("📎  إضافة صورة أو PDF","📎  Add image or PDF"));});

        LinearLayout location=section(root,tr("موقع الحدث","Event location"));locationName=input(tr("اسم المكان","Place name"));locationUrl=input(tr("رابط Google Maps أو الموقع","Google Maps or location link"));location.addView(locationName);location.addView(locationUrl);Button current=secondaryButton(tr("📍  استخدام موقعي الحالي","📍  Use my current location"));location.addView(current,new LinearLayout.LayoutParams(-1,dp(50)));current.setOnClickListener(v->useLocation());

        Button save=new Button(this);save.setAllCaps(false);save.setText(tr("حفظ المناسبة","Save event"));save.setTextColor(Color.WHITE);save.setTextSize(17);save.setTypeface(null,Typeface.BOLD);save.setBackground(round(primary,16));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(56));sp.setMargins(0,dp(4),0,dp(6));root.addView(save,sp);save.setOnClickListener(v->save());
        Button cancel=secondaryButton(tr("إلغاء","Cancel"));root.addView(cancel,new LinearLayout.LayoutParams(-1,dp(50)));cancel.setOnClickListener(v->finish());setContentView(scroll);
    }

    private String[] reminderLabels(){return ar()?new String[]{"وقت المناسبة","قبل 10 دقائق","قبل 30 دقيقة","قبل ساعة","قبل 3 ساعات","قبل يوم","قبل يومين"}:new String[]{"At event time","10 minutes before","30 minutes before","1 hour before","3 hours before","1 day before","2 days before"};}
    private LinearLayout section(LinearLayout root,String title){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(10),dp(14),dp(13));box.setBackground(round(Color.WHITE,20));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));root.addView(box,p);TextView t=label(title,18,true);t.setTextColor(primary);t.setPadding(0,0,0,dp(5));box.addView(t);return box;}
    private TextView fieldLabel(String x){TextView t=label(x,14,true);t.setTextColor(Color.rgb(55,65,81));t.setPadding(0,dp(8),0,dp(2));return t;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setTextColor(Color.rgb(31,42,55));e.setHintTextColor(Color.rgb(140,148,158));e.setPadding(dp(12),dp(10),dp(12),dp(10));e.setBackground(round(Color.rgb(246,248,250),13));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));e.setLayoutParams(p);return e;}
    private TextView label(String x,int z,boolean b){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(Color.rgb(31,42,55));if(b)t.setTypeface(null,Typeface.BOLD);return t;}
    private Button secondaryButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(14);b.setTextColor(primary);GradientDrawable g=round(Color.rgb(241,247,246),13);g.setStroke(dp(1),Color.rgb(214,226,223));b.setBackground(g);return b;}
    private Button linkButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(12);b.setTextColor(muted);b.setBackgroundColor(Color.TRANSPARENT);b.setGravity(Gravity.START);return b;}
    private LinearLayout.LayoutParams withBottom(int w,int h,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,0,0,bottom);return p;}

    private void loadExisting(){if(id==0)return;EventStore.Event e=EventStore.find(this,id);if(e==null)return;title.setText(e.title);details.setText(e.details);eventTime=Recurrence.isRecurring(e)?Recurrence.nextOccurrence(e,System.currentTimeMillis()):e.eventTime;category.setSelection(Categories.indexOf(e.category));recurrence.setSelection(Recurrence.indexOf(e.recurrence));attachmentUri=e.attachmentUri;attachmentName=e.attachmentName;attachmentType=e.attachmentType;attachBtn.setText(attachmentName.isEmpty()?tr("📎  إضافة صورة أو PDF","📎  Add image or PDF"):"📎  "+attachmentName);locationName.setText(e.locationName);locationUrl.setText(e.locationUrl);for(int i=0;i<reminderValues.length;i++)if(reminderValues[i]==e.reminderMinutes)reminder.setSelection(i);}
    private void pickDate(){Calendar c=Calendar.getInstance();c.setTimeInMillis(eventTime);new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.setTimeInMillis(eventTime);n.set(y,m,d);eventTime=n.getTimeInMillis();refreshDateTime();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private void pickTime(){Calendar c=Calendar.getInstance();c.setTimeInMillis(eventTime);new TimePickerDialog(this,(v,h,m)->{Calendar n=Calendar.getInstance();n.setTimeInMillis(eventTime);n.set(Calendar.HOUR_OF_DAY,h);n.set(Calendar.MINUTE,m);n.set(Calendar.SECOND,0);n.set(Calendar.MILLISECOND,0);eventTime=n.getTimeInMillis();refreshDateTime();},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();}
    private void refreshDateTime(){if(dateBtn==null)return;dateBtn.setText("📅  "+DateTools.gregorianShort(this,eventTime));timeBtn.setText("🕒  "+DateTools.time(this,eventTime));int o=AppSettings.hijriOffset(this);String suffix=o==0?tr("بدون تعديل","no adjustment"):(o>0?"+"+o:String.valueOf(o))+tr(" يوم"," day");hijriPreview.setText("☾  "+DateTools.hijri(this,eventTime)+"  •  "+suffix);}
    private void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"});startActivityForResult(i,PICK_FILE);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==PICK_FILE&&c==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}attachmentUri=u.toString();attachmentType=getContentResolver().getType(u);attachmentName=queryName(u);attachBtn.setText("📎  "+attachmentName);}}
    private String queryName(Uri u){try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}return tr("مرفق","Attachment");}

    private void useLocation(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOC);return;}fillLocation();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==LOC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)fillLocation();}
    @SuppressWarnings("MissingPermission") private void fillLocation(){LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);Location best=null;for(String p:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){try{Location x=lm.getLastKnownLocation(p);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}catch(Exception ignored){}}if(best==null){Toast.makeText(this,tr("تعذر تحديد الموقع الحالي. فعّل الموقع وحاول مرة أخرى.","Could not determine your current location. Enable location and try again."),Toast.LENGTH_LONG).show();return;}if(locationName.getText().toString().trim().isEmpty())locationName.setText(tr("الموقع الحالي","Current location"));locationUrl.setText("geo:"+best.getLatitude()+","+best.getLongitude()+"?q="+best.getLatitude()+","+best.getLongitude());}

    private void save(){String n=title.getText().toString().trim();if(n.isEmpty()){title.setError(tr("مطلوب","Required"));return;}List<EventStore.Event> all=new ArrayList<>(EventStore.load(this));EventStore.Event e=id==0?new EventStore.Event():EventStore.find(this,id);if(e==null)e=new EventStore.Event();if(id==0)e.id=System.currentTimeMillis();else ReminderScheduler.cancel(this,id);e.title=n;e.category=Categories.CODES[category.getSelectedItemPosition()];e.recurrence=Recurrence.CODES[recurrence.getSelectedItemPosition()];e.details=details.getText().toString().trim();e.eventTime=eventTime;e.reminderMinutes=reminderValues[reminder.getSelectedItemPosition()];e.attachmentUri=attachmentUri;e.attachmentName=attachmentName;e.attachmentType=attachmentType==null?"":attachmentType;e.locationName=locationName.getText().toString().trim();e.locationUrl=locationUrl.getText().toString().trim();long finalId=e.id;all.removeIf(x->x.id==finalId);all.add(e);EventStore.save(this,all);ReminderScheduler.schedule(this,e);Toast.makeText(this,tr("تم حفظ المناسبة","Event saved"),Toast.LENGTH_SHORT).show();finish();}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
