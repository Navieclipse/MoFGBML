package gbml;


import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import methods.MersenneTwisterFast;
import methods.ResultMaster;
import methods.StaticGeneralFunc;
import moead.Moead;
import nsga2.Nsga2;
import socket.SocketUnit;
import time.TimeWatcher;


public class GaManager {

	PopulationManager populationManager;

	Nsga2 nsga2;

	Moead moead;

	MersenneTwisterFast rnd;

	ForkJoinPool forkJoinPool;

	InetSocketAddress serverList[];

	TimeWatcher timeWatcher;

	int secondObjType = Consts.SECOND_OBJECTIVE_TYPE;

	ResultMaster resultMaster;

	int objectiveNum;
	long generationNum;

	int emoType;
	int populationSize;

	public GaManager( int popSize, PopulationManager populationManager, Nsga2 nsga2, Moead moead, MersenneTwisterFast rnd,
			ForkJoinPool forkJoinPool, InetSocketAddress serverList[], int objectiveNum, int generationNum,
			int emoType, ResultMaster resultMaster, TimeWatcher timeWatcher) {

		this.populationManager = populationManager;
		this.nsga2 = nsga2;
		this.moead = moead;

		this.forkJoinPool = forkJoinPool;

		this.serverList = serverList;

		this.resultMaster = resultMaster;
		this.timeWatcher = timeWatcher;

		this.objectiveNum = objectiveNum;
		this.generationNum = generationNum;
		this.emoType = emoType;
		this.populationSize = popSize;

	}

	public void gaFrame(DataSetInfo trainDataInfo, int repeat, int cv){

		//初期個体群評価
		parentEvaluation(trainDataInfo, populationManager);

		//MOEAD初期化 （２目的のみ）
		if(emoType > 0){
			moead.ini();
			moead.inidvi(populationManager.currentRuleSets);
		}
		else if(objectiveNum != 1 && emoType == 0){
			nsga2.calcRank(populationManager.currentRuleSets);
		}

		if(populationSize == 1){
			populationManager.bestOfAllGen = new RuleSet( populationManager.currentRuleSets.get(0) );
		}

		boolean doLog = Consts.DO_LOG_PER_LOG;
		for (int gen_i = 0; gen_i < generationNum; gen_i++) {

			if(gen_i % Consts.PER_SHOW_GENERATION_NUM == 0){
				System.out.print(".");
			}

			if(doLog){		//途中結果保持（テストデータは無理）
				genCheck(gen_i, repeat, cv);
			}

			//GA操作
			if(populationSize == 1){
				michiganTypeGa(trainDataInfo, gen_i);
			}
			else{
				if(emoType == 0||objectiveNum == 1){
					nsga2TypeGa(trainDataInfo, gen_i);

				}else{
					moeadTypeGa(trainDataInfo, gen_i);
				}
			}
		}

	}

	public void genCheck(int gen, int repeat, int cv){
		if( (gen+1) <=10 ||
			(gen+1) %10==0 && gen<=100||
			(gen+1) %100==0 && gen<=1000||
			(gen+1) %1000==0 && gen<=10000||
			(gen+1) %10000==0 && gen<=100000||
			(gen+1) %100000==0 && gen<=1000000||
			(gen+1) %1000000==0
		){

		RuleSet bestb;
		bestb = calcBestRuleSet();
		double trat = bestb.getMissRate();
		double tstt = bestb.getTestMissRate();
		double numr = bestb.getRuleNum();
		double lengtht = bestb.getRuleLength();
		resultMaster.writeBestLog(trat, tstt, numr, lengtht, gen+1, repeat, cv);

		}
	}

	void michiganTypeGa(DataSetInfo trainDataInfo, int gen){

		populationManager.newRuleSets.add(  new RuleSet( populationManager.currentRuleSets.get(0) )  );

		populationManager.michiganOperation(0, trainDataInfo, forkJoinPool);

		populationManager.newRuleSets.get(0).evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool);

		poplationUpdateOfMichigan();

	}

	void poplationUpdateOfMichigan(){

		boolean isES = Consts.IS_ES_UPDATE;
		if(isES){																	//ESか否か
			double currentRate = populationManager.currentRuleSets.get(0).getMissRate();
			double newRate = populationManager.newRuleSets.get(0).getMissRate();

			if(currentRate >= newRate){
				populationManager.currentRuleSets.get(0).copyRuleSet( populationManager.newRuleSets.get(0) );
				populationManager.bestOfAllGen = new RuleSet( populationManager.newRuleSets.get(0) );
			}
		}
		else{
			double bestRate = populationManager.bestOfAllGen.getMissRate();
			double newRate = populationManager.newRuleSets.get(0).getMissRate();

			if(bestRate >= newRate){
				populationManager.bestOfAllGen = new RuleSet( populationManager.newRuleSets.get(0) );
			}
			populationManager.currentRuleSets.get(0).copyRuleSet( populationManager.newRuleSets.get(0) );
		}

		populationManager.newRuleSets.clear();
	}

	void nsga2TypeGa(DataSetInfo trainDataInfo, int gen) {

		geneticOperation(trainDataInfo);

		deleteUnnecessaryRules();

		timeWatcher.start();
		offspringEvaluation(trainDataInfo, populationManager);
		timeWatcher.end();

		if(objectiveNum == 1){
			populationUpdateOfSingleObj();
		}
		else{
			nsga2.populationUpdate(populationManager);
		}

	}

	void moeadTypeGa(DataSetInfo trainDataInfo, int gen) {

		for(int i = 0;i < populationManager.currentRuleSets.size(); i++){
			populationManager.currentRuleSets.get(i).setSize(trainDataInfo.DataSize);
		}
		List<Integer> vectors = new ArrayList<Integer>();
		for (int i = 0; i < populationSize; i++) {
			vectors.add(i);
		}
		//StaticGeneralFunc.shuffle(UseVecNums, rnd);

		populationManager.newRuleSets.clear();
		populationManager.addNewPits(populationSize);

		for (int i = 0; i < populationSize; i++) {

			int nowVecIdx = vectors.get(i);

			populationManager.crossOverAndMichiganOpe(nowVecIdx, populationSize, forkJoinPool, trainDataInfo);
			populationManager.newRuleSetMutation(nowVecIdx);

			populationManager.newRuleSets.get(nowVecIdx).removeRule();
			populationManager.newRuleSets.get(nowVecIdx).evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool);

			moead.updateReference( populationManager.newRuleSets.get(nowVecIdx) );
			moead.updateNeighbors(populationManager.newRuleSets.get(nowVecIdx), populationManager.currentRuleSets, nowVecIdx, emoType);
		}

	}

	void socketEvaluation(DataSetInfo dataSetInfo, ArrayList<RuleSet> ruleSets){

			//個体群のソート
			boolean isSort = Consts.IS_RULESETS_SORT;
			if(isSort){
				Collections.sort( ruleSets, new RuleSetCompByRuleNum() );
			}
	        //個体群の分割
	        int divideNum = serverList.length;
	        ArrayList<ArrayList<RuleSet>> subRuleSets = new ArrayList<ArrayList<RuleSet>>();
	        for(int i=0; i<divideNum; i++){
	        	subRuleSets.add( new ArrayList<RuleSet>() );
	        }
	        int ruleIdx = 0;
	        while(ruleIdx < populationSize){
				for(int i=0; i<divideNum; i++){
					if(ruleIdx < populationSize){
						subRuleSets.get(i).add( ruleSets.get(ruleIdx++) );
					}else{
						break;
					}
				}
	        }

			//Socket用
			ExecutorService service = Executors.newCachedThreadPool();
			try{
				List< Callable<ArrayList<RuleSet>> > tasks = new ArrayList< Callable<ArrayList<RuleSet>> >();
				for(int i=0; i<divideNum; i++){
					tasks.add(  new SocketUnit( serverList[i], subRuleSets.get(i) )  );
				}
				//並列実行，同期
				List< Future<ArrayList<RuleSet>> > futures = null;
				try{
					futures = service.invokeAll(tasks);
				}
				catch(InterruptedException e){
					System.out.println(e+": make future");
				}
				//ルールセット置き換え
				ruleSets.clear();
				for(Future<ArrayList<RuleSet>> future : futures){
					try{
						ruleSets.addAll( future.get() );
					}
					catch(Exception e){
						System.out.println(e+": exchanging");
					}
				}
			}
			finally{
				if(service != null){
					service.shutdown();
				}
			}

	}

	void parentEvaluation(DataSetInfo trainDataInfo, PopulationManager popManager){

		boolean isRulePara = Consts.IS_RULE_PARALLEL;

		if(serverList != null){
			socketEvaluation(trainDataInfo, popManager.currentRuleSets);
		}
		else if(isRulePara){

			try{
				forkJoinPool.submit( () ->
				popManager.currentRuleSets.parallelStream()
				.forEach( rule -> rule.evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool) )
				).get();

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}

		}
		else{
			popManager.currentRuleSets.stream()
			.forEach( rule -> rule.evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool) );
		}

	}

	void offspringEvaluation(DataSetInfo trainDataInfo, PopulationManager popManager){

		boolean isRulePara = Consts.IS_RULE_PARALLEL;

		if(serverList != null){
			socketEvaluation(trainDataInfo, popManager.newRuleSets);
		}
		else if(isRulePara){
			try{
				forkJoinPool.submit( () ->
				popManager.newRuleSets.parallelStream()
				.forEach( rule -> rule.evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool) )
				).get();

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		else{
			popManager.newRuleSets.stream()
			.forEach( rule -> rule.evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool) );
		}

	}

	void deleteUnnecessaryRules() {
		for (int i = 0; i < populationManager.newRuleSets.size(); i++) {
			populationManager.newRuleSets.get(i).removeRule();
		}
	}

	void crossOverAll() {

		int length = populationManager.currentRuleSets.size();

		//子個体初期化
		populationManager.newRuleSets.clear();

		for (int s = 0; s < length; s++) {
			populationManager.newRuleSetsInit();
			populationManager.crossOver(s, populationSize);
			populationManager.newRuleSetMutation(s);
		}

	}

	void geneticOperation(DataSetInfo trainDataInfo){

		int length = populationManager.currentRuleSets.size();
		populationManager.newRuleSets.clear();

		for (int s = 0; s < length; s++) {
			populationManager.newRuleSetsInit();
			populationManager.crossOverAndMichiganOpe(s, populationSize, forkJoinPool, trainDataInfo);
			populationManager.newRuleSetMutation(s);
		}

	}

	void populationUpdateOfSingleObj() {

		Collections.sort(populationManager.currentRuleSets, new RuleSetComparator());
		Collections.sort(populationManager.newRuleSets, new RuleSetComparator());

		ArrayList<RuleSet> temp = new ArrayList<RuleSet>();

		StaticGeneralFunc.mergeSort(temp, populationManager.currentRuleSets, populationManager.newRuleSets);

		populationManager.currentRuleSets = new ArrayList<RuleSet>(temp);
		populationManager.newRuleSets.clear();

	}

	double out2objeAnother(RuleSet pit, int way){
		if(way == 0){
			return (double)(pit.getRuleLength());
		}else {
			return (double)(pit.getRuleNum());
		}
	}

	//ベスト系
	RuleSet calcBestRuleSet() {

		RuleSet best;
		best = new RuleSet(populationManager.currentRuleSets.get(0));
		if (objectiveNum == 1) {
			for (int i = 0; i < populationManager.currentRuleSets.size(); i++) {
				if (populationManager.currentRuleSets.get(i).getFitness(0) < best.getFitness(0)) {
					best = new RuleSet(populationManager.currentRuleSets.get(i));
				}
				else if (populationManager.currentRuleSets.get(i).getFitness(0) == best.getFitness(0)) {
					if (populationManager.currentRuleSets.get(i).getMissRate() < best.getMissRate()) {
						best = new RuleSet(populationManager.currentRuleSets.get(i));
					}
				}
			}
		}

		else {

			for (int i = 0; i < populationManager.currentRuleSets.size(); i++) {
				if (populationManager.currentRuleSets.get(i).getRank() == 0) {
					if (populationManager.currentRuleSets.get(i).getMissRate() < best.getMissRate()) {
						best = new RuleSet(populationManager.currentRuleSets.get(i));
					}
					else if (populationManager.currentRuleSets.get(i).getMissRate() == best.getMissRate()) {
						if (populationManager.currentRuleSets.get(i).getRuleNum() <= best.getRuleNum()) {
							if (populationManager.currentRuleSets.get(i).getRuleLength() <= best.getRuleLength()) {
								best = new RuleSet(populationManager.currentRuleSets.get(i));
							}
						}
					}
				}
			}
		}
		//途中で評価用の結果出すのはちょっとしんどい
		//double accTest = (double) best.CalcAccuracyPal(tstData, Dpop) / tstData.DataSize;
		//best.SetTestMissRate((1 - accTest) * 100);

		return best;

	}

	public RuleSet calcBestRuleSet(int objectiveNum, PopulationManager popManager, ResultMaster resultMaster,
									DataSetInfo testDataInfo, boolean isTest) {

		RuleSet bestRuleset;

		for (int i = 0; i < popManager.currentRuleSets.size(); i++) {

			double fitness = 0;

			if(popManager.currentRuleSets.get(i).getRuleNum() != 0){

				popManager.currentRuleSets.get(i).setNumAndLength();

				if(isTest){
					double acc = (double) popManager.currentRuleSets.get(i).calcMissPatterns(testDataInfo, forkJoinPool);
					popManager.currentRuleSets.get(i).setTestMissRate( ( acc / (double)testDataInfo.DataSize ) * 100.0 );
				}

				popManager.currentRuleSets.get(i).setNumAndLength();

				if (objectiveNum == 1) {
					fitness = Consts.W1 * popManager.currentRuleSets.get(i).getMissRate()
							+ Consts.W2 * popManager.currentRuleSets.get(i).getRuleNum()
							+ Consts.W3 * popManager.currentRuleSets.get(i).getRuleLength();
					popManager.currentRuleSets.get(i).setFitness(fitness, 0);
				} else if (objectiveNum == 2) {
					popManager.currentRuleSets.get(i).setFitness(popManager.currentRuleSets.get(i).getMissRate(), 0);
					popManager.currentRuleSets.get(i).setFitness(popManager.currentRuleSets.get(i).out2obje(secondObjType), 1);
				} else if (objectiveNum == 3) {
					popManager.currentRuleSets.get(i).setFitness(popManager.currentRuleSets.get(i).getMissRate(), 0);
					popManager.currentRuleSets.get(i).setFitness(popManager.currentRuleSets.get(i).getRuleNum(), 1);
					popManager.currentRuleSets.get(i).setFitness(popManager.currentRuleSets.get(i).getRuleLength(), 2);
				} else {
					System.out.println("not be difined");
				}
			}

			else {
				for (int o = 0; o < objectiveNum; o++) {
					fitness = 100000;
					popManager.currentRuleSets.get(i).setFitness(fitness, o);
				}
			}
		}


		bestRuleset = new RuleSet(popManager.currentRuleSets.get(0));
		if (objectiveNum == 1) {
			for (int i = 0; i < popManager.currentRuleSets.size(); i++) {

				if (popManager.currentRuleSets.get(i).getFitness(0) < bestRuleset.getFitness(0)) {
					bestRuleset = new RuleSet(popManager.currentRuleSets.get(i));
				}

				else if (popManager.currentRuleSets.get(i).getFitness(0) == bestRuleset.getFitness(0)) {
					if (popManager.currentRuleSets.get(i).getMissRate() < bestRuleset.getMissRate()) {
						bestRuleset = new RuleSet(popManager.currentRuleSets.get(i));
					}
				}

			}
		}

		else {

			for (int i = 0; i < popManager.currentRuleSets.size(); i++) {

				if (popManager.currentRuleSets.get(i).getRank() == 0) {
					int claNum = popManager.currentRuleSets.get(i).mulCla();	//そのルール集合の識別するクラス数

					resultMaster.setSolution(popManager.currentRuleSets.get(i).out2obje(secondObjType),
									popManager.currentRuleSets.get(i).getFitness(0),
									popManager.currentRuleSets.get(i).getTestMissRate(),
									out2objeAnother(popManager.currentRuleSets.get(i), secondObjType),
									claNum);

					if (popManager.currentRuleSets.get(i).getFitness(0) < bestRuleset.getFitness(0)) {
						bestRuleset = new RuleSet(popManager.currentRuleSets.get(i));
					}
					else if (popManager.currentRuleSets.get(i).getFitness(0) == bestRuleset.getFitness(0)) {
						if (popManager.currentRuleSets.get(i).getFitness(1) <= bestRuleset.getFitness(1)) {
							if (popManager.currentRuleSets.get(i).getRuleLength() <= bestRuleset.getRuleLength()) {
								bestRuleset = new RuleSet(popManager.currentRuleSets.get(i));
							}
						}
					}

				}
			}

		}
		if(isTest){
			double accTest = (double) bestRuleset.calcMissPatterns(testDataInfo, forkJoinPool) / testDataInfo.DataSize;
			bestRuleset.setTestMissRate(accTest * 100);
		}

		bestRuleset.setNumAndLength();

		return bestRuleset;

	}

	void RandomShuffle(ArrayList<RuleSet> rules) {
		for (int i = rules.size() - 1; i > 0; i--) {
			int t = rnd.nextInt(i + 1);

			RuleSet tmp = rules.get(i);
			rules.get(i).copyRuleSet(rules.get(t));
			rules.get(t).copyRuleSet(tmp);

		}
	}

	public class RuleSetComparator implements Comparator<RuleSet> {
	    //比較メソッド（データクラスを比較して-1, 0, 1を返すように記述する）
	    public int compare(RuleSet a, RuleSet b) {
	        double no1 = a.getFitness(0);
	        double no2 = b.getFitness(0);

	        //昇順でソート
	        if (no1 > no2) {
	            return 1;

	        } else if (no1 == no2) {
	            return 0;

	        } else {
	            return -1;
	        }
	    }

	}

	public class RuleSetCompByRuleNum implements Comparator<RuleSet> {

	    public int compare(RuleSet a, RuleSet b) {
	        int no1 = a.getRuleNum();
	        int no2 = b.getRuleNum();

	        //降順でソート
	        if (no1 < no2) {
	            return 1;

	        } else if (no1 == no2) {
	            return 0;

	        } else {
	            return -1;
	        }
	    }

	}

}
