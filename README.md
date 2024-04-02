# AutoClick
安卓、低代码中文编程、无障碍模拟点击拖拽、游戏辅助软件。

A script-based low-code programming, accessible click-and-drag simulation, helper software for Android, designed for gaming assistance.

娱乐作品，非专业安卓开发，java7古董代码，您可随意修改使用在其他app。

Entertainment piece, amateur Android development, Java 7 relics, feel free to modify and use in other game and application.

|游戏|角色|操作|视频地址|
|-|-|-|-|
|原神|纳维莱特|转圈AOE|https://www.bilibili.com/video/BV1TD42157nn/ |
|原神|散兵|染水17重击|https://www.bilibili.com/video/BV1qj41157dp/ |
|原神||剧情连点||

内置脚本：

Built-in script:

- calculating
```java
edt.putString(CODE_TITLE+0, "算数.例");
edt.putString(CODE+0,
  "//计算并输出\n" +
  "//按钮：保存->编译->试运行(输出结果)\n" +
  "数据 abc = 1+ (6+2*3-9) /3 \n" +
  "输出 abc");
```

- time && loop && if && closure
```java
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
```

- simulate click
```java
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
```


- game operation: Genshin Impact, Navi-Lite, spinning AoE
```java
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
```

- 停止其他正在运行的模拟操作流程。
- 请确保勾选了此脚本。这个按钮可能需要快速按多次，因为其他脚本的频繁操作，有可能打断你刚刚进行的按“停止”按钮的点击行为
- STOP CURRENT RUNNING SCRIPT.
- Please confirm that this script is checked before formally using scripts from the floating window.
- You may need to click this script button multiple times, because other frequent operations from script might interrupt your recent click on the "stop" button.
```java
edt.putString(CODE_TITLE+4, "婷芷！");
edt.putString(CODE+4,
  "//停止浮窗运行中的其他代码\n"+
  "//一定要勾选“婷芷”！！否则万一脚本写出无限循环，也许只能重启了嗷\n"+
  "//可能需多次点击！！\n"+
  "//因为正在运行的反复快速模拟点击，有可能打断刚点的“婷芷”点击！！\n"+
  "//按钮：保存->编译->代码列表勾选->浮窗按钮\n" +
  "数据 a");
```

- game operation: Genshin Impact, Scaramouche, perform 17 consecutive heavy attacks infused with Hydro element."
```java
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
```

- game operation: Genshin Impact, tap to progress through the storyline dialogue."
```java
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
```

