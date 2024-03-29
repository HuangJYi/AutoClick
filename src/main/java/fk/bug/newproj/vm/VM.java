package fk.bug.newproj.vm;

/* 例：水龙转圈3秒
#注释


异步：dispatcherGesture -> callback 中调用script-vm继续执行代码

代码域：
0   repeat 3{
1       long-touch 80 80 800    #有可能按下的操作因为网络延迟被忽略，导致操作失败，须测试
2       number t0 = now
3       repeat now < t0 + 3000 {
4           drag 80 40 10 40 400
5       }                       # goto 3
6   }                           # goto 0
* 程序区的机器码并不是直接执行，而是CPU拿到机器码，对比指令集，操作寄存器和运算器获取运算结果
* 优化为编译器：
    代码处理为 arr[] Code(line-index, Logic)
        Logic: func(param)
            0 - repeat 3 --->
                var     const['repeat_1'] = 3
                Logic   params = [3]
                        func = repeat_n(params){
                            if( compare ( params['repeat_1'], param[0])) goto(line+1)
                            else goto(line_end)
                        }

repeat number:  如果 block-stack-idx>0 goto下一行或退出行
repeat condition:   获取 condition-map 键值，解释并计算"now < t0+3000"，goto下一行或退出行
 */

/*
时间  now
数据  number a = 0

点按  touch x y
长按  long-touch x y t
划屏  drag fx fy tx ty t
暂停  sleep t

重复  repeat repeat-times{}
repeat condition{}
判断  if condition{}
跳行  goto line-number
 */

//js-bridge


import java.util.function.Consumer;


/**
 * 过期策略：
 * 运行期反复把时间信息写入Code字节码
 */
public class VM {
    public static VM that = null;

    //真机异步运行executor，非main-thread不能获取view_output
    public Error err = new Error();

    public Memory memory;
    public Compiler compiler;
    public Executor executor;
    public Thread executeThread;

    public int runMode;
    public static int MODE_DEV = 0;
    public static int MODE_USER_TEST = 1;
    public static int MODE_FORMAL = 2;

    public VM(){
        this(MODE_DEV);
    }
    public VM(int m){
        that = this;
        this.runMode = m;
        this.memory = new Memory(this);
        this.compiler = new Compiler(this);
        //this.executor = new Executor(this);
        // new Executor(vm)中设置vm.executor
        // 目前不支持多线程，新asyncExecute会挂起前一个executor_thread (线程interrupt,执行进度保留)
    }

    public boolean compile(String src){
        return this.compiler.compile(src);
    }

    public void changeVmMode(int m){
        this.runMode = m;
    }
    public void execute(){
        if(runMode == VM.MODE_FORMAL){
            asyncExecute();
        }else{
            new Executor(this).execute();
        }
    }
    public void asyncExecute(){
        suspendExecutor();

        Thread newThrd = new Thread(new Runnable() {
            @Override
            public void run() {
                new Executor(VM.that).execute();
            }
        });

        executeThread = newThrd;
        newThrd.start();
    }

    /**
     * 1.暂停继续必然是多线程
     * 2.线程会运行完毕，但执行进度还在 executor 中，可以另开线程继续执行
     */
    public void suspendExecutor(){
        if(executeThread != null) {
            /**
             * 1.execute()里循环判断interrupted()
             * 2.sleep状态下抛出 InterruptedException
             * 3.run状态下正常
             */
            executeThread.interrupt();
        }
    }

    public void printErr(){
        if(runMode == MODE_DEV)
            System.out.print(err.toString());
    }
    public String getErrStr(){
        return err.toString();
    }

    public void printCode(){
        if(runMode == MODE_DEV)
            this.memory.codeArr.forEach(new Consumer<Code>() {
                @Override
                public void accept(Code c) {
                    System.out.print(c.toString());
                }
            });
    }
    public String getCompiledCode(){
        final StringBuilder sb = new StringBuilder();
        this.memory.codeArr.forEach(new Consumer<Code>() {
            @Override
            public void accept(Code c) {
                sb.append(c.toString_Simple());
            }
        });
        return sb.toString();
    }
    public Code readCompiledCodeFromString(String str){
        return null;
    }


    public static class Error {
        public static int NO_ERROR = -9;
        public static int UNKNOWN_ERROR = -1;
        int line;
        String msg;
        private Error(){
            this.line = NO_ERROR;
            this.msg = "no error";
        }
        public Error(int line,String msg){
            this.line = line;
            this.msg=msg;
        }
        public void set(int line,String msg){
            this.line = line;
            this.msg=msg;
        }
        public String toString(){
            if(line == -9) return "无错";
            return String.format("错误:line[%d] %s\n",line,msg);
        }
    }
}
