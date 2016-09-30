package Broker;
import xPlatform.BrokerService;
import xPlatform.IOperation;
import java.io.IOException;
/**
 * Created by Vladok on 07.09.2016.
 */
public class Broker {
    public static void main(String[] args) throws IOException {
        System.out.println("BROKER OPTIONS:");
        IOperation broker=new BrokerService();
        String msg;
        while(true)
        {
            while(!(msg=broker.readAsync()).isEmpty())
                if(msg.equals("invalid"))
                {
                    System.out.println("--Broker loop--");
                    System.out.println("--INVALID MESSAGE--");
                }
                else
                {
                    System.out.println("--Broker loop--");
                    System.out.println("--VALID MESSAGE--");
                    broker.writeAsync(msg);
                }
        }
    }
}
