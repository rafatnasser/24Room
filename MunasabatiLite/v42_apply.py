from pathlib import Path

def patch(path, old, new, required=True, all_occurrences=False):
    p=Path(path); s=p.read_text(encoding='utf-8')
    if old not in s:
        if required: raise SystemExit(f'v4.2 patch target not found: {path}: {old[:120]!r}')
        print('skip:',path,old[:60]); return
    s=s.replace(old,new) if all_occurrences else s.replace(old,new,1)
    p.write_text(s,encoding='utf-8')

# ---------- Main screen: dynamic theme, calmer surfaces, hero summary, modern center ----------
main='app/src/main/java/com/rafat/munasabati/MainActivity.java'
patch(main,
'    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115),ink=Color.rgb(31,42,55);',
'    private int primary,bg,muted,ink; private final int accent=Color.rgb(208,151,56);')
patch(main,
'        super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();',
'        super.onCreate(b);V4Theme.apply(this);primary=V4Theme.primary(this);bg=ModernUi.screenBackground(this);muted=ModernUi.muted(this);ink=ModernUi.ink(this);getWindow().setStatusBarColor(primary);buildUi();')
patch(main,'new Intent(this,V4CenterActivity.class)','new Intent(this,ModernCenterActivity.class)',required=False,all_occurrences=True)
patch(main,
'hero.setBackground(round(primary,25));root.addView(hero,margin(-1,-2,0,0,0,dp(9)));',
'hero.setBackground(round(primary,27));hero.setElevation(dp(5));root.addView(hero,margin(-1,-2,0,0,0,dp(10)));')
patch(main,
'Button settings=heroIcon("⚙");top.addView(settings,new LinearLayout.LayoutParams(dp(46),dp(44)));settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));',
'Button settings=heroIcon("⚙");top.addView(settings,new LinearLayout.LayoutParams(dp(46),dp(44)));settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));addHeroMetrics(hero);')
patch(main,
'modes.setBackground(round(Color.WHITE,18));modes.setPadding(dp(4),dp(4),dp(4),dp(4));',
'modes.setBackground(round(ModernUi.surface(this),20));modes.setElevation(dp(2));modes.setPadding(dp(4),dp(4),dp(4),dp(4));')
patch(main,
'searchCard.setBackground(round(Color.WHITE,18));root.addView(searchCard,margin(-1,-2,0,0,0,dp(7)));',
'searchCard.setBackground(round(ModernUi.surface(this),20));searchCard.setElevation(dp(2));root.addView(searchCard,margin(-1,-2,0,0,0,dp(8)));')
patch(main,'search.setBackground(round(Color.rgb(247,249,250),13));','search.setBackground(ModernUi.fieldBackground(this,primary));')
patch(main,
'hero.setAlpha(0f);hero.setTranslationY(dp(-8));hero.animate().alpha(1f).translationY(0f).setDuration(320).start();',
'if(UiFeedback.motionEnabled(this)){hero.setAlpha(0f);hero.setTranslationY(dp(-8));hero.animate().alpha(1f).translationY(0f).setDuration(280).start();}',required=False)
patch(main,
'    private Button modeButton(String s){',
'''    private void addHeroMetrics(LinearLayout hero){\n        Calendar start=Calendar.getInstance();zeroTime(start);Calendar tomorrow=(Calendar)start.clone();tomorrow.add(Calendar.DAY_OF_YEAR,1);Calendar week=(Calendar)start.clone();week.add(Calendar.DAY_OF_YEAR,7);int today=0,next7=0;for(EventStore.Event e:EventStore.load(this)){if(Recurrence.firstOccurrenceBetween(e,start.getTimeInMillis(),tomorrow.getTimeInMillis())>=0)today++;if(Recurrence.firstOccurrenceBetween(e,start.getTimeInMillis(),week.getTimeInMillis())>=0)next7++;}\n        LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(10),0,0);hero.addView(row);TextView a=metricChip("◷  "+tr("اليوم ","Today ")+today);row.addView(a,new LinearLayout.LayoutParams(0,dp(35),1));TextView b=metricChip("↗  "+tr("7 أيام ","7 days ")+next7);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(35),1);bp.setMargins(dp(7),0,0,0);row.addView(b,bp);\n    }\n    private TextView metricChip(String s){TextView t=text(s,12,true);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setBackground(round(Color.argb(25,255,255,255),12));return t;}\n\n    private Button modeButton(String s){''')
patch(main,
'    private void setMode(int m){mode=m;updateModeButtons();contentHost.animate().alpha(.72f).setDuration(80).withEndAction(this::render).start();}',
'    private void setMode(int m){mode=m;updateModeButtons();if(UiFeedback.motionEnabled(this))contentHost.animate().alpha(.72f).setDuration(80).withEndAction(this::render).start();else render();}')
patch(main,
'contentHost.setAlpha(.82f);contentHost.setTranslationY(dp(4));contentHost.animate().alpha(1f).translationY(0f).setDuration(180).start();',
'if(UiFeedback.motionEnabled(this)){contentHost.setAlpha(.82f);contentHost.setTranslationY(dp(4));contentHost.animate().alpha(1f).translationY(0f).setDuration(180).start();}else{contentHost.setAlpha(1f);contentHost.setTranslationY(0f);}')
patch(main,
'outer.setBackground(round(soft,21));contentHost.addView(outer,margin(-1,-2,0,0,0,dp(9)));',
'outer.setBackground(round(soft,23));outer.setElevation(dp(2));contentHost.addView(outer,margin(-1,-2,0,0,0,dp(10)));',required=False,all_occurrences=True)
patch(main,
'cell.setBackground(round(today?Color.rgb(229,242,240):Color.WHITE,12));',
'cell.setBackground(round(today?Color.rgb(229,242,240):ModernUi.surface(this),14));',required=False)
patch(main,
'dayText.setTextColor(today?primary:Color.rgb(45,55,68));',
'dayText.setTextColor(today?primary:ModernUi.ink(this));',required=False)
patch(main,
'private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.BLACK);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}',
'private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(ModernUi.ink(this));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}',required=False)

# ---------- Event editor: modern surfaces, dynamic theme, softer cards ----------
edit='app/src/main/java/com/rafat/munasabati/EditEventActivity.java'
patch(edit,
'    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115),ink=Color.rgb(31,42,55);',
'    private int primary,bg,muted,ink; private final int accent=Color.rgb(208,151,56);')
patch(edit,
'        super.onCreate(b);getWindow().setStatusBarColor(primary);eventTime=',
'        super.onCreate(b);V4Theme.apply(this);primary=V4Theme.primary(this);bg=ModernUi.screenBackground(this);muted=ModernUi.muted(this);ink=ModernUi.ink(this);getWindow().setStatusBarColor(primary);eventTime=')
patch(edit,
'hero.setBackground(round(primary,26));root.addView(hero,margin(-1,-2,0,0,0,dp(12)));',
'hero.setBackground(round(primary,28));hero.setElevation(dp(5));root.addView(hero,margin(-1,-2,0,0,0,dp(13)));')
patch(edit,
'c.setBackground(round(Color.WHITE,21));root.addView(c,margin(-1,-2,0,0,0,dp(10)));',
'c.setBackground(round(ModernUi.surface(this),23));ModernUi.modernCard(c);root.addView(c,margin(-1,-2,0,0,0,dp(11)));')
patch(edit,'line.setBackgroundColor(Color.rgb(239,242,245));','line.setBackgroundColor(ModernUi.alpha(primary,20));')
patch(edit,'e.setBackground(round(Color.rgb(247,249,250),13));','e.setBackground(ModernUi.fieldBackground(this,primary));')
patch(edit,'c.setBackground(round(Color.rgb(247,249,250),13));return c;}','c.setBackground(ModernUi.fieldBackground(this,primary));return c;}',required=False)
patch(edit,'GradientDrawable g=round(Color.WHITE,14);g.setStroke(dp(1),Color.rgb(211,222,220));','GradientDrawable g=round(ModernUi.surface(this),15);g.setStroke(dp(1),ModernUi.alpha(primary,55));')
patch(edit,
'private void animateSections(){for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0f).setDuration(260).setStartDelay(i*45L).start();}}',
'private void animateSections(){if(!UiFeedback.motionEnabled(this))return;for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0f).setDuration(240).setStartDelay(i*38L).start();}}')

# ---------- Settings: credits, rights, sound/haptic controls, dynamic theme ----------
settings='app/src/main/java/com/rafat/munasabati/SettingsActivity.java'
patch(settings,
'    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115);',
'    private int primary,bg,muted; private final int accent=Color.rgb(208,151,56);')
patch(settings,
'    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();}',
'    @Override public void onCreate(Bundle b){super.onCreate(b);V4Theme.apply(this);primary=V4Theme.primary(this);bg=ModernUi.screenBackground(this);muted=ModernUi.muted(this);getWindow().setStatusBarColor(primary);buildUi();}')
patch(settings,
'        addAppearanceCard();addAhlBaytCalendarCard();addCalendarCard();addNotificationCard();addBackupCard();addAboutCard();',
'        addAppearanceCard();addInteractionCard();addAhlBaytCalendarCard();addCalendarCard();addNotificationCard();addBackupCard();addAboutCard();')
patch(settings,
'    private void addAhlBaytCalendarCard(){',
'''    private void addInteractionCard(){\n        LinearLayout card=card(tr("الصوت واللمس والحركة","Sound, touch & motion"),"◉",tr("تفاعل عصري هادئ يمكنك تخصيصه","Calm modern micro-interactions you can customize"));\n        Switch sound=new Switch(this);sound.setText(tr("♪ أصوات الواجهة والأيقونات","♪ Interface & icon sounds"));sound.setTextSize(14);sound.setChecked(UiFeedback.soundEnabled(this));card.addView(sound,new LinearLayout.LayoutParams(-1,dp(50)));sound.setOnCheckedChangeListener((b,v)->UiFeedback.setSoundEnabled(this,v));\n        Switch haptic=new Switch(this);haptic.setText(tr("⌁ اهتزاز لمسي خفيف عند الضغط","⌁ Light haptic feedback on tap"));haptic.setTextSize(14);haptic.setChecked(UiFeedback.hapticEnabled(this));card.addView(haptic,new LinearLayout.LayoutParams(-1,dp(50)));haptic.setOnCheckedChangeListener((b,v)->UiFeedback.setHapticEnabled(this,v));\n        Switch motion=new Switch(this);motion.setText(tr("✦ حركات وانتقالات ناعمة","✦ Smooth motion & transitions"));motion.setTextSize(14);motion.setChecked(UiFeedback.motionEnabled(this));card.addView(motion,new LinearLayout.LayoutParams(-1,dp(50)));motion.setOnCheckedChangeListener((b,v)->UiFeedback.setMotionEnabled(this,v));\n        Button preview=outlineButton(tr("تشغيل تجربة قصيرة للصوت واللمس","Preview sound & haptic"));preview.setTag("preview_feedback");card.addView(preview,margin(-1,dp(48),0,dp(7),0,0));preview.setOnClickListener(v->toast(tr("هذه هي استجابة الواجهة الجديدة","This is the new interface feedback")));\n        TextView note=text(tr("تحترم الأصوات وضع الصامت في الهاتف. يمكنك إيقاف أي نوع من التفاعل بشكل مستقل.","Interface sounds respect your phone's silent mode. Each interaction type can be disabled independently."),11,false);note.setTextColor(muted);note.setPadding(0,dp(6),0,0);card.addView(note);\n    }\n\n    private void addAhlBaytCalendarCard(){''')
patch(settings,
'info.setTextColor(muted);info.setPadding(0,dp(5),0,0);card.addView(info);',
'''info.setTextColor(muted);info.setPadding(0,dp(5),0,dp(12));card.addView(info);\n        LinearLayout credit=new LinearLayout(this);credit.setOrientation(LinearLayout.VERTICAL);credit.setPadding(dp(14),dp(12),dp(14),dp(12));credit.setBackground(round(Color.rgb(255,248,229),16));card.addView(credit);\n        TextView role=text(tr("تصميم وتطوير البرنامج","Designed & developed by"),12,true);role.setTextColor(Color.rgb(126,88,18));credit.addView(role);\n        TextView designer=text("رأفت ناصر الناصر  •  Rafat Nasser Alnasser",15,true);designer.setTextColor(Color.rgb(71,55,25));designer.setPadding(0,dp(3),0,dp(6));credit.addView(designer);\n        TextView rights=text(tr("© 2026 رأفت ناصر الناصر — جميع حقوق البرنامج محفوظة.","© 2026 Rafat Nasser Alnasser — All rights reserved."),11,false);rights.setTextColor(Color.rgb(112,91,54));credit.addView(rights);''')
patch(settings,
'c.setBackground(round(Color.WHITE,21));root.addView(c,margin(-1,-2,0,0,0,dp(10)));',
'c.setBackground(round(ModernUi.surface(this),23));ModernUi.modernCard(c);root.addView(c,margin(-1,-2,0,0,0,dp(11)));')
patch(settings,'line.setBackgroundColor(Color.rgb(239,242,245));','line.setBackgroundColor(ModernUi.alpha(primary,20));')
patch(settings,
'private void animateCards(){for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0f).setDuration(260).setStartDelay(i*55L).start();}}',
'private void animateCards(){if(!UiFeedback.motionEnabled(this))return;for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0f).setDuration(235).setStartDelay(i*40L).start();}}')
patch(settings,
'GradientDrawable d=round(Color.WHITE,14);d.setStroke(dp(1),Color.rgb(211,222,220));',
'GradientDrawable d=round(ModernUi.surface(this),15);d.setStroke(dp(1),ModernUi.alpha(primary,55));')
patch(settings,
't.setTextColor(Color.rgb(31,42,55));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}',
't.setTextColor(ModernUi.ink(this));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}',required=False)

# ---------- Sync center: keep service logic but align visual language ----------
sync='app/src/main/java/com/rafat/munasabati/SyncCenterActivity.java'
patch(sync,
'    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115);',
'    private int primary,bg,muted; private final int accent=Color.rgb(208,151,56);',required=False)
patch(sync,
'    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();showSkeleton();body.postDelayed(this::renderBody,180);}',
'    @Override public void onCreate(Bundle b){super.onCreate(b);V4Theme.apply(this);primary=V4Theme.primary(this);bg=ModernUi.screenBackground(this);muted=ModernUi.muted(this);getWindow().setStatusBarColor(primary);buildUi();showSkeleton();body.postDelayed(this::renderBody,180);}',required=False)
patch(sync,
'hero.setBackground(round(primary,26));root.addView(hero,margin(-1,-2,0,0,0,dp(12)));',
'hero.setBackground(round(primary,28));hero.setElevation(dp(5));root.addView(hero,margin(-1,-2,0,0,0,dp(13)));',required=False)
patch(sync,
'hero.setAlpha(0f);hero.setTranslationY(dp(-8));hero.animate().alpha(1f).translationY(0f).setDuration(320).start();',
'if(UiFeedback.motionEnabled(this)){hero.setAlpha(0f);hero.setTranslationY(dp(-8));hero.animate().alpha(1f).translationY(0f).setDuration(280).start();}',required=False)
patch(sync,
'for(int i=0;i<body.getChildCount();i++){View v=body.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(10));v.animate().alpha(1f).translationY(0f).setDuration(240).setStartDelay(i*55L).start();}',
'if(UiFeedback.motionEnabled(this))for(int i=0;i<body.getChildCount();i++){View v=body.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(10));v.animate().alpha(1f).translationY(0f).setDuration(220).setStartDelay(i*42L).start();}',required=False)
patch(sync,'c.setBackground(round(Color.WHITE,21));rootCard(c);','c.setBackground(round(ModernUi.surface(this),23));ModernUi.modernCard(c);rootCard(c);',required=False)
patch(sync,'card.setBackground(round(Color.WHITE,20));body.addView(card,margin(-1,dp(112),0,0,0,dp(9)));','card.setBackground(round(ModernUi.surface(this),22));ModernUi.modernCard(card);body.addView(card,margin(-1,dp(112),0,0,0,dp(9)));',required=False)

print('v4.2 modern UI patches applied')
