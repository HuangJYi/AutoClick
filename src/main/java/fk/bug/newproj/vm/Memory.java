package fk.bug.newproj.vm;

import android.accessibilityservice.GestureDescription;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.function.Function;
import fk.bug.newproj.MainActivity;
import fk.bug.newproj.MyAccessibilityService;
import fk.bug.newproj.R;
import fk.bug.newproj.pojo.MyGesture;

public class Memory {
    //逻辑控制单元，计算单元，存储单元
    private VM vm;  //VM.that
    public Memory(VM vm){
        vm = vm;
    }
    //寄存器
    public Register register = new Register();
    //代码区，DSL处理后的字节码
    public List<Code> codeArr;
    //runtime 函数调用栈 (暂不支持)
    //List<Object> involkStack = new ArrayList<>();
    //栈区
    public List<Integer> memoryStack;   //Closure.varNameMemoryMap

    //全局、静态区
    //!!!???    StaticInfo还可扩展配置/编译函数参数类型
    public static List<Function<int[], Integer>> memoryStatic = new ArrayList<>();
    public static Map<Object, StaticInfo> staticInfoByWordType = new HashMap<>();
    public static List<StaticInfo> staticInfoByIndex = new ArrayList<>();
    //编译需要根据word找memory_static_index,执行时需要根据memory_static_index找func_arg_count
    static class StaticInfo{
        public int index;
        public int argc;
        private StaticInfo(int index,int argc){
            this.index = index;
            this.argc = argc;
        }
    }

    /**
     * 统一使用Function.apply(List)替代Supplier.get/Consumer.accept/Predicate.test
     * 可避免compiler/executor反复校验、配置参数和返回值
     */
    static {
        int idx = 0;
        StaticInfo si;
        //0 println()
        si = new StaticInfo(idx++,1);
        staticInfoByWordType.put("输出",si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] a0) {
                if(VM.that.runMode == VM.MODE_DEV){
                    System.out.println(String.format("输出[%d],线程%s",a0[0],Thread.currentThread()));
                }else if(VM.that.runMode == VM.MODE_USER_TEST){
                    if(MainActivity.that == null) return -1;
                    ((TextView)MainActivity.that.findViewById(R.id.output))
                            .append(String.format("%d\n",a0[0]));
                }//浮窗功能正式（异步）运行不输出
                return 0;
            }
        });
        //1 now()
        si = new StaticInfo(idx++,0);
        staticInfoByWordType.put("时间",si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] unuse) {
                return 0x7fffffff & (int)(System.currentTimeMillis());
            }
        });
        //2 sleep(1)
        si = new StaticInfo(idx++,1);
        staticInfoByWordType.put("暂停",si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                //executor必须独立线程，否则会影响浮窗的按钮响应
                VM.that.executor.sleep(arr[0]);
                return 0;
            }
        });
        //3 doGesture(5)
        si = new StaticInfo(idx++,5);
        staticInfoByWordType.put("划屏",si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                if(VM.that.runMode == VM.MODE_DEV) return -1;
                try{
                    MyGesture g = new MyGesture(arr);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    /**
                     * willContinue * exec.sleep * sleep_time
                     * 根据实际目标控件、应用、平台选择，表现可能不一样
                     * 比如 willContinue true, 连续点击原生控件不生效，但是再游戏里表现非常好
                     *
                     * err: willCt + no_sleep 大量手势事件几乎同时触发，只执行（单位事件内上限个数）最后一个
                     */
                    GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                            g.path,0,g.duration,false);
                    builder.addStroke(stroke);
                    GestureDescription gesture = builder.build();
                    MyAccessibilityService.that.dispatchGesture(gesture, null, null);

                    //异步运行时，子线程需要等待主线程执行完毕该手势
                    /** 为什么要加50间隔：
                     * 1.dispatchGesture本身有一定异步事件时延
                     * 2.如果两个gesture_duration有重叠，后一个gesture打断前一个
                     * 3.由于[目标应用的操作处理方式]，有可能造成两个gesture都解析失败！
                     */
                    /*  责任专一，不再手势聚合暂停行为
                        在低代码中暂停
                    if(VM.that.runMode == VM.MODE_FORMAL){
                        //exec.sleep 有interrupt处理，不适用Thread.sleep(g.duration + 50);
                        VM.that.executor.sleep(g.duration + MyGesture.protectSleep);
                    }*/
                }catch (Exception e){
                    VM.that.err.set(VM.Error.UNKNOWN_ERROR, "未授权无障碍\n");
                    return -1;
                }
                return 0;
            }
        });

        //4
        si = new StaticInfo(idx++,1);
        staticInfoByWordType.put(Operator.NOT.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] == 0 ? 1 : 0;
            }
        });
        //5
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.ADD.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] + arr[1];
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.SUBSTRACT.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] - arr[1];
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.MULTIPLY.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] * arr[1];
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.DIVIDE.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] / arr[1];
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.MOD.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] % arr[1];
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.BIGGER.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] > arr[1] ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.BIGGER_EQUAL.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] >= arr[1] ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.SMALLER.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] < arr[1] ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.SMALLER_EQUAL.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] <= arr[1] ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.EQUAL.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] == arr[1] ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.NOT_EQUAL.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return arr[0] != arr[1] ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.AND.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return (arr[0]!=0) && (arr[1]!=0) ? 1:0;
            }
        });
        si = new StaticInfo(idx++,2);
        staticInfoByWordType.put(Operator.OR.type,si);
        staticInfoByIndex.add(si);
        memoryStatic.add(new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] arr) {
                return (arr[0]!=0) || (arr[1]!=0) ? 1:0;
            }
        });
        //赋值运算,需要修改vm.memStack，本代码块静态执行，不方便改变结构，故放在executor中检查执行
    }


}

class Register{
    RegisterUnit[] units = new RegisterUnit[20];
    {
        for(int i=0;i<units.length;i++){
            units[i] = new RegisterUnit();
        }
    }
    //getLastReturn不对上层提供，仅用于source拆分CodeArr时保存表达式结果
    //如 if(expr){}, 拆分为 Code[expr] -> Code[if(getLastReturn)_goto]
    int getLastReturn(){
        return units[0].value;
    }
    //表达式开始，说明上一表达式的计算结果r0不再被需要
    void exprStart(){
        units[0].recycle();
    }
    void recycleRegister(int i){
        units[i].recycle();
    }
    void set(int i,int val){
        units[i].set(val);
    }
    int get(int i){return units[i].value;}
    int getIdleRegisterIndex(){
        for(int i=0;i<units.length;i++){
            if(!units[i].used){
                units[i].hold();
                return i;
            }
        }
        return -1;
    }
}
class RegisterUnit{
    boolean used = false;
    int value = 0;
    boolean hold(){ //实际是单线程
        if(!used){
            return used = true;
        }
        return false;
    }
    void recycle(){
        this.used = false;
        this.value = 0;
    }
    void set(int value){
        this.value = value;
    }
}

