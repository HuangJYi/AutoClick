package fk.bug.newproj.vm;

import java.util.ArrayList;
import java.util.List;

/**
 * 区分Code(命令码) Word(表达式分词)
 */
public class Code {
    public static int codeCount = 0;    //总行数
    public int codeIndex = 0;          //new Code() 记录该行序号供goto(l)使用

    public int sourceLine;
    public int codeType;
    public int param_int = -1;              //简单参数 Declare(mem_idx) If(goto_line)
    public List<Word> param_list = null;  //复杂参数 Exp(oprt/var/int/reg)
    public Code(int sourceLine, int codeType, int param){
        this.sourceLine = sourceLine;
        this.codeType = codeType;
        this.param_int = param;

        this.codeIndex = Code.codeCount++;
    }
    public Code(int sourceLine, int codeType, List paramsList){
        this.sourceLine = sourceLine;
        this.codeType = codeType;
        this.param_list = paramsList;

        this.codeIndex = Code.codeCount++;
    }
    public Code next(VM vm){
        List codeArr = vm.memory.codeArr;
        if(codeArr.size() == this.codeIndex -1)
            return null;
        return vm.memory.codeArr.get(this.codeIndex + 1);
    }
    public String toString(){
        return String.format("codeLine(%d) sourceLine(%d) codeType(%s) params(%s)\n",
                codeIndex, sourceLine,CodeType.getCodeTypeById(codeType).name(),
                param_list == null ? param_int : param_list.toString());
    }
    public String toString_Simple(){
        if(param_list == null)
            return String.format("%d %d %d %d\n",
                    codeIndex, sourceLine, codeType, param_int);
        StringBuilder simple = new StringBuilder();
        for(int i=0;i<param_list.size();i++){
            Word w = param_list.get(i);
            simple.append(w.type).append(' ').append(w.val);
            if(i != param_list.size() - 1) simple.append(' ');
        }
        return String.format("%d %d %d %s\n",
                codeIndex, sourceLine, codeType, simple.toString());
    }
    //从simple_string (byte code str)中解析代码
    public static List<Code> readCodeListFromStr(String simple){
        if(simple == null ) return null;
        codeCount = 0;
        List<Code> codeList = new ArrayList<>();
        String[] codeLine = simple.trim().split("\n");
        for(int i=0;i<codeLine.length;i++){
            if(codeLine[i].length() == 0) continue;

            String[] codeField = codeLine[i].split(" ");
            Code code;
            if(codeField.length == 4){
                code = new Code(Integer.parseInt(codeField[1]),
                        Integer.parseInt(codeField[2]),
                        Integer.parseInt(codeField[3]));
            }else{
                List<Word> wordList = new ArrayList<>();
                for(int w=3;w<codeField.length;w+=2){
                    Word word = new Word(
                            Integer.parseInt(codeField[w]),
                            Integer.parseInt(codeField[w+1]));
                    wordList.add(word);
                }
                code = new Code(Integer.parseInt(codeField[1]),
                        Integer.parseInt(codeField[2]),
                        wordList);
            }
            codeList.add(code);
        }
        return codeList;
    }
}

/**
 * 字节码类型
 */
enum CodeType{
    IF(10),     //判断语句，参数[int/expression]，可实现if_else和while
    GOTO(20),   //goto用于if/while行号跳转，但不是编程关键字
    INT(30),    //声明语句，参数[mem_index][register_index]，语句的变量名被处理为index后
    EXPRESSION(90); //表达式，通用处理，如单行函数sleep(t)不被别的字节码类型识别，作为表达式处理
 //   BRACKET_LARGE_LEFT(98), BRACKET_LARGE_RIGHT(99);    //标记跳转行号、Closure切换
    public int id;
    CodeType(int id){this.id = id;}
    public static CodeType getCodeTypeById(int id){
        for(CodeType o : values()){
            if(o.id == id) return o;
        }
        return null;
    }
}
