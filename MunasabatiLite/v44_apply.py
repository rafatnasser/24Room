from pathlib import Path
import re

ROOT=Path('app/src/main')
JAVA=ROOT/'java/com/rafat/munasabati'

def replace(path, old, new):
    p=Path(path)
    if not p.exists(): return False
    s=p.read_text(encoding='utf-8')
    if old not in s: return False
    p.write_text(s.replace(old,new),encoding='utf-8')
    return True

def sub(path, pattern, repl):
    p=Path(path)
    if not p.exists(): return False
    s=p.read_text(encoding='utf-8')
    n=re.subn(pattern,repl,s,flags=re.S)
    if n[1]: p.write_text(n[0],encoding='utf-8')
    return bool(n[1])

# Fresh Identity palette: deep green / fresh green / mint / coral / ivory.
selected=[
    'MainActivity.java','EditEventActivity.java','SettingsActivity.java','SyncCenterActivity.java',
    'ModernCenterActivity.java','TemplateChooserActivity.java','V4CenterActivity.java','LauncherActivity.java',
    'PrivacySettingsActivity.java','AlertDiagnosticsActivity.java','SmartReminderSettingsActivity.java',
    'GuestManagerActivity.java','SyncConflictActivity.java'
]
for name in selected:
    p=JAVA/name
    if not p.exists(): continue
    s=p.read_text(encoding='utf-8')
    pairs={
        'Color.rgb(25,91,86)':'Color.rgb(15,77,67)',
        'Color.rgb(14,90,82)':'Color.rgb(15,77,67)',
        'Color.rgb(38,112,105)':'Color.rgb(46,125,107)',
        'Color.rgb(208,151,56)':'Color.rgb(255,123,92)',
        'Color.rgb(228,184,91)':'Color.rgb(255,123,92)',
        'Color.rgb(244,247,249)':'Color.rgb(255,247,242)',
        'Color.rgb(246,248,251)':'Color.rgb(255,247,242)',
        'Color.rgb(247,249,250)':'Color.rgb(250,252,250)',
        'Color.rgb(229,242,240)':'Color.rgb(226,244,238)',
        'Color.rgb(220,238,235)':'Color.rgb(214,238,229)',
        'Color.rgb(221,238,236)':'Color.rgb(214,238,229)',
        'Color.rgb(91,101,115)':'Color.rgb(96,126,118)',
        'Color.rgb(31,42,55)':'Color.rgb(15,77,67)'
    }
    for a,b in pairs.items(): s=s.replace(a,b)
    p.write_text(s,encoding='utf-8')

main=JAVA/'MainActivity.java'
replace(main,
    'private final int primary=Color.rgb(15,77,67),accent=Color.rgb(255,123,92),bg=Color.rgb(255,247,242),muted=Color.rgb(96,126,118),ink=Color.rgb(15,77,67);',
    'private final int primary=Color.rgb(15,77,67),accent=Color.rgb(255,123,92),bg=Color.rgb(255,247,242),muted=Color.rgb(96,126,118),ink=Color.rgb(15,77,67);')
replace(main,'root.setBackgroundColor(bg);','root.setBackgroundColor(ModernUi.screenBackground(this));')
replace(main,'hero.setBackground(round(primary,25));','hero.setBackground(freshHero());')
replace(main,'hero.setBackground(round(primary,28));','hero.setBackground(freshHero());')
replace(main,'Button add=premiumButton(tr("＋  مناسبة جديدة","＋  New event"));add.setTextColor(primary);add.setBackground(round(Color.WHITE,14));',
             'Button add=premiumButton(tr("＋  إضافة سريعة","＋  Quick add"));add.setTextColor(Color.WHITE);add.setBackground(round(accent,16));')
replace(main,'LinearLayout heroActions=new LinearLayout(this);heroActions.setPadding(0,dp(11),0,0);hero.addView(heroActions);',
             'LinearLayout heroActions=new LinearLayout(this);heroActions.setPadding(0,dp(11),0,0);hero.addView(heroActions);heroActions.setVisibility(View.GONE);')
replace(main,'private void renderToday(){Calendar start=Calendar.getInstance();', 'private void renderToday(){addFreshWelcome();Calendar start=Calendar.getInstance();')

welcome=r'''    private void addFreshWelcome(){
        long now=System.currentTimeMillis(),weekEnd=now+7L*86400000L,next=Long.MAX_VALUE;int week=0;EventStore.Event nearest=null;
        for(EventStore.Event e:EventStore.load(this)){long t=displayTime(e,now);if(t>=now&&t<weekEnd)week++;if(t>=now&&t<next){next=t;nearest=e;}}
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(17),dp(15),dp(17),dp(15));card.setBackground(round(ModernUi.isDark(this)?Color.rgb(20,66,57):Color.rgb(237,248,244),22));contentHost.addView(card,margin(-1,-2,0,0,0,dp(10)));
        TextView hi=text(tr("مرحبًا بك","Welcome"),22,true);hi.setTextColor(primary);card.addView(hi);
        TextView line=text(tr("نظّم مناسباتك بذكاء وبهدوء","Organize your moments simply and intelligently"),13,false);line.setTextColor(muted);line.setPadding(0,dp(3),0,dp(10));card.addView(line);
        LinearLayout stats=new LinearLayout(this);stats.setGravity(Gravity.CENTER_VERTICAL);card.addView(stats);
        TextView seven=text(tr("◷ خلال 7 أيام  ","◷ Next 7 days  ")+week,13,true);seven.setTextColor(primary);stats.addView(seven,new LinearLayout.LayoutParams(0,-2,1));
        TextView mark=text("✓",24,true);mark.setTextColor(accent);mark.setGravity(Gravity.CENTER);stats.addView(mark,new LinearLayout.LayoutParams(dp(42),dp(42)));
        if(nearest!=null){TextView nx=text(Categories.icon(nearest.category)+"  "+tr("الأقرب: ","Next: ")+nearest.title+"  •  "+relativeText(next),13,true);nx.setTextColor(ink);nx.setPadding(0,dp(9),0,0);card.addView(nx);}
    }

'''
if main.exists():
    s=main.read_text(encoding='utf-8')
    if 'private void addFreshWelcome()' not in s:
        s=s.replace('    private void renderToday(){',welcome+'    private void renderToday(){')
        main.write_text(s,encoding='utf-8')

# Replace v4.3 bottom navigation with the five-item Fresh Identity dock.
bottom=r'''    private void addBottomNavigation(LinearLayout root){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackground(round(ModernUi.surface(this),24));nav.setElevation(dp(7));
        bottomNav=new Button[5];
        bottomNav[0]=navButton("⌂\n"+tr("الرئيسية","Home"));
        bottomNav[1]=navButton("▦\n"+tr("التقويم","Calendar"));
        bottomNav[2]=navButton("＋");bottomNav[2].setTextSize(27);bottomNav[2].setTextColor(Color.WHITE);bottomNav[2].setBackground(round(accent,20));bottomNav[2].setElevation(dp(5));
        bottomNav[3]=navButton("≡\n"+tr("المناسبات","Events"));
        bottomNav[4]=navButton("•••\n"+tr("المزيد","More"));
        for(int i=0;i<bottomNav.length;i++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(60),i==2?.78f:1f);lp.setMargins(dp(2),0,dp(2),0);nav.addView(bottomNav[i],lp);}
        bottomNav[0].setOnClickListener(v->setMode(MODE_TODAY));bottomNav[1].setOnClickListener(v->setMode(MODE_CALENDAR));bottomNav[2].setOnClickListener(v->startActivity(new Intent(this,TemplateChooserActivity.class)));bottomNav[3].setOnClickListener(v->setMode(MODE_LIST));bottomNav[4].setOnClickListener(v->startActivity(new Intent(this,ModernCenterActivity.class)));
        root.addView(nav,margin(-1,dp(70),0,dp(6),0,0));updateBottomNavigation();
    }
    private Button navButton(String s){Button b=button(s,muted,Color.TRANSPARENT,11);b.setGravity(Gravity.CENTER);b.setTypeface(null,Typeface.BOLD);return b;}
    private void updateBottomNavigation(){if(bottomNav==null)return;int active=mode==MODE_TODAY?0:mode==MODE_CALENDAR?1:mode==MODE_LIST?3:-1;for(int i=0;i<bottomNav.length;i++){if(i==2)continue;boolean on=i==active;bottomNav[i].setTextColor(on?primary:muted);bottomNav[i].setBackground(round(on?ModernUi.alpha(V4Theme.mint(),58):Color.TRANSPARENT,16));}}

    private Button modeButton'''
sub(main,r'    private void addBottomNavigation\(LinearLayout root\)\{.*?    private Button modeButton',bottom)

# Fresh hero gradient helper.
if main.exists():
    s=main.read_text(encoding='utf-8')
    if 'private GradientDrawable freshHero()' not in s:
        s=s.replace('    private GradientDrawable round(int color,int r){',
'''    private GradientDrawable freshHero(){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{primary,Color.rgb(46,125,107)});g.setCornerRadius(dp(28));return g;}
    private GradientDrawable round(int color,int r){''')
        main.write_text(s,encoding='utf-8')

# Refine prominent secondary screens to coral accents and mint icon wells.
for name in ['TemplateChooserActivity.java','ModernCenterActivity.java']:
    p=JAVA/name
    replace(p,'private int primary,accent=Color.rgb(255,123,92);','private int primary,accent=Color.rgb(255,123,92);')
    replace(p,'Color.rgb(255,248,229)','Color.rgb(238,248,244)')

# Widget/resource palette migration.
for p in (ROOT/'res').rglob('*.xml'):
    s=p.read_text(encoding='utf-8')
    original=s
    for a,b in {
        '#195B56':'#0F4D43','#0E5A52':'#0F4D43','#D09738':'#FF7B5C','#E4B85B':'#FF7B5C',
        '#F7FAF9':'#FFF7F2','#F6F8FB':'#FFF7F2','#F4F7F9':'#FFF7F2','#DCE8E5':'#CDE8DE',
        '#E5ECEA':'#DDEEE8','#EBF4F3':'#EEF8F4'
    }.items(): s=s.replace(a,b)
    if s!=original:p.write_text(s,encoding='utf-8')

print('v4.4 Fresh Identity C applied')
