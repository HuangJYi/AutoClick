package fk.bug.newproj.vm;

import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

/**
 *  编译源码，对编程关键字的处理
 * */
@FunctionalInterface
interface IKeywordAction {
    boolean action(int sourceLine, VM vm, String[] words);
}

/**
 * IKeywordAction.action() 调用 KeywordActions.action_xxx(),仅为控制代码行数
 */
class KeywordActions{
    /**变量声明
     * 数据 xyz = 123
     */
    public static boolean action_declare(int sourceLine, VM vm, final String[] words) {
        if(words.length < 2) {
            vm.err.set(sourceLine, "格式：数据 xyz = 123");
            return false;
        }

        //???以后加上funcsMap
        Map<String, Integer> varsMap = vm.compiler.currentClosure.varNameMemoryMap;
        if(varsMap.get(words[1]) != null){
            vm.err.set(sourceLine, String.format("变量名%s已存在",words[1]));
            return false;
        }
        varsMap.put(words[1], vm.compiler.memoryIndex);
        vm.memory.codeArr.add(new Code(sourceLine, CodeType.INT.id, vm.compiler.memoryIndex));
        vm.compiler.memoryIndex ++;
        if(words.length == 2) return true;

        //calculator : number/var/expression/function
        return vm.compiler.processExpression(sourceLine, words,1,words.length);
    }

    /**循环语句
     * 重复 int_times {}
     * 重复 true/false_expression {}
     */
    public static boolean ignoreNextClosureStart = false;
    public static Stack<Code> closureStartStack = new Stack<>(); //记录Code.codeIndex
    public static Stack<Integer> closureTypeStack = new Stack<>();
    public enum ClosureType{
        LOOP(1),            //go_back(Code_EXPRESSION);if_not_goto(Code_IF)
        FAST_LOOP(2),       //i--;go_back(Code_EXPRESSION);if_not_goto(Code_IF)
        IF(3),              //if_not_goto(Code_IF)
        DEFAULT(0);     //开关closure
        public int type;
        ClosureType(int type){
            this.type = type;
        }
    }
    /**
     * 根据 specialClosureMark 在编译 } 后添加 goto back
     * 根据 fastLoopMark 在 } 前添加 i++
     */
    public static boolean action_loop(int sourceLine, VM vm, final String[] words){
        if(words.length == 2){
            //不复用action_declare()，避免反复转化int-string
            //初始化 int i = n;
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.INT.id, vm.compiler.memoryIndex));
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.EXPRESSION.id, Arrays.asList(
                    Word.STATIC_WORD_VALUE,
                    new Word(WordType.VAR.type, vm.compiler.memoryIndex),
                    new Word(WordType.INT.type, Integer.parseInt(words[1])),
                    Word.STATIC_WORD_REGISTER_0
            )));

            //if(i > 0)
            Code back;
            vm.memory.codeArr.add(back = new Code(sourceLine, CodeType.EXPRESSION.id, Arrays.asList(
                    Word.STATIC_WORD_BIGGER,
                    new Word(WordType.VAR.type, vm.compiler.memoryIndex),
                    new Word(WordType.INT.type, 0),
                    Word.STATIC_WORD_REGISTER_0
            )));
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.IF.id, -1));
            //匿名int_i
            vm.compiler.memoryIndex ++;

            //压栈标记与引用，{校验code.sourceLine同行或紧邻则跳过，}补写i--;go_back;if_not_goto
            closureStartStack.push(back);
            closureTypeStack.push(ClosureType.FAST_LOOP.type);
            ignoreNextClosureStart = true;
        }else{
            if(! vm.compiler.processExpression(sourceLine, words,1,words.length))
                return false;
            Code back = vm.memory.codeArr.get(vm.memory.codeArr.size() - 1);
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.IF.id, -1));

            closureStartStack.push(back);
            closureTypeStack.push(ClosureType.LOOP.type);
            ignoreNextClosureStart = true;
        }
        return true;
    }

    public static boolean action_if(int sourceLine, VM vm, final String[] words){
        if(! vm.compiler.processExpression(sourceLine, words,1,words.length))
            return false;
        Code back;
        vm.memory.codeArr.add(back = new Code(sourceLine, CodeType.IF.id, -1));
        closureStartStack.push(back);
        closureTypeStack.push(ClosureType.IF.type);
        ignoreNextClosureStart = true;
        return true;
    }
    public static boolean action_closure_start(int sourceLine, VM vm, final String[] words){
        //closureStartStack.top codeidx差2,跳过{
        if(ignoreNextClosureStart){
            ignoreNextClosureStart = false;
        }else {
            closureStartStack.push(null);
            closureTypeStack.push(ClosureType.DEFAULT.type);
        }

        Closure c = new Closure();
        c.parent = vm.compiler.currentClosure;
        vm.compiler.currentClosure.children.add(c);
        vm.compiler.currentClosure = c;
        return true;
    }
    public static boolean action_closure_end(int sourceLine, VM vm, final String[] words){
        //分ClosureType补写Code
        Code back = closureStartStack.pop();
        int type = closureTypeStack.pop();
        if(type == ClosureType.LOOP.type){
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.GOTO.id, back.codeIndex));
            back = back.next(vm);
            back.param_int = Code.codeCount;
        }else if(type == ClosureType.FAST_LOOP.type){
            Word tempI = (Word)(back.param_list.get(1));
            if(tempI.type != WordType.VAR.type){
                vm.err.set(sourceLine, "程序错误：fast_loop获取匿名idx失败");
                return false;
            }
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.EXPRESSION.id, Arrays.asList(
                    /**
                     * 还是采用统一的寄存器保存结果模式
                    Word.STATIC_WORD_SUBSTRACT,
                    tempI,
                    Word.STATIC_WORD_INT_1,
                    tempI
                    */
                    Word.STATIC_WORD_SUBSTRACT,
                    tempI,
                    Word.STATIC_WORD_INT_1,
                    Word.STATIC_WORD_REGISTER_0,

                    Word.STATIC_WORD_VALUE,
                    tempI,
                    Word.STATIC_WORD_REGISTER_0,
                    Word.STATIC_WORD_REGISTER_0
            )));
            vm.memory.codeArr.add(new Code(sourceLine, CodeType.GOTO.id, back.codeIndex));
            back = back.next(vm);
            back.param_int = Code.codeCount;
        }else if(type == ClosureType.IF.type){
            back.param_int = Code.codeCount;
        }
        vm.compiler.currentClosure = vm.compiler.currentClosure.parent;
        return true;
    }


}