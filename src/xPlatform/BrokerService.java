package xPlatform;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by Vladok on 14.09.2016.
 */
public class BrokerService implements IOperation {
    private ServerSocket serverSocket;                          //Server Socket(broker)
    private List<Letter> letterList;                            //List of letters(name+message)
    private List<Receiver>receiversList;                       //List of active receivers
    public BrokerService() {
        letterList =new ArrayList<>();                          //List creating
        receiversList=new ArrayList<>();                       //List creating
        try {
            serverSocket = new ServerSocket(1488);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public String readAsync() {
        Socket connectionSocket = null;
        Consumer<Receiver> styleRec = (Receiver p) -> System.out.println("Name: "+p.getName() +", Socket: "+p.getSocket());
        Consumer<Letter> printLetterConsumer= (Letter l)-> System.out.print("Name:"+l.getName()+", Message text: "+l.getMessage());
        try {
            connectionSocket = serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket finalConnectionSocket = connectionSocket;                //  Receiver's socket
        ExecutorService executor = Executors.newSingleThreadExecutor(); //  Thread Executor

        Callable<String> task=new Callable<String>() {
            public String call() {
                InputStream istream = null;
                StringBuffer result = new StringBuffer();
                try {
                    istream = finalConnectionSocket.getInputStream();
                    BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
                    String partlyTransData;
                    while(!(partlyTransData=receiveRead.readLine()).isEmpty())
                        result.append(partlyTransData.trim());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String message= result.toString();
                String answer="valid";
                System.out.println("--Received data from buffer--");
                System.out.println(message);
                DataParserManager xml=new DataParserManager(message);
                if(message.length()>=9 && message.substring(0,8).equals("connect ")) {
                    String name=message.substring(8,message.length());
                    System.out.println("parsed receiver: "+name+" to be connected");
                    receiversList.add(new Receiver(finalConnectionSocket,name));
                }else if(message.length()>=12 && message.substring(0,11).equals("disconnect ")) {
                    String name=message.substring(11,message.length());
                    System.out.println("parsed receiver: "+name+" to be disconnected");

                    letterList.add(new Letter(name,"disconnect\n"));

                }else if(xml.CheckIfXml()) {
                    System.out.println("--Parsed data--");
                    String msg=xml.getMessage();
                    System.out.println("Message from method : "+msg);
                    List<String>rec=xml.getReceivers();
                    System.out.println("Receivers : "+rec);
                    for(int i=0;i<rec.size();i++)
                        letterList.add(new Letter(rec.get(i),msg+"\n"));
                }else{
                    answer="invalid";
                    System.out.println("Message is not valid");
                    int port=finalConnectionSocket.getPort();
                    for (int i=0;i<receiversList.size();i++)
                        if(receiversList.get(i).getSocket().getPort()==port)
                        {
                            letterList.add(new Letter(receiversList.get(i).getName(),"IDIOT"+"\n"));
                            break;
                        }
                }
                System.out.println("---Receiver List:---");
                receiversList.forEach(styleRec);
                System.out.println("--Letters List--");
                letterList.forEach(printLetterConsumer);
                return answer;
            }
        };
        Future<String> future = executor.submit(task);
        String message = null;
        while(!future.isDone());
        try{
            message=future.get();
        } catch (InterruptedException ie) {
            ie.printStackTrace(System.err);
        } catch (ExecutionException ee) {
            ee.printStackTrace(System.err);
        }
        executor.shutdown();
        return message;
    }
    @Override
    public void writeAsync(String message) {
        BiConsumer<Receiver, Letter> recLetterMatch = (receiver, letter) -> {
            if(receiver.getName().equals(letter.getName()))
                try {
                    OutputStream ostream = receiver.getSocket().getOutputStream();
                    PrintWriter pwrite = new PrintWriter(ostream, true);
                    String messageToRec=letter.getMessage();
                    pwrite.println(messageToRec);    // sending to server
                    pwrite.flush();                 // flush the data
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    letter.setSent(true);
                    System.out.println("--Receiver name and Letter name adress MATCH--");
                    System.out.println("Receiver name: "+receiver.getName());
                    System.out.println("Letter name: "+letter.getName());
                    System.out.println("Message: "+letter.getMessage()+" was transmitted to "+letter.getName()+" succesfully...");
                    if (letter.getMessage().equals("disconnect\n"))
                    {
                        receiver.setDisconnected(true);
                        receiver.getSocket().close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        };
        Runnable r=new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<letterList.size();i++)
                {
                    int finalI = i;
                    receiversList.forEach(a -> recLetterMatch.accept(a,letterList.get(finalI)));
                }
                letterList.removeIf(l-> l.isSent());
                receiversList.removeIf(r->r.isDisconnected());
            }
        };
        Thread t=new Thread(r);
        t.start();

    }
}
