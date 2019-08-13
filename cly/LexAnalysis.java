package cly;
//词法分析器，读取源程序，写入LexTable.txt当中
import java.io.*;
public class LexAnalysis 
{//keyWord={"program","begin","end","if","then","else","const",
 //"procedure","var","do","while","call","read","write","odd"};
   /*关键字在ChTable 中初始化
	private static  int PROG=1;//program
    private static  int BEG=2;//begin
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
    private static  int ODD=15;//  odd     和keyWord中每个字的序号是相等的
*/
    private static  int EQU=16;//"="
    private static  int LES=17;//"<"
    private static  int LESE=18;//"<="
    private static  int LARE=19;//">="
    private static  int LAR=20;//">"
    private static  int NEQE=21;//"<>"


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

    private ChTable ct=new ChTable();
    private RValue rv=new RValue();
    private String[] keyWord=ct.getKeyWord();//关键字
    private String[] symTable=ct.getSymTable();//因为常量和变量的长度大小不一定，因此需要定义数组
    private int symLength=symTable.length;
    private String[] constTable=ct.getConstTable();//常量
    private int conLength=constTable.length;

    private char ch=' ';//读取一个字符缓冲区
    private String strToken;//存储单词的内容
    private String filename;
    private char[] buffer;
    private int searchPtr=0;//字符搜索指针
    private int line=1;
    private boolean errorHappen=false;

    public LexAnalysis(String _filename)
    {
    	//初始化标识符表和常量表为空
        for(int i=0;i<symLength;i++)
        {
            symTable[i]=null;
        }
        for(int j=0;j<conLength;j++)
        {
            constTable[j]=null;
        }
        filename=_filename;
    }

  //预处理函数：读取源文件内容到字符数组buffer中去(包括换行符)
    public char[] preManage()
    {
        File file=new File(filename);
        BufferedReader bf=null;
        
        try {
            //   System.out.println("read file test.txt...");
            bf=new BufferedReader(new FileReader(file));
            String temp1="",temp2 = "";
            while((temp1=bf.readLine())!=null)
            {
                temp2=temp2+temp1+String.valueOf('\n');
            }
            buffer=temp2.toCharArray();
            bf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return buffer;
    }
    public char getChar()
    {
        if(searchPtr<buffer.length)
        {
            ch=buffer[searchPtr];
            searchPtr++;
        }
        return ch;
    }


    public void getBC()//过滤掉空格、制表符、换行符
    {
        while( (ch==' '||ch=='	'||ch=='\n')&&(searchPtr<buffer.length))
        {
            if(ch=='\n')
            {
                line++;
            }
            getChar();
        }
    }

    public String concat()
    {
        strToken=strToken+String.valueOf(ch);
        return strToken;
    }

    public boolean isLetter()
    {
        if(Character.isLetter(ch))
        {
            return true;
        }
        return false;
    }

    public boolean isDigit()
    {
        if(Character.isDigit(ch))
        {
            return true;
        }
        return false;
    }

    public int reserve()
    {
        for(int i=0;i<keyWord.length;i++)
        {
            if(keyWord[i].equals(strToken))
            {
                return i+1;
            }
        }
        return 0;
    }

    public void retract()
    {
        searchPtr--;
        ch=' ';
    }

    public int insertId()
    {
        for(int i=0;i<symLength;i++)
        {
            if(symTable[i]==null)
            {
                symTable[i]=strToken;
                return i;
            }
        }
        return -1;//表示symTable已经满了
    }

    public int insertConst()
    {
        for(int i=0;i<conLength;i++)
        {
            if(constTable[i]==null)
            {
                constTable[i]=strToken;
                return i;
            }
        }
        return -1;//constTable已经满了
    }
  
    public void showError(char a)
    {
        System.out.println();
        System.out.print("ERROR: 不能识别这个符号:"+a+" 行 "+line);
        System.out.println();
        searchPtr++;
    }

    /**
     * 单词识别函数
     * 功能：通过读取buffer数组中的单个单词进行识别源程序中的各个元素，每次识别一个元素
     * @return 识别出的单词的属性：RV.getId()是属性，RV.getValue()是值，RV.getLine()是所在行号
     *
     * */
    public RValue analysis()
    {
        int code;//判断字符串是否为保留字
        int value;//记录常量在常量表中的索引或者变量在变量中的索引
        strToken="";
        getChar();
        getBC();//经过此步，如果ch=='\n'，则肯定是到达了文件末尾
        if(ch=='\n')
        {
            rv.setId(-1);
            rv.setValue("-1");
            rv.setLine(line);
            return rv;
        }
        if(isLetter())//变量或保留字
        {
            while((isLetter()||isDigit()))
            {
                concat();
                getChar();
            }
            retract();
            code=reserve();//查找保留字表
            if(code==0)//变量
            {
                value=insertId();//插入变量表，返回value索引
                rv.setId(SYM);//标识符
                rv.setValue(symTable[value]);
                rv.setLine(line);
                return rv;
            }
            else //保留字
            {
                rv.setId(code);//保留字的序号
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
        }
        else if(isDigit())//数字integer
        {
            while(isDigit())
            {
                concat();
                getChar();
            }
            retract();
            value=insertConst();//插入常量表，返回value索引
            rv.setId(CONST);
            rv.setValue(constTable[value]);
            rv.setLine(line);
            return rv;
        }
        else if(ch=='=')
        {
            rv.setId(EQU);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch=='+')
        {
            rv.setId(ADD);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch=='-')
        {
            rv.setId(SUB);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        } 
        else if(ch=='*')
        {
            rv.setId(MUL);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch=='/')
        {
            rv.setId(DIV);
            rv.setValue("/");
            rv.setLine(line);
            return rv;
        }
        else if(ch=='<')
        {
            getChar();
            if(ch=='=')
            {
                rv.setId(LESE);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
            else if(ch=='>')
            {
                rv.setId(NEQE);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
            else
            {
                retract();
                rv.setId(LES);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
        }
        else if(ch=='>')
        {
            getChar();
            if(ch=='=')
            {
                rv.setId(LARE);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
            else
            {
                retract();
                rv.setId(LAR);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
        }
        else if(ch==',')
        {
            rv.setId(COMMA);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch==';')
        {
            rv.setId(SEMIC);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch=='.')
        {
            rv.setId(POI);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch=='(')
        {
            rv.setId(LBR);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch==')')
        {
            rv.setId(RBR);
            rv.setValue("-");
            rv.setLine(line);
            return rv;
        }
        else if(ch==':')
        {
            getChar();
            if(ch=='=')
            {
                rv.setId(CEQU);
                rv.setValue("-");
                rv.setLine(line);
                return rv;
            }
            else
            {
            	System.out.println("这里缺少'='"+rv.getLine()+"行");
                retract();
            }
        }
        else
        {
        	 errorHappen=true;
        	 showError(ch);
             return null;
        }
        return rv;
    }


/**
 * 循环识别出所有单词并输出到文件LexTable.txt中
 * */
    public boolean bAnalysis()
    {   //开始词法分析
        preManage();
        RValue temp;
        String str="LexTable.txt";
        OutputStream myout=null;
        File file=new File(str);
        try{
            myout=new FileOutputStream(file);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        while(searchPtr<buffer.length)
        {
            
        	temp=analysis();//每次分析一个单词
            while(temp==null)
            {
            	temp=analysis();//每次分析一个单词
            }       	
            String tempId=String.valueOf(temp.getId()),tempLine=String.valueOf(temp.getLine());
            byte[] bid=tempId.getBytes();
            byte[] bname=temp.getValue().getBytes();
            byte[] bline=tempLine.getBytes();
            try
            {
                myout.write(bid);
                myout.write(' ');
                myout.write(bname);
                myout.write(' ');
                myout.write(bline);
                myout.write('\r');  //Windows换行需要输入\r\n
                myout.write('\n');
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }//while(searchPtr<buffer.length)
        try{
            myout.close();
        }catch(IOException e){
            e.printStackTrace();
        }
       if(errorHappen==true){
           return false;
       }else{
    	   return true;
       }
    }
}


















