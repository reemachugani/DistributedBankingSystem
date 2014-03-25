/**
 * Created with IntelliJ IDEA.
 * User: reema
 * Date: 13/3/14
 * Time: 9:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class Account {
    public enum Currency{SGD, USD, INR, CNY};

    String name;
    int accNumber;
    Currency currency;
    char[] password;
    double balance;

    Account(String n, String c, char[] pwd, double amt, int accNum){
        name = n;
        currency = Currency.valueOf(c);
        password = pwd;
        balance = amt;
        accNumber = accNum;
    }

    public void setPwd(char[] p){
        password = p;
    }

    public double getBalance(){
        return balance;
    }

    public void setBalance(double curBal){
        balance = curBal;
    }
}
