package fk.bug.newproj;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;
import fk.bug.newproj.vm.Memory;
import fk.bug.newproj.vm.VM;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
    @Test public void addition_isWrong() {
        assertEquals(4, 2 + 1);
    }

    @Test public void string_equals() {
        String a = "aa";
        String b = "aa";
        System.out.println( a == "aa" );
        System.out.println( a == b );
        String c = new String("aa");
        System.out.println( a == c );
        System.out.println( "aa" == c );
    }

    @Test
    public void split_multi_blank() {
        //String s = "   repeat   now<=time    + 3000  +200";
        //String s = "   "; //有一项""
        //String s = "a==b";
        //()为word,(|10|)为3个word,now0为word,_ff|/|ff为3个word
        String s = "var _ff/ff == now0(10) +af1 () + -2";    //???不支持负数分词
        String[] words = s.trim()
                //分隔符
                //.split("\\b");  //仅\b(字母数字的单词边界)会保留空格，且符号不被视为\b
                /* (?<=\b|\W) +(?=\b|\W)) 两端为\b单词边界或\W非字符符号连续空格
                   (?<! )\b(?! ) 两边没有空格的\b (有一边空格的\b即上述空格以\b为边界)*/
                .split("((?<=\\b|\\W) +(?=\\b|\\W))|(?<! )\\b(?! )");
        for(String word:words)
            System.out.println(String.format("word[%s] len[%d]\n",word,word.length()));

        s = "//顶格注释";
        words = s.trim().split("((?<=\\b|\\W) +(?=\\b|\\W))|(?<! )\\b(?! )");
        for(String word:words)
            System.out.println(String.format("word[%s] len[%d]\n",word,word.length()));
    }

    //public void getFunc() {
        //Consumer qqq = t -> addition_isCorrect();
        //Method toString = ExampleUnitTest::getFunc;
            //语法错误 C::M不是反射Method，而是匿名类对象的语法糖，依托于@FunctionalInterface
    //}


    @Test
    public void testCompileDeclareInt(){
        VM vm = new VM();
        /**待测：
         * argc错误
         * 算式格式错误
         */
        boolean suc = vm.compile("数据 abc = 1+2*3+时间-4/5 \n " +
                "数据 def = 5\n " +
                //"数据 def = 6\n " +     //重复声明测试
                "划屏 abc 1 2 3 4  \n" +
                " 暂停 def");
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
    }

    @Test
    public void testExpression(){
        VM vm = new VM();
        boolean suc = vm.compile("数据 abc = 1+6+2*3-9/3 \n " +
                "输出 abc");  //10
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
        vm.execute();
    }
    @Test
    public void testExpression_bracket(){
        VM vm = new VM();
        /**
         * bug: +(   )/   被识别为一个单词
         */
        boolean suc = vm.compile("数据 abc = 1+ (6+2*3-9) /3 \n " +
                "输出 abc");
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
        vm.execute();
    }
    @Test
    public void testExpression_logic(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "判断 1 == 1 || 100==100 \n"+
                "{\n"+
                    "输出 1\n"+
                "}\n");
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
        vm.execute();
    }

    @Test
    public void testFastLoop(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "数据 def = 3\n " +
                        "重复 3\n"+
                        "{\n"+
                        //"数据 def = 1\n"+   //局部变量无误
                        "def = def + 2\n"+
                        "}\n"+
                        "输出 def");    //错误，“数据xxx”是赋值语句，“输出xxx”是单参函数
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
        vm.execute();
    }

    @Test
    public void testLoop(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "数据 def = 3\n " +
                        "数据 i = 0\n " +
                        "重复 i < 5\n"+
                        "{\n"+
                        "def = def + 2\n"+
                        "i=i+1\n"+
                        "}\n"+
                        "输出 def");    //错误，“数据xxx”是赋值语句，“输出xxx”是单参函数
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
        vm.execute();
    }

    //仅支持if, 不支持if-else if-else_if
    @Test
    public void testIf(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "数据 i = 3\n " +
                        "判断 i <= 2\n"+
                        "{\n"+
                        "输出 100\n"+
                        "}\n"+
                        "判断 i >= 3 \n"+
                        "{\n"+
                        "输出 200\n"+
                        "}\n"
                        );
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();
        vm.execute();
    }

    @Test
    public void testTime(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "数据 t = 时间\n"+
                        "输出 t\n");
        if(!suc){
            vm.printErr();
            return;
        }
        vm.printCode();

        VM vm2 = new VM();
        vm2.compile(
                "数据 t = 时间\n"+
                        "输出 t\n");

        vm.execute();

        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {}

        vm2.execute();

        System.out.println(new Date().getTime());
        System.out.println(System.currentTimeMillis());
    }

    @Test
    public void testSleep(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "数据 a = 时间\n"+
                        "输出 a\n"+
                        "暂停 500\n " +

                        "数据 b = 时间\n"+
                        "输出 b\n"+
                        "暂停 500\n " +

                        "数据 c = 时间\n"+
                        "输出 c\n"
        );
        if(!suc){
            vm.printErr();
            return;
        }
        //vm.printCode();
        vm.execute();
        try {
            Thread.sleep(1300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * sleep状态下被其他线程interrupt()，将恢复线程并在sleep触发InterruptedException,
     *      须在catch中 thread.interrupt() 设置isInterrupted标记
     * run状态下被其他线程interrupt()直接设置为 isInterrupted
     * 线程循环过程中反复校验 isInterrupted()
     *
     * （注释掉sleep, 修改max）
     * 单线程 6.5s 执行 1kw 次 Loop (1M Hz) (android大概是PC的1/10)
     *  1_loop = 10_Code = 100_Word = 100_memory_operating = 1k_cpu_time
     *  约 1G Hz）
     */
    @Test
    public void testThreadInterrupt(){
        VM vm = new VM();
        boolean suc = vm.compile(
                "数据 t0 = 时间\n"+
                        "数据 i=0\n"+
                        "数据 max = 10000000\n"+
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
                        "输出 t\n"
        );
        if(!suc){
            vm.printErr();
            return;
        }
        //vm.printCode();
        vm.asyncExecute();   //输出 1 2 (打断) 1 2 3
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vm.asyncExecute();
        try {
            Thread.sleep(9000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testFunctionalType(){
        //Predicate-test(o)-rtn Supplier-get()-rtn Consumer-accept(o) Function-apply(o)-rtn
    }

    enum Type{
        AAA(1), BBB(2), CCC(3);
        Type(int i){this.val = i;}
        private int val;
    }
    @Test
    public void diffTypeInList(){
        List l = Arrays.asList(123,"asd",Type.CCC);
        for(Object o : l){
            System.out.print(o+"\t");
            if(o instanceof Type) System.out.print(((Type)o).val +"\t");
            System.out.println(o.getClass());
        }
    }

    @Test
    public void equal(){
        Integer a = 12345678;
        Integer b = 12345678;
        System.out.println(a==b);       //false
        System.out.println(a.equals(b));//true
        a = 123;
        b = 123;
        System.out.println(a==b);       //true
        System.out.println(a.equals(b));//true
    }

    @Test
    public void listLast(){
        List l = Arrays.asList(2,4,6,8);
        //System.out.println(l.get(-1));    //越界
        System.out.println(l.get(l.size() - 1));
    }

    // 0 1 A _ a
    @Test
    public void ascii(){
        System.out.println('_'+0);//95
        System.out.println('a'+0);//97
        System.out.println('A'+0);//65
        System.out.println('0'+0);//48
        System.out.println('1'+0);//49
    }

    //error
    @Test
    public void substrIdxOutOfRange(){
        String s = "12345";
        System.out.println(s.substring(0,9));
    }

    @Test
    public void regExpGetNums(){
        String s = "12 34 -56 78 -910";
        //Pattern p = Pattern.compile("\\d+");
        Pattern p = Pattern.compile("[-\\d]+");
        Matcher m = p.matcher(s);

        while(m.find()) {
            System.out.println(m.group(0));
            System.out.println(Integer.parseInt(m.group(0)));
        }
    }
}