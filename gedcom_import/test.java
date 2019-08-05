
public class test{
    public static void main(String[] args){
        String str = 'miller, matthew';
        String last_name = str.substring( 0, str.indexOf(","));
        System.out.println(last_name);
    }
}
