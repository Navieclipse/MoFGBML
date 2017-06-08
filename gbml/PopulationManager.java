package gbml;

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;

import methods.MersenneTwisterFast;
import methods.StaticGeneralFunc;
import moead.Moead;


public class PopulationManager{

	//コンストラクタ
	PopulationManager(){}

	public PopulationManager(MersenneTwisterFast rnd, int objectives){

		this.rnd = rnd;
		this.uniqueRnd = new MersenneTwisterFast( rnd.nextInt() );
		this.objectiveNum = objectives;

	}

	/******************************************************************************/
	//ランダム
	MersenneTwisterFast rnd;
	MersenneTwisterFast uniqueRnd;

	//個体群
	public ArrayList<RuleSet> currentRuleSets = new ArrayList<RuleSet>();

	public ArrayList<RuleSet> newRuleSets = new ArrayList<RuleSet>();

	public ArrayList<RuleSet> margeRuleSets = new ArrayList<RuleSet>();

	//ミシガン型GBML用
	public RuleSet bestOfAllGen;

	//読み取った値
	int generationNum;
	int osType;

	int attributeNum;
	int classNum;
	int trainDataSize;
	int testDataSize;

	int objectiveNum;

	/******************************************************************************/

	public void generateInitialPopulation(DataSetInfo dataSetInfo, int populationSize, ForkJoinPool forkJoinPool, int calclationType){

		attributeNum = dataSetInfo.getNdim();
		classNum = dataSetInfo.getCnum();
		trainDataSize = dataSetInfo.getDataSize();

		for(int i=0; i<populationSize; i++){
			currentRuleSets.add( new RuleSet( rnd, attributeNum, classNum, trainDataSize, testDataSize, objectiveNum) );
			currentRuleSets.get(i).generalInitialRules(dataSetInfo, forkJoinPool, calclationType);
		}

	}

	void newRuleSetMutation(int ruleSetIndex){
		int ruleSetsNum = newRuleSets.get(ruleSetIndex).getRuleNum();

		for(int i =0; i<ruleSetsNum; i++){
			for(int j =0; j<attributeNum; j++){
				if(rnd.nextInt(ruleSetsNum * attributeNum) == 0){
					newRuleSets.get(ruleSetIndex).micMutation(i, j);
				}
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
		newRuleSets.add( new RuleSet(rnd, attributeNum, classNum, trainDataSize, testDataSize, objectiveNum) );
	}

	void crossOver(int newRuleSetsIdx, int popSize){

		int mom, pop;
		int Nmom, Npop;

		boolean hasParent = Consts.HAS_PARENT;
		if(!hasParent){
			mom = StaticGeneralFunc.binaryT4(currentRuleSets, rnd, popSize, objectiveNum);
			pop = StaticGeneralFunc.binaryT4(currentRuleSets, rnd, popSize, objectiveNum);
		}
		else{
			int[] parent = StaticGeneralFunc.binaryTRand(currentRuleSets, rnd, popSize, objectiveNum);
			mom = parent[0];
			pop = parent[1];
		}

		if(rnd.nextDouble() < (double)(Consts.RULESET_CROSS_RT)){

			Nmom = rnd.nextInt(currentRuleSets.get(mom).getRuleNum()) + 1;
			Npop = rnd.nextInt(currentRuleSets.get(pop).getRuleNum()) + 1;

			if((Nmom + Npop) > Consts.MAX_RULE_NUM){
				int delNum = Nmom + Npop - Consts.MAX_RULE_NUM;
				for(int v=0;v<delNum;v++){
					if(rnd.nextBoolean()){
						Nmom--;
					}
					else{
						Npop--;
					}
				}
			}

	        int pmom[] = new int[Nmom];
	        int ppop[] = new int[Npop];

	        pmom = StaticGeneralFunc.sampringWithout2(Nmom, currentRuleSets.get(mom).getRuleNum(), rnd);
	        ppop = StaticGeneralFunc.sampringWithout2(Npop, currentRuleSets.get(pop).getRuleNum(), rnd);

	        newRuleSets.get(newRuleSetsIdx).micRules.clear();

	        for(int j=0;j<Nmom;j++){
	        	newRuleSets.get(newRuleSetsIdx).setMicRule( currentRuleSets.get(mom).getMicRule(pmom[j]) );
	        }
	        for(int j=0;j<Npop;j++){
	        	newRuleSets.get(newRuleSetsIdx).setMicRule( currentRuleSets.get(pop).getMicRule(ppop[j]) );
	        }

		}
		else{//親をそのまま子個体に
			if(rnd.nextBoolean()){
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
		mom = StaticGeneralFunc.binaryT4(currentRuleSets, rnd, popSize, objectiveNum);
		pop = StaticGeneralFunc.binaryT4(currentRuleSets, rnd, popSize, objectiveNum);

		if(rnd.nextDouble() < (double)Consts.RULE_OPE_RT){	//ルールの操作
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
		else{	//識別器自体の交叉
			if(rnd.nextDouble() < (double)(Consts.RULESET_CROSS_RT)){
				Nmom = rnd.nextInt(currentRuleSets.get(mom).getRuleNum()) + 1;
				Npop = rnd.nextInt(currentRuleSets.get(pop).getRuleNum()) + 1;

				if((Nmom + Npop) > Consts.MAX_RULE_NUM){
					int delNum = Nmom + Npop - Consts.MAX_RULE_NUM;
					for(int v=0;v<delNum;v++){
						if(rnd.nextBoolean()){
							Nmom--;
						}
						else{
							Npop--;
						}
					}
				}

		        int pmom[] = new int[Nmom];
		        int ppop[] = new int[Npop];

		        pmom = StaticGeneralFunc.sampringWithout2(Nmom, currentRuleSets.get(mom).getRuleNum(), rnd);
		        ppop = StaticGeneralFunc.sampringWithout2(Npop, currentRuleSets.get(pop).getRuleNum(), rnd);

		        newRuleSets.get(newRuleSetsIdx).micRules.clear();

		        for(int j=0;j<Nmom;j++){
		        	newRuleSets.get(newRuleSetsIdx).setMicRule(currentRuleSets.get(mom).getMicRule(pmom[j]));
		        }
		        for(int j=0;j<Npop;j++){
		        	newRuleSets.get(newRuleSetsIdx).setMicRule(currentRuleSets.get(pop).getMicRule(ppop[j]));
		        }

			}
			else{//親をそのまま子個体に
				if(rnd.nextBoolean()){
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
			newRuleSets.add( new RuleSet(rnd,attributeNum,classNum,trainDataSize,testDataSize,objectiveNum) );
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

		if(rnd.nextDouble() < (double)(Consts.RULESET_CROSS_RT)){

			Nmom = rnd.nextInt(currentRuleSets.get(mom).getRuleNum()) + 1;
			Npop = rnd.nextInt(currentRuleSets.get(pop).getRuleNum()) + 1;

			if((Nmom + Npop) > Consts.MAX_RULE_NUM){
				int delNum = Nmom + Npop - Consts.MAX_RULE_NUM;
				for(int v=0;v<delNum;v++){
					if(rnd.nextBoolean()){
						Nmom--;
					}
					else{
						Npop--;
					}
				}
			}

	        int pmom[] = new int[Nmom];
	        int ppop[] = new int[Npop];

	        pmom = StaticGeneralFunc.sampringWithout2(Nmom, currentRuleSets.get(mom).getRuleNum(), rnd);
	        ppop = StaticGeneralFunc.sampringWithout2(Npop, currentRuleSets.get(pop).getRuleNum(), rnd);

	        for(int j=0;j<Nmom;j++){
	        	newRuleSets.get(num).setMicRule(currentRuleSets.get(mom).getMicRule(pmom[j]));
	        }
	        for(int j=0;j<Npop;j++){
	        	newRuleSets.get(num).setMicRule(currentRuleSets.get(pop).getMicRule(ppop[j]));
	        }

		}
		else{//親をそのまま子個体に
			if(rnd.nextBoolean()){
				newRuleSets.get(num).replace(currentRuleSets.get(mom));
			}
			else{
				newRuleSets.get(num).replace(currentRuleSets.get(pop));
			}
		}
		newRuleSets.get(num).setRuleNum();

	}

}
