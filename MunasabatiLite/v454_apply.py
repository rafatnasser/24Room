from pathlib import Path
import re

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')
s=MAIN.read_text(encoding='utf-8')

# Add a serialized refresh pipeline so rapid lifecycle/filter/data events cannot race each other.
old='''    private ContentObserver calendarObserver;\n    private long lastObserverSync=0L;'''
new='''    private ContentObserver calendarObserver;\n    private long lastObserverSync=0L;\n    private final Handler uiRefreshHandler=new Handler(Looper.getMainLooper());\n    private boolean uiRefreshQueued=false;\n    private boolean activityVisible=false;\n    private long lastUiRefreshAt=0L;\n    private final Runnable uiRefreshRunnable=new Runnable(){@Override public void run(){uiRefreshQueued=false;if(!activityVisible||isFinishing()||contentHost==null)return;lastUiRefreshAt=System.currentTimeMillis();renderNow();scheduleMinuteBoundaryRefresh();}};\n    private final Runnable minuteRefreshRunnable=new Runnable(){@Override public void run(){if(activityVisible)requestUiRefresh(0);}};'''
if old not in s: raise SystemExit('v4.5.4 fields target not found')
s=s.replace(old,new,1)

# Replace lifecycle refresh behavior. onResume posts after the view hierarchy is attached and uses one queue only.
pat=r'''    @Override protected void onStart\(\)\{super\.onStart\(\);registerCalendarObserver\(\);\}\n    @Override protected void onStop\(\)\{unregisterCalendarObserver\(\);super\.onStop\(\);\}\n    @Override protected void onResume\(\)\{.*?\}\n'''
rep='''    @Override protected void onStart(){super.onStart();activityVisible=true;registerCalendarObserver();requestUiRefresh(0);}\n    @Override protected void onStop(){activityVisible=false;uiRefreshHandler.removeCallbacks(uiRefreshRunnable);uiRefreshHandler.removeCallbacks(minuteRefreshRunnable);uiRefreshQueued=false;unregisterCalendarObserver();super.onStop();}\n    @Override protected void onResume(){super.onResume();activityVisible=true;requestUiRefresh(40);if(CalendarIntegration.enabled(this)&&CalendarIntegration.hasPermission(this)&&System.currentTimeMillis()-CalendarIntegration.lastSyncTime(this)>10*60*1000L)syncQuietly();}\n    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus&&activityVisible)requestUiRefresh(30);}\n'''
s2,n=re.subn(pat,rep,s,flags=re.S)
if n!=1: raise SystemExit('v4.5.4 lifecycle target not found: '+str(n))
s=s2

# Replace render with a queued front door and a single synchronous renderer. This prevents removeAllViews/render races.
pat=r'''    private void render\(\)\{if\(contentHost==null\)return;contentHost\.removeAllViews\(\);if\(mode==MODE_TODAY\)renderToday\(\);else if\(mode==MODE_CALENDAR\)renderCalendar\(\);else if\(mode==MODE_FAVORITES\)renderFavorites\(\);else renderList\(\);.*?\}\n'''
rep='''    private void render(){requestUiRefresh(0);}\n    private void requestUiRefresh(long delayMs){if(contentHost==null)return;uiRefreshHandler.removeCallbacks(uiRefreshRunnable);uiRefreshQueued=true;uiRefreshHandler.postDelayed(uiRefreshRunnable,Math.max(0,delayMs));}\n    private void renderNow(){if(contentHost==null)return;contentHost.animate().cancel();contentHost.setAlpha(1f);contentHost.setTranslationY(0f);contentHost.removeAllViews();if(mode==MODE_TODAY)renderToday();else if(mode==MODE_CALENDAR)renderCalendar();else if(mode==MODE_FAVORITES)renderFavorites();else renderList();contentHost.setAlpha(.92f);contentHost.animate().alpha(1f).setDuration(110).start();}\n    private void scheduleMinuteBoundaryRefresh(){uiRefreshHandler.removeCallbacks(minuteRefreshRunnable);if(!activityVisible)return;long now=System.currentTimeMillis();long delay=60000L-(now%60000L)+250L;uiRefreshHandler.postDelayed(minuteRefreshRunnable,delay);}\n'''
s2,n=re.subn(pat,rep,s,flags=re.S)
if n!=1: raise SystemExit('v4.5.4 render target not found: '+str(n))
s=s2

# Mode changes should not run their own animation/render in parallel.
s=s.replace('''    private void setMode(int m){mode=m;updateBottomNavigation();if(contentHost!=null)contentHost.animate().alpha(.78f).setDuration(70).withEndAction(this::render).start();}''','''    private void setMode(int m){mode=m;updateBottomNavigation();requestUiRefresh(0);}''')

# Build completion should request one refresh, not leave a delayed render that can race with onResume/filter callbacks.
s=s.replace('''setContentView(root);showSkeleton();contentHost.postDelayed(this::render,120);''','''setContentView(root);showSkeleton();contentHost.postDelayed(()->requestUiRefresh(0),120);''')

# Calendar observer should converge into the same refresh queue even when no sync is performed.
s=s.replace('''public void onChange(boolean selfChange){super.onChange(selfChange);long now=System.currentTimeMillis();if(now-lastObserverSync<1500)return;lastObserverSync=now;if(CalendarIntegration.enabled(MainActivity.this)&&CalendarIntegration.hasPermission(MainActivity.this))syncQuietly();}''','''public void onChange(boolean selfChange){super.onChange(selfChange);long now=System.currentTimeMillis();if(now-lastObserverSync<1500){requestUiRefresh(120);return;}lastObserverSync=now;if(CalendarIntegration.enabled(MainActivity.this)&&CalendarIntegration.hasPermission(MainActivity.this))syncQuietly();else requestUiRefresh(120);}''')

# After sync finishes, always refresh UI from EventStore on the main thread.
s=s.replace('''runOnUiThread(()->{try{if(showToast)Toast.makeText(this,tr("تمت المزامنة","Synced"),Toast.LENGTH_SHORT).show();render();}catch(Exception ignored){}});''','''runOnUiThread(()->{try{if(showToast)Toast.makeText(this,tr("تمت المزامنة","Synced"),Toast.LENGTH_SHORT).show();requestUiRefresh(0);}catch(Exception ignored){}});''')
s=s.replace('''runOnUiThread(this::render);''','''runOnUiThread(()->requestUiRefresh(0));''')

# When returning from any child activity, onResume + focus refresh handles changes. Also refresh after permission result/data operations if present.
s=s.replace('''render();if(CalendarIntegration.enabled''','''requestUiRefresh(0);if(CalendarIntegration.enabled''')

MAIN.write_text(s,encoding='utf-8')
print('Applied Munasabati v4.5.4 live refresh consistency fix')
