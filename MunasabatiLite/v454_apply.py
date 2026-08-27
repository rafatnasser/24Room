from pathlib import Path
import re

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')
s=MAIN.read_text(encoding='utf-8')

# One serialized UI refresh queue. All lifecycle, filter and sync redraws converge here.
old='''    private ContentObserver calendarObserver;\n    private long lastObserverSync=0L;'''
new='''    private ContentObserver calendarObserver;\n    private long lastObserverSync=0L;\n    private final Handler uiRefreshHandler=new Handler(Looper.getMainLooper());\n    private boolean activityVisible=false;\n    private final Runnable uiRefreshRunnable=new Runnable(){@Override public void run(){if(!activityVisible||isFinishing()||contentHost==null)return;renderNow();scheduleMinuteBoundaryRefresh();}};\n    private final Runnable minuteRefreshRunnable=new Runnable(){@Override public void run(){if(activityVisible)requestUiRefresh(0);}};'''
if old not in s: raise SystemExit('v4.5.4 fields target not found')
s=s.replace(old,new,1)

# Refresh whenever the Home activity becomes visible/focused, and stop timers when hidden.
pat=r'''    @Override protected void onStart\(\)\{super\.onStart\(\);registerCalendarObserver\(\);\}\n    @Override protected void onStop\(\)\{unregisterCalendarObserver\(\);super\.onStop\(\);\}\n    @Override protected void onResume\(\)\{.*?\}\n'''
rep='''    @Override protected void onStart(){super.onStart();activityVisible=true;registerCalendarObserver();requestUiRefresh(0);}\n    @Override protected void onStop(){activityVisible=false;uiRefreshHandler.removeCallbacks(uiRefreshRunnable);uiRefreshHandler.removeCallbacks(minuteRefreshRunnable);unregisterCalendarObserver();super.onStop();}\n    @Override protected void onResume(){super.onResume();activityVisible=true;requestUiRefresh(40);if(CalendarIntegration.enabled(this)&&CalendarIntegration.hasPermission(this)&&System.currentTimeMillis()-CalendarIntegration.lastSyncTime(this)>10*60*1000L)syncQuietly();}\n    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus);if(hasFocus&&activityVisible)requestUiRefresh(30);}\n'''
s2,n=re.subn(pat,rep,s,flags=re.S)
if n!=1: raise SystemExit('v4.5.4 lifecycle target not found: '+str(n))
s=s2

# Replace direct redraw with queued redraw to prevent competing removeAllViews/render calls.
pat=r'''    private void render\(\)\{if\(contentHost==null\)return;contentHost\.removeAllViews\(\);if\(mode==MODE_TODAY\)renderToday\(\);else if\(mode==MODE_CALENDAR\)renderCalendar\(\);else if\(mode==MODE_FAVORITES\)renderFavorites\(\);else renderList\(\);contentHost\.setAlpha\(\.82f\);contentHost\.setTranslationY\(dp\(4\)\);contentHost\.animate\(\)\.alpha\(1f\)\.translationY\(0f\)\.setDuration\(180\)\.start\(\);\}\n'''
rep='''    private void render(){requestUiRefresh(0);}\n    private void requestUiRefresh(long delayMs){if(contentHost==null)return;uiRefreshHandler.removeCallbacks(uiRefreshRunnable);uiRefreshHandler.postDelayed(uiRefreshRunnable,Math.max(0,delayMs));}\n    private void renderNow(){if(contentHost==null)return;contentHost.animate().cancel();contentHost.setAlpha(1f);contentHost.setTranslationY(0f);contentHost.removeAllViews();if(mode==MODE_TODAY)renderToday();else if(mode==MODE_CALENDAR)renderCalendar();else if(mode==MODE_FAVORITES)renderFavorites();else renderList();contentHost.setAlpha(.94f);contentHost.animate().alpha(1f).setDuration(100).start();}\n    private void scheduleMinuteBoundaryRefresh(){uiRefreshHandler.removeCallbacks(minuteRefreshRunnable);if(!activityVisible)return;long now=System.currentTimeMillis();long delay=60000L-(now%60000L)+250L;uiRefreshHandler.postDelayed(minuteRefreshRunnable,delay);}\n'''
s2,n=re.subn(pat,rep,s,flags=re.S)
if n!=1: raise SystemExit('v4.5.4 render target not found: '+str(n))
s=s2

# v4.5.1 mode bar uses its own animation; remove that competing redraw path.
s=s.replace('''    private void setMode(int m){mode=m;updateBottomNavigation();if(contentHost!=null)contentHost.animate().alpha(.78f).setDuration(70).withEndAction(this::render).start();}''','''    private void setMode(int m){mode=m;updateBottomNavigation();requestUiRefresh(0);}''')
s=s.replace('''    private void setMode(int m){mode=m;updateModeButtons();contentHost.animate().alpha(.72f).setDuration(80).withEndAction(this::render).start();}''','''    private void setMode(int m){mode=m;updateModeButtons();requestUiRefresh(0);}''')

# Initial skeleton transitions into the same refresh queue.
s=s.replace('''setContentView(root);showSkeleton();contentHost.postDelayed(this::render,120);''','''setContentView(root);showSkeleton();contentHost.postDelayed(()->requestUiRefresh(0),120);''')
s=s.replace('''setContentView(root);updateModeButtons();showSkeleton();contentHost.postDelayed(this::render,180);''','''setContentView(root);updateModeButtons();showSkeleton();contentHost.postDelayed(()->requestUiRefresh(0),180);''')

# Calendar provider changes and sync completion should always refresh from EventStore.
s=s.replace('''if(now-lastObserverSync<1500)return;lastObserverSync=now;if(CalendarIntegration.enabled(MainActivity.this)&&CalendarIntegration.hasPermission(MainActivity.this))syncQuietly();''','''if(now-lastObserverSync<1500){requestUiRefresh(120);return;}lastObserverSync=now;if(CalendarIntegration.enabled(MainActivity.this)&&CalendarIntegration.hasPermission(MainActivity.this))syncQuietly();else requestUiRefresh(120);''')
s=s.replace('''runOnUiThread(this::render);''','''runOnUiThread(()->requestUiRefresh(0));''')
s=s.replace('''runOnUiThread(()->render());''','''runOnUiThread(()->requestUiRefresh(0));''')

MAIN.write_text(s,encoding='utf-8')
print('Applied Munasabati v4.5.4 live refresh consistency fix')
