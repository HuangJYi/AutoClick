package fk.bug.newproj.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import fk.bug.newproj.R;

public class UserCode {

    private static SharedPreferences sp;
    public static int codeCnt;
    public static int editCodeIdx;


    public static String CODE_COUNT = "codeCnt";

    public static String CODE_TITLE = "code_title_";
    public static String CODE = "code_";
    public static String CODE_COMPILE = "code_compile_";

    /**
     * 似乎ctt不影响shared
     */
    public static SharedPreferences getContextSP(Context ctt){
        if(sp == null) {
            sp = ctt.getSharedPreferences("sp", Context.MODE_PRIVATE);
            codeCnt = sp.getInt(CODE_COUNT,0);
            if(codeCnt==0){
                SharedPreferences.Editor edt = sp.edit();
                edt.putString(CODE_TITLE+0, "算数.例");
                edt.putString(CODE+0,
                        "//计算并输出\n" +
                        "//按钮：保存->编译->试运行(输出结果)\n" +
                        "数据 abc = 1+ (6+2*3-9) /3 \n" +
                        "输出 abc");

                edt.putString(CODE_TITLE+1, "时间统计.例");
                edt.putString(CODE+1,
                                "//统计代码运行时间\n"+
                                "//按钮：保存->编译->试运行(输出结果)\n" +
                                "数据 t0 = 时间\n"+
                                "数据 i=0\n"+
                                "数据 max = 100\n"+
                                "重复 i < max\n"+
                                "{\n"+
                                    "i = i+1\n"+
                                    "判断 i == 1 || i == max \n"+
                                    "{\n"+
                                        "输出 i\n"+
                                    "}\n"+
                                "}\n"+
                                "数据 t1 = 时间\n"+
                                "数据 t = t1 - t0\n"+
                                "输出 t\n");

                edt.putString(CODE_TITLE+2, "点击[显/隐坐标].例");
                edt.putString(CODE+2,
                                "//模拟点击“显/隐坐标”按钮\n" +
                                "//注意使用“显/隐坐标”功能，根据本机像素修改坐标\n" +
                                "//ab应位于按钮上，xy应与“显/隐坐标”后的数值相同\n" +
                                "//按钮：保存->编译->试运行(模拟点击)\n" +
                                "数据 a = 50\n"+
                                "数据 b = 450\n"+
                                "数据 x = 50\n"+
                                "数据 y = 450\n"+
                                "划屏 a b a b 10\n"+
                                "暂停 2000\n"+
                                "划屏 x y x y 10\n");

                edt.putString(CODE_TITLE+3, "转龙.原神");
                edt.putString(CODE+3,
                        "//注意使用“显/隐坐标”功能，根据本机像素修改坐标\n" +
                        "//按钮：保存->编译->代码列表勾选->浮窗按钮\n" +
                        "数据 x = 1920\n"+
                        "数据 y = 820\n"+
                        "数据 x2 = 400\n"+
                        "数据 t = 时间\n"+
                        "重复 时间 < t + 11000\n"+
                        "{\n"+
                            "划屏 x y x2 y 500\n"+
                            "暂停 550\n"+
                        "}\n");

                edt.putString(CODE_TITLE+4, "婷芷！");
                edt.putString(CODE+4, "//停止浮窗运行中的其他代码\n"+
                        "//一定要勾选“婷芷”！！否则万一脚本写出无限循环，也许只能重启了嗷\n"+
                        "//可能需多次点击！！\n"+
                        "//因为正在运行的反复快速模拟点击，有可能打断刚点的“婷芷”点击！！\n"+
                        "//按钮：保存->编译->代码列表勾选->浮窗按钮\n" +
                        "数据 a");

                edt.putString(CODE_TITLE+5, "散重.原神");
                edt.putString(CODE+5,
                        "//注意使用“显/隐坐标”功能，根据本机像素修改坐标\n" +
                        "//按钮：保存->编译->代码列表勾选->浮窗按钮\n" +
                        "数据 ax = 1920\n"+
                        "数据 ay = 820\n"+
                        "数据 ex = 1730\n"+
                        "数据 ey = 930\n"+
                        "划屏 ex ey ex ey 11\n"+
                        "暂停 450\n"+
                        "数据 t = 时间\n"+
                        "重复 时间 < t +13000\n"+
                        "{\n"+
                        "划屏 ax ay ax ay 300\n"+
                        "暂停 357\n"+
                        "}\n");

                edt.putString(CODE_TITLE+6, "剧情跳过.原神");
                edt.putString(CODE+6,
                        "//剧情对话可能有多个按钮，依次点击\n"+
                        "数据 x = 1600\n"+
                        "数据 y1 = 720\n"+
                        "数据 y2 = 500\n"+
                        "重复 10000\n"+
                        "{\n"+
                                "划屏 x y1 x y1 19\n"+
                                "暂停 300\n"+
                                "划屏 x y2 x y2 17\n"+
                                "暂停 300\n"+
                        "}\n");

                String q = "懒癌突发";
                String w = "//不想写增减按钮了哦~~";
                edt.putString(CODE_TITLE+7, q);
                edt.putString(CODE+7,w);
                edt.putString(CODE_TITLE+8, q);
                edt.putString(CODE+8,w);
                edt.putString(CODE_TITLE+9, q);
                edt.putString(CODE+9,w);
                edt.putString(CODE_TITLE+10, q);
                edt.putString(CODE+10,w);
                edt.putString(CODE_TITLE+11, q);
                edt.putString(CODE+11,w);
                edt.putString(CODE_TITLE+12, q);
                edt.putString(CODE+12,w);
                edt.putString(CODE_TITLE+13, q);
                edt.putString(CODE+13,w);
                edt.putString(CODE_TITLE+14, q);
                edt.putString(CODE+14,w);
                edt.putString(CODE_TITLE+15, q);
                edt.putString(CODE+15,w);
                edt.putString(CODE_TITLE+16, q);
                edt.putString(CODE+16,w);
                edt.putString(CODE_TITLE+17, q);
                edt.putString(CODE+17,w);
                edt.putString(CODE_TITLE+18, q);
                edt.putString(CODE+18,w);
                edt.putString(CODE_TITLE+19, q);
                edt.putString(CODE+19,w);

                codeCnt = 20;

                edt.putInt(CODE_COUNT, codeCnt);
                edt.commit();
            }
        }
        return sp;
    }

    public static void saveCodeTitle(Context ctt, int codeIdx, String title){
        SharedPreferences sp = getContextSP(ctt);
        SharedPreferences.Editor edt = sp.edit();
        edt.putString(CODE_TITLE+codeIdx, title);
        edt.commit();
    }
    public static void saveCode(Context ctt, int codeIdx, String codeStr){
        SharedPreferences sp = getContextSP(ctt);
        SharedPreferences.Editor edt = sp.edit();
        edt.putString(CODE+codeIdx, codeStr);

        edt.putInt(CODE_COUNT, ++codeCnt);
        edt.commit();
    }
    public static void compileCode(Context ctt, int codeIdx, String codeStr){
        SharedPreferences sp = getContextSP(ctt);
        SharedPreferences.Editor edt = sp.edit();
        edt.putString(CODE_COMPILE+codeIdx, codeStr);
        edt.commit();
    }

    public static void updateCodeList(Activity ctt) {
        SharedPreferences sp = getContextSP(ctt);
        for(int i=0;i<codeCnt;i++) {
            String title = sp.getString(CODE_TITLE + i, null);
            LinearLayout list = ctt.findViewById(R.id.code_list_area);
            if( i >= list.getChildCount()){  //有新增的代码，需要增加列表项
                LayoutInflater li = LayoutInflater.from(ctt);
                li.inflate(R.layout.sample_code_list_item, list);//(返回值是root)list
            }
            LinearLayout ll = (LinearLayout) list.getChildAt(i);
            // 口title (有无编译文件)      删除
            CheckBox cb = (CheckBox)(ll.getChildAt(0));
            cb.setTag(i);
            //cb.setChecked();
            TextView tt = (TextView)(ll.getChildAt(1));
            tt.setText(title);
            tt.setTag(i);
            TextView cpl = (TextView)(ll.getChildAt(2));
            boolean compiled = sp.getString(CODE_COMPILE+i,null) != null;
            cpl.setText(compiled ? "(有编译文件)":"(无编译文件)");
            if(!compiled){
                cb.setActivated(false);
            }
            Button del = (Button)(ll.getChildAt(3));
            del.setTag(i);
        }
    }
}
