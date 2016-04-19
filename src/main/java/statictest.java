/**
 * Created by dong on 15-8-25.
 */
public class statictest {
    public static int a;
    int b;

    static {
        a = 0;
    }

    public void add() {
        a += 1;
    }

    public static void main(String[] args)
    {
        /*statictest s1=new statictest();
        s1.add();
        statictest s2=new statictest();
        System.out.println(statictest.a);*/
        String s="123456";
        System.out.println(s.length());
        char []s1=new char[721];
        s.getChars(0,3,s1,0);
        char a=126;
        System.out.println(a);
    }
}
