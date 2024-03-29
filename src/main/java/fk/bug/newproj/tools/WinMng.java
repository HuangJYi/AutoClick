package fk.bug.newproj.tools;

import android.content.Context;
import android.content.ContextWrapper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

public class WinMng {
    /**
     * 应用的Context，不随activity关闭，每次打开app的浮窗独立，应该在浮窗内触发关闭浮窗
     * (WindowManager)actvOrSvc.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
     * activity/service 的Context，随activity关闭，service未知
     * (WindowManager)actvOrSvc.getSystemService(Context.WINDOW_SERVICE);
     * Activity的Context
     * activity.getWindowManager();
     */
    //不能static, activity和service的wm不同
    //public static WindowManager wm;

    //增删悬浮窗
    //还有个 updateView(v, wlp) api
    public static void removeFloat(View view, ContextWrapper actvOrSvc){
        if (Settings.canDrawOverlays(actvOrSvc)) {
            WindowManager wm = (WindowManager) actvOrSvc.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(view);
        }
    }
    public static void addFloat(View view, WindowManager.LayoutParams wlp,
                                ContextWrapper actvOrSvc, int x, int y){
        if (Settings.canDrawOverlays(actvOrSvc)) {
            WindowManager wm = (WindowManager) actvOrSvc.getSystemService(Context.WINDOW_SERVICE);
            // 修改窗体参数
            wlp.x = x;
            wlp.y = y;
            // 将悬浮窗控件添加到WindowManager
            wm.addView(view, wlp);
        }
    }

}
