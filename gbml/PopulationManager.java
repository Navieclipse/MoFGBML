package gbml;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import methods.MersenneTwisterFast;
import methods.StaticGeneralFunc;
import moead.Moead;
import socket.SocketUnit;


public class PopulationManager implements Serializable{

	//コンストラクタ
	PopulationManager(){}

	public PopulationManager(MersenneTwisterFast rnd, int objectives){

		this.uniqueRnd = new MersenneTwisterFast( rnd.nextInt() );
		this.objectiveNum = objectives;

	}

	public PopulationManager(MersenneTwisterFast rnd, int objectives, int generationNum){

		this.uniqueRnd = new MersenneTwisterFast( rnd.nextInt() );
		this.objectiveNum = objectives;
		this.terminationGen = generationNum;

	}

	public PopulationManager(PopulationManager[] popManagers){

		this.objectiveNum = popManagers[0].objectiveNum;
		this.osType = popManagers[0].osType;
		this.attributeNum = popManagers[0].attributeNum;
		this.classNum = popManagers[0].classNum;
		this.objectiveNum = popManagers[0].objectiveNum;
		this.terminationGen = popManagers[0].terminationGen;

		currentRuleSets.clear();
		for(int d=0; d<popManagers.length; d++){
			currentRuleSets.addAll(popManagers[d].currentRuleSets);
		}

		newRuleSets.clear();
		for(int d=0; d<popManagers.length; d++){
			newRuleSets.addAll(popManagers[d].newRuleSets);
		}

		this.bestOfAllGen = popManagers[0].bestOfAllGen;
	}

	public PopulationManager(ArrayList<PopulationManager> popManagers){

		this.objectiveNum = popManagers.get(0).objectiveNum;
		this.osType = popManagers.get(0).osType;
		this.attributeNum = popManagers.get(0).attributeNum;
		this.classNum = popManagers.get(0).classNum;
		this.objectiveNum = popManagers.get(0).objectiveNum;
		this.terminationGen = popManagers.get(0).terminationGen;

		currentRuleSets.clear();
		for(int d=0; d<popManagers.size(); d++){
			currentRuleSets.addAll(popManagers.get(d).currentRuleSets);
		}

		newRuleSets.clear();
		for(int d=0; d<popManagers.size(); d++){
			newRuleSets.addAll(popManagers.get(d).newRuleSets);
		}

		this.bestOfAllGen = popManagers.get(0).bestOfAllGen;
	}

	/******************************************************************************/
	//ランダム
	MersenneTwisterFast uniqueRnd;

	//個体群
	public ArrayList<RuleSet> currentRuleSets = new ArrayList<RuleSet>();

	public ArrayList<RuleSet> newRuleSets = new ArrayList<RuleSet>();

	public ArrayList<RuleSet> margeRuleSets = new ArrayList<RuleSet>();

	//ミシガン型GBML用
	public RuleSet bestOfAllGen;

	//Island model用
	int nowGen = 0;
	int intervalGen;
	int terminationGen;
	boolean isEvaluation = false;
	int dataIdx;
	int islandPopNum;

	//読み取った値
	int generationNum;
	int osType;

	int attributeNum;
	int classNum;
	int trainDataSize;
	int testDataSize;

	int objectiveNum;

	/******************************************************************************/

	public void setTerminationGen(int terminationGen){
		this.terminationGen = terminationGen;
	}

	public int getTerminatinGen(){
		return terminationGen;
	}

	public int getObjectiveNum(){
		return objectiveNum;
	}

	public MersenneTwisterFast getRnd(){
		return uniqueRnd;
	}

	public void setIslandPopNum(int popNum){
		this.islandPopNum = popNum;
	}

	public int getIslandPopNum(){
		return this.islandPopNum;
	}

	public void setDataIdx(int dataIdx){
		this.dataIdx = dataIdx;
	}

	public int getDataIdx(){
		return this.dataIdx;
	}

	public void setIsEvaluation(boolean isEva){
		this.isEvaluation = isEva;
	}

	public boolean getIsEvalutation(){
		return this.isEvaluation;
	}

	public void setNowGen(int nowGen){
		this.nowGen = nowGen;
	}

	public int getNowGen(){
		return this.nowGen;
	}

	public void setIntervalGen(int intervalGen){
		this.intervalGen = intervalGen;
	}

	public int getIntervalGen(){
		return this.intervalGen;
	}

	public void setDataIdxtoRuleSets(int dataIdx, boolean isParent){

		if(isParent){
			currentRuleSets.stream().forEach( r->r.setDataIdx(dataIdx) );
		}else{
			newRuleSets.stream().forEach( r->r.setDataIdx(dataIdx) );
		}

	}

	void generateInitialPopSocket(ArrayList<RuleSet> ruleSets, InetSocketAddress[] serverList){
		//個体群の分割
		int divideNum = serverList.length;

		//受信元での操作メソッドを１にする.
		for(int i=0; i<ruleSets.size(); i++){
			ruleSets.get(i).setSocketMethodNum(1);
		}

		ArrayList<ArrayList<RuleSet>> subRuleSets = new ArrayList<ArrayList<RuleSet>>();
		for(int i=0; i<divideNum; i++){
			subRuleSets.add( new ArrayList<RuleSet>() );
		}
		int ruleIdx = 0;
		while(ruleIdx < ruleSets.size()){
			for(int i=0; i<divideNum; i++){
				if(ruleIdx < ruleSets.size()){
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

		//操作メソッドを元に戻す.
		for(int i=0; i<ruleSets.size(); i++){
			ruleSets.get(i).setSocketMethodNum(0);
		}
	}

	public void generateInitialPopulation(DataSetInfo dataSetInfo, int populationSize, ForkJoinPool forkJoinPool,
														int calclationType, int dataIdx, InetSocketAddress[] serverList){

		attributeNum = dataSetInfo.getNdim();
		classNum = dataSetInfo.getCnum();
		trainDataSize = dataSetInfo.getDataSize();

		for(int i=0; i<populationSize; i++){
			currentRuleSets.add( new RuleSet( uniqueRnd, attributeNum, classNum, trainDataSize, testDataSize, objectiveNum) );
		}

		if(calclationType == 0){
			for(int i=0; i<populationSize; i++){
				currentRuleSets.get(i).generalInitialRules(dataSetInfo, forkJoinPool);
			}
		}else if(calclationType == 1){
			for(int i=0; i<currentRuleSets.size(); i++){
				currentRuleSets.get(i).setDataIdx(dataIdx);
			}
			generateInitialPopSocket(currentRuleSets, serverList);
		}

	}

	public void generateInitialPopulationOnly(DataSetInfo dataSetInfo, int populationSize, ForkJoinPool forkJoinPool){

		attributeNum = dataSetInfo.getNdim();
		classNum = dataSetInfo.getCnum();
		trainDataSize = dataSetInfo.getDataSize();

		for(int i=0; i<populationSize; i++){
			currentRuleSets.add( new RuleSet( uniqueRnd, attributeNum, classNum, trainDataSize, testDataSize, objectiveNum) );
		}
		for(int i=0; i<populationSize; i++){
			currentRuleSets.get(i).generalInitialRules(dataSetInfo, forkJoinPool);
		}

	}

	void newRuleSetMutation(int ruleSetIndex, ForkJoinPool forkJoinPool, DataSetInfo trainData){

		int ruleSetsNum = newRuleSets.get(ruleSetIndex).getRuleNum();

		for(int i =0; i<ruleSetsNum; i++){
			if(uniqueRnd.nextInt(ruleSetsNum) == 0){
				int att = uniqueRnd.nextInt(attributeNum);
				newRuleSets.get(ruleSetIndex).micMutation(i, att, forkJoinPool, trainData);
			}
		}

	}

	void michiganOperation(int num, DataSetInfo trainDataInfo, ForkJoinPool forkJoinPool){
		if(newRuleSets.get(num).getRuleNum() != 0){
			boolean isHeuris = Consts.DO_HEURISTIC_GENERATION;
			if(isHeuris){
				newRuleSets.get(num).micGenHeuris(trainDataInfo, forkJoinPool);
			}
			else{
				newRuleSets.get(num).micGenRandom();
			}
		}
	}

	void newRuleSetsInit(){
		newRuleSets.add( new RuleSet(uniqueRnd, attributeNum, classNum, trainDataSize, testDataSize, objectiveNum) );
	}

	void crossOver(int newRuleSetsIdx, int popSize){

		int mom, pop;
		int Nmom, Npop;

		boolean hasParent = Consts.HAS_PARENT;
		if(!hasParent){
			mom = StaticGeneralFunc.binaryT4(currentRuleSets, uniqueRnd, popSize, objectiveNum);
			pop = StaticGeneralFunc.binaryT4(currentRuleSets, uniqueRnd, popSize, objectiveNum);
		}
		else{
			int[] parent = StaticGeneralFunc.binaryTRand(currentRuleSets, uniqueRnd, popSize, objectiveNum);
			mom = parent[0];
			pop = parent[1];
		}

		if(uniqueRnd.nextDouble() < (double)(Consts.RULESET_CROSS_RT)){

			Nmom = uniqueRnd.nextInt(currentRuleSets.get(mom).getRuleNum()) + 1;
			Npop = uniqueRnd.nextInt(currentRuleSets.get(pop).getRuleNum()) + 1;

			if((Nmom + Npop) > Consts.MAX_RULE_NUM){
				int delNum = Nmom + Npop - Consts.MAX_RULE_NUM;
				for(int v=0;v<delNum;v++){
					if(uniqueRnd.nextBoolean()){
						Nmom--;
					}
					else{
						Npop--;
					}
				}
			}

	        int pmom[] = new int[Nmom];
	        int ppop[] = new int[Npop];

	        pmom = StaticGeneralFunc.sampringWithout2(Nmom, currentRuleSets.get(mom).getRuleNum(), uniqueRnd);
	        ppop = StaticGeneralFunc.sampringWithout2(Npop, currentRuleSets.get(pop).getRuleNum(), uniqueRnd);

	        newRuleSets.get(newRuleSetsIdx).micRules.clear();

	        for(int j=0;j<Nmom;j++){
	        	newRuleSets.get(newRuleSetsIdx).setMicRule( currentRuleSets.get(mom).getMicRule(pmom[j]) );
	        }
	        for(int j=0;j<Npop;j++){
	        	newRuleSets.get(newRuleSetsIdx).setMicRule( currentRuleSets.get(pop).getMicRule(ppop[j]) );
	        }

		}
		else{//親をそのまま子個体に
			if(uniqueRnd.nextBoolean()){
				RuleSet deep = new RuleSet(currentRuleSets.get(mom));
				newRuleSets.get(newRuleSetsIdx).copyRuleSet(deep);
			}
			else{
				RuleSet deep = new RuleSet(currentRuleSets.get(pop));
				newRuleSets.get(newRuleSetsIdx).copyRuleSet(deep);
			}
		}
		newRuleSets.get(newRuleSetsIdx).setRuleNum();

	}

	void crossOverAndMichiganOpe(int newRuleSetsIdx, int popSize, ForkJoinPool forkJoinPool, DataSetInfo trainDataInfo){

		int mom, pop;
		int Nmom, Npop;

		//親選択
		mom = StaticGeneralFunc.binaryT4(currentRuleSets, uniqueRnd, popSize, objectiveNum);
		pop = StaticGeneralFunc.binaryT4(currentRuleSets, uniqueRnd, popSize, objectiveNum);

		//ルールの操作
		if(uniqueRnd.nextDouble() < (double)Consts.RULE_OPE_RT){
			RuleSet deep = new RuleSet( currentRuleSets.get(mom) );
			newRuleSets.get(newRuleSetsIdx).copyRuleSet(deep);
			newRuleSets.get(newRuleSetsIdx).setRuleNum();

			if(newRuleSets.get(newRuleSetsIdx).getRuleNum() != 0){
				boolean doHeuris = Consts.DO_HEURISTIC_GENERATION;
				if(doHeuris){
					newRuleSets.get(newRuleSetsIdx).micGenHeuris(trainDataInfo, forkJoinPool);
				}else{
					newRuleSets.get(newRuleSetsIdx).micGenRandom();
				}
			}
		}
		//識別器自体の交叉
		else{
			if(uniqueRnd.nextDouble() < (double)(Consts.RULESET_CROSS_RT)){

				Nmom = uniqueRnd.nextInt(currentRuleSets.get(mom).getRuleNum()) + 1;
				Npop = uniqueRnd.nextInt(currentRuleSets.get(pop).getRuleNum()) + 1;

				if((Nmom + Npop) > Consts.MAX_RULE_NUM){
					int delNum = Nmom + Npop - Consts.MAX_RULE_NUM;
					for(int v=0;v<delNum;v++){
						if(uniqueRnd.nextBoolean()){
							Nmom--;
						}
						else{
							Npop--;
						}
					}
				}

		        int pmom[] = new int[Nmom];
		        int ppop[] = new int[Npop];

		        pmom = StaticGeneralFunc.sampringWithout2(Nmom, currentRuleSets.get(mom).getRuleNum(), uniqueRnd);
		        ppop = StaticGeneralFunc.sampringWithout2(Npop, currentRuleSets.get(pop).getRuleNum(), uniqueRnd);

		        newRuleSets.get(newRuleSetsIdx).micRules.clear();

		        for(int j=0;j<Nmom;j++){
		        	newRuleSets.get(newRuleSetsIdx).setMicRule(currentRuleSets.get(mom).getMicRule(pmom[j]));
		        }
		        for(int j=0;j<Npop;j++){
		        	newRuleSets.get(newRuleSetsIdx).setMicRule(currentRuleSets.get(pop).getMicRule(ppop[j]));
		        }

			}
			else{//親をそのまま子個体に
				if(uniqueRnd.nextBoolean()){
					RuleSet deep = new RuleSet(currentRuleSets.get(mom));
					newRuleSets.get(newRuleSetsIdx).copyRuleSet(deep);
				}
				else{
					RuleSet deep = new RuleSet(currentRuleSets.get(pop));
					newRuleSets.get(newRuleSetsIdx).copyRuleSet(deep);
				}
			}
			newRuleSets.get(newRuleSetsIdx).setRuleNum();
		}

	}

	void addNewPits(int num){
		for(int i = 0; i<num; i++){
			newRuleSets.add( new RuleSet(uniqueRnd,attributeNum,classNum,trainDataSize,testDataSize,objectiveNum) );
		}
	}

	void crossOverRandom(int num, int popSize, Moead moe){

		int mom, pop;
		int Nmom, Npop;

		int[] numOfParents;
		do{
			numOfParents = moe.matingSelection(num, 2);
		}while(currentRuleSets.get(numOfParents[0]).getRuleNum() == 0||currentRuleSets.get(numOfParents[1]).getRuleNum() == 0);

		mom = numOfParents[0];
		pop = numOfParents[1];

		if(uniqueRnd.nextDouble() < (double)(Consts.RULESET_CROSS_RT)){

			Nmom = uniqueRnd.nextInt(currentRuleSets.get(mom).getRuleNum()) + 1;
			Npop = uniqueRnd.nextInt(currentRuleSets.get(pop).getRuleNum()) + 1;

			if((Nmom + Npop) > Consts.MAX_RULE_NUM){
				int delNum = Nmom + Npop - Consts.MAX_RULE_NUM;
				for(int v=0;v<delNum;v++){
					if(uniqueRnd.nextBoolean()){
						Nmom--;
					}
					else{
						Npop--;
					}
				}
			}

	        int pmom[] = new int[Nmom];
	        int ppop[] = new int[Npop];

	        pmom = StaticGeneralFunc.sampringWithout2(Nmom, currentRuleSets.get(mom).getRuleNum(), uniqueRnd);
	        ppop = StaticGeneralFunc.sampringWithout2(Npop, currentRuleSets.get(pop).getRuleNum(), uniqueRnd);

	        for(int j=0;j<Nmom;j++){
	        	newRuleSets.get(num).setMicRule(currentRuleSets.get(mom).getMicRule(pmom[j]));
	        }
	        for(int j=0;j<Npop;j++){
	        	newRuleSets.get(num).setMicRule(currentRuleSets.get(pop).getMicRule(ppop[j]));
	        }

		}
		else{//親をそのまま子個体に
			if(uniqueRnd.nextBoolean()){
				newRuleSets.get(num).replace(currentRuleSets.get(mom));
			}
			else{
				newRuleSets.get(num).replace(currentRuleSets.get(pop));
			}
		}
		newRuleSets.get(num).setRuleNum();

	}

}
