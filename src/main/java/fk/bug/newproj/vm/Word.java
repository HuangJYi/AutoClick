package fk.bug.newproj.vm;

import java.util.HashMap;

/**编译原理：
 * 前端（源语言）
 *      字符流 -> 词法分析 -（词法单元流）> 语法分析 -（语法树）> 语义分析 ↓
 * 中端（中间语言）
 *      -> 中间代码 -> 机器无关的代码优化 ↓
 * 后端（目标语言）
 *      -> 目标代码 -> 目标代码优化
 *
 * 词法分析其实是不做校验的，比如 int a = 1; 写为 inte a = 1;
 * 本应是[关键字，标识符，运算符，常量] 识别为[标识符，标识符，运算符，常量]
 * 然后语法分析阶段，[id,id,oprs,const]无法匹配如声明语句<declare> = <type><id>，才报错。
 *
 * 词法分析：分词，记值：<type:关键字/标识符/常量/运算符/界限符, val:while/var_name/123/+/;{}>
 * 语法分析：语句模式匹配：声明语句，赋值语句，控制语句，调用语句...
 * 语义分析：
 *      校验作用域
 *      校验类型匹配
 *      安排存储位置和大小
 */

/**
 * 区分Code(命令码) Word(表达式分词)
 */
public class Word{
    public static Word STATIC_WORD_REGISTER_0 = new Word(WordType.REGISTER.type, 0);
    public static Word STATIC_WORD_INT_1 = new Word(WordType.INT.type, 1);

    public static Word STATIC_WORD_VALUE = new Word(WordType.OPERATOR.type, Operator.VALUE.type);
    public static Word STATIC_WORD_BIGGER = new Word(WordType.OPERATOR.type, Operator.BIGGER.type);
    public static Word STATIC_WORD_ADD = new Word(WordType.OPERATOR.type, Operator.ADD.type);
    public static Word STATIC_WORD_SUBSTRACT = new Word(WordType.OPERATOR.type, Operator.SUBSTRACT.type);
    /**例
     * type WordType.int/var/func, val int/var_idx/func_idx
     * type WordType.OPERATOR(99), val OPERATOR.ADD.type
     */
    int type;
    int val;
    Word(int type,int val){
        this.type = type;
        this.val = val;
    }
    public String toString(){
        return String.format("word[%d %d]",type,val);
    }
    /** 例 a = (b + 1) * 2 + now      (a_idx:0 b_idx:1 now_idx:0)
     * var相当于*地址求值运算符
     * oprt/sys/user_func相当于()函数运算符
     *  1.词法分析：（词语是什么意思，输出词语表）
     *  v0 toString= toString( v1 toString+ i1 toString) toString* i2 toString+ f_now
     *  2.语法分析：（语句/子语句是什么意思，输出语法树；
     *              这里使用<符号栈><数栈>双栈代替，因为运算符出栈执行，相当于子语句）
     *  （遇到低优先级符号时，出栈符号及其所需数自，把语句转化为执行顺序）
     *  (>数栈 >>操作栈)(>push <pop)
     *      >v0 >>toString=
     *      >>( >v1 >>toString+ >i1
     *  toString)  <<toString+ <i1 <v1 >toString+(v1,i1)=>r0  (r0在此代指压栈，在编译结果代指register)
     *      >>toString* >i2
     *  toString+  <<toString* <i2 <r0 >toString*(r0,i2)=>r1
     *      >>toString+ >f_now
     *语句完<<toString+ <f_now <r1 >toString+(r1,f_now)=>r2
     *      <<toString= <r2 <v0 >toString=(v0,r2)=>r3
     *符栈空，数栈剩结果r3
     *  （编译结果）
     *      toString+ v1 i1 r0
     *      toString* r0 i2 r1     (r0已被使用，可释放)
     *      toString+ r1 f_now r2
     *      toString= v0 r2 r3
     *      last_return: r3
     *  （register一旦被使用，可立即释放复用为返回值）(1*2+3*4+5*6 此例会同时使用多个register)
     *      toString+ v1 i1 r0
     *      toString* r0 i2 r0
     *      toString+ r0 f_now r0
     *      toString= v0 r0 r0
     *      last_return: r0
     *  3.语义分析：（已被双栈过程包括）
     */
}

enum WordType{
    UNDEFINED(0),
    /**寄存器,处理方式与VAR相似,
     * 用于每个表达式临时存放func/operator计算结果，
     * 注意回收资源
     */
    REGISTER(88),
    VAR(1),
    INT(2),
    SYS_FUNC(20),    //sys_func由vm维持，user_func由executor维持
    USER_FUNC(21),  //自定义函数该如何实现？{}的跳转标记吗？
    OPERATOR(99);
    WordType(int type){
        this.type = type;
    }
    public int type;
}

/**
 * 运算符,(优先级作百位，100~)
 * <100留作其他WordType如 0-undefined 1-int 2-var 3-sys_func 4-user_func
 */
enum Operator {
    BRACKET_SMALL_LEFT("(",1,100,0),
    BRACKET_SMALL_RIGHT(")",99,101,0),

    NOT("!",2,200,1),

    MULTIPLY("*",3,300,2),
    DIVIDE("/",3,301,2),
    MOD("%",3,302,2),

    ADD("+",4,400,2),
    SUBSTRACT("-",4,401,2),

    BIGGER(">",6,600,2),
    BIGGER_EQUAL(">=",6,601,2),
    SMALLER("<",6,602,2),
    SMALLER_EQUAL("<=",6,603,2),

    EQUAL("==",7,700,2),
    NOT_EQUAL("!=",7,701,2),

    AND("&&",11,1100,2),
    OR("||",12,1200,2),
    VALUE("=",14,1400,2);

    Operator(String symbol, int priority, int type, int argc){
        this.symbol = symbol;
        this.priority = priority;
        this.type = type;
        this.argc = argc;
    }
    public String symbol;
    public int priority;
    public int type;
    public int argc;
    public static Operator getOperatorByType(int type){
        for(Operator o : values()){
            if(o.type == type) return o;
        }
        return null;
    }
    public static int getPriorityByType(int type){
        return type / 100;
    }
    public static int getArgcByType(int type){
        for(Operator o : values()){
            if(o.type == type) return o.argc;
        }
        return 0;
    }
}
