from pathlib import Path
import re

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')
s=MAIN.read_text(encoding='utf-8')

# v4.4.2: keep the home dashboard live and explicitly show both upcoming and past events.
if 'private TextView heroTodayMetric,heroNextMetric;' not in s:
    s=s.replace('    private Button[] bottomNav;','    private Button[] bottomNav;\n    private TextView heroTodayMetric,heroNextMetric;')

hero=r'''    private void addHeroMetrics(LinearLayout hero){
        LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(10),0,0);hero.addView(row);
        heroTodayMetric=metricChip("");row.addView(heroTodayMetric,new LinearLayout.LayoutParams(0,dp(35),1));
        heroNextMetric=metricChip("");LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(35),1);bp.setMargins(dp(7),0,0,0);row.addView(heroNextMetric,bp);
        refreshHeroMetrics();
    }
    private void refreshHeroMetrics(){
        if(heroTodayMetric==null||heroNextMetric==null)return;
        Calendar start=Calendar.getInstance();zeroTime(start);Calendar tomorrow=(Calendar)start.clone();tomorrow.add(Calendar.DAY_OF_YEAR,1);
        long now=System.currentTimeMillis(),weekEnd=now+7L*86400000L;int today=0,next7=0;
        for(EventStore.Event e:EventStore.load(this)){
            if(Recurrence.firstOccurrenceBetween(e,start.getTimeInMillis(),tomorrow.getTimeInMillis())>=0)today++;
            long t=displayTime(e,now);if(t>=now&&t<weekEnd)next7++;
        }
        heroTodayMetric.setText("◷  "+tr("اليوم ","Today ")+today);
        heroNextMetric.setText("↗  "+tr("7 أيام ","7 days ")+next7);
    }
    private TextView metricChip(String s){TextView t=text(s,12,true);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);t.setBackground(round(Color.argb(25,255,255,255),12));return t;}
'''
pattern=r'    private void addHeroMetrics\(LinearLayout hero\)\{.*?    private TextView metricChip\(String s\)\{.*?return t;\}\s*'
s,n=re.subn(pattern,lambda m:hero,s,count=1,flags=re.S)
if n!=1:
    raise SystemExit('v4.4.2 could not patch live hero metrics')

s=s.replace('private void render(){if(contentHost==null)return;contentHost.removeAllViews();',
            'private void render(){if(contentHost==null)return;refreshHeroMetrics();contentHost.removeAllViews();',1)

welcome=r'''    private void addFreshWelcome(){
        long now=System.currentTimeMillis(),nextWeek=now+7L*86400000L,pastWeek=now-7L*86400000L,next=Long.MAX_VALUE;
        int upcoming=0,previous=0;EventStore.Event nearest=null;
        for(EventStore.Event e:EventStore.load(this)){
            long t=displayTime(e,now);if(t>=now&&t<nextWeek)upcoming++;if(t>=now&&t<next){next=t;nearest=e;}
            if(Recurrence.firstOccurrenceBetween(e,pastWeek,now)>=0)previous++;
        }
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(17),dp(15),dp(17),dp(15));card.setBackground(round(ModernUi.isDark(this)?Color.rgb(20,66,57):Color.rgb(237,248,244),22));contentHost.addView(card,margin(-1,-2,0,0,0,dp(10)));
        TextView hi=text(tr("مرحبًا بك","Welcome"),22,true);hi.setTextColor(primary);card.addView(hi);
        TextView line=text(tr("ملخص محدث للمناسبات السابقة والقادمة","A live summary of past and upcoming events"),13,false);line.setTextColor(muted);line.setPadding(0,dp(3),0,dp(10));card.addView(line);
        LinearLayout stats=new LinearLayout(this);stats.setGravity(Gravity.CENTER_VERTICAL);card.addView(stats);
        TextView up=text(tr("↗ القادمة خلال 7 أيام  ","↗ Next 7 days  ")+upcoming,12,true);up.setTextColor(primary);up.setGravity(Gravity.CENTER);up.setBackground(round(ModernUi.alpha(V4Theme.mint(),48),13));stats.addView(up,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView prev=text(tr("↙ السابقة خلال 7 أيام  ","↙ Past 7 days  ")+previous,12,true);prev.setTextColor(primary);prev.setGravity(Gravity.CENTER);prev.setBackground(round(ModernUi.alpha(V4Theme.mint(),34),13));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(0,dp(44),1);pp.setMargins(dp(7),0,0,0);stats.addView(prev,pp);
        if(nearest!=null){TextView nx=text(Categories.icon(nearest.category)+"  "+tr("الأقرب: ","Next: ")+nearest.title+"  •  "+relativeText(next),13,true);nx.setTextColor(ink);nx.setPadding(0,dp(10),0,0);card.addView(nx);}else{TextView nx=text(tr("لا توجد مناسبات قادمة حاليًا","No upcoming events right now"),12,true);nx.setTextColor(muted);nx.setPadding(0,dp(10),0,0);card.addView(nx);}
    }

'''
pattern=r'    private void addFreshWelcome\(\)\{.*?\n    \}\n\n(?=    private void renderToday\(\))'
s,n=re.subn(pattern,lambda m:welcome,s,count=1,flags=re.S)
if n!=1:
    raise SystemExit('v4.4.2 could not patch Fresh Welcome')

home=r'''    private void renderToday(){
        addFreshWelcome();
        Calendar start=Calendar.getInstance();zeroTime(start);Calendar tomorrow=(Calendar)start.clone();tomorrow.add(Calendar.DAY_OF_YEAR,1);
        long todayStart=start.getTimeInMillis(),tomorrowStart=tomorrow.getTimeInMillis();boolean any=false;
        any|=renderRange(tr("اليوم","Today"),todayStart,tomorrowStart);
        any|=renderHomeUpcoming(tomorrowStart,5);
        any|=renderHomePast(todayStart,5);
        if(!any)empty(tr("لا توجد مناسبات سابقة أو قادمة مطابقة للبحث.","No past or upcoming events match your filters."));
    }
    private boolean renderHomeUpcoming(long start,int limit){
        ArrayList<Occurrence> rows=new ArrayList<>();
        for(EventStore.Event e:EventStore.load(this)){long t=displayTime(e,start);if(t>=start&&matches(e,t))rows.add(new Occurrence(e,t));}
        rows.sort((a,b)->{int x=Long.compare(a.time,b.time);if(x!=0)return x;if(a.e.pinned!=b.e.pinned)return a.e.pinned?-1:1;if(a.e.favorite!=b.e.favorite)return a.e.favorite?-1:1;return 0;});
        if(rows.isEmpty())return false;section(tr("القادمة","Upcoming"));for(int i=0;i<Math.min(limit,rows.size());i++){Occurrence o=rows.get(i);addCard(o.e,o.time);}return true;
    }
    private boolean renderHomePast(long before,int limit){
        ArrayList<Occurrence> rows=new ArrayList<>();
        for(EventStore.Event e:EventStore.load(this)){long t=previousOccurrence(e,before);if(t>=0&&matches(e,t))rows.add(new Occurrence(e,t));}
        rows.sort((a,b)->{int x=Long.compare(b.time,a.time);if(x!=0)return x;if(a.e.pinned!=b.e.pinned)return a.e.pinned?-1:1;if(a.e.favorite!=b.e.favorite)return a.e.favorite?-1:1;return 0;});
        if(rows.isEmpty())return false;section(tr("السابقة","Past"));for(int i=0;i<Math.min(limit,rows.size());i++){Occurrence o=rows.get(i);addCard(o.e,o.time);}return true;
    }
    private long previousOccurrence(EventStore.Event e,long before){
        if(e==null)return-1;if(!Recurrence.isRecurring(e))return e.eventTime<before?e.eventTime:-1;
        long day=86400000L,floor=Math.max(e.eventTime,before-370L*day),cursor=floor,last=-1;
        for(int guard=0;guard<400;guard++){long t=Recurrence.firstOccurrenceBetween(e,cursor,before);if(t<0)break;last=t;cursor=t+1L;}
        return last;
    }
'''
pattern=r'    private void renderToday\(\)\{.*?(?=    private boolean renderRange\()'
s,n=re.subn(pattern,lambda m:home,s,count=1,flags=re.S)
if n!=1:
    raise SystemExit('v4.4.2 could not patch home timeline')

MAIN.write_text(s,encoding='utf-8')
print('v4.4.2 live home timeline applied')
