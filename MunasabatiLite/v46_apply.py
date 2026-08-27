from pathlib import Path

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')

def replace_method(src, signature, new_code):
    start=src.find(signature)
    if start<0: raise SystemExit('v4.6 method not found: '+signature)
    brace=src.find('{',start)
    if brace<0: raise SystemExit('v4.6 opening brace not found: '+signature)
    depth=0
    for i in range(brace,len(src)):
        c=src[i]
        if c=='{': depth+=1
        elif c=='}':
            depth-=1
            if depth==0: return src[:start]+new_code+src[i+1:]
    raise SystemExit('v4.6 closing brace not found: '+signature)

s=MAIN.read_text(encoding='utf-8')

# Quality/stability state: keep viewport stable during live refreshes and restore UI state after rotation/process recreation.
marker='    private TextView heroTodayMetric,heroNextMetric;'
if marker not in s: raise SystemExit('v4.6 hero metric field marker not found')
if 'private ScrollView mainScroll;' not in s:
    s=s.replace(marker,marker+'\n    private ScrollView mainScroll;\n    private boolean animateNextRender=false;\n    private static final String STATE_MODE="ui_mode",STATE_DATE="ui_date",STATE_MONTH="ui_month",STATE_SEARCH="ui_search",STATE_CAT="ui_cat",STATE_DAY="ui_day";',1)

# v4.5.1 created a local ScrollView. Promote it to a field so refreshes can preserve the viewport.
old='''        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);contentHost=new LinearLayout(this);contentHost.setOrientation(LinearLayout.VERTICAL);contentHost.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(contentHost,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));'''
new='''        mainScroll=new ScrollView(this);mainScroll.setFillViewport(true);mainScroll.setVerticalScrollBarEnabled(false);mainScroll.setClipToPadding(false);mainScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);contentHost=new LinearLayout(this);contentHost.setOrientation(LinearLayout.VERTICAL);contentHost.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);mainScroll.addView(contentHost,new ScrollView.LayoutParams(-1,-2));root.addView(mainScroll,new LinearLayout.LayoutParams(-1,0,1));'''
if old not in s: raise SystemExit('v4.6 ScrollView target not found')
s=s.replace(old,new,1)

# Debounce noisy filter/text callbacks. All refreshes still converge on the single v4.5.4 queue.
s=replace_method(s,'    private void render()', '''    private void render(){requestUiRefresh(55);}''')

# Preserve scroll position across background sync/minute refresh and only animate explicit navigation changes.
s=replace_method(s,'    private void renderNow()', '''    private void renderNow(){
        if(contentHost==null)return;
        refreshHeroMetrics();
        final int keepY=mainScroll==null?0:mainScroll.getScrollY();
        final boolean animate=animateNextRender;animateNextRender=false;
        contentHost.animate().cancel();contentHost.setAlpha(1f);contentHost.setTranslationY(0f);contentHost.removeAllViews();
        if(mode==MODE_TODAY)renderToday();else if(mode==MODE_CALENDAR)renderCalendar();else if(mode==MODE_FAVORITES)renderFavorites();else renderList();
        if(mainScroll!=null){mainScroll.post(()->mainScroll.scrollTo(0,Math.max(0,keepY)));}
        if(animate){contentHost.setAlpha(.94f);contentHost.setTranslationY(dp(2));contentHost.animate().alpha(1f).translationY(0f).setDuration(110).start();}
    }''')

s=replace_method(s,'    private void setMode(int m)', '''    private void setMode(int m){
        if(mode!=m){mode=m;animateNextRender=true;if(mainScroll!=null)mainScroll.scrollTo(0,0);}else mode=m;
        updateBottomNavigation();requestUiRefresh(0);
    }''')

# State restoration makes the responsive UI behave professionally through rotation and configuration changes.
if '    @Override protected void onSaveInstanceState(Bundle out)' not in s:
    anchor='    @Override public void onWindowFocusChanged(boolean hasFocus)'
    pos=s.find(anchor)
    if pos<0: raise SystemExit('v4.6 lifecycle insertion anchor not found')
    lifecycle='''    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt(STATE_MODE,mode);out.putLong(STATE_DATE,selectedDate);out.putLong(STATE_MONTH,shownMonth.getTimeInMillis());out.putString(STATE_SEARCH,search==null?"":search.getText().toString());out.putInt(STATE_CAT,categoryFilter==null?0:categoryFilter.getSelectedItemPosition());out.putInt(STATE_DAY,dayFilter==null?0:dayFilter.getSelectedItemPosition());}\n    @Override protected void onRestoreInstanceState(Bundle state){super.onRestoreInstanceState(state);if(state==null)return;mode=state.getInt(STATE_MODE,MODE_TODAY);selectedDate=state.getLong(STATE_DATE,0L);long month=state.getLong(STATE_MONTH,0L);if(month>0)shownMonth.setTimeInMillis(month);if(search!=null)search.setText(state.getString(STATE_SEARCH,""));if(categoryFilter!=null)categoryFilter.setSelection(Math.max(0,state.getInt(STATE_CAT,0)));if(dayFilter!=null)dayFilter.setSelection(Math.max(0,state.getInt(STATE_DAY,0)));if(dateFilter!=null)dateFilter.setText(selectedDate>0?DateTools.gregorianShort(this,selectedDate):tr("كل التواريخ  ▣","All dates  ▣"));updateBottomNavigation();requestUiRefresh(0);}\n'''
    s=s[:pos]+lifecycle+s[pos:]

# Make bottom navigation and compact icon controls more accessible without changing the visual identity.
needle='''bottomNav[0]=navButton("⌂\\n"+tr("الرئيسية","Home"));bottomNav[1]=navButton("▦\\n"+tr("التقويم","Calendar"));bottomNav[2]=navButton("＋");'''
if needle in s:
    repl=needle+'''bottomNav[0].setContentDescription(tr("الرئيسية","Home"));bottomNav[1].setContentDescription(tr("التقويم","Calendar"));bottomNav[2].setContentDescription(tr("إضافة مناسبة","Add event"));'''
    s=s.replace(needle,repl,1)
needle2='''bottomNav[3]=navButton("☆\\n"+tr("المفضلة","Favorites"));bottomNav[4]=navButton("•••\\n"+tr("المزيد","More"));'''
if needle2 in s:
    repl2=needle2+'''bottomNav[3].setContentDescription(tr("المفضلة","Favorites"));bottomNav[4].setContentDescription(tr("المزيد","More"));'''
    s=s.replace(needle2,repl2,1)

# Ensure event card actions are at least 48dp touch targets on all devices while keeping the cards compact.
s=s.replace('new LinearLayout.LayoutParams(dp(responsive(32,36,42)),dp(responsive(34,38,44)))','new LinearLayout.LayoutParams(dp(responsive(44,48,52)),dp(responsive(44,48,52)))')
s=s.replace('LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(dp(responsive(32,36,42)),dp(responsive(34,38,44)))','LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(dp(responsive(44,48,52)),dp(responsive(44,48,52)))')

MAIN.write_text(s,encoding='utf-8')
print('Applied Munasabati v4.6 quality/stability polish')
