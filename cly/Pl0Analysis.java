package cly;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 语义分析
 * 关键字const对应的标识符是con
 * 而常数对应的标识符是const
 * 常量登入符号表：将地址加1
 * 变量登录符号表：将地址加1
 * 过程登录符号表：首先将level加1，传入参数将address置0，过程完成后再将level减一，再将address恢复
 */
import java.io.*;
public class Pl0Analysis 
{
    private static  int PROG=1;//program
    private static  int BEGIN=2;//begin
    private static  int END=3;//end
    private static  int IF=4;//if
    private static  int THEN=5;//then
    private static  int ELS=6;//else
    private static  int CON=7;//const
    private static  int PROC=8;//procdure
    private static  int VAR=9;//var
    private static  int DO=10;//do
    private static  int WHI=11;//while
    private static  int CAL=12;//call
    private static  int REA=13;//read
    private static  int WRI=14;//write
    private static  int ODD=15;//  oddl      和keyWord中每个字的序号是相等的

    private static  int EQU=16;//"="
    private static  int LES=17;//"<"
    private static  int LESE=18;//"<="
    private static  int LARE=19;//">="
    private static  int LAR=20;//">"
    private static  int NEQU=21;//"<>"


    private static  int ADD=22;//"+"
    private static  int SUB=23;//"-"
    private static  int MUL=24;//"*"
    private static  int DIV=25;//"/"

    private static  int SYM=26;//标识符
    private static  int CONST=27;//常量

    private static  int CEQU=28;//":="

    private static  int COMMA=29;//","
    private static  int SEMIC=30;//";"
    private static  int POI=31;//"."
    private static  int LBR=32;//"("
    private static  int RBR=33;//")"

    LexAnalysis lex;
    private boolean errorHappen=false;
    private int rvLength=1000;    //单词最大个数
    private RValue[] rv=new RValue[rvLength];
    private int terPtr=0;       //RValue的迭代器 iterator pointer

    private SymTable STable=new SymTable();       //符号表
    private SymTable ST=new SymTable();       //符号表
    private APcode  Pcode=new APcode();                 //存放目标代码


    private int level=0;                //主程序为第0层
    private int address=0;             //主程序或变量的声明是为0
    private int addrIncrement=1;//TabelUnit中的address的增量，遇见变量时会使其增加

    public Pl0Analysis(String filename)
    {
    	//初始化单词
        for(int i=0;i<rvLength;i++)
        {
            rv[i]=new RValue();
            rv[i].setId(-2);
            rv[i].setValue("-2");
        }
        lex=new LexAnalysis(filename);
    }
    
    public void readLex()
    {
        String filename="LexTable.txt";
        File file=new File(filename);
        BufferedReader ints=null;
        String tempLex,temp[];
        try{
            ints=new BufferedReader(new FileReader(file));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        try{
            int i=0;
            while((tempLex=ints.readLine())!=null) {
                temp = tempLex.split(" ");
                rv[i].setId(Integer.parseInt(temp[0], 10));//将字符串temp[0]转换成十进制数
                rv[i].setValue(temp[1]);//读字符串
                rv[i].setLine(Integer.parseInt(temp[2]));//将字符串temp[2]转换成十进制数
                i++;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void prog()//程序递归调用<prog> → program <id>；<block>
    {
        if(rv[terPtr].getId()==PROG)
        {
            terPtr++;
            if(rv[terPtr].getId()!=SYM)
            {
                errorHappen=true;
                showError(1,"");
            }
            else
            {
                terPtr++;
                if(rv[terPtr].getId()!=SEMIC)
                {
                    errorHappen=true;
                    showError(0,"");
                    return;
                }
                else
                {
                    terPtr++;
                    block();
                }
            }
        }
        else 
        {
            errorHappen = true;
            showError(2,"");
            return;//结束
        }
    }

    public void block()//块、程序体递归调用
    //<block> → [<condecl>][<vardecl>][<proc>]<body>
    {
        int addr0=address;        //记录本层之前的数据量（本层活动记录的起始地址），以便恢复时返回
        int tx0=STable.getTablePtr();       //记录本层过程在符号表的初始位置
        int cx0;							//保存需要回填的jmp指令在pcode中的索引
        int  propos=0;					   //本层过程在符号表中的位置
        if(tx0>0)
        {
            propos=STable.getLevelPorc();        
            tx0=tx0- STable.getRow(propos).getSize();   //记录 <本层变量> 在符号表的开始位置
        }
        if(tx0==0)//最外层还未开辟活动存储空间
        {
            address=3;      //每一层最开始位置的三个空间用来存放静态连SL、动态连DL、和返回地址RA
        }
        else
        {
            //每一层最开始位置的三个空间用来存放静态连SL该过程直接外层的活动首地址、
        	//动态连DL调用者的活动记录首地址、和返回地址RA
            //紧接着放形参的个数,形参个数存储在过程的size域
            address=3+STable.getAllTable()[propos].getSize();
        }

        //暂存当前Pcode.codePtr的值，即jmp,0,0在codePtr中的位置，用来一会回填
        cx0= Pcode.getCodePtr();
        Pcode.gen(Pcode.getJMP(),0,0);
        
        
        if(rv[terPtr].getId()!=CON&&rv[terPtr].getId()!=VAR&&
           rv[terPtr].getId()!=PROC&&rv[terPtr].getId()!=BEGIN)
        {
        	errorHappen = true;
        	showError(17,rv[terPtr].getValue());
        	return;
        }
        if(rv[terPtr].getId()==CON)
        {//此处没有terPtr++
            condecl();
        } 
        if(rv[terPtr].getId()==VAR)
        {
            vardecl();
        } 
        if(rv[terPtr].getId()==PROC)
        {
            proc();
            level--;
        }
        if(errorHappen==true)
        {
        	return;
        }
        /*
        * 声明部分完成，进入语句处理部分，之前生成的jmp，0，0应当跳转到这个位置
        *
        * */
        //回填jmp，0，0的跳转地址
        if(tx0>0)
        {//实参数数值传入 形参变量
            for(int i=0;i<STable.getAllTable()[propos].getSize();i++)
            {
                Pcode.gen(Pcode.getSTO(),0,STable.getAllTable()[propos].getSize()+3-1-i);
            }
        }
        
        //回填至无条件指令
        Pcode.getPcodeArray()[cx0].setA(Pcode.getCodePtr());
        Pcode.gen(Pcode.getINT(),0,address);        //生成本层程序活动记录内存的代码
        if(tx0==0)
        {
           // STable.getRow(tx0).setValue(Pcode.getCodePtr());     
        	//将本过程在符号表中的值设为本过程执行语句开始的位置
        }
        else 
        {
            STable.getRow(propos).setValue(Pcode.getCodePtr()-1-STable.getAllTable()[propos].getSize());     
          //过程入口地址填入value
        }

        body();
        Pcode.gen(Pcode.getOPR(),0,0);      //生成退出过程的代码，若是主程序，则直接退出程序

        address=addr0;      //分程序结束，恢复address 和符号表下一项即将填写那一项指针
        STable.setTablePtr(tx0);  
        STable.clearTheFowllowing(tx0);
    }
 
    public void condecl()//常量说明 递归子程序
    //<condecl> → const <const>{,<const>};
    {         
        if(rv[terPtr].getId()==CON)
        {// const这个字母
            terPtr++;
            myconst();
            if(errorHappen==true)
            {
            	return;
            }
            while(rv[terPtr].getId()==COMMA)
            {//  逗号 
                terPtr++;
                myconst();
                if(errorHappen==true)
                {
                	return;
                }
            }
            if(rv[terPtr].getId()!=SEMIC)
            {//分号
                errorHappen=true;
                showError(0,"");
                return;
            }
            else
            {
                terPtr++;
            }
        }
        else
        {
            errorHappen=true;
            showError(-1,"");
            return;
        }
    }

    public void myconst()//常量递归子程序
    //<const> → <id>:=<integer>
    {
        String name;
        int value;
        if(rv[terPtr].getId()==SYM)
        {//标识符
            name=rv[terPtr].getValue();
            terPtr++;
            if(rv[terPtr].getId()==CEQU)
            {// 赋值号：=
                terPtr++;
                if(rv[terPtr].getId()==CONST)
                {
                    value=Integer.parseInt(rv[terPtr].getValue());//字符串转换整数
                    if(STable.isNowExistSTable(name, level))
                    {
                        errorHappen=true;
                        showError(15,name);
                    }
                    STable.enterConst(name,level,value,address);//登录符号表
                    terPtr++;     
                }
            }
            else
            {
                errorHappen=true;
                showError(3,"");
                return;
            }
        }
        else 
        {
            errorHappen=true;
            showError(1,"");
            return;
        }
    }

    public void vardecl()//变量说明
    //<vardecl> → var <id>{,<id>};
    {
        String name;
        int value;
        if(rv[terPtr].getId()==VAR)
        {
            terPtr++;
            if(rv[terPtr].getId()==SYM)
            {//标识符
                name=rv[terPtr].getValue();
                if(STable.isNowExistSTable(name, level))//同层检测
                {
                    errorHappen=true;
                    showError(15,name);
                }
                STable.enterVar(name,level,address);//登录符号表
                address+=addrIncrement;//遇见变量加一
                terPtr++;
                while(rv[terPtr].getId()==COMMA)
                {
                    terPtr++;
                    if(rv[terPtr].getId()==SYM)
                    {
                        name=rv[terPtr].getValue();
                        if(STable.isNowExistSTable(name, level))
                        {
                            errorHappen=true;
                            showError(15,name);
                        }
                        STable.enterVar(name,level,address);
                        address+=addrIncrement;     //变量个数加1登录符号表
                        terPtr++;
                    }
                    else
                    {
                        errorHappen=true;
                        showError(1,"");
                        return;
                    }
                }
                if(rv[terPtr].getId()!=SEMIC)
                {//分号
                    errorHappen=true;
                    showError(0,"");
                    return;
                }
                else
                {
                    terPtr++;
                }
            }
            else 
            {
                errorHappen=true;
                showError(1,"");
                return;
            }

        }
        else
        {
            errorHappen=true;
            showError(-1,"");
            return;
        }
    }

    public void proc()//分程序
    //<proc> → procedure <id>（[<id>{,<id>}]）;<block>{;<proc>}
    {
        if(rv[terPtr].getId()==PROC)
        {
            terPtr++;
            int count=0;//用来记录proc中形参的个数
            int propos;// 记录本分程序在符号表中的位置
            if(rv[terPtr].getId()==SYM)
            {
                String name=rv[terPtr].getValue();
                if(STable.isNowExistSTable(name, level))
                {
                    errorHappen=true;
                    showError(15,name);
                }
                propos=STable.getTablePtr();
                STable.enterProc(rv[terPtr].getValue(),level,address);//登录符号表
                level++;                //level值加一，因为其后的所有定义均在该新的proc中完成
                terPtr++;
                if(rv[terPtr].getId()==LBR)
                {
                    terPtr++;
                    if(rv[terPtr].getId()==SYM)
                    {
                    	//形参当做变量压入符号表（不用进行是否已经存在的检查）
                        STable.enterVar(rv[terPtr].getValue(),level,3+count) ;//（3+count）形式单元位于连接数据之后（P244）
                        count++;
                        STable.getAllTable()[propos].setSize(count);
                        //用本过程在符号表中的size域记录形参的个数
                        terPtr++;
                        while(rv[terPtr].getId()==COMMA)
                        {
                            terPtr++;
                            if(rv[terPtr].getId()==SYM)
                            {
                                STable.enterVar(rv[terPtr].getValue(),level,3+count) ;      //3+count+1为形参在存储空间中的位置
                                count++;
                                STable.getAllTable()[propos].setSize(count); //用本过程在符号表中的size域记录形参的个数
                                terPtr++;
                            }
                            else
                            {
                                errorHappen=true;
                                showError(1,"");
                                return;
                            }
                        }
                    }
                    if(rv[terPtr].getId()==RBR)
                    {
                        terPtr++;
                        if(rv[terPtr].getId()!=SEMIC)
                        {
                            errorHappen=true;
                            showError(0,"");
                            return;
                        }
                        else
                        {
                            terPtr++;
                            block();
                            while(rv[terPtr].getId()==SEMIC)
                            {
                                terPtr++;
                                proc();
                            }
                        }
                    }
                    else
                    {
                        errorHappen=true;
                        showError(5,"");
                        return;
                    }

                }
                else
                {
                    errorHappen=true;
                    showError(4,"");
                    return;
                }
            }
            else
            {
                errorHappen=true;
                showError(1,"");
                return;
            }

        }
        else
        {
            errorHappen=true;
            showError(-1,"");
            return;
        }
    }

    public void body()//复合语句
    //begin <statement>{;<statement>}end
    {
        if(rv[terPtr].getId()==BEGIN)
        {
            terPtr++;
            statement();
            while(rv[terPtr].getId()==SEMIC||rv[terPtr].getId()!=END)
            {
              if(rv[terPtr].getId()==SEMIC)
              {      
            	terPtr++;
                statement();
              }
              else
              {
            	errorHappen=true;
                showError(0,"");
                return; 
              }
            }
            if(rv[terPtr].getId()==END)
            {
                terPtr++;
            }
            else
            {
                errorHappen=true;
                showError(7,"");
                return;
            }
        }
        else
        {
            errorHappen=true;
            showError(6,"");
            return;
        }
    }

    public void statement()//语句
   /* <statement> → <id> := <exp>               
    			   |if <lexp> then <statement>[else <statement>]
                   |while <lexp> do <statement>
                   |call <id>（[<exp>{,<exp>}]）
                   |<body>
                   |read (<id>{，<id>})
                   |write (<exp>{,<exp>})*/

    {
        if(rv[terPtr].getId()==IF)//if <lexp> then <statement>[else <statement>]
        {
            int cx1;
            terPtr++;
            lexp();
            if(rv[terPtr].getId()==THEN)
            {
                cx1=Pcode.getCodePtr(); //用cx1记录jpc ，0，0在Pcode中的地址，用来回填
                Pcode.gen(Pcode.getJPC(),0,0);//产生条件转移指令，条件的bool值为0时跳转，
                							  //跳转的目的地址暂时填为0
                terPtr++;
                statement();
                int cx2=Pcode.getCodePtr();  //cx2记录jmp在Pcode中的地址，一会用来回填
                Pcode.gen(Pcode.getJMP(),0,0);
                
                Pcode.getPcodeArray()[cx1].setA(Pcode.getCodePtr()); 
                //地址回填，将jpc，0，0中的A回填
                Pcode.getPcodeArray()[cx2].setA(Pcode.getCodePtr());//考虑到没有else的情况
                if(rv[terPtr].getId()==ELS)
                {
                    terPtr++;
                    statement();
                    Pcode.getPcodeArray()[cx2].setA(Pcode.getCodePtr());
                }
            }
            else
            {
                errorHappen=true;
                showError(8,"");
                return;
            }                                 
        }
        else if(rv[terPtr].getId()==WHI)//while <lexp> do <statement>
        {
            int cx1=Pcode.getCodePtr(); //保存条件表达式在Pcode中的地址，便于do后的循环
            terPtr++;
            lexp();
            if(rv[terPtr].getId()==DO)
            {
                int cx2=Pcode.getCodePtr();//保存条件跳转指令的地址，在回填时使用，仍是条件不符合是跳转
                Pcode.gen(Pcode.getJPC(),0,0);
                terPtr++;
                statement();
                Pcode.gen(Pcode.getJMP(),0,cx1); //完成DO后的相关语句后，需要跳转至条件表达式处，
                								//检查是否符合条件，即是否继续循环
                Pcode.getPcodeArray()[cx2].setA(Pcode.getCodePtr()); //回填JPC条件转移指令
            }
            else
            {
                errorHappen=true;
                showError(9,"");
                return;
            }
        }
        
        else if(rv[terPtr].getId()==CAL)//call <id>（[<exp>{,<exp>}])
        {
            terPtr++;
            int count=0;//用来检验传入的参数和设定的参数是否相等
            TableUnit tempRow;
            if(rv[terPtr].getId()==SYM)
            {
                if(STable.isPreExistSTable(rv[terPtr].getValue(),level))
                {        //符号表中存在该标识符
                     tempRow=STable.getRow(STable.getNameRow(rv[terPtr].getValue()));  
                     //获取该标识符所在行的所有信息，保存在tempRow中
                    if(tempRow.getType()==STable.getProc()) 
                    { //判断该标识符类型是否为procedure，SymTable中procdure类型用proc变量来表示，
                        ;
                    }           //if类型为proc(不作处理)
                    else
                    {       //cal类型不一致的错误
                        errorHappen=true;
                        showError(11,"");
                        return;
                    }
                }       //if符号表中存在标识符
                else
                {           //cal 未定义变量的错误
                    errorHappen=true;
                    showError(10,"");
                    return;
                }
                terPtr++;
                if(rv[terPtr].getId()==LBR)
                {
                    terPtr++;
                    if(rv[terPtr].getId()==RBR)
                    {//先把没有参数情况列到最前
                        terPtr++;
                        Pcode.gen(Pcode.getCAL(),level-tempRow.getLevel(),tempRow.getValue());        //调用过程中的保存现场由解释程序完成，这里只产生目标代码,+3需详细说明
                    }
                    else
                    {
                        exp();
                        count++;
                        while(rv[terPtr].getId()==COMMA)
                        {
                            terPtr++;
                            exp();
                            count++;
                        }
                        if(count!=tempRow.getSize())//参数个数是否匹配
                        {
                            errorHappen=true;
                            showError(16,tempRow.getName());
                            return;
                        }
                        //地址存在tempRow.getValue()
                        Pcode.gen(Pcode.getCAL(),level-tempRow.getLevel(),tempRow.getValue());
                        //调用过程中的保存现场由解释程序完成，这里只产生目标代码
                        if(rv[terPtr].getId()==RBR)
                        {
                            terPtr++;
                        }
                        else
                        {
                            errorHappen=true;
                            showError(5,"");
                            return;
                        }
                    }
                }
                else
                {
                    errorHappen=true;
                    showError(4,"");
                    return;
                }
            }
            else
            {
                errorHappen=true;
                showError(1,"");
                return;
            }
            
        }
        else if(rv[terPtr].getId()==REA)//read (<id>{，<id>})
        {
            terPtr++;
            if(rv[terPtr].getId()==LBR)
            {//(
                terPtr++;
                if(rv[terPtr].getId()==SYM)
                {//标识符
                    if(!STable.isPreExistSTable((rv[terPtr].getValue()),level))
                    {      //首先判断在符号表中在本层或本层之前是否有此变量
                        errorHappen=true;
                        showError(10,"");
                        return;
                    }//if判断在符号表中是否有此变量
                    else
                    {           //sto未定义变量的错误
                        TableUnit tempTable=STable.getRow(STable.getNameRow(rv[terPtr].getValue()));
                        if(tempTable.getType()==STable.getVar())
                        {       //该标识符是否为变量类型
                            Pcode.gen(Pcode.getOPR(),0,16); //OPR 0 16	从命令行读入一个输入置于栈顶   
                            Pcode.gen(Pcode.getSTO(),level-tempTable.getLevel(),tempTable.getAddress());  
                            //STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
                        }//if标识符是否为变量类型
                        else
                        {       //sto类型不一致的错误
                            errorHappen=true;
                            showError(12,"");
                            return;
                        }
                    }
                    terPtr++;
                    while(rv[terPtr].getId()==COMMA)
                    {
                        terPtr++;
                        if(rv[terPtr].getId()==SYM)
                        {
                            if(!STable.isPreExistSTable((rv[terPtr].getValue()),level))
                            {      //首先判断在符号表中是否有此变量
                                errorHappen=true;
                                showError(10,"");
                                return;

                            }//if判断在符号表中是否有此变量
                            else
                            {           //sto未定义变量的错误
                                TableUnit tempTable=STable.getRow(STable.getNameRow(rv[terPtr].getValue()));
                                if(tempTable.getType()==STable.getVar())
                                {       //该标识符是否为变量类型
                                    Pcode.gen(Pcode.getOPR(),0,16);         
                                    //OPR 0 16	从命令行读入一个输入置于栈顶 
                                    Pcode.gen(Pcode.getSTO(),level-tempTable.getLevel(),tempTable.getAddress());  //STO L ，a 将数据栈栈顶的内容存入变量（相对地址为a，层次差为L）
                                }//if标识符是否为变量类型
                                else
                                {       //sto类型不一致的错误
                                    errorHappen=true;
                                    showError(12,"");
                                    return;
                                }
                            }
                            terPtr++;
                        }else
                        {
                            errorHappen=true;
                            showError(1,"");
                            return;
                        }
                    }
                    if(rv[terPtr].getId()==RBR)
                    {//)
                        terPtr++;
                    }
                    else
                    {
                        errorHappen=true;
                        showError(5,"");
                    }
                }
                else
                {
                    errorHappen=true;
                    showError(26,"");
                }
            }
            else
            {
                errorHappen=true;
                showError(4,"");
                return;
            }
        }
        else if(rv[terPtr].getId()==WRI)//write (<exp>{,<exp>})
        {
            terPtr++;
            if(rv[terPtr].getId()==LBR)
            {
                terPtr++;
                exp();
                Pcode.gen(Pcode.getOPR(),0,14);         //输出栈顶的值到屏幕
                while(rv[terPtr].getId()==COMMA)
                {
                    terPtr++;
                    exp();
                    Pcode.gen(Pcode.getOPR(),0,14);         //输出栈顶的值到屏幕
                }

                Pcode.gen(Pcode.getOPR(),0,15);         //输出换行
                if(rv[terPtr].getId()==RBR)
                {
                    terPtr++;
                }
                else
                {
                    errorHappen=true;
                    showError(5,"");
                    return;
                }
            }
            else
            {
                errorHappen=true;
                showError(4,"");
                return;
            }
           
        }
        else if(rv[terPtr].getId()==BEGIN)
        {         //body不生成目标代码
            body();
        }
        else if(rv[terPtr].getId()==SYM)//<id> := <exp>
        {      //赋值语句
            String name=rv[terPtr].getValue();
            terPtr++;
            if(rv[terPtr].getId()==CEQU)
            {//:=
                terPtr++;
                exp();
                if(!STable.isPreExistSTable(name,level))
                {        //检查标识符是否在符号表中存在
                    errorHappen=true;
                    showError(14,name);
                    return;
                }//if判断在符号表中是否有此变量
                else
                {           //sto未定义变量的错误
                    TableUnit tempTable=STable.getRow(STable.getNameRow(name));
                    if(tempTable.getType()==STable.getVar())//检查标识符是否为变量类型
                    {           //检查标识符是否为变量类型
                        Pcode.gen(Pcode.getSTO(),level-tempTable.getLevel(),tempTable.getAddress());  
                        //STO L ，a 将数据栈栈顶的内容存入变量
                    }
                    else
                    {       //类型不一致的错误
                        errorHappen=true;
                        showError(13,name);
                        return;
                    }
                }
            }
            else
            {
                errorHappen=true;
                showError(3,"");
                return;
            }
        }
        else
        {
            errorHappen=true;
            showError(1,"");
            return;
        }
    }

    public void lexp()//条件
    //<lexp> → <exp> <lop> <exp>|odd <exp>
    {
        if(rv[terPtr].getId()==ODD)
        {
            terPtr++;
            exp();
            Pcode.gen(Pcode.getOPR(),0,6);  //OPR 0 6	栈顶元素的奇偶判断，结果值在栈顶
        }
        else
        {
            exp();
            int operator=lop();        //返回值用来产生目标代码，如下
            exp();
            if(operator==EQU)
            {
                Pcode.gen(Pcode.getOPR(),0,8);      //OPR 0 8	次栈顶与栈顶是否相等，退两个栈元素，结果值进栈
            }
            else if(operator==NEQU)
            {
                Pcode.gen(Pcode.getOPR(),0,9);      //OPR 0 9	次栈顶与栈顶是否不等，退两个栈元素，结果值进栈
            }
            else if(operator==LES)
            {
                Pcode.gen(Pcode.getOPR(),0,10);     //OPR 0 10	次栈顶是否小于栈顶，退两个栈元素，结果值进栈
            }
            else if(operator==LESE)
            {
                Pcode.gen(Pcode.getOPR(),0,13);     // OPR 0 13	次栈顶是否小于等于栈顶，退两个栈元素，结果值进栈
            }
            else if(operator==LAR)
            {
                Pcode.gen(Pcode.getOPR(),0,12);     //OPR 0 12	次栈顶是否大于栈顶，退两个栈元素，结果值进栈
            }
            else if(operator==LARE)
            {
                Pcode.gen(Pcode.getOPR(),0,11);     //OPR 0 11	次栈顶是否大于等于栈顶，退两个栈元素，结果值进栈
            }
        }
    }

    public void exp()//表达式
    //<exp> → [+|-]<term>{<aop><term>}
    {
        int tempId=rv[terPtr].getId();
        if(rv[terPtr].getId()==ADD)
        {
            terPtr++;
        }
        else if(rv[terPtr].getId()==SUB)
        {
            terPtr++;
        }
        term();
        if(tempId==SUB)
        {
            Pcode.gen(Pcode.getOPR(),0,1); //  OPR 0 1	栈顶元素取反,说明是负数
        }
        while(rv[terPtr].getId()==ADD||rv[terPtr].getId()==SUB)
        {
            tempId=rv[terPtr].getId();
            terPtr++;
            term();
            if(tempId==ADD)
            {
                Pcode.gen(Pcode.getOPR(),0,2); //OPR 0 2	次栈顶与栈顶相加，退两个栈元素，结果值进栈
            }
            else if(tempId==SUB)
            {
                Pcode.gen(Pcode.getOPR(),0,3);  //OPR 0 3	次栈顶减去栈顶，退两个栈元素，结果值进栈
            }
        }
    }

    public void term()
    //<term> → <factor>{<mop><factor>}
    {
        factor();
        while(rv[terPtr].getId()==MUL||rv[terPtr].getId()==DIV)
        {
            int tempId=rv[terPtr].getId();
            terPtr++;
            factor();
            if(tempId==MUL)
            {
                Pcode.gen(Pcode.getOPR(),0,4);       //OPR 0 4	次栈顶乘以栈顶，退两个栈元素，结果值进栈
            }
            else if(tempId==DIV)
            {
                Pcode.gen(Pcode.getOPR(),0,5);      // OPR 0 5	次栈顶除以栈顶，退两个栈元素，结果值进栈
            }
        }
    } 

    public void factor()//因子
    //<factor>→<id>|<integer>|(<exp>)
    {
        if(rv[terPtr].getId()==CONST)
        {//integer
            Pcode.gen(Pcode.getLIT(),0,Integer.parseInt(rv[terPtr].getValue()));    
            //是个数字,  LIT 0 a 取常量a放入数据栈栈顶
            terPtr++;
        }
        else if(rv[terPtr].getId()==LBR)
        {//(
            terPtr++;
            exp();
            if(rv[terPtr].getId()==RBR)
            {
                terPtr++;
            }
            else
            {
                errorHappen=true;
                showError(5,"");
            }
        }
        else if(rv[terPtr].getId()==SYM)
        {
            String name=rv[terPtr].getValue();
            if(!STable.isPreExistSTable(name,level))
            {     //判断因子中标识符在符号表中是否存在
                errorHappen=true;
                showError(10,"");
                return;
            }
            else
            {           //未定义变量的错误
                TableUnit tempRow= STable.getRow(STable.getNameRow(name));
                //通过 name ->行号 ->这一行所有的值
                if(tempRow.getType()==STable.getVar())
                { //标识符是变量类型
                    Pcode.gen(Pcode.getLOD(),level-tempRow.getLevel(),tempRow.getAddress());    
                    //变量，LOD L  取变量（相对地址为a，层差为L）放到数据栈的栈顶
                }
                else if (tempRow.getType()==STable.getMyconst())
                {
                    Pcode.gen(Pcode.getLIT(),0,tempRow.getValue());         //常量，LIT 0 a 取常量a放入数据栈栈顶
                }
                else
                {       //类型不一致的错误
                    errorHappen=true;
                    showError(12,"");
                    return;
                }
            }
            terPtr++;
        }
        else 
        {
            errorHappen=true;
            showError(1,"");
        }
    }
 
    public int lop()//关系运算符
    //<lop> → =|<>|<|<=|>|>=
    {
        String loperator;
        if(rv[terPtr].getId()==EQU)
        {
            terPtr++;
            return EQU;//=
        }
        else if(rv[terPtr].getId()==NEQU)
        {
            terPtr++;
            return NEQU;//<>
        }
        else if(rv[terPtr].getId()==LES)
        {
            terPtr++;
            return LES;//<
        }
        else if(rv[terPtr].getId()==LESE)
        {
            terPtr++;
            return LESE;//<=
        }
        else if(rv[terPtr].getId()==LAR)
        {
            terPtr++;
            return LAR;//>
        }
        else if(rv[terPtr].getId()==LARE)
        {
            terPtr++;//>=
            return LARE;
        }
        return -1;
    }

    public boolean mgpAnalysis()
    {
       if( lex.bAnalysis())
       {  //进行词法分析
	    	readLex();
	        prog();
	        return errorHappen;
       }
       else
       {
    	    return true;//有词法错误
       }
    }

    public void printTable()
    {
        if(errorHappen)
        {
        	return;
        }
    	String str="SymbolTable.txt";
        OutputStream myout=null;
        TableUnit temp;//接收每一次的一个类
    	
        File file=new File(str);
        try
        {
            myout=new FileOutputStream(file);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        String ttype;
        String tname ;
        String tlevel ;
        String taddress;
        String tvalue;
        String tsize;
        
        for(int i=0;i<STable.getLength();i++)
        {
        	temp = STable.getRow(i);
        	ttype = String.valueOf(temp.getType());
        	tname =String.valueOf(temp.getName());
            tlevel = String.valueOf(temp.getLevel()) ;
            taddress = String.valueOf(temp.getAddress());
            tvalue = String.valueOf(temp.getValue());
            tsize= String.valueOf(temp.getSize());
        	byte [] btype = ttype.getBytes();
        	byte [] bname = tname.getBytes();
        	byte [] blevel = tlevel.getBytes();
        	byte [] baddress = taddress.getBytes();
        	byte [] bvalue = tvalue.getBytes();
        	byte [] bsize = tsize.getBytes();
        	
            try{
                myout.write(btype);
                myout.write(' ');
                myout.write(bname);
                myout.write(' ');
                myout.write(blevel);
                myout.write(' ');
                myout.write(baddress);
                myout.write(' ');
                myout.write(bvalue);
                myout.write(' ');
                myout.write(bsize);
                myout.write('\r'); 
                myout.write('\n'); 
            }catch(IOException e){
                e.printStackTrace();
            }

        }
        
        try{
            myout.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void showPcode()
    {
    	
        for(int i=0;i<Pcode.getCodePtr();i++)
        {
           switch(Pcode.getPcodeArray()[i].getF())
           {
               case 0:
                   System.out.print("LIT  ");
                   break;
               case 1:
                   System.out.print("OPR  ");
                   break;
               case 2:
                   System.out.print("LOD  ");
                   break;
               case 3:
                   System.out.print("STO  ");
                   break;
               case 4:
                   System.out.print("CAL  ");
                   break;
               case 5:
                   System.out.print("INT  ");
                   break;
               case 6:
                   System.out.print("JMP  ");
                   break;
               case 7:
                   System.out.print("JPC  ");
                   break;
               case 8:
                   System.out.print("RED  ");
                   break;
               case 9:
                   System.out.print("WRI  ");
                   break;
           }
            System.out.println(Pcode.getPcodeArray()[i].getL()+"  "+Pcode.getPcodeArray()[i].getA());
        }
    }

    public void showPcodeInStack()
    {
    	if(errorHappen)
    	{
    		return;
    	}
        Interpreter inter=new Interpreter();
        inter.setPcode(Pcode);
        
        OutputStream myout=null ;
        String str="PcodeTable.txt";
        File file =new File(str);
        try 
        {
        	myout = new FileOutputStream(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();	
        }
        String t_o = null;
        String t_f;
        String t_l;
        String t_a;
        for(int i=0;i<inter.getCode().getCodePtr();i++)
        {
            switch (inter.getCode().getPcodeArray()[i].getF())
            {
                case 0:
                  //  System.out.print("LIT  ");
                    t_o ="LIT"; 
                    break;
                case 1:
                //    System.out.print("OPR  ");
                    t_o ="OPR";
                    break;
                case 2:
                //    System.out.print("LOD  ");
                    t_o ="LOD";
                    break;
                case 3:
                  //  System.out.print("STO  ");
                    t_o ="STO";
                    break;
                case 4:
                //    System.out.print("CAL  ");
                    t_o ="CAL";
                    break;
                case 5:
                //    System.out.print("INT  ");
                    t_o ="INT";
                    break;
                case 6:
                //    System.out.print("JMP  ");
                    t_o ="JMP";
                    break;
                case 7:
                //    System.out.print("JPC  ");
                    t_o ="JPC";
                    break;
                case 8:
                 //   System.out.print("RED  ");
                    t_o ="RED";
                    break;
                case 9:
                 //   System.out.print("WRI  ");
                    t_o ="WRI";
                    break;
            }
            
            t_f = t_o;
            t_l = String.valueOf(inter.getCode().getPcodeArray()[i].getL());
            t_a = String.valueOf(inter.getCode().getPcodeArray()[i].getA());
            
            byte [] b_f=t_f.getBytes();
            byte [] b_l= t_l.getBytes();
            byte [] b_a=t_a.getBytes();
            try{
            	myout.write(b_f);
            	myout.write(' ');
            	myout.write(b_l);
            	myout.write(' ');
            	myout.write(b_a);
            	myout.write('\r');
            	myout.write('\n');
            }catch(IOException e){
                e.printStackTrace();
            }
            
         //   System.out.println(inter.getCode().getPcodeArray()[i].getL()+"  "+inter.getCode().getPcodeArray()[i].getA());
        }
    }

    public void showError(int i,String name)
    {
        switch (i)
        {
            case -1:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("wrong token");        //常量定义不是const开头,变量定义不是var 开头
                break;
            case 0:
                System.out.print("ERROR "+i+" "+"in line " + (rv[terPtr].getLine()-1)+":");
                System.out.println("Missing semicolon");        //缺少分号
                break;
            case 1:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Identifier illegal");       //标识符不合法
                break;
            case 2:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("The beginning of program must be 'program'");       //程序开始第一个字符必须是program
                break;
            case 3:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Assign must be ':='");       //赋值没用：=
                break;
            case 4:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing '('");       //缺少左括号
                break;
            case 5:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing ')'");       //缺少右括号
                break;
            case 6:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'begin'");       //缺少begin
                break;
            case 7:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'end'");       //缺少end
                break;
            case 8:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'then'");       //缺少then
                break;
            case 9:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Missing 'do'");       //缺少do
                break;
            case 10:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Not exist "+"'"+rv[terPtr].getValue()+"'");       //call，write，read语句中，不存在标识符
                break;
            case 11:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("'"+rv[terPtr].getValue()+"'"+"is not a procedure");       //该标识符不是proc类型
                break;
            case 12:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("'"+rv[terPtr].getValue()+"'"+"is not a variable");       //read，write语句中，该标识符不是var类型
                break;
            case 13:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("'"+name+"'"+"is not a variable");       //赋值语句中，该标识符不是var类型
                break;
            case 14:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Not exist"+"'"+name+"'");       //赋值语句中，该标识符不存在
                break;
            case 15:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Already exist"+"'"+name+"'");       //该标识符已经存在
                break;
            case 16:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("Number of parameters of procedure "+"'"+name+"'"+"is incorrect");       //该标识符已经存在
                break;
            case 17:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println("ILLEGAL"+"'"+name+"'");       //该标识符不存在
                break;
            case 26:
                System.out.print("ERROR "+i+" "+"in line " + rv[terPtr].getLine()+":");
                System.out.println(name+"is not Var");       //该标识符已经存在
                break;
        }

    }
    public void interpreter()
    {
        if(errorHappen)
        {
            return;
        }
        Interpreter inter=new Interpreter();
        inter.setPcode(Pcode);      //将目标代码传递给解释程序进行解释执行
        inter.interpreter();
    }

}


