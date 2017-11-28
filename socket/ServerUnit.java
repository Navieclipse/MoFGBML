package socket;

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
import gbml.RuleSet;
import methods.DataLoader;
import methods.Divider;
import methods.Output;
import time.TimeWatcher;

public class ServerUnit {

    public static void main(String[] args) throws IOException {
    	//名前とポート番号と最大スレッド数
        ServerUnit serval = new ServerUnit( args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
        								Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]),
        								Integer.parseInt(args[7]) );
        serval.start();
    }

    private String dataName;
    private String dataLocation;
    private int port = -1;
    private int maxThreadNum = 1;
    private int islandNum = 1;

    private int cv_i = 0;
    private int rep_i = 0;

    private int dataPreDivNum = 1;

    public ServerUnit(String dataName, String dataLocation, int port, int maxThreadNum,
    														int islandNum, int cv_i, int rep_i, int dataPreDivNum) {
        this.dataName = dataName;
        this.dataLocation = dataLocation;
        this.port = port;
        this.maxThreadNum = maxThreadNum;
        this.islandNum = islandNum;
        this.cv_i = cv_i;
        this.rep_i = rep_i;
        this.dataPreDivNum = dataPreDivNum;
    }

    public void start() throws IOException {

        System.out.println("start: " + dataName);

        /************************************************************/
        //データの読み込み
		InetSocketAddress[] test = null;
        DataSetInfo trainDataInfo = new DataSetInfo();
        String dataFileName = Output.makeFileNameOne(dataName, dataLocation, cv_i, rep_i, true);
        DataLoader.inputFile(trainDataInfo, dataFileName);

        //データのサンプリング
        //データをクラスごとに均等に分けて一部だけ取り出す．
		Divider preDivider = new Divider(dataPreDivNum);
		DataSetInfo preDivTrainDataInfo = preDivider.letsDivide(trainDataInfo, 1, test)[0];

		//データの分割
		DataSetInfo[] trainDataInfos = null;
		Divider divider = new Divider(this.islandNum);
		trainDataInfos = divider.letsDivide(preDivTrainDataInfo, 1, test);

		System.out.println(trainDataInfos.length);
		for(int i=0; i<this.islandNum; i++){
			System.out.println( trainDataInfos[i].getDataSize() );
		}

        /************************************************************/
        //フォークジョイン準備
        ForkJoinPool forkJoinPool = new ForkJoinPool(maxThreadNum);
        Socket socket;
        System.out.println("Ready.....");

        //無限ループ！
        while (true) {
			try {
				ServerSocket server = new ServerSocket(this.port);

				//受付まで待機
				socket = server.accept();

				//個体評価開始（並列）
				accept(socket, trainDataInfos, forkJoinPool);

				server.close();
			}
			catch(Exception e){
				System.out.println(e);
			}
        }

    }

    @SuppressWarnings("unchecked")
	void accept(Socket socket, DataSetInfo[] trainDatas, ForkJoinPool forkJoinPool){

        try {
        	//トライ
            ObjectInputStream recieve = new ObjectInputStream( socket.getInputStream() );
            ObjectOutputStream send = new ObjectOutputStream( socket.getOutputStream() );

			//ルールセットを受信
			ArrayList<RuleSet> subRuleSets = ( (ArrayList<RuleSet>) recieve.readObject() );


			//TODO
			TimeWatcher timeWatcher = new TimeWatcher();
			timeWatcher.start();


			//メソッドナンバ０なら個体評価
			if(subRuleSets.get(0).getSocketMethodNum() == 0){
				evaluationProcess(subRuleSets, trainDatas, forkJoinPool);
			}else if(subRuleSets.get(0).getSocketMethodNum() == 1){
				makeRuleProcessModify(subRuleSets, trainDatas, forkJoinPool);
			}


			//TODO
			timeWatcher.end();

			//ルールセットを送信
			send.writeObject( subRuleSets );


			//TODO
			String fileName = this.dataName + "_times.txt";
			Output.writeln( fileName, timeWatcher.getNano() );


			//クローズ
			send.close();
			recieve.close();
			socket.close();
        }
        catch(Exception e){
        	System.out.println("error: accept");
        }


    }

    void evaluationProcess(ArrayList<RuleSet> subRuleSets, DataSetInfo[] trainDatas, ForkJoinPool forkJoinPool){

		//評価する
		try{
			forkJoinPool.submit( () ->
			subRuleSets.parallelStream()
			.forEach( rule -> rule.evaluationRuleIsland(trainDatas) )
			).get();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

    }

    void makeRuleProcessModify(ArrayList<RuleSet> subRuleSets, DataSetInfo[] trainDatas, ForkJoinPool forkJoinPool){

		for(int i=0; i<subRuleSets.size(); i++){
			 int dataIdx = subRuleSets.get(i).getDataIdx();
			 subRuleSets.get(i).generalInitialRules(trainDatas[dataIdx], forkJoinPool ) ;
		}

    }

}