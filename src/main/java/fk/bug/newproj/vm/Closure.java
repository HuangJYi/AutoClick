package fk.bug.newproj.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Closure {
    Closure parent = null;
    List<Closure> children = new ArrayList<>();

    //编译变量名临时存储，<var_name, mem_addr>，编译后仅使用mem_addr
    Map<String, Integer> varNameMemoryMap = new HashMap<>();

    public boolean varNameExists(String vName){
        return varNameExistsInClosure(vName) ||
                (parent != null ? parent.varNameExists(vName) : false);
    }
    private boolean varNameExistsInClosure(String vName){
        return varNameMemoryMap.containsKey(vName);
    }

    public Integer getMemAddrByVarName(String vName){
        return varNameExistsInClosure(vName) ? varNameMemoryMap.get(vName) :
                (parent != null ? parent.getMemAddrByVarName(vName) : null);
    }
}