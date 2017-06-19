package gbml;

import java.util.Date;

import javax.management.JMException;

import methods.DataLoader;
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
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.partitionNum, sets.islandNum);
		}else{
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.divideNum, sets.islandNum);
		}

		//実験パラメータ出力 + ディレクトリ作成
		if(sets.crossValidationNum == 0 && sets.repeatTimes == 0){
			String settings = StaticGeneralFunc.getExperimentSettings(args);
			if(sets.calclationType == 1){
				resultDir = Output.makeDir(sets.dataName, sets.calclationType, sets.partitionNum, sets.islandNum);
			}else{
				resultDir = Output.makeDir(sets.dataName, sets.calclationType, sets.divideNum, sets.islandNum);
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
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.partitionNum, sets.seed);
		}else{
			resultDir = Output.makeDirName(sets.dataName, sets.calclationType, sets.divideNum, sets.seed);
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
		//時間計測開始
		TimeWatcher evaWatcher = new TimeWatcher();
		TimeWatcher timeWatcher = new TimeWatcher();
		timeWatcher.start();

		/************************************************************/
		//データを読み込む
		DataSetInfo trainDataInfo = null;

		if(sets.calclationType == 0){
			trainDataInfo = new DataSetInfo();
			DataLoader.inputFile(trainDataInfo, nowTrainFile);
		}
		else if(sets.calclationType == 1){
			trainDataInfo = new DataSetInfo();
			DataLoader.inputFileOneLine(trainDataInfo, nowTrainFile);
		}
		/************************************************************/
		//島モデル
		//データの分割
		DataSetInfo[] trainDataInfos = null;
		if(sets.islandNum == 1){
			trainDataInfos = new DataSetInfo[1];
			trainDataInfos[0] = trainDataInfo;
		}else if(sets.calclationType == 0){
			Divider divider = new Divider(rnd, sets.islandNum);
			trainDataInfos = divider.letsDivide(trainDataInfo);
		}

		//初期個体群の生成（複数）
		PopulationManager[] populationManagers = null;
		if(sets.islandNum == 1){
			populationManagers = new PopulationManager[1];
			populationManagers[0] = new PopulationManager(rnd, sets.objectiveNum);
			populationManagers[0].generateInitialPopulation(trainDataInfos[0], sets.populationSize, sets.forkJoinPool, sets.calclationType);
		}else{
			populationManagers = new PopulationManager[sets.islandNum];
			for(int d=0; d<sets.islandNum; d++){
				populationManagers[d] = new PopulationManager(rnd, sets.objectiveNum);
				populationManagers[d].generateInitialPopulation(trainDataInfos[d], sets.populationSize, sets.forkJoinPool, sets.calclationType);
			}
		}
		/************************************************************/

		//EMOアルゴリズム初期化
		Moead moead = new Moead(sets.populationSize, Consts.VECTOR_DIVIDE_NUM, Consts.MOEAD_ALPHA, sets.emoType,
								sets.objectiveNum, Consts.SELECTION_NEIGHBOR_NUM, Consts.UPDATE_NEIGHBOR_NUM, rnd);
		Nsga2 nsga2 = new Nsga2(sets.objectiveNum, rnd);

		//GA操作
		GaManager gaManager = new GaManager(sets.populationSize, populationManagers, nsga2, moead, rnd, sets.forkJoinPool, sets.serverList,
											sets.objectiveNum, sets.generationNum, sets.emoType, sets.islandNum, resultMaster, evaWatcher);
		gaManager.gaFrame(trainDataInfos, repeatNum, crossValidationNum);

		//時間計測終了
		timeWatcher.end();
		resultMaster.setTime( timeWatcher.getSec() );
		resultMaster.writeTime(timeWatcher.getSec(), timeWatcher.getNano(), crossValidationNum, repeatNum);
		resultMaster.writeTime(evaWatcher.getSec(), evaWatcher.getNano(), 100+crossValidationNum, 100+repeatNum);

		/***********************これ以降出力操作************************/
		//評価用DataFrame作成
		DataSetInfo testDataInfo = null;

		testDataInfo = new DataSetInfo();
		DataLoader.inputFile(testDataInfo, nowTestFile);

		//全ての島を集める
		PopulationManager allPopManager  = new PopulationManager(populationManagers);

		RuleSet bestRuleSet =
		gaManager.calcBestRuleSet(sets.objectiveNum, allPopManager,	resultMaster, trainDataInfo, testDataInfo, true);

		resultMaster.setBest(bestRuleSet);
		resultMaster.writeAllbest(bestRuleSet, crossValidationNum, repeatNum);
		resultMaster.outputRules(allPopManager, crossValidationNum, repeatNum);
		resultMaster.outputVec(allPopManager, crossValidationNum, repeatNum);

		if(sets.objectiveNum != 1){
			resultMaster.outSolution(crossValidationNum, repeatNum);
			resultMaster.resetSolution();
		}

	}

}

