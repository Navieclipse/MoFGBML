package gbml;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

import methods.CommandLineFunc;
import methods.OsSpecified;

//TODO
/*
 コマンドラインから個別に指定（今は順に指定）
 設定ファイルから指定（今はコマンドラインからのみ）
 */


public class Settings {

	public Settings(String args[]){

		//OSの種類
		if(OsSpecified.isLinux()==true || OsSpecified.isMac()==true){
			osType = Consts.UNIX;	//linux mac
			System.out.println("OS: Linux or Mac");
		}else{
			osType = Consts.WINDOWS;	//win
			System.out.println("OS: Windows");
		}

		/******************************************************************************/
	    //0: single node, 1:Apache Spark, 2:Simple Socket
	    calclationType = Integer.parseInt(args[0]);

		//コマンドライン引数が足りてるかどうか
	    if(calclationType == 0){
			CommandLineFunc.lessArgs(args, 10);
	    }else if(calclationType == 1){
			CommandLineFunc.lessArgs(args, 14);
	    }

		/******************************************************************************/
		//基本設定
	    dataName = args[1];	//データ名
	    generationNum = Integer.parseInt(args[2]);	//世代数
	    populationSize = Integer.parseInt(args[3]);	//個体数

	    objectiveNum = Integer.parseInt(args[4]);	//目的数
	    emoType = Integer.parseInt(args[5]);	//EMOアルゴリズム

	    crossValidationNum = Integer.parseInt(args[6]);	//CrossValidation
	    repeatTimes = Integer.parseInt(args[7]);	//CVの繰り返し回数

	    seed = Integer.parseInt(args[8]);	//乱数シード値

	    //1回ずつ試行を終了させる
	    isOnceExe = Boolean.parseBoolean(args[9]);

		/******************************************************************************/
		//個別設定
	    if(calclationType == 0){
	    	setSingleNode(args);
	    }else if(calclationType == 1){
	    	setSimpleSocket(args);
	    }

	}

	//0: Single node時の設定
	void setSingleNode(String args[]){
	    divideNum = Integer.parseInt(args[10]);
		forkJoinPool = new ForkJoinPool(divideNum);

		islandNum = Integer.parseInt(args[11]);
	}

	void setSimpleSocket(String args[]){

		isDistributed = Boolean.parseBoolean(args[10]);
		partitionNum = Integer.parseInt(args[11]);

		portNum = Integer.parseInt(args[12]);

		//島の分割数
		islandNum = Integer.parseInt(args[13]);

		nodeNames = new ArrayList<String>();
		for(int i=0; i<partitionNum; i++){
			nodeNames.add(args[i+14]);
		}

		serverList = new InetSocketAddress[nodeNames.size()];

		for(int i=0; i<nodeNames.size(); i++){
			serverList[i] = new InetSocketAddress(nodeNames.get(i), portNum);
		}

		//テスト用
		forkJoinPool = new ForkJoinPool(threadNum);

	}

	/******************************************************************************/
	//基本設定

	int osType;

	String dataName = "glass";	//データ名
	int generationNum = 1000; //世代数
	int populationSize = 100;	//個体数

	int objectiveNum = 2;	//目的数
	int emoType = 0;	//EMOアルゴリズム

	int crossValidationNum = 1;	//CrossValidation
	int repeatTimes = 1;	//CVの繰り返し回数

	int seed = 2017;	//乱数シード値

	//1回ずつ試行を終了させる
	boolean isOnceExe = true;

	//0: single node, 1:Apache Spark, 2:Simple Socket
	int calclationType = 0;

	/******************************************************************************/
	//0: single node時の使用コア数
	ForkJoinPool forkJoinPool = null;
	int divideNum = 1;
	int islandNum = 1;

	/******************************************************************************/
	//分散環境かどうか
	boolean isDistributed = true;

	//データの分割数
	int partitionNum = 4;

	int portNum = 50000;	//ポート番号

	int threadNum = 18;	//各ノードのスレッド数？

	ArrayList<String> nodeNames;	//ノードの名前

	InetSocketAddress[] serverList;	//サーバーリスト

	/******************************************************************************/

}
