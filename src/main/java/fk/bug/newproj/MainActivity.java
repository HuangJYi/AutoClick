package fk.bug.newproj;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import fk.bug.newproj.pojo.MyGesture;
import fk.bug.newproj.tools.UserCode;
import fk.bug.newproj.tools.WinMng;
import fk.bug.newproj.vm.Code;
import fk.bug.newproj.vm.VM;

public class MainActivity extends AppCompatActivity {
    public static Activity that; //给vm.executor使用

    //pst 辅助显示位置浮窗
    private boolean showPst = false;
    private LinearLayout pstView;
    private WindowManager.LayoutParams pst_wlp;

    private boolean showFloat = false;
    //private boolean floatSrvcBind = false;    //暂不使用service
    private LinearLayout floatView;
    private WindowManager.LayoutParams float_wlp;

    private int checkedCodes;   //0b0101 最低位为index_0，0b1 << 1循环即可遍历每一位所表示的true/false

    /**
     * 1.编译后，记录到codeListArr
     * 2.显示悬浮窗时，加载sharesPreferrence的codeStr到codeListArr
     * 3.运行时从此设置vm.codeArr
     */
    List<Code>[] codeListArr = new List[12];

    /**
     * floatSvc才需要绑定，accSvc在floatSvc的onClick中static调用即可
     */
    //此时floatSvc尚未实例化new Intent()不报错但闪退
    //private Intent floatItt = new Intent(this,FloatService.class);;
    public Service floatService;
    ServiceConnection floatConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            floatService = ((FloatService.FloatBinder)service).getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) { }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        that = this;
        UserCode.updateCodeList(this);
    }
    @Override
    protected void onDestroy(){
        if(showPst) WinMng.removeFloat(pstView,this);
        if(showFloat) WinMng.removeFloat(floatView,this);
        super.onDestroy();
    }

    public void floatPstToast(View view) {
        Toast.makeText(this,"点击了悬浮坐标",Toast.LENGTH_SHORT).show();
    }

    //没找到 动态判断无障碍权限 的API
    public void grantAccess(View view) {
        if(showPst) {
            WinMng.removeFloat(pstView,this);
            showPst = false;
        }
        if(showFloat) {
            WinMng.removeFloat(floatView,this);
            showFloat = false;
        }
        startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),0);
    }
    /* 悬浮窗权限
        1.配置<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
        2.手动授权 Settings.ACTION_MANAGE_OVERLAY_PERMISSION
     */
    public void grantFloat(View view) {
        Intent itt_setting = new Intent();
        itt_setting.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        itt_setting.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(itt_setting, 0);
    }
    public void stopFloat(View view) {
        Intent floatItt = new Intent(this,FloatService.class);
        stopService(floatItt);
    }

    public void showHideFloat(View view){
        //暂不使用服务
        //Intent floatItt = new Intent(this,FloatService.class);
        //bindService(floatItt,floatConn, Context.BIND_AUTO_CREATE);
        if(checkedCodes == 0){
            Toast.makeText(this,"尚未勾选代码", Toast.LENGTH_SHORT).show();
            return;
        }

        if(floatView == null){
            //这里与pstView不同，floatView仅是容器，float_user.xml充气为组件再添加到floatView
            floatView = new LinearLayout(this);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            floatView.setLayoutParams(llp);
        }
        if(float_wlp == null){
            // 设置LayoutParam
            float_wlp = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                float_wlp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                float_wlp.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            float_wlp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    //    | WindowManager.LayoutParams.FLAG_FULLSCREEN  //悬浮窗本来就没有状态栏和导航栏
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            float_wlp.format = PixelFormat.RGBA_8888;
            float_wlp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            float_wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            //配置起始点，默认正中为0,0
            float_wlp.gravity = Gravity.TOP | Gravity.START;
        }

        if(showFloat){
            //hide
            showFloat = false;
            WinMng.removeFloat(floatView, this);
        }else{
            //show
            showFloat = true;
            EditText etX = findViewById(R.id.float_x);
            EditText etY = findViewById(R.id.float_y);
            int x = Integer.parseInt(etX.getText().toString());
            int y = Integer.parseInt(etY.getText().toString());

            updateFloatViewBtn();

            WinMng.addFloat(floatView, float_wlp,this,x,y);
        }
    }
    private void updateFloatViewBtn(){
        floatView.removeAllViews();

        int bb=0b1,idx=0;
        while(bb>0){
            if((checkedCodes & bb) != 0){
                LinearLayout ll = new LinearLayout(this);
                LayoutInflater inflater = LayoutInflater.from(this);
                inflater.inflate(R.layout.float_user, ll);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                ll.setLayoutParams(llp);

                TextView tv = (TextView)(ll.getChildAt(0));
                SharedPreferences sp = UserCode.getContextSP(this);
                String title = sp.getString(UserCode.CODE_TITLE +idx,null);
                tv.setText(title.length() < 2 ? title : ""+title.charAt(0));
                tv.setTag(idx);

                floatView.addView(ll);

                loadCodeList(idx);
            }
            idx++;
            bb<<=1;
        }
    }
    private void loadCodeList(int idx){
        if(codeListArr[idx] != null) return;
        SharedPreferences sp = UserCode.getContextSP(this);
        String cc = sp.getString(UserCode.CODE_COMPILE +idx,null);
        if(cc == null){
            Toast.makeText(this,
                    String.format("找不到代码[%d]的编译结果",idx),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //解析字符串
        codeListArr[idx] = Code.readCodeListFromStr(cc);
    }

    public void showHidePosition(View view) {
        if(pstView == null){
            //float_position.xml先动态添加到viewGroup，再把viewGroup添加到windowManager
            pstView = new LinearLayout(this);
            LayoutInflater inflater = LayoutInflater.from(this);
            inflater.inflate(R.layout.float_position, pstView);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            pstView.setLayoutParams(llp);
            //pstView.setVisibility(View.GONE);
        }
        if(pst_wlp == null){
            // 设置LayoutParam
            pst_wlp = new WindowManager.LayoutParams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pst_wlp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                pst_wlp.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            pst_wlp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                //    | WindowManager.LayoutParams.FLAG_FULLSCREEN  //悬浮窗本来就没有状态栏和导航栏
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            pst_wlp.format = PixelFormat.RGBA_8888;
            pst_wlp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            pst_wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            //配置起始点，默认正中为0,0
            pst_wlp.gravity = Gravity.TOP | Gravity.START;
            //pst_wlp.x = x;
            //pst_wlp.y = y;   //负数不生效，全面屏不能遮盖底边
        }

        if(showPst){
            //hide
            showPst = false;
            WinMng.removeFloat(pstView, this);
        }else{
            //show
            showPst = true;
            EditText etX = findViewById(R.id.sample_x);
            EditText etY = findViewById(R.id.sample_y);
            int x = Integer.parseInt(etX.getText().toString());
            int y = Integer.parseInt(etY.getText().toString());

            float dp2px = getResources().getDisplayMetrics().density;
            int bios = (int)(25 * dp2px + 0.5);
            x -= bios;
            y -= bios;

            WinMng.addFloat(pstView, pst_wlp,this,x,y);
        }
    }

    public void btnNewCode(View view) {
        if(UserCode.codeCnt > 10){
            Toast.makeText(this,"代码页不能超过十个",Toast.LENGTH_LONG).show();
            return;
        }
        EditText title = (EditText)findViewById(R.id.code_title);
        EditText code = (EditText)findViewById(R.id.code);
        title.setText("输入标题"+new Date().getTime());
        code.setText("//代码");
    }

    public void btnChooseCode(View view) {
        LinearLayout edit = (LinearLayout)findViewById(R.id.code_edit_area);
        edit.setVisibility(View.GONE);
        LinearLayout list = (LinearLayout)findViewById(R.id.code_list_area);
        list.setVisibility(View.VISIBLE);
        UserCode.updateCodeList(this);
    }

    //勾选，保存在activity本地checkedCodes，显隐float_window时把checkedCodes传递给floatService
    public void checkCodeFromList(View view){
        Object tag = view.getTag();
        if(tag == null) return;
        int i = tag instanceof Integer ? (Integer)tag : Integer.parseInt(tag.toString());

        SharedPreferences sp = UserCode.getContextSP(this);
        String codeStr = sp.getString(UserCode.CODE_COMPILE + i, null);
        if( codeStr == null){
            CheckBox cb = (CheckBox)view;
            cb.setChecked(false);
            Toast.makeText(this,"代码未编译",Toast.LENGTH_SHORT).show();
        }else{
            int b = 0b1 << i;
            checkedCodes ^= b;
        }

        StringBuilder sb = new StringBuilder("已选：");
        int bb=0b1,idx=0;
        while(bb>0){
            if((checkedCodes & bb) != 0){
                sb.append(idx).append(" ");
            }
            idx++;
            bb<<=1;
        }
        Toast.makeText(this,sb.toString(),Toast.LENGTH_SHORT).show();
    }
    public void chooseCodeFromList(View view){
        Object tag = view.getTag();
        if(tag == null) return;
        int i = tag instanceof Integer ? (Integer)tag : Integer.parseInt(tag.toString());
        UserCode.editCodeIdx = i;
        SharedPreferences sp = UserCode.getContextSP(this);
        String title = sp.getString(UserCode.CODE_TITLE +i,null);
        String code = sp.getString(UserCode.CODE +i,null);

        findViewById(R.id.code_list_area).setVisibility(View.GONE);
        findViewById(R.id.code_edit_area).setVisibility(View.VISIBLE);
        ((EditText)findViewById(R.id.code_title)).setText(title);
        ((EditText)findViewById(R.id.code)).setText(code);
        ((EditText)findViewById(R.id.output)).setText("");
    }

    //12345 -> 1345 暂时不支持
    public void deleteCode(View view){
        Object tag = view.getTag();
        if(tag == null) return;
        int i = tag instanceof Integer ? (Integer)tag : Integer.parseInt(tag.toString());

    }

    public void saveCode(View view) {
        SharedPreferences sp = UserCode.getContextSP(this);
        SharedPreferences.Editor edt = sp.edit();
        String title = ((EditText)(findViewById(R.id.code_title))).getText().toString();
        String code = ((EditText)findViewById(R.id.code)).getText().toString();
        edt.putString(UserCode.CODE_TITLE +UserCode.editCodeIdx, title);
        edt.putString(UserCode.CODE +UserCode.editCodeIdx, code);
        edt.commit();
    }

    public static VM vm;    //仅编译、试运行
    public void compileCode(View view) {
        String code = ((EditText)findViewById(R.id.code)).getText().toString();
        if(vm == null) vm = new VM();
        boolean suc = vm.compile(code);

        EditText et = findViewById(R.id.output);
        if(!suc){
            et.setText(vm.getErrStr());
            return;
        }
        String cc = vm.getCompiledCode();
        et.setText(cc);

        SharedPreferences sp = UserCode.getContextSP(this);
        SharedPreferences.Editor edt = sp.edit();
        edt.putString(UserCode.CODE_COMPILE +UserCode.editCodeIdx, cc);
        edt.commit();

        codeListArr[UserCode.editCodeIdx] = vm.memory.codeArr;
    }

    //!!!??? 千万注意不要非主线程修改view，会报各种不同的异常，甚至可能不报异常
    public void testCode(View view) {
        if(vm == null) {
            EditText et = findViewById(R.id.output);
            et.setText("未编译");
            return;
        }
        EditText et = findViewById(R.id.output);
        et.setText("");

        int mode = Integer.parseInt(view.getTag().toString());

        vm.changeVmMode(mode);
        vm.execute();
    }

    public void runCode(View view) {
        if(vm == null) {
            vm = new VM();
        }
        vm.changeVmMode(VM.MODE_FORMAL);

        Object tag = view.getTag();
        if(tag == null) return;
        int i = tag instanceof Integer ? (Integer)tag : Integer.parseInt(tag.toString());

        if(codeListArr[i] == null) return;
        vm.memory.codeArr = codeListArr[i];
        vm.execute();
    }

    public void showUnprocessedError(View view){
        if(vm == null) return;
        EditText et = findViewById(R.id.output);
        et.setText(vm.getErrStr());
    }

    public void clearUnprocessedError(View view){
        if(vm == null) return;
        vm.err.set(VM.Error.NO_ERROR, "");
        EditText et = findViewById(R.id.output);
        et.setText(vm.getErrStr());
    }

}
