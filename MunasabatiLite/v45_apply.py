from pathlib import Path
import re

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')

def sub(pattern,repl,required=True):
    s=MAIN.read_text(encoding='utf-8')
    n=re.subn(pattern,lambda m:repl,s,flags=re.S)
    if required and n[1]==0: raise SystemExit('v4.5 target not found: '+pattern[:100])
    if n[1]: MAIN.write_text(n[0],encoding='utf-8')

def rep(old,new,required=False):
    s=MAIN.read_text(encoding='utf-8')
    if old not in s:
        if required: raise SystemExit('v4.5 target not found: '+old[:100])
        return
    MAIN.write_text(s.replace(old,new),encoding='utf-8')

# Responsive helpers and compact dimensions.
rep('    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}', '''    private int widthDp(){return (int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density);}\n    private boolean compact(){return widthDp()<380;}\n    private boolean tablet(){return widthDp()>=600;}\n    private int responsive(int small,int phone,int tab){return tablet()?tab:(compact()?small:phone);}\n    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}''', True)
rep('root.setPadding(dp(12),dp(9),dp(12),dp(9));','root.setPadding(dp(responsive(8,10,18)),dp(responsive(7,8,12)),dp(responsive(8,10,18)),dp(responsive(7,8,12)));',True)
rep('hero.setPadding(dp(17),dp(13),dp(17),dp(14));','hero.setPadding(dp(responsive(13,16,22)),dp(responsive(10,12,16)),dp(responsive(13,16,22)),dp(responsive(11,13,17)));',True)
rep('TextView title=text(tr("مناسباتي","Munasabati"),28,true);','TextView title=text(tr("مناسباتي","Munasabati"),responsive(24,27,31),true);',True)
rep('TextView sub=text(tr("اليوم، القادم، والذكريات المهمة","Today, upcoming events and important memories"),13,false);','TextView sub=text(tr("اليوم، القادم، والذكريات المهمة","Today, upcoming events and important memories"),responsive(11,12,14),false);',True)

# Keep the hero simple: alert diagnostics + settings only. Sync and sacred calendar remain under More.
sub(r'        Button sync=heroIcon\("↔"\);.*?Button settings=heroIcon\("⚙"\);top.addView\(settings,new LinearLayout.LayoutParams\(dp\(46\),dp\(44\)\)\);settings.setOnClickListener\(v->startActivity\(new Intent\(this,SettingsActivity.class\)\)\);addHeroMetrics\(hero\);', '''        Button alerts=heroIcon("⌁");top.addView(alerts,new LinearLayout.LayoutParams(dp(responsive(40,44,50)),dp(responsive(40,44,50))));alerts.setOnClickListener(v->startActivity(new Intent(this,AlertDiagnosticsActivity.class)));\n        Button settings=heroIcon("⚙");top.addView(settings,new LinearLayout.LayoutParams(dp(responsive(40,44,50)),dp(responsive(40,44,50))));settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));addHeroMetrics(hero);''', True)

# Make hero metrics more informative and less bulky.
sub(r'    private void addHeroMetrics\(LinearLayout hero\)\{.*?    private TextView metricChip\(String s\)\{.*?\}', r'''    private void addHeroMetrics(LinearLayout hero){
        Calendar start=Calendar.getInstance();zeroTime(start);Calendar tomorrow=(Calendar)start.clone();tomorrow.add(Calendar.DAY_OF_YEAR,1);Calendar week=(Calendar)start.clone();week.add(Calendar.DAY_OF_YEAR,7);int today=0,next7=0;for(EventStore.Event e:EventStore.load(this)){if(Recurrence.firstOccurrenceBetween(e,start.getTimeInMillis(),tomorrow.getTimeInMillis())>=0)today++;if(Recurrence.firstOccurrenceBetween(e,start.getTimeInMillis(),week.getTimeInMillis())>=0)next7++;}
        LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(responsive(7,9,11)),0,0);hero.addView(row);TextView a=metricChip("◷  "+tr("اليوم ","Today ")+today);row.addView(a,new LinearLayout.LayoutParams(0,dp(responsive(32,35,40)),1));TextView b=metricChip("↗  "+tr("7 أيام ","7 days ")+next7);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(responsive(32,35,40)),1);bp.setMargins(dp(6),0,0,0);row.addView(b,bp);
    }
    private TextView metricChip(String s){TextView t=text(s,responsive(10,11,13),true);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setBackground(round(Color.argb(24,255,255,255),12));return t;}''', True)

# Tighter search and filters, responsive spacing.
rep('searchCard.setPadding(dp(10),dp(8),dp(10),dp(8));','searchCard.setPadding(dp(responsive(8,10,14)),dp(responsive(7,8,10)),dp(responsive(8,10,14)),dp(responsive(7,8,10)));',True)
rep('searchCard.addView(search,new LinearLayout.LayoutParams(-1,dp(44)));','searchCard.addView(search,new LinearLayout.LayoutParams(-1,dp(responsive(40,44,48))));',True)
rep('f1.addView(categoryFilter,new LinearLayout.LayoutParams(0,dp(44),1));','f1.addView(categoryFilter,new LinearLayout.LayoutParams(0,dp(responsive(40,43,48)),1));',True)
rep('f1.addView(dayFilter,new LinearLayout.LayoutParams(0,dp(44),1));','f1.addView(dayFilter,new LinearLayout.LayoutParams(0,dp(responsive(40,43,48)),1));',True)
rep('f2.addView(dateFilter,new LinearLayout.LayoutParams(0,dp(42),1));','f2.addView(dateFilter,new LinearLayout.LayoutParams(0,dp(responsive(39,42,47)),1));',True)
rep('LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(42),1);','LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(responsive(39,42,47)),1);',True)

# Replace welcome card with balanced, responsive summary blocks.
sub(r'    private void addFreshWelcome\(\)\{.*?\n    \}\n\n    private void renderToday', r'''    private void addFreshWelcome(){
        long now=System.currentTimeMillis(),weekEnd=now+7L*86400000L,next=Long.MAX_VALUE;int week=0;EventStore.Event nearest=null;
        for(EventStore.Event e:EventStore.load(this)){long t=displayTime(e,now);if(t>=now&&t<weekEnd)week++;if(t>=now&&t<next){next=t;nearest=e;}}
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(responsive(13,16,21)),dp(responsive(12,14,18)),dp(responsive(13,16,21)),dp(responsive(12,14,18)));card.setBackground(round(ModernUi.isDark(this)?Color.rgb(20,66,57):Color.rgb(238,248,244),22));card.setElevation(dp(1));contentHost.addView(card,margin(-1,-2,0,0,0,dp(10)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);card.addView(top);LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView hi=text(tr("مرحبًا بك","Welcome"),responsive(19,21,25),true);hi.setTextColor(primary);labels.addView(hi);TextView line=text(tr("نظّم مناسباتك بذكاء وهدوء","Keep your occasions organized and calm"),responsive(11,12,14),false);line.setTextColor(muted);labels.addView(line);TextView symbol=text("✓",responsive(22,25,30),true);symbol.setTextColor(accent);symbol.setGravity(Gravity.CENTER);top.addView(symbol,new LinearLayout.LayoutParams(dp(responsive(40,44,52)),dp(responsive(40,44,52))));
        LinearLayout stats=new LinearLayout(this);stats.setPadding(0,dp(10),0,0);card.addView(stats);LinearLayout seven=freshStat("▣",tr("خلال 7 أيام","Next 7 days"),String.valueOf(week));stats.addView(seven,new LinearLayout.LayoutParams(0,dp(responsive(68,74,82)),1));LinearLayout nearestBox=freshStat(nearest==null?"•":Categories.icon(nearest.category),tr("أقرب مناسبة","Next event"),nearest==null?tr("لا توجد","None"):relativeText(next));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(responsive(68,74,82)),1);np.setMargins(dp(7),0,0,0);stats.addView(nearestBox,np);
        if(nearest!=null){TextView nx=text(nearest.title,responsive(12,13,15),true);nx.setTextColor(ink);nx.setMaxLines(1);nx.setEllipsize(TextUtils.TruncateAt.END);nx.setPadding(0,dp(8),0,0);card.addView(nx);}
    }
    private LinearLayout freshStat(String icon,String title,String value){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(11),dp(8),dp(11),dp(8));box.setBackground(round(ModernUi.surface(this),16));TextView a=text(icon+"  "+title,responsive(10,11,13),true);a.setTextColor(primary);box.addView(a);TextView b=text(value,responsive(13,15,17),true);b.setTextColor(accent);b.setPadding(0,dp(3),0,0);box.addView(b);return box;}

    private void renderToday''', True)

# Replace table-like event card with a clean responsive compact card.
sub(r'    private void addCard\(EventStore.Event e,long shownTime\)\{.*?\n    \}\n\n    private String relativeText', r'''    private void addCard(EventStore.Event e,long shownTime){
        int base=ColorPalette.color(this,e);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(responsive(11,13,17)),dp(responsive(10,12,15)),dp(responsive(11,13,17)),dp(responsive(9,11,14)));GradientDrawable bgc=round(ModernUi.surface(this),20);bgc.setStroke(dp(1),ModernUi.alpha(base,55));card.setBackground(bgc);card.setElevation(dp(1));contentHost.addView(card,margin(-1,-2,0,0,0,dp(8)));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);TextView icon=text(Categories.icon(e.category),responsive(22,25,29),false);icon.setGravity(Gravity.CENTER);icon.setBackground(round(ModernUi.alpha(base,20),16));head.addView(icon,new LinearLayout.LayoutParams(dp(responsive(42,48,56)),dp(responsive(42,48,56))));LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,-2,1);nlp.setMargins(dp(9),0,dp(5),0);head.addView(names,nlp);TextView n=text(e.title,responsive(15,17,20),true);n.setTextColor(ink);n.setMaxLines(2);n.setEllipsize(TextUtils.TruncateAt.END);names.addView(n);TextView meta=text(Categories.label(this,e.category)+"  •  "+relativeText(shownTime),responsive(10,11,13),true);meta.setTextColor(base);meta.setPadding(0,dp(2),0,0);names.addView(meta);Button star=chip(e.favorite?"★":"☆");star.setTextColor(e.favorite?accent:muted);head.addView(star,new LinearLayout.LayoutParams(dp(responsive(36,40,44)),dp(responsive(36,40,44))));star.setOnClickListener(v->toggleFavorite(e));Button pin=chip(e.pinned?"📌":"⌖");head.addView(pin,new LinearLayout.LayoutParams(dp(responsive(36,40,44)),dp(responsive(36,40,44))));pin.setOnClickListener(v->togglePinned(e));
        TextView date=text("📅  "+DateTools.gregorian(this,shownTime,true)+"  •  "+DateTools.time(this,shownTime),responsive(10,12,14),false);date.setTextColor(muted);date.setPadding(0,dp(7),0,0);date.setMaxLines(1);date.setEllipsize(TextUtils.TruncateAt.END);card.addView(date);if(!e.locationName.trim().isEmpty()){TextView loc=text("⌖  "+e.locationName,responsive(10,11,13),false);loc.setTextColor(muted);loc.setPadding(0,dp(3),0,0);loc.setMaxLines(1);loc.setEllipsize(TextUtils.TruncateAt.END);card.addView(loc);}if(mode==MODE_LIST&&!e.details.trim().isEmpty()){TextView d=text(e.details,responsive(10,11,13),false);d.setTextColor(muted);d.setPadding(0,dp(5),0,0);d.setMaxLines(2);d.setEllipsize(TextUtils.TruncateAt.END);card.addView(d);}
        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,dp(7),0,0);card.addView(actions);Button open=miniAction(tr("✎ تعديل","✎ Edit"));actions.addView(open,new LinearLayout.LayoutParams(0,dp(responsive(36,40,44)),1));open.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));Button share=miniAction(tr("↗ مشاركة","↗ Share"));LinearLayout.LayoutParams shp=new LinearLayout.LayoutParams(0,dp(responsive(36,40,44)),1);shp.setMargins(dp(6),0,0,0);actions.addView(share,shp);share.setOnClickListener(v->shareEvent(e,shownTime));Button del=iconDanger("🗑");LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(dp(responsive(40,44,48)),dp(responsive(36,40,44)));dpv.setMargins(dp(6),0,0,0);actions.addView(del,dpv);del.setOnClickListener(v->confirmDelete(e));card.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));
    }

    private String relativeText''', True)

# Responsive five-item dock with larger tablet spacing and safe margins.
sub(r'    private void addBottomNavigation\(LinearLayout root\)\{.*?    private Button modeButton', r'''    private void addBottomNavigation(LinearLayout root){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(responsive(3,5,8)),dp(5),dp(responsive(3,5,8)),dp(5));nav.setBackground(round(ModernUi.surface(this),24));nav.setElevation(dp(7));bottomNav=new Button[5];bottomNav[0]=navButton("⌂\n"+tr("الرئيسية","Home"));bottomNav[1]=navButton("▦\n"+tr("التقويم","Calendar"));bottomNav[2]=navButton("＋");bottomNav[2].setTextSize(responsive(24,27,31));bottomNav[2].setTextColor(Color.WHITE);bottomNav[2].setBackground(round(accent,20));bottomNav[2].setElevation(dp(5));bottomNav[3]=navButton("☆\n"+tr("المفضلة","Favorites"));bottomNav[4]=navButton("•••\n"+tr("المزيد","More"));for(int i=0;i<bottomNav.length;i++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(responsive(54,58,66)),i==2?.82f:1f);lp.setMargins(dp(1),0,dp(1),0);nav.addView(bottomNav[i],lp);}bottomNav[0].setOnClickListener(v->setMode(MODE_TODAY));bottomNav[1].setOnClickListener(v->setMode(MODE_CALENDAR));bottomNav[2].setOnClickListener(v->startActivity(new Intent(this,TemplateChooserActivity.class)));bottomNav[3].setOnClickListener(v->setMode(MODE_FAVORITES));bottomNav[4].setOnClickListener(v->startActivity(new Intent(this,ModernCenterActivity.class)));root.addView(nav,margin(-1,dp(responsive(62,68,76)),0,dp(4),0,0));updateBottomNavigation();
    }
    private Button navButton(String s){Button b=button(s,muted,Color.TRANSPARENT,responsive(9,10,12));b.setGravity(Gravity.CENTER);b.setTypeface(null,Typeface.BOLD);b.setPadding(0,0,0,0);return b;}
    private void updateBottomNavigation(){if(bottomNav==null)return;int active=mode==MODE_TODAY?0:mode==MODE_CALENDAR?1:mode==MODE_FAVORITES?3:-1;for(int i=0;i<bottomNav.length;i++){if(i==2)continue;boolean on=i==active;bottomNav[i].setTextColor(on?primary:muted);bottomNav[i].setBackground(round(on?ModernUi.alpha(V4Theme.mint(),58):Color.TRANSPARENT,16));}}

    private Button modeButton''', True)

# Calendar cells adapt to phone/tablet width instead of fixed oversized cells.
rep('grid.addView(h,cellParams(dp(28)));','grid.addView(h,cellParams(dp(responsive(24,27,34))));')
rep('grid.addView(new LinearLayout(this),cellParams(dp(64)));','grid.addView(new LinearLayout(this),cellParams(dp(responsive(48,56,70))));')
rep('grid.addView(cell,cellParams(dp(64)));','grid.addView(cell,cellParams(dp(responsive(48,56,70))));')

print('v4.5 responsive UI applied')
