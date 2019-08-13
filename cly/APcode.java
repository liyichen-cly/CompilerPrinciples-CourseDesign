package cly;
/**
 * 目标代码的生成
 */
public class APcode 
{
    private static int LIT = 0; //LIT 0 ，a 取常量a放入数据栈栈顶
    private static int OPR = 1; //OPR 0 ，a 执行运算，a表示执行某种运算，具体是何种运算见上面的注释
    private static int LOD = 2; //LOD L ，a 取变量（相对地址为a，层差为L）放到数据栈的栈顶
    private static int STO = 3; //STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
    private static int CAL = 4; //CAL L ，a 调用过程（转子指令）（入口地址为a，层次差为L）
    private static int INT = 5; //INT 0 ，a 数据栈栈顶指针增加a
    private static int JMP = 6; //JMP 0 ，a无条件转移到地址为a的指令
    private static int JPC = 7; //JPC 0 ，a 条件转移指令，转移到地址为a的指令
    private static int RED = 8; //RED L ，a 读数据并存入变量（相对地址为a，层次差为L）
    private static int WRT = 9; //WRT 0 ，0 将栈顶内容输出

    private int MAX_PCODE=10000;
    private int codePtr=0;          //指向下一条将要产生的代码的在APcode中的地址


    private SPcode[] pcodeArray=new SPcode[MAX_PCODE];//建立一个目标代码数组

    public APcode()
    {
    	//初始化
        for(int i=0;i<MAX_PCODE;i++)
        {
            pcodeArray[i]=new SPcode(-1,-1,-1);
        }
    }
    //产生目标代码
    public void gen(int f,int l,int a)
    {
        pcodeArray[codePtr].setF(f);
        pcodeArray[codePtr].setL(l);
        pcodeArray[codePtr].setA(a);
        codePtr++;
    }

    public int getCodePtr()
    {
        return codePtr;
    }

    public int getOPR()
    {
        return OPR;
    }

    public int getLIT() 
    {
        return LIT;
    }

    public int getLOD() 
    {
        return LOD;
    }

    public int getSTO()
    {
        return STO;
    }

    public int getCAL() 
    {
        return CAL;
    }

    public int getINT() 
    {
        return INT;
    }

    public int getJMP() 
    {
        return JMP;
    }

    public int getJPC() 
    {
        return JPC;
    }

    public int getRED() 
    {
        return RED;
    }

    public int getWRT() 
    {
        return WRT;
    }

    public SPcode[] getPcodeArray() 
    {
        return pcodeArray;
    }

}

