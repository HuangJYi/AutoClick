package fk.bug.newproj;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class FloatService extends Service {

    public static View floatView;

    private WindowManager windowManager;

    private FloatService that;
    {
        that = this;
    }

    public class FloatBinder extends Binder {
        public FloatService getService(){
            return FloatService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new FloatBinder();
    }

    @Override
    public void onDestroy(){
        windowManager.removeView(floatView);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void showFloatingWindow(int x, int y) {
        if (Settings.canDrawOverlays(this)) {
            // 获取WindowManager服务
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            // 新建悬浮窗控件
            final Button button = new Button(getApplicationContext());
            button.setId(R.id.floatViewId);
            button.setText("start");
            button.setBackgroundColor(Color.BLUE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    button.setText("change text!");
                    //浮窗点击事件未被AccessibilityService监听到，不知原因，改为静态调用
                    MyAccessibilityService.doTask();
                    //Toast.makeText(that, "float view onclick: "+getPackageName(), Toast.LENGTH_LONG).show();
                }
            });

            // 设置LayoutParam
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            /**
             * FLAG_NOT_TOUCH_MODAL 不捕获外围 touch 事件(否则拦截全屏触摸事件)
             * FLAG_NOT_FOCUSABLE   不捕获外围 keyboard 事件(否则外围不能弹出软键盘)
             * FLAG_LAYOUT_IN_SCREEN    坐标以屏幕为准，而非parent_view
             * FLAG_LAYOUT_NO_LIMITS    可以超出屏幕，而非贴边
             */
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    //    | WindowManager.LayoutParams.FLAG_FULLSCREEN  //悬浮窗本来就没有状态栏和导航栏
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            layoutParams.format = PixelFormat.RGBA_8888;
            layoutParams.width = 200;
            layoutParams.height = 100;
            //配置起始点，默认正中为0,0
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.x = x;
            layoutParams.y = y;   //负数不生效，全面屏不能遮盖底边

            // 将悬浮窗控件添加到WindowManager
            floatView = button;
            windowManager.addView(button, layoutParams);

            floatView.setVisibility(View.GONE);
        }
        /**
            动态View两种方式：LayoutInflater引入layout_xml   java代码直接生成对象

         private View addView1() {
         // TODO 动态添加布局(xml方式)
         LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
         LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
         //LayoutInflater inflater1=(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         //  LayoutInflater inflater2 = getLayoutInflater();
         LayoutInflater inflater3 = LayoutInflater.from(mContext);
         View view = inflater3.inflate(R.layout.block_gym_album_list_item, null);
         view.setLayoutParams(lp);
         return view;
         }
         */
    }
}
