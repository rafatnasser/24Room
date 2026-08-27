from pathlib import Path

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')

def replace_method(src, signature, new_code):
    start=src.find(signature)
    if start<0: raise SystemExit('v4.5.4 method not found: '+signature)
    brace=src.find('{',start)
    if brace<0: raise SystemExit('v4.5.4 opening brace not found: '+signature)
    depth=0
    for i in range(brace,len(src)):
        c=src[i]
        if c=='{': depth+=1
        elif c=='}':
            depth-=1
            if depth==0:
                return src[:start]+new_code+src[i+1:]
    raise SystemExit('v4.5.4 closing brace not found: '+signature)

s=MAIN.read_text(encoding='utf-8')

# Single serialized refresh queue for all Home redraw triggers.
old='''    private ContentObserver calendarObserver;\n    private long lastObserverSync=0L;'''
new='''    private ContentObserver calendarObserver;\n    private long lastObserverSync=0L;\n    private final Handler uiRefreshHandler=new Handler(Looper.getMainLooper());\n    private boolean activityVisible=false;\n    private final Runnable uiRefreshRunnable=new Runnable(){@Override public void run(){if(!activityVisible||isFinishing()||contentHost==null)return;renderNow();scheduleMinuteBoundaryRefresh();}};\n    private final Runnable minuteRefreshRunnable=new Runnable(){@Override public void run(){if(activityVisible)requestUiRefresh(0);}};'''
if old not in s: raise SystemExit('v4.5.4 fields target not found')
s=s.replace(old,new,1)

# Preserve v4.3 privacy relock while making the Home data refresh deterministically.
s=replace_method(s,'    @Override protected void onStart()', '''    @Override protected void onStart(){super.onStart();activityVisible=true;maybeRelock();registerCalendarObserver();requestUiRefresh(0);}''')
s=replace_method(s,'    @Override protected void onStop()', '''    @Override protected void onStop(){activityVisible=false;uiRefreshHandler.removeCallbacks(uiRefreshRunnable);uiRefreshHandler.removeCallbacks(minuteRefreshRunnable);unregisterCalendarObserver();super.onStop();}''')
s=replace_method(s,'    @Override protected void onResume()', '''    @Override protected void onResume(){super.onResume();activityVisible=true;requestUiRefresh(40);if(CalendarIntegration.enabled(this)&&CalendarIntegration.hasPermission(this)&&System.currentTimeMillis()-CalendarIntegration.lastSyncTime(this)>10*60*1000L)syncQuietly();}''')

# Add focus refresh once, without disturbing onUserLeaveHint / privacy hooks.
if '    @Override public void onWindowFocusChanged(boolean hasFocus)' not in s:
    marker='    private String tr(String ar,String en)'
    pos=s.find(marker)
    if pos<0: raise SystemExit('v4.5.4 focus insertion marker not found')
    focus='''    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus&&activityVisible)requestUiRefresh(30);}\n'''
    s=s[:pos]+focus+s[pos:]

# All visual redraws go through one queue; no competing removeAllViews/animation paths.
s=replace_method(s,'    private void render()', '''    private void render(){requestUiRefresh(0);}''')
marker='    private void render()'
start=s.find(marker)
if start<0: raise SystemExit('v4.5.4 render marker missing after replacement')
# Insert helpers right after render() if not present.
if '    private void requestUiRefresh(long delayMs)' not in s:
    brace=s.find('{',start); depth=0; end=-1
    for i in range(brace,len(s)):
        if s[i]=='{': depth+=1
        elif s[i]=='}':
            depth-=1
            if depth==0: end=i+1; break
    if end<0: raise SystemExit('v4.5.4 render end not found')
    helpers='''\n    private void requestUiRefresh(long delayMs){if(contentHost==null)return;uiRefreshHandler.removeCallbacks(uiRefreshRunnable);uiRefreshHandler.postDelayed(uiRefreshRunnable,Math.max(0,delayMs));}\n    private void renderNow(){if(contentHost==null)return;contentHost.animate().cancel();contentHost.setAlpha(1f);contentHost.setTranslationY(0f);contentHost.removeAllViews();if(mode==MODE_TODAY)renderToday();else if(mode==MODE_CALENDAR)renderCalendar();else if(mode==MODE_FAVORITES)renderFavorites();else renderList();contentHost.setAlpha(.94f);contentHost.animate().alpha(1f).setDuration(100).start();}\n    private void scheduleMinuteBoundaryRefresh(){uiRefreshHandler.removeCallbacks(minuteRefreshRunnable);if(!activityVisible)return;long now=System.currentTimeMillis();long delay=60000L-(now%60000L)+250L;uiRefreshHandler.postDelayed(minuteRefreshRunnable,delay);}\n'''
    s=s[:end]+helpers+s[end:]

# v4.5.1 bottom navigation: remove its independent render animation to avoid races.
s=replace_method(s,'    private void setMode(int m)', '''    private void setMode(int m){mode=m;updateBottomNavigation();requestUiRefresh(0);}''')

# Initial skeleton -> serialized data refresh.
s=s.replace('contentHost.postDelayed(this::render,120);','contentHost.postDelayed(()->requestUiRefresh(0),120);')
s=s.replace('contentHost.postDelayed(this::render,180);','contentHost.postDelayed(()->requestUiRefresh(0),180);')

# Calendar observer changes should also converge on the same refresh queue.
s=s.replace('if(now-lastObserverSync<1500)return;lastObserverSync=now;if(CalendarIntegration.enabled(MainActivity.this)&&CalendarIntegration.hasPermission(MainActivity.this))syncQuietly();',
'''if(now-lastObserverSync<1500){requestUiRefresh(120);return;}lastObserverSync=now;if(CalendarIntegration.enabled(MainActivity.this)&&CalendarIntegration.hasPermission(MainActivity.this))syncQuietly();else requestUiRefresh(120);''')

# Background sync completion should re-read EventStore before drawing.
s=s.replace('runOnUiThread(this::render);','runOnUiThread(()->requestUiRefresh(0));')
s=s.replace('runOnUiThread(()->render());','runOnUiThread(()->requestUiRefresh(0));')

MAIN.write_text(s,encoding='utf-8')
print('Applied Munasabati v4.5.4 live refresh consistency fix')
