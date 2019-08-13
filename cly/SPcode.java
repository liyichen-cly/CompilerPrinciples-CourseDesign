package cly;
/**
 * 单个目标代码
 */
public class SPcode 
{
/**  
    *    F段代表伪操作码
    *    L段代表调用层与说明层的层差值
    *    A段代表位移量（相对地址）*/

    private int F;
    private int L;
    private int A;

    public SPcode(int f,int l,int a)
    {
        F=f;
        L=l;
        A=a;
    }
    public void setF(int f)
    {
        F=f;
    }
    public void setL(int l)
    {
        L=l;
    }
    public void setA(int a)
    {
        A=a;
    }
    public int getF()
    {
        return F;
    }
    public int getL()
    {
        return L;
    }
    public int getA()
    {
        return A;
    }
}