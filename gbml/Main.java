package gbml;

import java.util.ArrayList;
import java.util.Date;

import javax.management.JMException;

import methods.DataLoader;
import methods.Democracy;
import methods.Divider;
import methods.MersenneTwisterFast;
import methods.Output;
import methods.ResultMaster;
import methods.StaticGeneralFunc;
import moead.Moead;
import nsga2.Nsga2;
import time.TimeWatcher;

public class Main {

	public static void main(String[] args) throws JMException {
		System.out.println("ver." + 20.0);

		/******************************************************************************/
		//基本設定
		Settings sets = new Settings(args);

		/******************************************************************************/
	    //ファジィ分割の生成
	    //StaticFuzzyFunc kk = new StaticFuzzyFunc();
	    //kk.KKkk(Consts.MAX_FUZZY_DIVIDE_NUM);
	    /******************************************************************************/
	    //基本データ出力と実行
		System.out.println("Processors:" + Runtime.getRuntime().availableProcessors() + " ");

		System.out.print("args: ");
		for(int i=0; i<args.length; i++){
			System.out.print(args[i] + " ");
		}
		System.out.println();
		System.out.println();

	    Date date = new Date();
		System.out.print("START: ");
		System.out.println(date);

		//一回ごとにjarファイルを呼び出すか否か
		if(sets.isOnceExe){
			onceExection(sets, args);
		}else{
			repeatExection(sets, args);
		}
		/******************************************************************************/
	}

	static public void onceExection(Settings sets, String[] args){

		//読み込みファイル名とディレクトリ名
		String traFile = Output.makeFileNameOne(sets.dataName, sets.crossValidationNum, sets.repeatTimes, true);
		String tstFile = Output.makeFileNameOne(sets.dataName,sets.crossValidationNum, sets.repeatTimes, false);
		String resultDir;
		if(sets.calclationType == 1){
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.serverNum, sets.islandNum, sets.preDivNum);
		}else{
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.parallelCores, sets.islandNum, sets.preDivNum);
		}

		//実験パラメータ出力 + ディレクトリ作成
		if(sets.crossValidationNum == 0 && sets.repeatTimes == 0){
			String settings = StaticGeneralFunc.getExperimentSettings(args);
			if(sets.calclationType == 1){
				resultDir = Output.makeDir(sets.dataName, sets.calclationType, sets.serverNum, sets.islandNum, sets.preDivNum);
			}else{
				resultDir = Output.makeDir(sets.dataName, sets.calclationType, sets.parallelCores, sets.islandNum, sets.preDivNum);
			}
			Output.makeDirRule(resultDir);
			Output.writeSetting(sets.dataName, resultDir, settings);
	    }

		System.out.println(resultDir);

		//出力専用クラス
		ResultMaster resultMaster = new ResultMaster(resultDir, sets.osType);
		/************************************************************/
		//繰り返しなし
		int repeat_i = sets.repeatTimes;
		int cv_i = sets.crossValidationNum;

		MersenneTwisterFast rnd = new MersenneTwisterFast(sets.seed);

		System.out.print(repeat_i + " " + cv_i);
		startExeperiment(sets, traFile, tstFile, rnd, resultMaster, cv_i, repeat_i, traFile, tstFile);
		System.out.println();

		/************************************************************/
		//出力
		resultMaster.writeAveTime();
		resultMaster.writeBestAve();
		Date end = new Date();
		System.out.println("END: " + end);

	}

	static public void repeatExection(Settings sets, String[] args){

		//読み込みファイル名
		String traFiles[][] = new String[sets.repeatTimes][sets.crossValidationNum];
	    String tstFiles[][] = new String[sets.repeatTimes][sets.crossValidationNum];
	    Output.makeFileName(sets.dataName, traFiles, tstFiles);

	    //データディレクトリ作成
		String resultDir;
		if(sets.calclationType == 1){
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.serverNum, sets.preDivNum, sets.seed);
		}else{
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.parallelCores, sets.preDivNum, sets.seed);
		}
	    Output.makeDirRule(resultDir);

	    //実験パラメータ出力
		String settings = StaticGeneralFunc.getExperimentSettings(args);
	    Output.writeSetting(sets.dataName, resultDir, settings);

	    //出力専用クラス
	    ResultMaster resultMaster = new ResultMaster(resultDir, sets.osType);

	    /************************************************************/
	    //繰り返し
		MersenneTwisterFast rnd = new MersenneTwisterFast(sets.seed);

		for(int repeat_i=0; repeat_i<sets.repeatTimes; repeat_i++){
			for(int cv_i=0; cv_i<sets.crossValidationNum; cv_i++){
				System.out.print(repeat_i + " " + cv_i);

				startExeperiment(sets, traFiles[repeat_i][cv_i], tstFiles[repeat_i][cv_i], rnd, resultMaster,
									cv_i, repeat_i, traFiles[repeat_i][cv_i], tstFiles[repeat_i][cv_i]);

				System.out.println();
			}
		}
		/************************************************************/
		//出力
		resultMaster.writeAveTime();
		resultMaster.writeBestAve();
		Date end = new Date();
		System.out.println("END: " + end);

	}

	static public void startExeperiment(Settings sets, String traFile, String testFile, MersenneTwisterFast rnd,
			ResultMaster resultMaster, int crossValidationNum, int repeatNum, String nowTrainFile, String nowTestFile){

		/************************************************************/
		//学習用データ全体を読み込む
		DataSetInfo originalTrainDataInfo = null;
		if(sets.calclationType == 0){
			originalTrainDataInfo = new DataSetInfo();
			DataLoader.inputFile(originalTrainDataInfo, nowTrainFile);
		}
		else if(sets.calclationType == 1){ //ヘッダのみ読み込み
			originalTrainDataInfo = new DataSetInfo();
			DataLoader.inputFileOneLine(originalTrainDataInfo, nowTrainFile, sets.dirLocasion);
		}

		/************************************************************/
        //データのサンプリング
        //データをクラスごとに均等に分けて一部だけ取り出す．
		DataSetInfo trainDataInfo = new DataSetInfo();
		if(sets.calclationType == 0){
			Divider preDivider = new Divider(sets.preDivNum);
			trainDataInfo = preDivider.letsDivide(originalTrainDataInfo, 1, sets.serverList)[0];
		}else if(sets.calclationType == 1){
			trainDataInfo = originalTrainDataInfo;
			trainDataInfo.setDataSize( (int)Math.ceil(originalTrainDataInfo.getDataSize() / sets.preDivNum) );
		}

		/************************************************************/
		//島モデル
		//データの分割
		DataSetInfo[] trainDataInfos = null;
		if(sets.calclationType == 0){
			if(sets.islandNum == 1){
				trainDataInfos = new DataSetInfo[1];
				//ランダムなパターンで
				boolean is_random = Consts.IS_RANDOM_PATTERN_SELECT;
				DataSetInfo[] randomTrains = null;
				if(is_random){
					Divider divider = new Divider(rnd, sets.migrationItv);
					randomTrains = divider.letsDivide(trainDataInfo, sets.calclationType, sets.serverList);
					trainDataInfos[0] = randomTrains[0];
				}
				else{
					trainDataInfos[0] = trainDataInfo;
				}
				trainDataInfos[0].setSetting(sets.calclationType, sets.serverList);
			}
			else {
				Divider divider = new Divider(rnd, sets.islandNum);
				trainDataInfos = divider.letsDivide(trainDataInfo, sets.calclationType, sets.serverList);
			}
		}

		//評価だけ複数サーバで並列用
/*		if(sets.calclationType == 1){
			if(sets.islandNum == 1){
				trainDataInfos = new DataSetInfo[1];
				trainDataInfos[0] = trainDataInfo;
				trainDataInfos[0].setSetting(sets.calclationType, sets.serverList);
			}
		}
*/
		/************************************************************/
		//時間計測開始
		TimeWatcher evaWatcher = new TimeWatcher();
		TimeWatcher timeWatcher = new TimeWatcher();
		timeWatcher.start();

		/************************************************************/
		//EMOアルゴリズム初期化
		//全体用Moead
		Moead moead = new Moead(sets.populationSize, Consts.VECTOR_DIVIDE_NUM, Consts.MOEAD_ALPHA, sets.emoType,
								sets.objectiveNum, Consts.SELECTION_NEIGHBOR_NUM, Consts.UPDATE_NEIGHBOR_NUM, trainDataInfo.getDataSize(), rnd);
		ArrayList<Moead> moeads = new ArrayList<Moead>();
		//島用Moead
		if(sets.islandNum != 1){
			if(sets.objectiveNum == 2){	//２目的用
				int realIslandNum = sets.islandNum;
				boolean isNotEqual = Consts.IS_NOT_EQUAL_DIVIDE_NUM;
				if(isNotEqual){
					realIslandNum = sets.serverNum;
				}
				for(int d=0; d<realIslandNum; d++){
					moeads.add( new Moead(0, 0, Consts.MOEAD_ALPHA, sets.emoType,
						sets.objectiveNum, Consts.SELECTION_NEIGHBOR_NUM, Consts.UPDATE_NEIGHBOR_NUM, 0, rnd) );
				}
			}
		}
		moeads.add(moead);

		//NSGA-II
		Nsga2 nsga2 = new Nsga2(sets.objectiveNum, rnd);

		//GA操作
		GaManager gaManager = new GaManager(sets.populationSize, nsga2, moeads, rnd, sets.forkJoinPool, sets.serverList, sets.serverNum,
											sets.objectiveNum, sets.generationNum, sets.emoType, sets.islandNum, resultMaster, evaWatcher, sets.dataName);
		//GA実行
		PopulationManager[] populationManagers =  gaManager.gaFrame(trainDataInfos, sets.migrationItv, sets.calclationType, repeatNum, crossValidationNum);

		//時間計測終了
		timeWatcher.end();
		resultMaster.setTime( timeWatcher.getSec() );
		resultMaster.writeTime(timeWatcher.getSec(), timeWatcher.getNano(), crossValidationNum, repeatNum);
		resultMaster.writeTime(evaWatcher.getSec(), evaWatcher.getNano(), 100+crossValidationNum, 100+repeatNum);

		/***********************これ以降出力操作************************/
		//評価用DataFrame作成
		DataSetInfo testDataInfo = null;

		testDataInfo = new DataSetInfo();
		if(sets.calclationType == 1){
			nowTestFile = sets.dirLocasion + nowTestFile;
		}
		DataLoader.inputFile(testDataInfo, nowTestFile);

		//多数決にして評価用誤識別率を求める
		boolean isDemo = Consts.IS_DEMOCRACY;
		if(isDemo && sets.islandNum != 1){
			//全ての島のベストを求めて配列にまとめる
			RuleSet[] bests = gaManager.calcBestRuleSets(populationManagers);
			//aa
			Democracy demo = new Democracy(bests, testDataInfo.getCnum());
			double testErrRate = (demo.calcDemocraticRate(testDataInfo, sets.forkJoinPool) / testDataInfo.DataSize) * 100.0;
			resultMaster.writeDemoTestRate(testErrRate);
		}

		//全ての識別器のそれぞれの非劣解の結果の出力
		//全ての島を集める
		PopulationManager allPopManager  = new PopulationManager(populationManagers);
		allPopManager.setDataIdxtoRuleSets( sets.islandNum, true);

		//TODO
		//ベストを各島でとって，多数決にしてテストデータを識別する
		RuleSet bestRuleSet =
		gaManager.calcBestRuleSet(sets.objectiveNum, nsga2, allPopManager,	resultMaster,
														sets.calclationType, trainDataInfos, testDataInfo, true);

		//書き込み
		resultMaster.setBest(bestRuleSet);
		resultMaster.writeAllbest(bestRuleSet, crossValidationNum, repeatNum);
		resultMaster.outputRules(allPopManager, crossValidationNum, repeatNum);
		resultMaster.outputVec(allPopManager, crossValidationNum, repeatNum);

		//単一目的の場合の書き込み
		if(sets.objectiveNum != 1){
			resultMaster.outSolution(crossValidationNum, repeatNum);
			resultMaster.resetSolution();
		}
		/************************************************************/
	}

}

