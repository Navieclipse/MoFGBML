package island;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import gbml.DataSetInfo;
import gbml.GaManager;
import gbml.PopulationManager;
import gbml.RuleSet;
import methods.DataLoader;
import methods.Divider;
import methods.Output;

public class ServerUnit2 {

    public static void main(String[] args) throws IOException {
    	//名前とポート番号と最大スレッド数
        ServerUnit2 serval = new ServerUnit2( args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
        								Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]) );
        serval.start();
    }

    private String dataName;
    private String dataLocation;
    private int port = -1;
    private int maxThreadNum = 1;
    private int islandNum = 1;

    private int cv_i = 0;
    private int rep_i = 0;

    public ServerUnit2(String dataName, String dataLocation, int port, int maxThreadNum, int islandNum, int cv_i, int rep_i) {
        this.dataName = dataName;
        this.dataLocation = dataLocation;
        this.port = port;
        this.maxThreadNum = maxThreadNum;
        this.islandNum = islandNum;
        this.cv_i = cv_i;
        this.rep_i = rep_i;
    }

    public void start() throws IOException {

        System.out.println("start: " + dataName);

        /************************************************************/
        //データの読み込み
        DataSetInfo trainDataInfo = new DataSetInfo();
        String dataFileName = Output.makeFileNameOne(dataName, dataLocation, cv_i, rep_i, true);
        DataLoader.inputFile(trainDataInfo, dataFileName);

		//データの分割
		DataSetInfo[] trainDataInfos = null;
		InetSocketAddress[] test = null;
		Divider divider = new Divider(this.islandNum);
		trainDataInfos = divider.letsDivide(trainDataInfo, 1, test);

		for(int i=0; i<this.islandNum; i++){
			System.out.println( trainDataInfos[i].getDataSize() );
		}

        /************************************************************/
        //フォークジョイン準備
        ForkJoinPool forkJoinPool = new ForkJoinPool(maxThreadNum);
        Socket socket;
        System.out.println("Ready..");

        //無限ループ！
        while (true) {
			try {
				ServerSocket server = new ServerSocket(this.port);

				//受付まで待機
				socket = server.accept();

				//個体評価開始（並列）
				japari(socket, trainDataInfos, forkJoinPool);

				server.close();
			}
			catch(Exception e){
				System.out.println(e);
			}
        }

    }

    @SuppressWarnings("unchecked")
	void japari(Socket socket, DataSetInfo[] trainDatas, ForkJoinPool forkJoinPool){

        try {
        	//トライ
            ObjectInputStream recieve = new ObjectInputStream( socket.getInputStream() );
            ObjectOutputStream send = new ObjectOutputStream( socket.getOutputStream() );

			//ルールセットを受信
			ArrayList<PopulationManager> subPopManagers = null;
            try{
				subPopManagers = ( (ArrayList<PopulationManager>) recieve.readObject() );
            }catch(Exception e){
            	System.out.print("dddd");
            }

			//現個体の評価確認
			for(int i=0; i<subPopManagers.size(); i++){
				if( !subPopManagers.get(i).getIsEvalutation() ){
					evaluationProcess(subPopManagers.get(i).currentRuleSets, trainDatas[subPopManagers.get(i).getDataIdx()], forkJoinPool);
				}
			}

			//NSGAII
			GaManager gaManager = new GaManager();
			gaManager.nsga2Socket( trainDatas, subPopManagers, forkJoinPool, subPopManagers.get(0).getNowGen(), subPopManagers.get(0).getIntervalGen() );

			//ルールセットを送信
			send.writeObject( subPopManagers );

			//クローズ
			send.close();
			recieve.close();
			socket.close();
        }
        catch(Exception e){
        	System.out.println("error: japar");
        }

    }

    void evaluationProcess(ArrayList<RuleSet> subRuleSets, DataSetInfo trainData, ForkJoinPool forkJoinPool){

		//評価する
		try{
			forkJoinPool.submit( () ->
			subRuleSets.parallelStream()
			.forEach( rule -> rule.evaluationRuleIsland2(trainData) )
			).get();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

    }

}