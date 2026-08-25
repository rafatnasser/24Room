from pathlib import Path

p=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')
s=p.read_text(encoding='utf-8')
s=s.replace('settings.setOnClickListener(v->showSettings());','settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));')
s=s.replace('root.setBackgroundColor(bg);root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);','root.setBackgroundColor(bg);root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);root.setLayoutTransition(new android.animation.LayoutTransition());')
s=s.replace('root.addView(hero,margin(-1,-2,0,0,0,dp(9)));','root.addView(hero,margin(-1,-2,0,0,0,dp(9)));hero.setAlpha(0f);hero.setTranslationY(dp(-8));hero.animate().alpha(1f).translationY(0f).setDuration(320).start();')
s=s.replace('if(contentHost==null)return;contentHost.removeAllViews();','if(contentHost==null)return;contentHost.removeAllViews();contentHost.setAlpha(0.86f);contentHost.animate().alpha(1f).setDuration(180).start();')
p.write_text(s,encoding='utf-8')

settings=Path('app/src/main/java/com/rafat/munasabati/SettingsActivity.java')
t=settings.read_text(encoding='utf-8')
t=t.replace('import android.transition.LayoutTransition;','import android.animation.LayoutTransition;')
settings.write_text(t,encoding='utf-8')
