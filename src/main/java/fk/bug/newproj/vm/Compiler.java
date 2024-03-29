package fk.bug.newproj.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


class Compiler {
    VM vm;
    //0~999栈区
    int memoryIndex = 0;
    //闭包，用于编译阶段判断声明变量合法
    Closure currentClosure;
    /** 重构
     * compiler.compile(source)
     * 根据{}逐级划分codeBlock.compile_line
     * 每个codeBlock有自己的closure
     *
     * 主代码在main()，跟其他函数在形式上应该相同
     */


    /** 编译，存储对不同关键字的编译方法
     * IKeywordAction(vm,words)
     * modify Code[]
     */
    private static Map<String, IKeywordAction> keywordsActionMap = new HashMap<>();
    static {
        putKeywordActionMap();
    }
    private static Map<String, Operator> operatorMap = new HashMap<>();
    static {
        putOperatorMap();
    }


    public Compiler(VM vm){
        this.vm = vm;
        resetCompiler();
    }
    private void resetCompiler(){
        currentClosure = new Closure();
        memoryIndex = 0;
        Code.codeCount = 0;
        vm.memory.codeArr = new ArrayList<>();
        vm.memory.memoryStack = new ArrayList<>();
    }

    public boolean compile(String src){
        resetCompiler();

        String[] lineArr = src.split("\n");
        for (int i=0; i<lineArr.length; i++) {
            boolean compileLine = compileLine(i, lineArr[i]);
            if(! compileLine) return false;
        }
        return true;
    }
    //编译错误时，赋值compileInfo，供调试
    public boolean compileLine(int sourceLine, String line){
        String[] split_words = line.trim()
                .split("((?<=\\b|\\W) +(?=\\b|\\W))|(?<! )\\b(?! )");
        if("".equals(split_words[0])) return true;
        if("//".equals(split_words[0])) return true;

        //关键字命令，如：if/while/{/}/int/
        IKeywordAction ka = keywordsActionMap.get(split_words[0]);
        if(ka != null){
            return ka.action(sourceLine, vm, split_words);
        }

        return processExpression(sourceLine,split_words,0,split_words.length);
        /**
         * 关键字以外都当作表达式处理
        //执行函数（如划屏）（仅memory_static中的Consumer单独成行作为语句）
        Memory.StaticInfo si = Memory.staticInfoByWordType.get(split_words[0]);
        if(si != null){
            Object st = Memory.memoryStatic.get(si.index);
            if(!(st instanceof Consumer)){
                //???注意，不一定是Consumer(无返回值)
                err.set(sourceLine, "暂时仅支持几个Consumer函数用作语句");
                return false;
            }
            return processExpression(sourceLine,split_words,0,split_words.length);
        }

        //赋值语句
        if(split_words.length > 1 && "=".equals(split_words[1])){
            return processExpression(sourceLine,split_words,0,split_words.length);
        }

        err.set(sourceLine, "未知错误，请联系开发者");
        return false;
         */
    }

    /**
     * 先 keywordsActionMap 处理语句，提取关键字后再进行表达式处理
     *
     * 运算语句，把words处理为 number/operator/variable/function
     * var会判断(func/val)map是否存在单词
     */
    boolean processExpression(int sourceLine, String[] split, int start, int end){
        vm.memory.register.exprStart();
        List<Word> wordAnal = new ArrayList<>();
        if(! transSplit2Words(sourceLine,split,start,end,wordAnal)){
            return false;
        }

        /** wordAnal 须处理，通过栈，把<单词序列>转化为<VM指令序列>
         * 根据 memoryStaticInfo.argc 处理多个函数，如
         * + 1 2 r0 * r0 3 r0 + 4 5 r1 % r0 r1 r0
         * 最终返回结果r0（其他register都在使用时被释放了）
         */
        List<Word> cmdWords = new ArrayList<>();
        if(! transWords2Sentence(sourceLine,wordAnal,cmdWords)){
            return false;
        }

        Code c = new Code(sourceLine, CodeType.EXPRESSION.id, cmdWords);
        vm.memory.codeArr.add(c);
        return true;
    }
    private boolean transSplit2Words(int sourceLine, String[] split, int start, int end, List<Word> out_wordAnal){
        if(start >= end){
            vm.err.set(sourceLine, String.format("分词程序错误start[%d]end[%d]，请联系开发者",start,end));
            return false;
        }
        for(int i=start;i<end;i++){
            String w = split[i];
            if(w.length() == 0) continue;
            char c = w.charAt(0);
            Object temp = null;
            if('0'<=c && c<='9'){ //number
                Word wa = new Word(WordType.INT.type, Integer.parseInt(w));
                out_wordAnal.add(wa);
            }else if('a'<=c && c<='z' || 'A'<=c && c<='Z' || c=='_'){ //var
                if(! currentClosure.varNameExists(w)){
                    vm.err.set(sourceLine, String.format("变量名不存在[%s]",w));
                    return false;
                }
                Word wa = new Word(WordType.VAR.type, currentClosure.getMemAddrByVarName(w));
                out_wordAnal.add(wa);
            }else if((temp = Memory.staticInfoByWordType.get(w)) != null){ //func //汉字函数
                Memory.StaticInfo si = (Memory.StaticInfo) temp;
                Word wa = new Word(WordType.SYS_FUNC.type, si.index);
                out_wordAnal.add(wa);
                if(i + si.argc >= end){
                    vm.err.set(sourceLine, String.format("函数[%s]参数个数应为[%d]",w,si.argc));
                    return false;
                }
                //!!!???
                //参数可以是 INT 或 VAR，暂时调用函数时再校验
                //VM.staticInfo.. 已配置参数个数，还可配置/编译函数参数类型
            }else if((temp = operatorMap.get(w)) != null) { //operator
                Operator op = (Operator) temp;
                Word wa = new Word(WordType.OPERATOR.type, op.type);
                out_wordAnal.add(wa);
            }else{
                vm.err.set(sourceLine, String.format("当前版本未处理单词[%s]",split[i]));
                return false;
            }
        }
        return true;
    }
    /** wordAnal 须处理，通过栈，把<单词序列>转化为<VM指令序列>(注意register)
     * 根据 memoryStaticInfo.argc 处理多个函数，如
     * + 1 2 r0     * r0 3 r0   + 4 5 r1    % r0 r1 r0
     * 最终返回结果r0（其他register都在使用时被释放了）
     */
    private boolean transWords2Sentence(int sourceLine, List<Word> words, List<Word> out_cmdWords){
        Stack<Word> stack_value = new Stack<>();
        Stack<Word> stack_operator = new Stack<>();
        for(int i=0;i<words.size();i++){
            Word w = words.get(i);
            if(w.type == WordType.UNDEFINED.type){
                vm.err.set(sourceLine,"WordType.UNDEFINED 单词处理失败");
                return false;
            }else if(w.type == WordType.INT.type || w.type == WordType.VAR.type){
                stack_value.push(w);
            }else if(w.type == WordType.SYS_FUNC.type){
                if(i!=0){
                    Object o = Memory.memoryStatic.get(w.val);
                    if(!(o instanceof Function || o instanceof Supplier || o instanceof Predicate)){
                        vm.err.set(sourceLine,"作为数值计算的系统函数必须有返回值");
                        return false; //暂时无用，都统一使用java.util.function.Function
                    }
                }
                Memory.StaticInfo si = Memory.staticInfoByIndex.get(w.val);
                out_cmdWords.add(w);
                //???注意测试函数参数
                for(int j=0;j<si.argc;j++){
                    i++;
                    out_cmdWords.add(words.get(i));
                }
                int regId = vm.memory.register.getIdleRegisterIndex();
                if(regId<0){
                    vm.err.set(sourceLine,"可用寄存器不足");
                    return false;
                }
                Word r = new Word(WordType.REGISTER.type, regId);
                out_cmdWords.add(r);
                stack_value.push(r);
            }else if(w.type == WordType.USER_FUNC.type){
                vm.err.set(sourceLine,"WordType.USER_FUNC 暂不支持");
                return false;
            }else if(w.type == WordType.OPERATOR.type){
                if(stack_operator.size()==0){
                    stack_operator.push(w);
                }else if(w.val == Operator.BRACKET_SMALL_RIGHT.type){
                    //出栈直到( //以后还应支持*p|!true|(1)|true?1:2等形式
                    Word top = null;
                    while(true){
                        if(stack_operator.size() == 0) break;
                        top = stack_operator.pop();
                        if(top.val == Operator.BRACKET_SMALL_LEFT.type){ //缺少错误校验() (0)等
                            break;
                        }else {
                            if(! afterPopOperator_CalculateAndPushResult(top,
                                    stack_value,
                                    sourceLine,out_cmdWords)) return false;
                        }
                    }
                }else{
                    //比较符号优先级，继续压栈或出栈
                    Word top = null;
                    while(true){
                        if(stack_operator.size() == 0) {
                            stack_operator.push(w); //只有)不压栈
                            break;
                        }
                        top = stack_operator.peek();
                        //注意括号很多地方需要特别处理
                        if(top.val == Operator.BRACKET_SMALL_LEFT.type){
                            stack_operator.push(w);
                            break;
                        }
                        //符号栈顶priority数字大，即优先级低，后续符号直接压栈（并在出栈时先计算）
                        if(Operator.getPriorityByType(top.val) > Operator.getPriorityByType(w.val)){
                            stack_operator.push(w);
                            break;
                        }
                        top = stack_operator.pop();
                        if(! afterPopOperator_CalculateAndPushResult(top,
                                stack_value,
                                sourceLine,out_cmdWords)) return false;
                    }
                }
            }
        }
        //pop the rest stack
        Word top = null;
        while(stack_operator.size() > 0){
            top = stack_operator.pop();
            if(! afterPopOperator_CalculateAndPushResult(top,
                    stack_value,
                    sourceLine,out_cmdWords)) return false;
        }
        //注意，表达式也要处理单行无返回值函数，如sleep 100，也要return null占用一个寄存器位
        if(stack_value.size() != 1){
            vm.err.set(sourceLine,"表达式解析错误，符号处理后有数据剩余");
            return false;
        }
        return true;
    }
    private boolean afterPopOperator_CalculateAndPushResult(
            Word wOprt, Stack<Word> stack_value,
            int sourceLine, List<Word> out_cmdWords){
        //根据出栈符号类型，决定值栈出栈个数
        int popValCnt = Operator.getArgcByType(wOprt.val);
        Word[] data = new Word[popValCnt];
        for(int i=0;i<popValCnt;i++){
            data[i] = stack_value.pop();
            if(data[i] == null){
                vm.err.set(sourceLine, "表达式解析错误，参数不足");
                return false;
            }
        }
        //输出计算序，压栈result register
        out_cmdWords.add(wOprt);
        for(int i=popValCnt-1;i>=0;i--){
            out_cmdWords.add(data[i]);
            if(data[i].type == WordType.REGISTER.type){
                vm.memory.register.recycleRegister(data[i].val);
            }
        }
        //输出计算序最后一个Word: Register_Index。并且压栈Reg作为下一个Operator的操作数
        int r = vm.memory.register.getIdleRegisterIndex();
        if(r<0){
            vm.err.set(sourceLine,"可用寄存器不足");
            return false;
        }
        Word wRstRgst = new Word(WordType.REGISTER.type, r);
        out_cmdWords.add(wRstRgst);
        stack_value.push(wRstRgst);
        return true;
    }

    /** 编译
     * Compiler预处理，规定了编译语法，源码编译为vm码，并存储于memory.codeArr
     * 注意，这里不要操作valueMap，valueMap是codeArr运行时动态存储
     * 当然，valueMap可用于编译语法检查，运行时清空
     */
    private static void putKeywordActionMap(){


        /**
         *  Source: 0输出 1var
         *  Code:   print var_name
         */


        //Source:  0数据 1var [2= 3数字/运算]
        keywordsActionMap.put("数据", new IKeywordAction() {
            @Override
            public boolean action(int sourceLine, VM vm, final String[] words) {
                return KeywordActions.action_declare(sourceLine,vm,words);
            }
        });

        /**
         * repeat n
         */
        /**
         *  goto思路
         * int i(randIdx)=n;
         * [l:loop-start]   （做个标记栈，标记下一个{对应的}须返回的行号）
         * if expression(! i_rd>0) goto loop-end;
         * （读取到"}"再返回本行填写loop_end_line，可以用stack保存code_blocks_start_line）
         *      (codes);
         *      i_rd--;
         *      goto loop-start
         * [l:loop-end]
         * (next code) release i_rd
         */
        /**
         * funcPointer/codeBlocks/closure思路 (缺点是代码分层不紧凑，break_loop实现麻烦)
         * int i=n;
         * [l:loop-start]
         * expression(i>0) ? inner_codeArr : next_code
         * {//inner_codeArr
         *      (codes)
         *      i--
         *      goto:loop-start
         * }
         * (next code)
         *
         */
        keywordsActionMap.put("重复", new IKeywordAction() {
            @Override
            public boolean action(int sourceLine, VM vm, String[] words) {
                return KeywordActions.action_loop(sourceLine,vm,words);
            }
        });
        keywordsActionMap.put("判断",new IKeywordAction() {
            @Override
            public boolean action(int sourceLine, VM vm, String[] words) {
                return KeywordActions.action_if(sourceLine,vm,words);
            }
        });
        /**
         * 闭包
         * { 入栈行号，新增子closure，编译为 CLLSURE_START line_start
         * } 出栈行号，返回父closure，编译为 CLLSURE_END line_start
         */
        keywordsActionMap.put("{",new IKeywordAction() {
            @Override
            public boolean action(int sourceLine, VM vm, String[] words) {
                return KeywordActions.action_closure_start(sourceLine,vm,words);
            }
        });
        keywordsActionMap.put("}",new IKeywordAction() {
            @Override
            public boolean action(int sourceLine, VM vm, String[] words) {
                return KeywordActions.action_closure_end(sourceLine,vm,words);
            }
        });
    }
    private static void putOperatorMap(){
        operatorMap.put("(",Operator.BRACKET_SMALL_LEFT);
        operatorMap.put(")",Operator.BRACKET_SMALL_RIGHT);
        operatorMap.put("!",Operator.NOT);
        operatorMap.put("*",Operator.MULTIPLY);
        operatorMap.put("/",Operator.DIVIDE);
        operatorMap.put("%",Operator.MOD);
        operatorMap.put("+",Operator.ADD);
        operatorMap.put("-",Operator.SUBSTRACT);
        operatorMap.put(">",Operator.BIGGER);
        operatorMap.put(">=",Operator.BIGGER_EQUAL);
        operatorMap.put("<",Operator.SMALLER);
        operatorMap.put("<=",Operator.SMALLER_EQUAL);
        operatorMap.put("==",Operator.EQUAL);
        operatorMap.put("!=",Operator.NOT_EQUAL);
        operatorMap.put("&&",Operator.AND);
        operatorMap.put("||",Operator.OR);
        operatorMap.put("=",Operator.VALUE);
    }

}
