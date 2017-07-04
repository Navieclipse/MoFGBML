package island;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import gbml.Consts;
import gbml.PopulationManager;

public class SocketUnit2 implements Callable< ArrayList<PopulationManager> >{

    private InetSocketAddress address = null;

    ArrayList<PopulationManager> subPopManagers = new ArrayList<PopulationManager>();

    ArrayList<PopulationManager> newPopManagers;

    public SocketUnit2(InetSocketAddress address, ArrayList<PopulationManager> subPopManagers) {
        this.address = address;
        this.subPopManagers = subPopManagers;
    }

    public ArrayList<PopulationManager> getRuleSets(){
    	return newPopManagers;
    }

    @SuppressWarnings("unchecked")
	@Override
    public ArrayList<PopulationManager> call() throws Exception {

        try {
            Socket socket = new Socket();
            socket.connect(this.address, Consts.WAIT_SECOND);

            ObjectOutputStream send = new ObjectOutputStream( socket.getOutputStream() );
            ObjectInputStream recieve = new ObjectInputStream( socket.getInputStream() );

            //ルールセットを送信
            send.writeObject( subPopManagers );

            //ルールセットを受信
            newPopManagers = ( (ArrayList<PopulationManager>) recieve.readObject() );

            //クローズ
            send.close();
            recieve.close();
            socket.close();
        }
        catch(Exception e){
        	System.out.println(e + ": SoketUnit error");
        }

        return newPopManagers;
    }

}
