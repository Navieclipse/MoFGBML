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

import island.SocketUnit2;
import methods.MersenneTwisterFast;
import methods.ResultMaster;
import methods.StaticGeneralFunc;
import moead.Moead;
import nsga2.Nsga2;
import socket.SocketUnit;
import time.TimeWatcher;


public class GaManager {

	Nsga2 nsga2;

	Moead moead;

	MersenneTwisterFast rnd;

	ForkJoinPool forkJoinPool;

	InetSocketAddress serverList[];

	TimeWatcher timeWatcher;

	ResultMaster resultMaster;

	int islandNum;

	int secondObjType = Consts.SECOND_OBJECTIVE_TYPE;

	int objectiveNum;
	long generationNum;

	int emoType;
	int populationSize;

	public GaManager(){}

	public GaManager(Nsga2 nsga2){
		this.nsga2 = nsga2;
	}

	public GaManager( int popSize, Nsga2 nsga2, Moead moead, MersenneTwisterFast rnd,
			ForkJoinPool forkJoinPool, InetSocketAddress serverList[], int objectiveNum, int generationNum,
			int emoType, int islandNum, ResultMaster resultMaster, TimeWatcher timeWatcher) {

		this.rnd = rnd;
		this.nsga2 = nsga2;
		this.moead = moead;

		this.forkJoinPool = forkJoinPool;

		this.serverList = serverList;

		this.resultMaster = resultMaster;
		this.timeWatcher = timeWatcher;

		this.objectiveNum = objectiveNum;
		this.generationNum = generationNum;
		this.emoType = emoType;
		this.islandNum = islandNum;
		this.populationSize = popSize;

	}

	PopulationManager[] generateInisialPop(DataSetInfo[] trainDataInfos, int[] dataIdx, int calclationType){

		/**********************************************************************************/
		PopulationManager[] popManagers = null;

		//初期個体群の生成（複数）
		if(islandNum == 1){
			popManagers = new PopulationManager[1];
			popManagers[0] = new PopulationManager(rnd, objectiveNum);
			popManagers[0].generateInitialPopulation(trainDataInfos[0], populationSize, forkJoinPool, calclationType, 0, serverList);
		}else{
			popManagers = new PopulationManager[islandNum];
			for(int d=0; d<islandNum; d++){
				popManagers[d] = new PopulationManager(rnd, objectiveNum);
				popManagers[d].generateInitialPopulation(trainDataInfos[d], populationSize, forkJoinPool, calclationType, d, serverList);
			}
		}

		//島ごとのデータ番号初期化
		for(int i=0; i<islandNum; i++){
			popManagers[i].setDataIdxtoRuleSets(dataIdx[i], true);
		}

		/**********************************************************************************/
		//初期個体群評価
		PopulationManager allPopManager  = new PopulationManager(popManagers);
		evaluationIndividual(trainDataInfos, allPopManager.currentRuleSets);

		//MOEAD初期化 （２目的のみ）
		if(emoType > 0){
			moead.ini();
			for(int d=0; d<islandNum; d++){
				moead.inidvi(popManagers[d].currentRuleSets);
			}
		}

		//NSGAII初期評価
		else if(objectiveNum != 1 && emoType == 0){
			for(int d=0; d<islandNum; d++){
				nsga2.calcRank(popManagers[d].currentRuleSets);
			}
		}

		//ミシガン型の場合
		if(populationSize == 1){
			popManagers[0].bestOfAllGen = new RuleSet( popManagers[0].currentRuleSets.get(0) );
		}

		return popManagers;
	}

	int[] calcIslandPopNums(int populationNum){
		int[] islandPopNums = new int[islandNum];
		int patNum = 0;
		while(patNum < populationSize){
			for(int i=0; i<islandNum; i++){
				if(patNum < populationSize){
					islandPopNums[i]++;
					patNum++;
				}else{
					break;
				}
			}
		}
		return islandPopNums;
	}

	public PopulationManager[] gaFrame(DataSetInfo[] trainDataInfos, int migrationItv, int calclationType, int repeat, int cv){

		//個体群の生成
		PopulationManager[] popManagers = null;
		//データ番号の生成
		int[] dataIdx = new int[islandNum];
		for(int i=0; i<islandNum; i++){
			dataIdx[i] = i;
		}
		//個体群の初期化
		if(islandNum == 1 || calclationType == 0){
			popManagers =  generateInisialPop(trainDataInfos, dataIdx, calclationType);
		}else{
			popManagers = new PopulationManager[islandNum];
			for(int d=0; d<islandNum; d++){
				popManagers[d] = new PopulationManager(rnd, objectiveNum, (int)generationNum);
			}
			//各島の個体群数
			int[] islandPopNums = calcIslandPopNums(populationSize);
			for(int i=0; i<islandNum; i++){
				popManagers[i].setIslandPopNum(islandPopNums[i]);
			}
		}

		/**********************************************************************************/
		//ここから世代開始
		boolean doLog = Consts.DO_LOG_PER_LOG;
		int nowGen = 0;
		for (int gen_i = 0; gen_i < generationNum; gen_i++) {

			if(gen_i % Consts.PER_SHOW_GENERATION_NUM == 0){
				System.out.print(".");
			}

			//途中結果保持（テストデータは無理）
			if(doLog){
				genCheck(gen_i, repeat, cv, trainDataInfos, popManagers);
			}

			//GA操作
			if(populationSize == 1){
				//ソケット対応せず
				michiganTypeGa(trainDataInfos[0], popManagers[0], gen_i);
			}
			else{
				if(emoType == 0||objectiveNum == 1){
					if(calclationType == 1 && islandNum != 1){
						nowGen = callSocketNSGA2(popManagers, dataIdx, nowGen, migrationItv);
					}else{
						nsga2Type2(trainDataInfos, popManagers, dataIdx, gen_i);
					}
				}
				else{
					for(int d=0; d<islandNum; d++){
						//ソケット対応せず
						moeadTypeGa(trainDataInfos[dataIdx[d]], popManagers[d], gen_i);
					}
				}
			}

			//移住操作＋データ交換操作
			if (islandNum != 1 && gen_i % migrationItv == 0 && nowGen == 0 || islandNum != 1 && nowGen % migrationItv == 0 && nowGen != 0 ){

				//移住操作とデータ交換
				System.out.print(",");
				migration(popManagers);
				exchangeData(dataIdx);

				//移動した島の環境で再評価
				for(int i=0; i<popManagers.length; i++){
					popManagers[i].setDataIdxtoRuleSets(dataIdx[i], true);
				}
				//サーバー1つの場合
				if(calclationType == 0){
					PopulationManager allPopManager = new PopulationManager(popManagers);
					evaluationIndividual(trainDataInfos, allPopManager.currentRuleSets);
				}
				//複数サーバーの場合は現個体群の評価をfalseに設定する
				else if(calclationType == 1){
					for(int i=0; i<popManagers.length; i++){
						popManagers[i].setIsEvaluation(false);
					}
				}

			}

			if(nowGen >= generationNum){
				break;
			}

		}
		/**********************************************************************************/

		return popManagers;
	}

	int callSocketNSGA2(PopulationManager[] popManagers, int[] dataIdx, int nowGen, int interval){

		//個体群への世代の設定
		//個体群へのインターバル（次の移住操作＆データ交換操作への）の設定
		//データ番号の付与
		for(int i=0; i<popManagers.length; i++){
			popManagers[i].setNowGen(nowGen);
			popManagers[i].setIntervalGen(interval);
			popManagers[i].setDataIdx(dataIdx[i]);
		}

		//個体群の分割
		int divideNum = serverList.length;
		ArrayList<ArrayList<PopulationManager>> subPopManager = new ArrayList<ArrayList<PopulationManager>>();
		for(int i=0; i<divideNum; i++){
			subPopManager.add( new ArrayList<PopulationManager>() );
		}

		int ruleIdx = 0;
		while(ruleIdx < popManagers.length){
			for(int i=0; i<divideNum; i++){
				if(ruleIdx < populationSize){
					subPopManager.get(i).add( popManagers[ruleIdx++] );
				}else{
					break;
				}
			}
		}

		ArrayList<PopulationManager> newPopManager = new ArrayList<PopulationManager>();
		//Socket用
		ExecutorService service = Executors.newCachedThreadPool();
		try{
			List< Callable<ArrayList<PopulationManager>> > tasks = new ArrayList< Callable<ArrayList<PopulationManager>> >();
			for(int i=0; i<divideNum; i++){
				tasks.add(  new SocketUnit2( serverList[i], subPopManager.get(i) )  );
			}
			//並列実行，同期
			List< Future<ArrayList<PopulationManager>> > futures = new ArrayList< Future<ArrayList<PopulationManager>> >();
			try{
				futures = service.invokeAll(tasks);
			}
			catch(InterruptedException e){
				System.out.println(e+": make future");
			}
			//ルールセット置き換え
			for(Future<ArrayList<PopulationManager>> future : futures){
				try{
					newPopManager.addAll( future.get() );
				}
				catch(Exception e){
					System.out.println(e+": exchanging error");
				}
			}
		}
		finally{
			if(service != null){
				service.shutdown();
			}
		}

		for(int i=0; i<popManagers.length; i++){
			popManagers[i] = newPopManager.get(i);
		}

		return nowGen + interval;
	}


	public void nsga2Socket(DataSetInfo[] trainDataInfos, ArrayList<PopulationManager> popManagers, ForkJoinPool fjp, int nowGen, int interval) {

		int popMageNum = popManagers.size();

		for(int gen = 0; gen<interval; gen++){
			//子個体生成
			for(int d=0; d<popMageNum; d++){
				geneticOperation( trainDataInfos[popManagers.get(d).getDataIdx()], popManagers.get(d), fjp);
			}

			//不要ルール削除
			for(int d=0; d<popMageNum; d++){
				deleteUnnecessaryRules(popManagers.get(d));
			}

			//データ番号付与
			for(int i=0; i<popMageNum; i++){
				popManagers.get(i).setDataIdxtoRuleSets(popManagers.get(i).getDataIdx(), false);
			}

			PopulationManager allPopManager  = new PopulationManager(popManagers);
			//個体を評価
			evaluationIndividualOnly(trainDataInfos, allPopManager.newRuleSets, fjp);

			//世代更新
			for(int d=0; d<popMageNum; d++){
				if(objectiveNum == 1){
					populationUpdateOfSingleObj(popManagers.get(d));
				}
				else{
					nsga2.populationUpdate(popManagers.get(d));
				}
			}

		}

	}

	void migration(PopulationManager[] popManagers){

		//各島のベストを計算
		RuleSet[] bests = new RuleSet[islandNum];
		for(int d=0; d<popManagers.length; d++){
			bests[d] = calcBestRuleSet(popManagers[d]);
		}

		//交換先はとりあえず一番うしろの個体（ランク最低）
		int nextRuleSetIdx = 1;
		for(int d=0; d<popManagers.length; d++){
			int ruleSetSize = popManagers[d].currentRuleSets.size() - 1;
			//次の番号の島に移動．
			popManagers[d].currentRuleSets.get(ruleSetSize).copyRuleSet(bests[nextRuleSetIdx++]);
			if(nextRuleSetIdx > popManagers.length-1){
				nextRuleSetIdx = 0;
			}
		}

	}

	void exchangeData(int[] dataIdx){

		//移住と逆向き
		int temp = dataIdx[0];
		for(int i=0; i<dataIdx.length-1; i++){
			dataIdx[i] = dataIdx[i+1];
		}
		dataIdx[dataIdx.length-1] = temp;

	}

	public void genCheck(int gen, int repeat, int cv, DataSetInfo[] trainDataInfos, PopulationManager[] popManagers){

		if( (gen+1) <=10 ||
			(gen+1) %10==0 && gen<=100||
			(gen+1) %100==0 && gen<=1000||
			(gen+1) %1000==0 && gen<=10000||
			(gen+1) %10000==0 && gen<=100000||
			(gen+1) %100000==0 && gen<=1000000||
			(gen+1) %1000000==0
		){

		//学習用データ全てで再計算はコスト的に．．．(このままだとシャローコピー）
		//PopulationManager allPopManager  = new PopulationManager(popManagers);
		//allPopManager.setDataIdxtoRuleSets( this.islandNum, true);
		//evaluationIndividual(trainDataInfos, allPopManager.currentRuleSets);

		RuleSet bestb;
		bestb = calcBestRuleSets(popManagers);
		double trat = bestb.getMissRate();
		double tstt = bestb.getTestMissRate();
		double numr = bestb.getRuleNum();
		double lengtht = bestb.getRuleLength();
		resultMaster.writeBestLog(trat, tstt, numr, lengtht, gen+1, repeat, cv);
		}

	}

	void michiganTypeGa(DataSetInfo trainDataInfo, PopulationManager popManager, int gen){

		popManager.newRuleSets.add(  new RuleSet( popManager.currentRuleSets.get(0) )  );

		popManager.michiganOperation(0, trainDataInfo, forkJoinPool);

		popManager.newRuleSets.get(0).evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool);

		poplationUpdateOfMichigan(popManager);

	}

	void poplationUpdateOfMichigan(PopulationManager popManager){

		boolean isES = Consts.IS_ES_UPDATE;
		if(isES){																	//ESか否か
			double currentRate = popManager.currentRuleSets.get(0).getMissRate();
			double newRate = popManager.newRuleSets.get(0).getMissRate();

			if(currentRate >= newRate){
				popManager.currentRuleSets.get(0).copyRuleSet( popManager.newRuleSets.get(0) );
				popManager.bestOfAllGen = new RuleSet( popManager.newRuleSets.get(0) );
			}
		}
		else{
			double bestRate = popManager.bestOfAllGen.getMissRate();
			double newRate = popManager.newRuleSets.get(0).getMissRate();

			if(bestRate >= newRate){
				popManager.bestOfAllGen = new RuleSet( popManager.newRuleSets.get(0) );
			}
			popManager.currentRuleSets.get(0).copyRuleSet( popManager.newRuleSets.get(0) );
		}

		popManager.newRuleSets.clear();
	}

	void nsga2Type2(DataSetInfo[] trainDataInfos, PopulationManager[] popManagers, int[] dataIdx, int gen_i) {

		//子個体生成
		for(int d=0; d<islandNum; d++){
			geneticOperation(trainDataInfos[dataIdx[d]], popManagers[d], forkJoinPool);
		}

		for(int d=0; d<islandNum; d++){
			deleteUnnecessaryRules(popManagers[d]);
		}

		//データ番号付与
		for(int i=0; i<popManagers.length; i++){
			popManagers[i].setDataIdxtoRuleSets(dataIdx[i], false);
		}
		//各島の個体をまとめて評価
		//timeWatcher.start();
		PopulationManager allPopManager  = new PopulationManager(popManagers);
		evaluationIndividual(trainDataInfos, allPopManager.newRuleSets);
		//timeWatcher.end();

		//世代更新
		for(int d=0; d<islandNum; d++){
			if(objectiveNum == 1){
				populationUpdateOfSingleObj(popManagers[d]);
			}
			else{
				nsga2.populationUpdate(popManagers[d]);
			}
		}

	}

	void moeadTypeGa(DataSetInfo trainDataInfo, PopulationManager popManager, int gen) {

		for(int i = 0;i < popManager.currentRuleSets.size(); i++){
			popManager.currentRuleSets.get(i).setSize(trainDataInfo.DataSize);
		}
		List<Integer> vectors = new ArrayList<Integer>();
		for (int i = 0; i < populationSize; i++) {
			vectors.add(i);
		}
		//StaticGeneralFunc.shuffle(UseVecNums, rnd);

		popManager.newRuleSets.clear();
		popManager.addNewPits(populationSize);

		for (int i = 0; i < populationSize; i++) {

			int nowVecIdx = vectors.get(i);

			popManager.crossOverAndMichiganOpe(nowVecIdx, populationSize, forkJoinPool, trainDataInfo);
			//とりあえずの処置
			//popManager.newRuleSetMutation(nowVecIdx);

			popManager.newRuleSets.get(nowVecIdx).removeRule();
			popManager.newRuleSets.get(nowVecIdx).evaluationRule(trainDataInfo, objectiveNum, secondObjType, forkJoinPool);

			moead.updateReference( popManager.newRuleSets.get(nowVecIdx) );
			moead.updateNeighbors(popManager.newRuleSets.get(nowVecIdx), popManager.currentRuleSets, nowVecIdx, emoType);
		}

	}

	void socketEvaluation(ArrayList<RuleSet> ruleSets){
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

	public void evaluationIndividual(DataSetInfo[] trainDataInfos, ArrayList<RuleSet> ruleSets){

		//ルール数でソート
		boolean isSort = Consts.IS_RULESETS_SORT;
		if(isSort){
			Collections.sort( ruleSets, new RuleSetCompByRuleNum() );
		}

		//評価（分散or単一）
		if(trainDataInfos[0].getSetting() == 1 && islandNum == 1){
			socketEvaluation(ruleSets);
		}else{
			try{
				forkJoinPool.submit( () ->
				ruleSets.parallelStream()
				.forEach( rule -> rule.evaluationRuleIsland(trainDataInfos) )
				).get();

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

	}

	public void evaluationIndividualOnly(DataSetInfo[] trainDataInfos, ArrayList<RuleSet> ruleSets, ForkJoinPool forkJoinPool){

		//ルール数でソート
		boolean isSort = Consts.IS_RULESETS_SORT;
		if(isSort){
			Collections.sort( ruleSets, new RuleSetCompByRuleNum() );
		}

		//評価（分散or単一）
		try{
			forkJoinPool.submit( () ->
			ruleSets.parallelStream()
			.forEach( rule -> rule.evaluationRuleIsland(trainDataInfos) )
			).get();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	void deleteUnnecessaryRules(PopulationManager popManager) {
		for (int i = 0; i < popManager.newRuleSets.size(); i++) {
			popManager.newRuleSets.get(i).removeRule();
		}
	}

	void crossOverAll(PopulationManager popManager) {

		int length = popManager.currentRuleSets.size();

		//子個体初期化
		popManager.newRuleSets.clear();

		for (int s = 0; s < length; s++) {
			popManager.newRuleSetsInit();
			popManager.crossOver(s, populationSize);

			//とりあえずの処置
			//popManager.newRuleSetMutation(s);
		}

	}

	void geneticOperation(DataSetInfo trainDataInfo, PopulationManager popManager,  ForkJoinPool forkJoinPool){

		int length = popManager.currentRuleSets.size();

		popManager.newRuleSets.clear();

		for (int s = 0; s < length; s++) {
			popManager.newRuleSetsInit();
			popManager.crossOverAndMichiganOpe(s, length, forkJoinPool, trainDataInfo);
			popManager.newRuleSetMutation(s, forkJoinPool, trainDataInfo);
		}

	}

	void populationUpdateOfSingleObj(PopulationManager popManager) {

		Collections.sort(popManager.currentRuleSets, new RuleSetComparator());
		Collections.sort(popManager.newRuleSets, new RuleSetComparator());

		ArrayList<RuleSet> temp = new ArrayList<RuleSet>();

		StaticGeneralFunc.mergeSort(temp, popManager.currentRuleSets, popManager.newRuleSets);

		popManager.currentRuleSets = new ArrayList<RuleSet>(temp);
		popManager.newRuleSets.clear();

	}

	double out2objeAnother(RuleSet pit, int way){
		if(way == 0){
			return (double)(pit.getRuleLength());
		}else {
			return (double)(pit.getRuleNum());
		}
	}

	//ベスト系
	/******************************************************************/
	//TODO 学習用データ全体で再計算する必要がある

	//世代途中の最良の学習用誤識別率
	RuleSet calcBestRuleSets(PopulationManager[] popManagers){

		RuleSet[] bests = new RuleSet[islandNum];
		//各島のベストを計算
		for(int d=0; d<popManagers.length; d++){
			bests[d] = calcBestRuleSet(popManagers[d]);
		}

		double minRate = bests[0].getMissRate();
		int minIdx = 0;
		for(int d=1; d<bests.length; d++){
			if( minRate > bests[d].getMissRate() ){
				minRate = bests[d].getMissRate();
				minIdx = d;
			}
		}

		return bests[minIdx];
	}

	RuleSet calcBestRuleSet(PopulationManager popManager) {

		RuleSet best;
		best = new RuleSet(popManager.currentRuleSets.get(0));
		if (objectiveNum == 1) {
			for (int i = 0; i < popManager.currentRuleSets.size(); i++) {
				if (popManager.currentRuleSets.get(i).getFitness(0) < best.getFitness(0)) {
					best = new RuleSet(popManager.currentRuleSets.get(i));
				}
				else if (popManager.currentRuleSets.get(i).getFitness(0) == best.getFitness(0)) {
					if (popManager.currentRuleSets.get(i).getMissRate() < best.getMissRate()) {
						best = new RuleSet(popManager.currentRuleSets.get(i));
					}
				}
			}
		}
		else {
			for (int i = 0; i < popManager.currentRuleSets.size(); i++) {
				if (popManager.currentRuleSets.get(i).getRank() == 0) {
					if (popManager.currentRuleSets.get(i).getMissRate() < best.getMissRate()) {
						best = new RuleSet(popManager.currentRuleSets.get(i));
					}
					else if (popManager.currentRuleSets.get(i).getMissRate() == best.getMissRate()) {
						if (popManager.currentRuleSets.get(i).getRuleNum() <= best.getRuleNum()) {
							if (popManager.currentRuleSets.get(i).getRuleLength() <= best.getRuleLength()) {
								best = new RuleSet(popManager.currentRuleSets.get(i));
							}
						}
					}
				}
			}
		}

		return best;
	}

	public RuleSet calcBestRuleSet(int objectiveNum, PopulationManager popManager, ResultMaster resultMaster, int calcType,
									 DataSetInfo[] trainDataInfos, DataSetInfo testDataInfo, boolean isTest) {

		//学習用データ誤識別率を再計算
		if( calcType == 0){
			popManager.setDataIdxtoRuleSets(trainDataInfos.length-1, true);
			evaluationIndividual(trainDataInfos, popManager.currentRuleSets);
		}

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
	/******************************************************************/

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
