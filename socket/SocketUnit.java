package socket;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import gbml.Consts;
import gbml.RuleSet;

public class SocketUnit implements Callable< ArrayList<RuleSet> >{

    private InetSocketAddress address = null;

    ArrayList<RuleSet> subRuleSets = new ArrayList<RuleSet>();

    ArrayList<RuleSet> newRuleSets;

    public SocketUnit(InetSocketAddress address, ArrayList<RuleSet> subRuleSets) {
        this.address = address;
        this.subRuleSets = subRuleSets;
    }

    public ArrayList<RuleSet> getRuleSets(){
    	return newRuleSets;
    }

    @SuppressWarnings("unchecked")
	@Override
    public ArrayList<RuleSet> call() throws Exception {

        try {
            Socket socket = new Socket();
            socket.connect(this.address, Consts.WAIT_SECOND);

            ObjectOutputStream send = new ObjectOutputStream( socket.getOutputStream() );
            ObjectInputStream recieve = new ObjectInputStream( socket.getInputStream() );

            //ルールセットを送信
            send.writeObject( subRuleSets );

            //ルールセットを受信
            newRuleSets = ( (ArrayList<RuleSet>) recieve.readObject() );

            //クローズ
            send.close();
            recieve.close();
            socket.close();
        }
        catch(Exception e){
        	System.out.println(e + ": SoketUnit");
        }

        return newRuleSets;
    }

}
