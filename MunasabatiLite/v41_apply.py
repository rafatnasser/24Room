from pathlib import Path

def patch(path, old, new, required=True):
    p=Path(path); s=p.read_text(encoding='utf-8')
    if old not in s:
        if required: raise SystemExit(f'v4.1 patch target not found: {path}: {old[:100]!r}')
        return
    p.write_text(s.replace(old,new),encoding='utf-8')

main='app/src/main/java/com/rafat/munasabati/MainActivity.java'

# Selected calendar day: show built-in Ahl al-Bayt occasions as read-only premium cards.
patch(main,
'boolean any=false,up=false,past=false;for(EventStore.Event e:events)',
'boolean any=false,up=false,past=false;if(selectedDate>0){Calendar ab=Calendar.getInstance();ab.setTimeInMillis(selectedDate);zeroTime(ab);Calendar ae=(Calendar)ab.clone();ae.add(Calendar.DAY_OF_YEAR,1);List<AhlBaytCalendar.Occurrence> sacred=ahlBaytBetween(ab.getTimeInMillis(),ae.getTimeInMillis());if(!sacred.isEmpty()){section(tr("✦ مناسبات أهل البيت عليهم السلام","✦ Ahl al-Bayt occasions"));for(AhlBaytCalendar.Occurrence o:sacred)addAhlBaytCard(o);any=true;}}for(EventStore.Event e:events)')

# Calendar month: include Ahl al-Bayt markers in the day cells.
patch(main,
'List<Occurrence> matches=occurrencesOnDay(start,end);LinearLayout cell=',
'List<Occurrence> matches=occurrencesOnDay(start,end);List<AhlBaytCalendar.Occurrence> sacred=ahlBaytBetween(start,end);LinearLayout cell=')
patch(main,
'TextView dayText=text(String.valueOf(day),13,!matches.isEmpty()||today);',
'TextView dayText=text(String.valueOf(day),13,!matches.isEmpty()||!sacred.isEmpty()||today);')
patch(main,
'if(matches.size()>4){TextView plus=text("+",9,true);plus.setTextColor(muted);dots.addView(plus);}grid.addView(cell,cellParams(dp(64)));',
'if(matches.size()>4){TextView plus=text("+",9,true);plus.setTextColor(muted);dots.addView(plus);}for(int si=0;si<Math.min(2,sacred.size());si++){View dot=new View(this);GradientDrawable dg=new GradientDrawable();dg.setShape(GradientDrawable.OVAL);dg.setColor(AhlBaytCalendar.color(sacred.get(si).occasion));dot.setBackground(dg);LinearLayout.LayoutParams dv=new LinearLayout.LayoutParams(dp(8),dp(8));dv.setMargins(dp(2),0,dp(2),0);dots.addView(dot,dv);}grid.addView(cell,cellParams(dp(64)));')
patch(main,
'النقاط الملوّنة تمثل فئات المناسبات. اضغط على اليوم لعرض تفاصيله.',
'النقاط الملوّنة تمثل مناسباتك، والذهبي للمواليد والعنابي للوفيات والاستشهادات. اضغط على اليوم لعرض التفاصيل.')
patch(main,
'Colored dots represent event categories. Tap a day to view details.',
'Colored dots represent your events; gold marks births and burgundy marks deaths/martyrdoms. Tap a day for details.')

# Filtering/search for built-in occasions follows the existing calendar filters.
patch(main,
'    private long displayTime(EventStore.Event e,long now){',
'''    private List<AhlBaytCalendar.Occurrence> ahlBaytBetween(long start,long end){ArrayList<AhlBaytCalendar.Occurrence> out=new ArrayList<>();for(AhlBaytCalendar.Occurrence o:AhlBaytCalendar.occurrencesBetween(this,start,end))if(matchesAhlBayt(o))out.add(o);return out;}\n    private boolean matchesAhlBayt(AhlBaytCalendar.Occurrence o){int cat=categoryFilter==null?0:categoryFilter.getSelectedItemPosition();String code=o.occasion.kind==AhlBaytCalendar.Kind.BIRTH?"mawlid":"martyrdom";if(cat>0&&!Categories.CODES[cat-1].equals(code))return false;int day=dayFilter==null?0:dayFilter.getSelectedItemPosition();if(day>0&&DateTools.dayOfWeek(o.time)!=day)return false;String q=search==null?"":normalize(search.getText().toString());if(q.isEmpty())return true;String hay=AhlBaytCalendar.title(this,o.occasion)+" "+AhlBaytCalendar.kindLabel(this,o.occasion)+" "+AhlBaytCalendar.note(this,o.occasion)+" "+DateTools.gregorian(this,o.time,false)+" "+DateTools.hijri(this,o.time);return normalize(hay).contains(q);}\n    private long displayTime(EventStore.Event e,long now){''')

# Read-only card for built-in Ahl al-Bayt calendar entries.
patch(main,
'    private void openAttachment(EventStore.Event e){',
'''    private void addAhlBaytCard(AhlBaytCalendar.Occurrence o){\n        int base=AhlBaytCalendar.color(o.occasion),soft=ColorPalette.soft(base);LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setBackground(round(soft,21));contentHost.addView(outer,margin(-1,-2,0,0,0,dp(9)));View stripe=new View(this);stripe.setBackgroundColor(base);outer.addView(stripe,new LinearLayout.LayoutParams(dp(6),-1));LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(12));outer.addView(card,new LinearLayout.LayoutParams(0,-2,1));\n        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);TextView icon=text(AhlBaytCalendar.icon(o.occasion),22,false);head.addView(icon,new LinearLayout.LayoutParams(dp(38),-2));LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(AhlBaytCalendar.title(this,o.occasion),17,true);title.setTextColor(ink);titles.addView(title);TextView type=text(AhlBaytCalendar.kindLabel(this,o.occasion),12,true);type.setTextColor(base);titles.addView(type);\n        TextView g=text("📅  "+DateTools.gregorian(this,o.time,false),13,false);g.setTextColor(Color.rgb(55,65,81));g.setPadding(0,dp(7),0,0);card.addView(g);TextView h=text("☾  "+DateTools.hijri(this,o.time),13,true);h.setTextColor(base);h.setPadding(0,dp(2),0,0);card.addView(h);String note=AhlBaytCalendar.note(this,o.occasion);if(!note.isEmpty()){TextView n=text("ⓘ  "+note,12,false);n.setTextColor(muted);n.setPadding(0,dp(6),0,0);card.addView(n);}TextView built=text(tr("مناسبة مدمجة • التحكم في ظهورها من الإعدادات","Built-in occasion • visibility is controlled from Settings"),11,true);built.setTextColor(muted);built.setPadding(0,dp(7),0,0);card.addView(built);\n    }\n\n    private void openAttachment(EventStore.Event e){''')

settings='app/src/main/java/com/rafat/munasabati/SettingsActivity.java'
patch(settings,
'        addAppearanceCard();addCalendarCard();addNotificationCard();addBackupCard();addAboutCard();',
'        addAppearanceCard();addAhlBaytCalendarCard();addCalendarCard();addNotificationCard();addBackupCard();addAboutCard();')
patch(settings,
'    private void addCalendarCard(){',
'''    private void addAhlBaytCalendarCard(){\n        LinearLayout card=card(tr("مناسبات أهل البيت عليهم السلام","Ahl al-Bayt occasions"),"☾",tr("مواليد ووفيات المعصومين الأربعة عشر داخل التقويم الهجري","Birth and death anniversaries of the Fourteen Infallibles in the Hijri calendar"));\n        TextView intro=text(tr("تظهر هذه المناسبات تلقائيًا كل سنة حسب التاريخ الهجري ولا تُضاف إلى بياناتك الشخصية أو المزامنة الخارجية.","These occasions recur automatically by Hijri date and are not added to your personal events or external calendar sync."),12,false);intro.setTextColor(muted);intro.setPadding(0,0,0,dp(8));card.addView(intro);\n        Switch births=new Switch(this);births.setText(tr("✦ إظهار مواليد أهل البيت عليهم السلام","✦ Show Ahl al-Bayt birth anniversaries"));births.setTextSize(14);births.setChecked(AhlBaytCalendar.showBirths(this));card.addView(births,new LinearLayout.LayoutParams(-1,dp(52)));\n        Switch deaths=new Switch(this);deaths.setText(tr("🕯 إظهار الوفيات والاستشهادات","🕯 Show deaths and martyrdom anniversaries"));deaths.setTextSize(14);deaths.setChecked(AhlBaytCalendar.showDeaths(this));card.addView(deaths,new LinearLayout.LayoutParams(-1,dp(52)));\n        births.setOnCheckedChangeListener((b,v)->{AhlBaytCalendar.setShowBirths(this,v);});deaths.setOnCheckedChangeListener((b,v)->{AhlBaytCalendar.setShowDeaths(this,v);});\n        TextView legend=text(tr("● ذهبي: المواليد     ● عنابي: الوفيات والاستشهادات","● Gold: births     ● Burgundy: deaths & martyrdoms"),12,true);legend.setTextColor(primary);legend.setPadding(0,dp(7),0,dp(4));card.addView(legend);\n        TextView count=text(tr("مضاف: ","Included: ")+AhlBaytCalendar.birthCount()+tr(" مولد و"," births and ")+AhlBaytCalendar.deathCount()+tr(" وفاة/استشهاد"," deaths/martyrdoms"),11,false);count.setTextColor(muted);card.addView(count);\n        TextView variants=text(tr("عند وجود رواية تاريخية أخرى مشهورة ستظهر ملاحظة داخل المناسبة.","When another well-known historical narration exists, a note is shown inside the occasion."),11,false);variants.setTextColor(muted);variants.setPadding(0,dp(5),0,0);card.addView(variants);\n    }\n\n    private void addCalendarCard(){''')

# About text acknowledges the built-in Hijri Ahl al-Bayt calendar.
patch(settings,
'منظم مناسبات شخصي بالتاريخين الهجري والميلادي، مع تنبيهات متقدمة، Backup ومزامنة تقويم ثنائية.',
'منظم مناسبات شخصي بالتاريخين الهجري والميلادي، مع تقويم مناسبات أهل البيت عليهم السلام، تنبيهات متقدمة، Backup ومزامنة تقويم ثنائية.')
patch(settings,
'Personal event organizer with Hijri/Gregorian dates, advanced alerts, backup and two-way calendar sync.',
'Personal event organizer with Hijri/Gregorian dates, a built-in Ahl al-Bayt calendar, advanced alerts, backup and two-way calendar sync.')

print('v4.1 Ahl al-Bayt calendar patches applied')
