package cly;
/*
 * 
 * 综述：
 * SymTable和TableUnit组合完成符号表的登录操作，其中TableUnit是SymTable中的表项
 * 登陆符号表：常量、变量、过程
 */
import java.io.*;
import java.util.Scanner;

public class Pl0
{
    public static void main(String [] args) 
    {
        String filename;
        System.out.println("输入源程序文件名:");
        System.out.print(">>");
        Scanner s=new Scanner(System.in);
        filename=s.next();
        Pl0Analysis mp=new Pl0Analysis(filename);
        if(!mp.mgpAnalysis())
        {
            System.out.println(">编译成功");
            System.out.println(">按 y执行程序:");       
            mp.printTable();//输出符号表符号表每次退出都清空        
            mp.showPcodeInStack();//输出PCode
            String choice;
            Scanner s1=new Scanner(System.in);
            choice = s1.next();
            System.out.print(">>");
            if(choice.equals("y")) 
            {
            	System.out.println("开始执行:");
            	mp.interpreter();
            } 
        }
        System.out.println("程序结束！");
    }
}
