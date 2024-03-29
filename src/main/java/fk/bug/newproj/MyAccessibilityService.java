package fk.bug.newproj;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.view.accessibility.AccessibilityEvent;

import fk.bug.newproj.pojo.MyGesture;

/* 对比
<callback链式调用多个dispatchGesture>
<一个path里多次使用lineTo>
    android根据path和duration计算轨迹节点，数据对应的滑动操作移速过快时，造成节点偏移
<willContinue:true>
    一次dispatchGesture里有断点，目标应用不一定有正确处理，而且可能被检测
 */

/**
 * 注册一个accSvc可以使用无障碍功能
 * accSvc.dispatchGesture       模拟手势
 * accSvc.onAccessibilityEvent  监听事件、模拟事件
 */
public class MyAccessibilityService extends AccessibilityService {
    /**
     * 注册的myAccSvc示例不一定具有AccSvc权限，须用户手动授权
     * 所以在myAccSvc.onServiceConnected()回调中补写ready标记供外部判断
     * 但是，AccSvc没有disconn接口？？
     */
    public static boolean ready = false;
    public static MyAccessibilityService that;
    private static MyGesture[] op = new MyGesture[9];
    static{
        op[0] = new MyGesture(2006,907,602);
        for(int i=1;i<op.length;i++)
            op[i] = new MyGesture(2001,404,105,404,404);
    }
    /*
    监听activity(或浮窗或别的事件)按钮事件
    触发(MyAccessbilityService) this.dispatchGesture()
     */
    private static int op_idx=0;
    private static GestureResultCallback gestureCallback = new GestureResultCallback(){
        @Override
        public void onCompleted(GestureDescription gestureDescription) {
            super.onCompleted(gestureDescription);

            op_idx++;
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(
                            op[op_idx].path, 0,
                            op[op_idx].duration, false);
            if(op_idx == op.length -1 ){
                that.dispatchGesture(
                        new GestureDescription.Builder().addStroke(stroke).build(),
                        null,null);
                op_idx = 0;
            }else {
                that.dispatchGesture(
                        new GestureDescription.Builder().addStroke(stroke).build(),
                        gestureCallback, null);
            }
        }
    };
    public static void doTask(){
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        op[op_idx].path,0,op[op_idx].duration,false);
        builder.addStroke(stroke);
        GestureDescription gesture = builder.build();
        that.dispatchGesture(gesture, gestureCallback, null);

        //willContinue:true 示例
//        Builder builder = new Builder();
//        StrokeDescription stroke = new StrokeDescription(p0,0,3000,true);
//        builder.addStroke(stroke);
//        GestureDescription gesture = builder.build();
//        GestureResultCallback callback = new GestureResultCallback(){
//            @Override
//            public void onCompleted(GestureDescription gestureDescription) {
//                super.onCompleted(gestureDescription);
//
//                // willContinue:false 手势完毕，触发activity onClick事件
//                StrokeDescription stroke2 = new StrokeDescription(p1,0,3000,false);
//                that.dispatchGesture(new Builder().addStroke(stroke2).build(), null,null);
//            }
//        };
//        boolean suc = that.dispatchGesture(gesture, callback, null);

        //dispatchGesture不能连用，即使正确设置了willContinue，也不能正确运行
//        StrokeDescription stroke3 = new StrokeDescription(p2,6500,3000,false);
//        that.dispatchGesture(new Builder().addStroke(stroke3).build(), null,null);
    }

    //必须
    //触发无障碍事件，理论上应该是自动手势执行之后，而非调用之前，为什么doTask没出问题？？
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
//        String info = String.format("捕获事件:pkg[%s] src[%s]",
//                event.getPackageName(),
//                event.getSource().getViewIdResourceName()
//                );
//        Toast.makeText(this, info, Toast.LENGTH_LONG).show();

    /**
        //以下是选取nodeInfo执行操作的方式
        // get the source node of the event
        AccessibilityNodeInfo nodeInfo = event.getSource();
        //注意，这里应该获取目标操作对象的nodeInfo

        // Use the event and node information to determine
        // what action to take

        // take action on behalf of the user
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        // recycle the nodeInfo object
        nodeInfo.recycle();
 */
    }

    //必须
    @Override
    public void onInterrupt() {
    }

    /**
     //AccessibilityService.onBind 被设为final，不允许应用向外再提供service
     //但是可以static在应用内调用
     public class AccessBinder extends Binder {
     public MyAccessibilityService getService(){
     return MyAccessibilityService.this;
     }
     }
     @Override
     public IBinder onBind(Intent intent) {
     return new AccessBinder();
     }
     */


    //可选,相当于service.onBind (在AccessibilityService中被设为final，可能是为了防止获取binder)
    @Override
    public void onServiceConnected() {
        ready = true;
        /*
        无障碍服务配置，主配置在Manifest中通过<meta-data android:resource>引用
        代码配置可在任何时候调用，动态修改配置，但只能修改以下5个属性
         */

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        //

        // 改用浮窗static调用无障碍，不监听事件
//        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED;
//                AccessibilityEvent.TYPES_ALL_MASK;
//                | AccessibilityEvent.TYPE_VIEW_FOCUSED;   //focused 用于判断适用VIEW

        // If you only want this service to work with specific applications, set their
        // package names here. Otherwise, when the service is activated, it will listen
        // to events from all applications.
        //实测，不配置packageNames默认本包(且全局侧边返回手势失效)，配置为len_0无法运行，""/"*"被理解为包名而非通配
//        info.packageNames = new String[]{getPackageName()};
                //{"com.example.android.myFirstApp", "com.example.android.mySecondApp"};

        // Set the type of feedback your service will provide.
        //实测可以不配置
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;

        // Default services are invoked only if no package-specific ones are present
        // for the type of AccessibilityEvent generated. This service *is*
        // application-specific, so the flag isn't necessary. If this was a
        // general-purpose service, it would be worth considering setting the
        // DEFAULT flag.

        info.flags = AccessibilityServiceInfo.DEFAULT;

        info.notificationTimeout = 100;

        this.setServiceInfo(info);
        that = this;
    }
}
