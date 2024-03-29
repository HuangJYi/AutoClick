package fk.bug.newproj.vm;

import android.widget.TextView;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import fk.bug.newproj.MainActivity;
import fk.bug.newproj.R;

class Executor {
    private VM vm;
    private List<Code> codeArr;
    public int current = 0;

    public Executor(VM vm){
        this.vm = vm;
        vm.executor = this;
    }

    //注意vm创建时，直接运行executor还是新建线程运行
    //目前是 vm.execute() 里新建线程执行
    //可优化为执行线程检测taskList的生产-消费者模型
    public void sleep(int ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //Thread.interrupted();
            Thread.currentThread().interrupt();
            //this.sleep_interrupted = true;
            //e.printStackTrace();
        }
    }

    public void execute(){
        execute(vm.memory.codeArr);
    }
    public void execute(List<Code> codeList){
        try{
            this.current = 0;
            codeArr = codeList;
            for(; !Thread.currentThread().isInterrupted() && current < codeArr.size();current++){
                executeLine(codeArr.get(current));
            }
        }catch(Exception e){
            if(VM.that.runMode == VM.MODE_DEV)
                e.printStackTrace();
            else {
                vm.err.set(-1,e.toString());
                //!!!??? 错误，只有主线程可以获取页面元素
                //((TextView) MainActivity.that.findViewById(R.id.output)).append(e);
            }
        }
    }
    public void executeLine(Code code){
        if(code.codeType == CodeType.INT.id){
            vm.memory.memoryStack.add(new Integer(0));  // 声明语句不需要表面index的param_int
        }else if(code.codeType == CodeType.EXPRESSION.id){
            execExpr(code.param_list);
        }else if(code.codeType == CodeType.IF.id){
            int exprRtn = vm.memory.register.getLastReturn();   //long怎么转int??? timeMillis
            if(exprRtn == 0)
                current = code.param_int -1;
        }else if(code.codeType == CodeType.GOTO.id){
            current = code.param_int -1;
        }
    }
    public void execExpr(List<Word> exprWords){
        for(int i=0;i<exprWords.size();i++){
            Word w = exprWords.get(i);
            if(w.type == WordType.OPERATOR.type
                && w.val == Operator.VALUE.type){
                //赋值语句特殊处理
                Word a = exprWords.get(i+1);
                Word b = exprWords.get(i+2);
                Word rst = exprWords.get(i+3);
                i+=3;
                int rtn = b.type == WordType.REGISTER.type ?
                        vm.memory.register.units[b.val].value : b.val;
                vm.memory.memoryStack.set(a.val, rtn);
                vm.memory.register.set(rst.val, rtn);
            }else{
                int funcIdx=0, argc=0;
                if(w.type == WordType.SYS_FUNC.type) {
                    funcIdx = w.val;
                    argc = Memory.staticInfoByIndex.get(w.val).argc;
                }else if(w.type == WordType.OPERATOR.type) {
                    Memory.StaticInfo si = Memory.staticInfoByWordType.get(w.val);
                    funcIdx = si.index;
                    argc = Operator.getArgcByType(w.val);
                }
                i++;
                List params = exprWords.subList(i,i+argc);
                int[] paramArr = params.stream().mapToInt(new ToIntFunction() {
                    @Override
                    public int applyAsInt(Object o) {
                        Word w = (Word)o;
                        if(w.type == WordType.VAR.type) return vm.memory.memoryStack.get(w.val);
                        if(w.type == WordType.REGISTER.type) return vm.memory.register.get(w.val);
                        return w.val;//INT
                    }
                }).toArray();
                i += argc;  //指向函数的返回值寄存器
                int rtn = Memory.memoryStatic.get(funcIdx).apply(paramArr);
                vm.memory.register.set(exprWords.get(i).val, rtn);
            }//else error
        }
    }
}
