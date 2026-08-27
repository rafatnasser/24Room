from pathlib import Path

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')

def find_method(src, signature):
    start=src.find(signature)
    if start<0: raise SystemExit('v4.5.3 method not found: '+signature)
    brace=src.find('{',start)
    if brace<0: raise SystemExit('v4.5.3 opening brace not found: '+signature)
    depth=0
    for i in range(brace,len(src)):
        c=src[i]
        if c=='{': depth+=1
        elif c=='}':
            depth-=1
            if depth==0: return start,brace,i
    raise SystemExit('v4.5.3 closing brace not found: '+signature)

s=MAIN.read_text(encoding='utf-8')

# Restore past personal events directly on the Home screen after Today's events.
start,brace,end=find_method(s,'    private void renderToday()')
body=s[brace+1:end]
if 'renderPreviousHome(' not in body:
    body=body.rstrip()+'\n        renderPreviousHome(start.getTimeInMillis());\n    '
    s=s[:brace+1]+body+s[end:]

helper='''\n    private void renderPreviousHome(long todayStart){
        ArrayList<Occurrence> rows=new ArrayList<>();
        for(EventStore.Event e:EventStore.load(this)){
            if(Recurrence.isRecurring(e))continue;
            long t=e.eventTime;
            if(t<todayStart&&matches(e,t))rows.add(new Occurrence(e,t));
        }
        rows.sort((a,b)->Long.compare(b.time,a.time));
        if(rows.isEmpty())return;
        section(tr("المناسبات السابقة","Previous events"));
        for(Occurrence o:rows)addCard(o.e,o.time);
    }\n\n'''
if 'private void renderPreviousHome(' not in s:
    marker='    private void addUpcomingSeven()'
    pos=s.find(marker)
    if pos<0:
        marker='    private boolean renderRange'
        pos=s.find(marker)
    if pos<0: raise SystemExit('v4.5.3 insertion marker not found')
    s=s[:pos]+helper+s[pos:]

MAIN.write_text(s,encoding='utf-8')
print('v4.5.3 previous events restored on Home')
