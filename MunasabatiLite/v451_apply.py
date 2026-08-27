from pathlib import Path
import re

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')

def sub(pattern,repl,required=True):
    s=MAIN.read_text(encoding='utf-8')
    n=re.subn(pattern,lambda m:repl,s,flags=re.S)
    if required and n[1]==0:
        raise SystemExit('v4.5.1 target not found: '+pattern[:120])
    if n[1]: MAIN.write_text(n[0],encoding='utf-8')
    return n[1]

def rep(old,new,required=False):
    s=MAIN.read_text(encoding='utf-8')
    if old not in s:
        if required: raise SystemExit('v4.5.1 target not found: '+old[:120])
        return False
    MAIN.write_text(s.replace(old,new),encoding='utf-8')
    return True

# Build the home shell from the approved visual language instead of layering on the legacy mode strip.
new_build=r'''    private void buildUi(){
        int side=widthDp()>=900?72:(tablet()?36:(compact()?8:12));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(side),dp(responsive(7,9,14)),dp(side),dp(responsive(5,7,12)));root.setBackgroundColor(ModernUi.screenBackground(this));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(responsive(13,16,22)),dp(responsive(10,12,17)),dp(responsive(13,16,22)),dp(responsive(10,12,16)));hero.setBackground(freshHero());hero.setElevation(dp(3));root.addView(hero,margin(-1,-2,0,0,0,dp(10)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("مناسباتي","Munasabati"),responsive(24,28,32),true);title.setTextColor(Color.WHITE);titles.addView(title);
        TextView sub=text(tr("اليوم، القادم، والذكريات المهمة","Today, upcoming & important memories"),responsive(10,12,14),false);sub.setTextColor(Color.rgb(213,237,229));sub.setMaxLines(1);sub.setEllipsize(TextUtils.TruncateAt.END);titles.addView(sub);
        Button alerts=heroIcon("⌁");top.addView(alerts,new LinearLayout.LayoutParams(dp(responsive(39,44,50)),dp(responsive(39,44,50))));alerts.setOnClickListener(v->startActivity(new Intent(this,AlertDiagnosticsActivity.class)));
        Button settings=heroIcon("⚙");LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(dp(responsive(39,44,50)),dp(responsive(39,44,50)));slp.setMargins(dp(5),0,0,0);top.addView(settings,slp);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        addHeroMetrics(hero);

        LinearLayout searchCard=new LinearLayout(this);searchCard.setOrientation(LinearLayout.VERTICAL);searchCard.setPadding(dp(responsive(8,10,14)),dp(responsive(8,9,12)),dp(responsive(8,10,14)),dp(responsive(8,9,12)));searchCard.setBackground(round(ModernUi.surface(this),22));searchCard.setElevation(dp(1));root.addView(searchCard,margin(-1,-2,0,0,0,dp(9)));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(responsive(12,14,16));search.setHint(tr("ابحث بالاسم، الفئة، التاريخ، الموقع...","Search name, category, date, location..."));search.setCompoundDrawablesWithIntrinsicBounds(0,0,android.R.drawable.ic_menu_search,0);search.setCompoundDrawablePadding(dp(8));search.setTextColor(ink);search.setHintTextColor(muted);search.setBackground(ModernUi.fieldBackground(this,primary));search.setPadding(dp(13),0,dp(13),0);searchCard.addView(search,new LinearLayout.LayoutParams(-1,dp(responsive(42,46,52))));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){render();}public void afterTextChanged(Editable e){}});

        LinearLayout f1=new LinearLayout(this);f1.setGravity(Gravity.CENTER_VERTICAL);f1.setPadding(0,dp(7),0,0);searchCard.addView(f1);
        categoryFilter=new Spinner(this);categoryFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,true)));categoryFilter.setBackground(ModernUi.fieldBackground(this,primary));f1.addView(categoryFilter,new LinearLayout.LayoutParams(0,dp(responsive(40,43,48)),1));
        dayFilter=new Spinner(this);dayFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,dayLabels()));dayFilter.setBackground(ModernUi.fieldBackground(this,primary));LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(0,dp(responsive(40,43,48)),1);dlp.setMargins(dp(7),0,0,0);f1.addView(dayFilter,dlp);
        AdapterView.OnItemSelectedListener filterListener=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}public void onNothingSelected(AdapterView<?> p){}};categoryFilter.setOnItemSelectedListener(filterListener);dayFilter.setOnItemSelectedListener(filterListener);

        LinearLayout f2=new LinearLayout(this);f2.setPadding(0,dp(6),0,0);searchCard.addView(f2);
        dateFilter=outlineButton(tr("كل التواريخ  ▣","All dates  ▣"));f2.addView(dateFilter,new LinearLayout.LayoutParams(0,dp(responsive(38,41,46)),1));dateFilter.setOnClickListener(v->pickFilterDate());
        Button clear=outlineButton(tr("مسح  ↻","Clear  ↻"));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(responsive(38,41,46)),1);cp.setMargins(dp(7),0,0,0);f2.addView(clear,cp);clear.setOnClickListener(v->{search.setText("");categoryFilter.setSelection(0);dayFilter.setSelection(0);selectedDate=0;dateFilter.setText(tr("كل التواريخ  ▣","All dates  ▣"));render();});

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);contentHost=new LinearLayout(this);contentHost.setOrientation(LinearLayout.VERTICAL);contentHost.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(contentHost,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        addBottomNavigation(root);setContentView(root);showSkeleton();contentHost.postDelayed(this::render,120);
    }

    private Button modeButton'''
sub(r'    private void buildUi\(\)\{.*?\n    \}\n\n    private Button modeButton',new_build,True)

# Bottom navigation is the only primary navigation surface.
new_bottom=r'''    private void addBottomNavigation(LinearLayout root){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(responsive(2,4,7)),dp(4),dp(responsive(2,4,7)),dp(4));nav.setBackground(round(ModernUi.surface(this),23));nav.setElevation(dp(8));bottomNav=new Button[5];
        bottomNav[0]=navButton("⌂\n"+tr("الرئيسية","Home"));bottomNav[1]=navButton("▦\n"+tr("التقويم","Calendar"));bottomNav[2]=navButton("＋");bottomNav[2].setTextSize(responsive(24,28,32));bottomNav[2].setTextColor(Color.WHITE);bottomNav[2].setBackground(round(accent,21));bottomNav[2].setElevation(dp(5));bottomNav[3]=navButton("☆\n"+tr("المفضلة","Favorites"));bottomNav[4]=navButton("•••\n"+tr("المزيد","More"));
        for(int i=0;i<bottomNav.length;i++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(responsive(52,58,66)),i==2?.80f:1f);lp.setMargins(dp(1),0,dp(1),0);nav.addView(bottomNav[i],lp);}bottomNav[0].setOnClickListener(v->setMode(MODE_TODAY));bottomNav[1].setOnClickListener(v->setMode(MODE_CALENDAR));bottomNav[2].setOnClickListener(v->startActivity(new Intent(this,TemplateChooserActivity.class)));bottomNav[3].setOnClickListener(v->setMode(MODE_FAVORITES));bottomNav[4].setOnClickListener(v->startActivity(new Intent(this,ModernCenterActivity.class)));root.addView(nav,new LinearLayout.LayoutParams(-1,dp(responsive(60,66,74))));updateBottomNavigation();
    }
    private Button navButton(String s){Button b=button(s,muted,Color.TRANSPARENT,responsive(9,10,12));b.setGravity(Gravity.CENTER);b.setTypeface(null,Typeface.BOLD);b.setPadding(0,0,0,0);return b;}
    private void updateBottomNavigation(){if(bottomNav==null)return;int active=mode==MODE_TODAY?0:mode==MODE_CALENDAR?1:mode==MODE_FAVORITES?3:-1;for(int i=0;i<bottomNav.length;i++){if(i==2)continue;boolean on=i==active;bottomNav[i].setTextColor(on?primary:muted);bottomNav[i].setBackground(round(on?ModernUi.alpha(V4Theme.mint(),72):Color.TRANSPARENT,16));}}

    private Button modeButton'''
sub(r'    private void addBottomNavigation\(LinearLayout root\)\{.*?    private Button modeButton',new_bottom,True)

# Ensure every mode change updates the real navigation bar, not the removed legacy strip.
sub(r'    private void setMode\(int m\)\{.*?\}', '    private void setMode(int m){mode=m;updateBottomNavigation();if(contentHost!=null)contentHost.animate().alpha(.78f).setDuration(70).withEndAction(this::render).start();}', True)

# Approved home information hierarchy: greeting -> next 7 days -> today.
new_today=r'''    private void renderToday(){
        addFreshWelcome();addUpcomingSeven();
        Calendar start=Calendar.getInstance();zeroTime(start);Calendar end=(Calendar)start.clone();end.add(Calendar.DAY_OF_YEAR,1);ArrayList<Occurrence> today=new ArrayList<>();for(EventStore.Event e:EventStore.load(this)){long t=Recurrence.firstOccurrenceBetween(e,start.getTimeInMillis(),end.getTimeInMillis());if(t>=0&&matchesBasic(e,t))today.add(new Occurrence(e,t));}today.sort((a,b)->Long.compare(a.time,b.time));section(tr("اليوم","Today"));if(today.isEmpty()){emptyCompact(tr("لا توجد مناسبات اليوم","No events today"));}else for(Occurrence o:today)addCard(o.e,o.time);
    }
    private void addUpcomingSeven(){long now=System.currentTimeMillis(),end=now+7L*86400000L,next=Long.MAX_VALUE;EventStore.Event nearest=null;int count=0;for(EventStore.Event e:EventStore.load(this)){long t=Recurrence.firstOccurrenceBetween(e,now,end);if(t>=0&&matchesBasic(e,t)){count++;if(t<next){next=t;nearest=e;}}}section(tr("خلال 7 أيام","Next 7 days"));if(nearest==null){emptyCompact(tr("لا توجد مناسبات قريبة","No upcoming events"));return;}LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(responsive(11,13,17)),dp(responsive(9,11,14)),dp(responsive(11,13,17)),dp(responsive(9,11,14)));GradientDrawable gd=round(ModernUi.surface(this),18);gd.setStroke(dp(1),ModernUi.alpha(primary,35));row.setBackground(gd);contentHost.addView(row,margin(-1,-2,0,0,0,dp(8)));TextView icon=text(Categories.icon(nearest.category),responsive(22,25,29),false);icon.setGravity(Gravity.CENTER);icon.setBackground(round(ModernUi.alpha(ColorPalette.color(this,nearest),22),16));row.addView(icon,new LinearLayout.LayoutParams(dp(responsive(44,48,56)),dp(responsive(44,48,56))));LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(10),0,dp(8),0);row.addView(labels,lp);TextView name=text(nearest.title,responsive(13,15,18),true);name.setTextColor(ink);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);labels.addView(name);TextView rel=text(tr("الأقرب: ","Next: ")+relativeText(next)+(count>1?tr("  •  "+count+" مناسبات","  •  "+count+" events"):""),responsive(10,11,13),true);rel.setTextColor(accent);labels.addView(rel);TextView go=text("‹",responsive(21,23,27),true);go.setTextColor(primary);go.setGravity(Gravity.CENTER);row.addView(go,new LinearLayout.LayoutParams(dp(34),dp(40)));final EventStore.Event target=nearest;row.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",target.id)));
    }
    private void emptyCompact(String msg){TextView t=text(msg,responsive(11,12,14),false);t.setTextColor(muted);t.setGravity(Gravity.CENTER);t.setPadding(dp(10),dp(12),dp(10),dp(12));t.setBackground(round(ModernUi.alpha(ModernUi.surface(this),235),16));contentHost.addView(t,margin(-1,-2,0,0,0,dp(8)));}

    private boolean renderRange'''
sub(r'    private void renderToday\(\)\{.*?\n    private boolean renderRange',new_today,True)

# Welcome is now a single calm banner, not a second dashboard inside the dashboard.
new_welcome=r'''    private void addFreshWelcome(){
        LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(responsive(13,16,21)),dp(responsive(12,14,18)),dp(responsive(13,16,21)),dp(responsive(12,14,18)));card.setBackground(round(ModernUi.isDark(this)?Color.rgb(20,66,57):Color.rgb(238,248,244),22));card.setElevation(dp(1));contentHost.addView(card,margin(-1,-2,0,0,0,dp(10)));LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView hi=text(tr("مرحبًا بك","Welcome"),responsive(19,22,26),true);hi.setTextColor(primary);labels.addView(hi);TextView line=text(tr("نظّم مناسباتك بذكاء ويسر","Organize your moments simply and intelligently"),responsive(10,12,14),false);line.setTextColor(muted);line.setPadding(0,dp(3),0,0);labels.addView(line);TextView symbol=text("✓",responsive(25,29,34),true);symbol.setTextColor(primary);symbol.setGravity(Gravity.CENTER);symbol.setBackground(round(ModernUi.alpha(V4Theme.mint(),85),18));card.addView(symbol,new LinearLayout.LayoutParams(dp(responsive(46,52,60)),dp(responsive(46,52,60))));
    }

    private LinearLayout freshStat'''
sub(r'    private void addFreshWelcome\(\)\{.*?\n    \}\n    private LinearLayout freshStat',new_welcome,True)

# Clean event card: no dense edit/share/delete row on the home surface.
new_card=r'''    private void addCard(EventStore.Event e,long shownTime){
        int base=ColorPalette.color(this,e);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(responsive(11,13,17)),dp(responsive(10,12,15)),dp(responsive(11,13,17)),dp(responsive(9,11,14)));GradientDrawable bgc=round(ModernUi.surface(this),19);bgc.setStroke(dp(1),ModernUi.alpha(base,48));card.setBackground(bgc);card.setElevation(dp(1));contentHost.addView(card,margin(-1,-2,0,0,0,dp(8)));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);TextView icon=text(Categories.icon(e.category),responsive(22,25,29),false);icon.setGravity(Gravity.CENTER);icon.setBackground(round(ModernUi.alpha(base,20),16));head.addView(icon,new LinearLayout.LayoutParams(dp(responsive(42,48,56)),dp(responsive(42,48,56))));LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,-2,1);nlp.setMargins(dp(9),0,dp(5),0);head.addView(names,nlp);TextView n=text(e.title,responsive(15,17,20),true);n.setTextColor(ink);n.setMaxLines(2);n.setEllipsize(TextUtils.TruncateAt.END);names.addView(n);TextView meta=text(Categories.label(this,e.category)+"  •  "+relativeText(shownTime),responsive(10,11,13),true);meta.setTextColor(base);meta.setPadding(0,dp(2),0,0);names.addView(meta);Button star=chip(e.favorite?"★":"☆");star.setTextColor(e.favorite?accent:muted);head.addView(star,new LinearLayout.LayoutParams(dp(responsive(34,38,44)),dp(responsive(34,38,44))));star.setOnClickListener(v->{toggleFavorite(e);});Button pin=chip(e.pinned?"📌":"⌖");head.addView(pin,new LinearLayout.LayoutParams(dp(responsive(34,38,44)),dp(responsive(34,38,44))));pin.setOnClickListener(v->{togglePinned(e);});
        TextView date=text("▣  "+DateTools.gregorian(this,shownTime,true)+"  •  "+DateTools.time(this,shownTime),responsive(10,12,14),false);date.setTextColor(muted);date.setPadding(0,dp(7),0,0);date.setMaxLines(1);date.setEllipsize(TextUtils.TruncateAt.END);card.addView(date);if(!e.locationName.trim().isEmpty()){TextView loc=text("⌖  "+e.locationName,responsive(10,11,13),false);loc.setTextColor(muted);loc.setPadding(0,dp(3),0,0);loc.setMaxLines(1);loc.setEllipsize(TextUtils.TruncateAt.END);card.addView(loc);}card.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));
    }

    private String relativeText'''
sub(r'    private void addCard\(EventStore.Event e,long shownTime\)\{.*?\n    \}\n\n    private String relativeText',new_card,True)

# Tablet and compact calendar geometry.
rep('cellParams(dp(64))','cellParams(dp(responsive(48,56,70)))')
rep('cellParams(dp(28))','cellParams(dp(responsive(24,28,32)))')
rep('TextView month=text(monthTitle(),20,true);','TextView month=text(monthTitle(),responsive(17,20,24),true);')

print('v4.5.1 full responsive UI applied')
