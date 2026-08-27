from pathlib import Path

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')

def replace_method(src, signature, new_code):
    start=src.find(signature)
    if start<0: raise SystemExit('v4.5.5 method not found: '+signature)
    brace=src.find('{',start)
    if brace<0: raise SystemExit('v4.5.5 opening brace not found: '+signature)
    depth=0
    for i in range(brace,len(src)):
        c=src[i]
        if c=='{': depth+=1
        elif c=='}':
            depth-=1
            if depth==0: return src[:start]+new_code+src[i+1:]
    raise SystemExit('v4.5.5 closing brace not found: '+signature)

s=MAIN.read_text(encoding='utf-8')

# Keep references to the hero counters so they can be updated without rebuilding the Activity.
if 'private TextView heroTodayMetric,heroNextMetric;' not in s:
    marker='    private Button[] bottomNav;'
    if marker not in s: raise SystemExit('v4.5.5 bottomNav field marker not found')
    s=s.replace(marker,marker+'\n    private TextView heroTodayMetric,heroNextMetric;',1)

s=replace_method(s,'    private void addHeroMetrics(LinearLayout hero)',r'''    private void addHeroMetrics(LinearLayout hero){
        LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(responsive(7,9,11)),0,0);hero.addView(row);
        heroTodayMetric=metricChip("");row.addView(heroTodayMetric,new LinearLayout.LayoutParams(0,dp(responsive(32,35,40)),1));
        heroNextMetric=metricChip("");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(responsive(32,35,40)),1);bp.setMargins(dp(6),0,0,0);row.addView(heroNextMetric,bp);
        refreshHeroMetrics();
    }''')

# Insert a live metrics refresher after metricChip.
if '    private void refreshHeroMetrics()' not in s:
    sig='    private TextView metricChip(String s)'
    start=s.find(sig)
    if start<0: raise SystemExit('v4.5.5 metricChip marker not found')
    brace=s.find('{',start);depth=0;end=-1
    for i in range(brace,len(s)):
        if s[i]=='{': depth+=1
        elif s[i]=='}':
            depth-=1
            if depth==0: end=i+1;break
    if end<0: raise SystemExit('v4.5.5 metricChip end not found')
    helper=r'''
    private void refreshHeroMetrics(){
        if(heroTodayMetric==null||heroNextMetric==null)return;
        Calendar day=Calendar.getInstance();zeroTime(day);Calendar tomorrow=(Calendar)day.clone();tomorrow.add(Calendar.DAY_OF_YEAR,1);
        long now=System.currentTimeMillis(),weekEnd=now+7L*86400000L;int today=0,next7=0;
        for(EventStore.Event e:EventStore.load(this)){
            if(Recurrence.firstOccurrenceBetween(e,day.getTimeInMillis(),tomorrow.getTimeInMillis())>=0)today++;
            if(Recurrence.firstOccurrenceBetween(e,now,weekEnd)>=0)next7++;
        }
        heroTodayMetric.setText("◷  "+tr("اليوم ","Today ")+today);
        heroNextMetric.setText("↗  "+tr("7 أيام ","7 days ")+next7);
    }
'''
    s=s[:end]+helper+s[end:]

# The welcome panel is data-driven: it shows both upcoming and recent-past counts on every refresh.
s=replace_method(s,'    private void addFreshWelcome()',r'''    private void addFreshWelcome(){
        long now=System.currentTimeMillis(),futureEnd=now+7L*86400000L,pastStart=now-7L*86400000L,next=Long.MAX_VALUE;int upcoming=0,previous=0;EventStore.Event nearest=null;
        for(EventStore.Event e:EventStore.load(this)){
            long f=Recurrence.firstOccurrenceBetween(e,now,futureEnd);if(f>=0){upcoming++;if(f<next){next=f;nearest=e;}}
            long p=previousOccurrenceBefore(e,now);if(p>=pastStart&&p<now)previous++;
        }
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(responsive(13,16,21)),dp(responsive(12,14,18)),dp(responsive(13,16,21)),dp(responsive(12,14,18)));card.setBackground(round(ModernUi.isDark(this)?Color.rgb(20,66,57):Color.rgb(238,248,244),22));card.setElevation(dp(1));contentHost.addView(card,margin(-1,-2,0,0,0,dp(10)));
        TextView hi=text(tr("مرحبًا بك","Welcome"),responsive(19,22,26),true);hi.setTextColor(primary);card.addView(hi);
        TextView line=text(tr("ملخص محدث للمناسبات السابقة والقادمة","Live summary of past and upcoming events"),responsive(10,12,14),false);line.setTextColor(muted);line.setPadding(0,dp(3),0,dp(9));card.addView(line);
        LinearLayout stats=new LinearLayout(this);card.addView(stats);
        LinearLayout up=homeStat("↗",tr("القادمة خلال 7 أيام","Next 7 days"),String.valueOf(upcoming));stats.addView(up,new LinearLayout.LayoutParams(0,dp(responsive(58,64,72)),1));
        LinearLayout prev=homeStat("↙",tr("السابقة خلال 7 أيام","Past 7 days"),String.valueOf(previous));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(responsive(58,64,72)),1);pp.setMargins(dp(7),0,0,0);stats.addView(prev,pp);
        if(nearest!=null){TextView nx=text(Categories.icon(nearest.category)+"  "+tr("الأقرب: ","Next: ")+nearest.title+"  •  "+relativeText(next),responsive(10,12,14),true);nx.setTextColor(ink);nx.setMaxLines(1);nx.setEllipsize(TextUtils.TruncateAt.END);nx.setPadding(0,dp(9),0,0);card.addView(nx);}
    }''')

if '    private LinearLayout homeStat(' not in s:
    marker='    private void renderToday()'
    pos=s.find(marker)
    if pos<0: raise SystemExit('v4.5.5 renderToday marker not found')
    helper=r'''    private LinearLayout homeStat(String icon,String title,String value){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(responsive(9,11,14)),dp(7),dp(responsive(9,11,14)),dp(7));box.setBackground(round(ModernUi.surface(this),15));TextView a=text(icon+"  "+title,responsive(9,10,12),true);a.setTextColor(primary);a.setMaxLines(1);a.setEllipsize(TextUtils.TruncateAt.END);box.addView(a);TextView b=text(value,responsive(13,15,18),true);b.setTextColor(accent);box.addView(b);return box;}

'''
    s=s[:pos]+helper+s[pos:]

# Home is now a real timeline: Today, Upcoming, then Previous, all read fresh from EventStore.
s=replace_method(s,'    private void renderToday()',r'''    private void renderToday(){
        addFreshWelcome();
        Calendar start=Calendar.getInstance();zeroTime(start);Calendar end=(Calendar)start.clone();end.add(Calendar.DAY_OF_YEAR,1);
        long todayStart=start.getTimeInMillis(),tomorrowStart=end.getTimeInMillis();
        ArrayList<Occurrence> today=new ArrayList<>();for(EventStore.Event e:EventStore.load(this)){long t=Recurrence.firstOccurrenceBetween(e,todayStart,tomorrowStart);if(t>=0&&matchesBasic(e,t))today.add(new Occurrence(e,t));}today.sort((a,b)->Long.compare(a.time,b.time));
        section(tr("اليوم","Today"));if(today.isEmpty())emptyCompact(tr("لا توجد مناسبات اليوم","No events today"));else for(Occurrence o:today)addCard(o.e,o.time);
        renderUpcomingHome(tomorrowStart,6);
        renderPreviousHomeLive(todayStart,6);
    }''')

# Add bounded, readable upcoming/past sections. Past recurring events use their latest occurrence.
if '    private void renderUpcomingHome(' not in s:
    marker='    private void addUpcomingSeven()'
    pos=s.find(marker)
    if pos<0: raise SystemExit('v4.5.5 addUpcomingSeven marker not found')
    helpers=r'''    private void renderUpcomingHome(long start,int limit){
        ArrayList<Occurrence> rows=new ArrayList<>();for(EventStore.Event e:EventStore.load(this)){long t=displayTime(e,start);if(t>=start&&matchesBasic(e,t))rows.add(new Occurrence(e,t));}
        rows.sort((a,b)->Long.compare(a.time,b.time));section(tr("المناسبات القادمة","Upcoming events"));if(rows.isEmpty()){emptyCompact(tr("لا توجد مناسبات قادمة","No upcoming events"));return;}for(int i=0;i<Math.min(limit,rows.size());i++){Occurrence o=rows.get(i);addCard(o.e,o.time);}
    }
    private void renderPreviousHomeLive(long before,int limit){
        ArrayList<Occurrence> rows=new ArrayList<>();for(EventStore.Event e:EventStore.load(this)){long t=previousOccurrenceBefore(e,before);if(t>=0&&matchesBasic(e,t))rows.add(new Occurrence(e,t));}
        rows.sort((a,b)->Long.compare(b.time,a.time));section(tr("المناسبات السابقة","Previous events"));if(rows.isEmpty()){emptyCompact(tr("لا توجد مناسبات سابقة","No previous events"));return;}for(int i=0;i<Math.min(limit,rows.size());i++){Occurrence o=rows.get(i);addCard(o.e,o.time);}
    }
    private long previousOccurrenceBefore(EventStore.Event e,long before){
        if(e==null)return-1;if(!Recurrence.isRecurring(e))return e.eventTime<before?e.eventTime:-1;
        long day=86400000L,cursor=Math.max(e.eventTime,before-370L*day),last=-1;for(int guard=0;guard<400;guard++){long t=Recurrence.firstOccurrenceBetween(e,cursor,before);if(t<0)break;last=t;cursor=t+1L;}return last;
    }

'''
    s=s[:pos]+helpers+s[pos:]

# v4.5.4 already serializes refreshes. Make every queued redraw also refresh the hero counters.
render_now=r'''    private void renderNow(){if(contentHost==null)return;refreshHeroMetrics();contentHost.animate().cancel();contentHost.setAlpha(1f);contentHost.setTranslationY(0f);contentHost.removeAllViews();if(mode==MODE_TODAY)renderToday();else if(mode==MODE_CALENDAR)renderCalendar();else if(mode==MODE_FAVORITES)renderFavorites();else renderList();contentHost.setAlpha(.94f);contentHost.animate().alpha(1f).setDuration(100).start();}'''
s=replace_method(s,'    private void renderNow()',render_now)

MAIN.write_text(s,encoding='utf-8')
print('Applied Munasabati v4.5.5 live Home timeline + counters fix')
