package gbml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import methods.MersenneTwisterFast;
import methods.StaticFuzzyFunc;
import methods.StaticGeneralFunc;

public class RuleSet{
	/******************************************************************************/
	//コンストラクタ

	RuleSet(){}

	RuleSet(MersenneTwisterFast rnd, int Ndim, int Cnum, int DataSize, int DataSizeTst, int objectibes){
		this.rnd = rnd;
		this.uniqueRnd = new MersenneTwisterFast( rnd.nextInt() );
		this.Ndim = Ndim;
		this.Cnum = Cnum;
		this.DataSize = DataSize;
		this.DataSizeTst = DataSizeTst;

		this.evaflag = 0;
		this.rank = 0;
		this.crowding = 0;
		this.vecNum = 0;
		this.fitnesses = new double[objectibes];
		this.firstobj = new double[objectibes];
	}

	public RuleSet(RuleSet ruleSet){

		this.rnd = ruleSet.rnd;
		this.uniqueRnd = ruleSet.uniqueRnd;
		this.Ndim = ruleSet.Ndim;
		this.Cnum = ruleSet.Cnum;
		this.DataSize = ruleSet.DataSize;
		this.DataSizeTst = ruleSet.DataSizeTst;

		this.missRate = ruleSet.missRate;
		this.ruleNum = ruleSet.ruleNum;
		this.ruleLength = ruleSet.ruleLength;
		this.fitness = ruleSet.fitness;

		this.vecNum = ruleSet.vecNum;

		Rule a;
		this.micRules.clear();
		for(int i=0;i<ruleSet.micRules.size();i++){
			 a = new Rule(ruleSet.micRules.get(i));
			this.micRules.add(a);
		}

		this.evaflag = ruleSet.evaflag;
		this.testMissRate = ruleSet.testMissRate;
		this.rank = ruleSet.rank;
		this.crowding = ruleSet.crowding;
		fitnesses = Arrays.copyOf(ruleSet.fitnesses, ruleSet.fitnesses.length);
		firstobj = Arrays.copyOf(ruleSet.firstobj, ruleSet.fitnesses.length);

		if(ruleSet.missPatterns != null){
			this.missPatterns = new ArrayList<Integer>();
			for(int i=0; i<ruleSet.missPatterns.size(); i++){
				this.missPatterns.add( ruleSet.missPatterns.get(i) );
			}
		}
		this.MissPatNum = ruleSet.MissPatNum;

	}

	public RuleSet(RuleSet ruleSet, int vecNum){

		this.rnd = ruleSet.rnd;
		this.uniqueRnd = ruleSet.uniqueRnd;
		this.Ndim = ruleSet.Ndim;
		this.Cnum = ruleSet.Cnum;
		this.DataSize = ruleSet.DataSize;
		this.DataSizeTst = ruleSet.DataSizeTst;

		this.missRate = ruleSet.missRate;
		this.ruleNum = ruleSet.ruleNum;
		this.ruleLength = ruleSet.ruleLength;
		this.fitness = ruleSet.fitness;

		this.vecNum = vecNum;

		Rule a;
		this.micRules.clear();
		for(int i=0;i<ruleSet.micRules.size();i++){
			 a = new Rule(ruleSet.micRules.get(i));
			this.micRules.add(a);
		}

		this.evaflag = ruleSet.evaflag;
		this.testMissRate = ruleSet.testMissRate;
		this.rank = ruleSet.rank;
		this.crowding = ruleSet.crowding;
		fitnesses = Arrays.copyOf(ruleSet.fitnesses, ruleSet.fitnesses.length);
		firstobj = Arrays.copyOf(ruleSet.firstobj, ruleSet.fitnesses.length);

		if(ruleSet.missPatterns != null){
			this.missPatterns = new ArrayList<Integer>();
			for(int i=0; i<ruleSet.missPatterns.size(); i++){
				this.missPatterns.add( ruleSet.missPatterns.get(i) );
			}
		}
		this.MissPatNum = ruleSet.MissPatNum;
	}

	public void copyRuleSet(RuleSet ruleSet){
		this.rnd = ruleSet.rnd;
		this.uniqueRnd = ruleSet.uniqueRnd;
		this.Ndim = ruleSet.Ndim;
		this.Cnum = ruleSet.Cnum;
		this.DataSize = ruleSet.DataSize;
		this.DataSizeTst = ruleSet.DataSizeTst;

		this.missRate = ruleSet.missRate;
		this.ruleNum = ruleSet.ruleNum;
		this.ruleLength = ruleSet.ruleLength;
		this.fitness = ruleSet.fitness;

		this.vecNum = ruleSet.vecNum;

		this.evaflag = ruleSet.evaflag;
		this.testMissRate = ruleSet.testMissRate;
		this.rank = ruleSet.rank;
		this.crowding = ruleSet.crowding;
		this.fitnesses = Arrays.copyOf(ruleSet.fitnesses, ruleSet.fitnesses.length);
		firstobj = Arrays.copyOf(ruleSet.firstobj, ruleSet.fitnesses.length);

		Rule a;
		this.micRules.clear();
		for(int i=0; i<ruleSet.micRules.size(); i++){
			a = new Rule( ruleSet.micRules.get(i) );
			this.micRules.add(a);
		}

		if(ruleSet.missPatterns != null){
			this.missPatterns = new ArrayList<Integer>();
			for(int i=0; i<ruleSet.missPatterns.size(); i++){
				this.missPatterns.add( ruleSet.missPatterns.get(i) );
			}
		}
		this.MissPatNum = ruleSet.MissPatNum;
	}

	/******************************************************************************/
	//ランダム
	MersenneTwisterFast rnd;
	MersenneTwisterFast uniqueRnd;

    //学習用
	int Ndim;												//次元
	int Cnum;												//クラス数
	int DataSize;											//パターン数
	int DataSizeTst;										//パターン数

	//基本値
	ArrayList<Rule> micRules = new ArrayList<Rule>();

	ArrayList<Rule> newMicRules = new ArrayList<Rule>();

	double missRate;
	double testMissRate;
	int ruleNum;
	int ruleLength;

	//ミスパターン保存用リスト
	ArrayList<Integer> missPatterns = new ArrayList<Integer>();
	int MissPatNum;

	//並べ替えの基準(1obj)
	double fitness;
	//2目的以上
	double fitnesses[];

	//MOEAD用
	int vecNum;
	double otherDataRate;

	//NSGA用
	int evaflag;
	int rank;
	double crowding;
	double firstobj[];

	/******************************************************************************/
	//メソッド
	public void generalInitialRules(DataSetInfo trainDataInfo, ForkJoinPool forkJoinPool, int calclationType){
		//ヒューリスティック生成を行う場合
		boolean isHeuris = Consts.DO_HEURISTIC_GENERATION;

        int sampleNums[] = null;
        if(isHeuris){ //サンプリング
        	if(calclationType == 0){
				sampleNums = new int[Consts.INITIATION_RULE_NUM];
				sampleNums = StaticGeneralFunc.sampringWithout(Consts.INITIATION_RULE_NUM, DataSize, uniqueRnd);
        	}
        }

        do{ //while( micRules.size() == 0)

        	for(int i=0; i<Consts.INITIATION_RULE_NUM; i++){
        		micRules.add( new Rule(uniqueRnd, Ndim, Cnum, DataSize, DataSizeTst) );
        		micRules.get(i).setMic();

        		if(isHeuris){		//ヒューリスティック生成
					micRules.get(i).makeRuleSingle( trainDataInfo.getPattern(sampleNums[i]), uniqueRnd );
					micRules.get(i).calcRuleConc(trainDataInfo, forkJoinPool);
        		}else{				//完全ランダム生成
					micRules.get(i).makeRuleRnd1(uniqueRnd);
					micRules.get(i).makeRuleRnd2();
        		}
        	}
        	removeRule();

        }while( micRules.size()==0 );

		ruleNum = micRules.size();
		ruleLength = ruleLengthCalc();

	}

	public void setFitness(DataSetInfo trainDataInfo, ForkJoinPool forkJoinPool, int calclationType){

		removeRule();

		ruleNum = micRules.size();
		ruleLength = ruleLengthCalc();

		if(ruleNum==0 || ruleLength==0){
			fitness = 1000000;
			missRate = 1000000;
		}
		else{
			double ans = 0.0;
			boolean isRulePara = Consts.IS_RULE_PARALLEL;
			if(isRulePara){
				ans = calcMissPatternsWithRule(trainDataInfo);
			}
			else{
				ans = calcMissPatterns(trainDataInfo, forkJoinPool);
			}
			double acc = ans / trainDataInfo.getDataSize();
			missRate = ( acc * 100.0 );
			fitness = StaticFuzzyFunc.fitness(missRate, (double)ruleNum, (double)ruleLength);
		}

	}

	public void removeRule(){

		for (int i = 0; i < micRules.size(); i++) {
			int size = 0;
			while (micRules.size() > size) {
				if (micRules.get(size).getCf() <= 0 || micRules.get(size).getRuleLength() == 0) {
				//if(micRules.get(size).getConc() == -1){
					micRules.remove(size);
				}
				else {
					size++;
				}
			}
		}
	}

	public int ruleLengthCalc(){
		int ans = 0;
		for(int i=0; i<ruleNum; i++){
			ans += micRules.get(i).getLength();
		}

		ruleLength = ans;

		return ans;
	}

	public int getRuleNum(){
		return ruleNum;
	}

	public int getRuleLength(){
		return ruleLength;
	}

	public double getMissRate(){
		return missRate;
	}

	public void setRuleNum(){
		ruleNum = micRules.size();
	}

	public void setLength(){
		ruleLength = ruleLengthCalc();
	}

	public double getFitness(){
		return fitness;
	}

	public Rule getMicRule(int num){
		return micRules.get(num);
	}

	public void setMicRule(Rule micrule){

		Rule mic = new Rule(micrule);
		micRules.add(mic);

	}

	public void micMutation(int num, int i){
		micRules.get(num).mutation(i, uniqueRnd);
	}

	public void micGenRandom(){

		//交叉個体数（ルールの20％）あるいは１個
		int snum;
		if(uniqueRnd.nextDouble() < (double)Consts.RULE_OPE_RT){
			snum = (int)((ruleNum - 0.00001) * Consts.RULE_CHANGE_RT) + 1;
		}else{
			snum = 1;
		}

		//合計生成個体数
		int heuNum, genNum = 0;
		if(snum % 2 == 0){
			heuNum = snum/2;
			genNum = snum/2;
		}
		else{
			int plus = uniqueRnd.nextInt(2);
			heuNum = (snum-1)/2 + plus;
			genNum = snum - heuNum;
		}

		for(int i=0;i<genNum;i++){
			ruleCross(i);
		}

		for(int i=genNum; i<snum; i++){
			randomGeneration(i);
		}

		//旧個体の削除，新個体の追加
		micUpdate(snum);

	}

	public void micGenHeuris(DataSetInfo trainDataInfo, ForkJoinPool forkJoinPool){

		//交叉個体数（ルールの20％）あるいは１個
		int snum;
		if(uniqueRnd.nextDouble() < (double)Consts.RULE_OPE_RT){
			snum = (int)((ruleNum - 0.00001) * Consts.RULE_CHANGE_RT) + 1;
		}else{
			snum = 1;
		}

		//合計生成個体数
		int heuNum, genNum = 0;
		if(snum % 2 == 0){
			heuNum = snum/2;
			genNum = snum/2;
		}
		else{
			int plus = uniqueRnd.nextInt(2);
			heuNum = (snum-1)/2 + plus;
			genNum = snum - heuNum;
		}

		//ヒューリスティック生成の誤識別パターン
		//足りないor無い場合はランダムに追加
		while(missPatterns.size() < heuNum){
			missPatterns.add(  rnd.nextInt( trainDataInfo.getDataSize() )  );
		}

		int missPatternsSampleIdx[] = new int[heuNum];
		missPatternsSampleIdx = StaticGeneralFunc.sampringWithout(heuNum, missPatterns.size(), uniqueRnd);

		for(int i=0;i<genNum;i++){
			ruleCross(i);
			newMicRules.get(i).calcRuleConc(trainDataInfo, forkJoinPool);
		}
		int missPatIndex = 0;
		for(int i=genNum; i<snum; i++){
			heuristicGeneration(i, trainDataInfo.getPattern(missPatterns.get( missPatternsSampleIdx[missPatIndex++]) ) , trainDataInfo, forkJoinPool);
		}

		//旧個体の削除，新個体の追加
		micUpdate(snum);

	}

	public void ruleCross(int num){

		newMicRules.add( new Rule(uniqueRnd, Ndim, Cnum, DataSize, DataSizeTst) );
		newMicRules.get(num).setMic();

		//親個体選択（バイナリトーナメントは計算量が異常にかかるので，同じ結論部の個体同士で交叉，無ければ諦める(ルール数回で）
		int mom = uniqueRnd.nextInt(ruleNum);
		int pop = uniqueRnd.nextInt(ruleNum);
		int count = 0;
		while( micRules.get(pop).getConc() != micRules.get(mom).getConc() && count < ruleNum){
			pop = uniqueRnd.nextInt(ruleNum);
			count++;
		}

		if(uniqueRnd.nextDouble() < (Consts.RULE_CROSS_RT)){
			//一様交叉
			int k=0;
			int k2=0;
			int o = 0;
			for(int i=0;i<Ndim;i++){
				k = uniqueRnd.nextInt(2);
				if(k==0){
					newMicRules.get(num).setRule(i, micRules.get(mom).getRule(i));
				}
				else{
					newMicRules.get(num).setRule(i, micRules.get(pop).getRule(i));
				}
				k2 = uniqueRnd.nextInt(Ndim);
				//突然変異
				if(k2==0){
					do{
						o = uniqueRnd.nextInt(Consts.FUZZY_SET_NUM +1);
					}while(o == newMicRules.get(num).getRule(i));
					newMicRules.get(num).setRule(i, o);
				}
			}
		}

		else{
			int o = 0;
			int k2 = 0;
			for(int i=0;i<Ndim;i++){
				newMicRules.get(num).setRule(i, micRules.get(mom).getRule(i));
				//突然変異
				k2 = uniqueRnd.nextInt(Ndim);
				if(k2==0){
					do{
						o = uniqueRnd.nextInt(Consts.FUZZY_SET_NUM +1);
					}while(o == newMicRules.get(num).getRule(i));
					newMicRules.get(num).setRule(i, o);
				}
			}
		}

		//結論部はmomに合わす．ルール重みはランダムな割合で合計
		double cfRate = uniqueRnd.nextDouble();
		double newCf = micRules.get(mom).getCf() * cfRate + micRules.get(pop).getCf() * (1.0-cfRate);

		newMicRules.get(num).makeRuleCross( micRules.get(mom).getConc(), newCf );

	}

	int[] calcHaveClass(){
		int noCla[] = micRules.stream()
					.mapToInt(r ->r.getConc())
					.distinct()
					.sorted()
					.toArray();
		return noCla;
	}

	int[] calcNoClass(){
		int haveClass[] = calcHaveClass();

		List<Integer> noCla = new ArrayList<Integer>();
		for (int num= 0; num < Cnum; num++) {
			boolean ishave = false;
			for (int have_i = 0; have_i < haveClass.length; have_i++) {
				if(num == haveClass[have_i]) ishave =true;
			}
			if(!ishave) noCla.add(num);
		}

		int noClass[] = noCla.stream().mapToInt(s->s).toArray();

		return noClass;
	}

	public void randomGeneration(int num){
		//足りていないクラスの個体生成を優先
		//識別器中のクラス判別
		int noCla[] = calcNoClass();
		newMicRules.add( new Rule(uniqueRnd, Ndim, Cnum, DataSize, DataSizeTst) );
		newMicRules.get(num).setMic();
		newMicRules.get(num).makeRuleRnd1(uniqueRnd);
		if(noCla.length == 0){
			newMicRules.get(num).makeRuleRnd2();
		}else{
			newMicRules.get(num).makeRuleNoCla(noCla);
		}

	}

	//HDFSを使うわない場合
	public void heuristicGeneration(int num, Pattern line, DataSetInfo trainDataInfo, ForkJoinPool forkJoinPool){
		newMicRules.add( new Rule(uniqueRnd, Ndim, Cnum, DataSize, DataSizeTst) );
		newMicRules.get(num).setMic();
		newMicRules.get(num).makeRuleSingle(line, uniqueRnd);
		newMicRules.get(num).calcRuleConc(trainDataInfo, forkJoinPool);
	}

	public void micUpdate(int snum){

		boolean doAddRules = Consts.DO_ADD_RULES;
		if(!doAddRules){ //ルールを入れ替える
			boolean isHeuris = Consts.DO_HEURISTIC_GENERATION;
			if(isHeuris){ //CF順に入れ替え
				Collections.sort( micRules, new ruleComparatorByFitness() );	//ルール使用回数sort
				int num = 0;
				for(int i=ruleNum-snum; i<ruleNum; i++){
					micRules.get(i).copyRule( newMicRules.get(num) );
					num++;
				}
			}
			else{ //ランダムに入れ替え
				int repNum[] = StaticGeneralFunc.sampringWithout2(snum, micRules.size(), uniqueRnd);
				for(int i=0; i<snum; i++){
					micRules.get(repNum[i]).copyRule( newMicRules.get(i) );
				}
			}
		}
		else{ //ルールを追加する（MAXを超えた場合CF順に破棄する）
			int overRuleNum = micRules.size() + newMicRules.size() - Consts.MAX_RULE_NUM;
			if( overRuleNum > 0 ){
				Collections.sort( newMicRules, new ruleComparator() );
				for(int i=0; i<overRuleNum; i++){
					newMicRules.remove( newMicRules.size()-1 );
				}
			}
			for(int i=0; i<newMicRules.size(); i++){
				micRules.add(  new Rule( newMicRules.get(i) )  );
			}
		}

	}

	public class ruleComparatorByFitness implements Comparator<Rule> {
	    public int compare(Rule a, Rule b) {
	        int no1 = a.getFitness();
	        int no2 = b.getFitness();

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

	public class ruleComparator implements Comparator<Rule> {
	    public int compare(Rule a, Rule b) {
	        double no1 = a.getCf();
	        double no2 = b.getCf();

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

	//MOEAD
	public void replace(RuleSet rules) {
		this.copyRuleSet(rules);
	}

	public int mulCla(){
		int CnumNum[] = new int[Cnum];
		int claNum = 0;
		for(int i =0; i<micRules.size(); i++){
			if( micRules.get(i).getConc() != -1 ){
				CnumNum[micRules.get(i).getConc()]++;
			}
		}
		for(int i = 0; i<Cnum; i++){
			if(CnumNum[i]>0){
				claNum++;
			}
		}
		return claNum;
	}

	public void setVecNum(int n){
		vecNum = n;
	}

	public int getVecNum(){
		return vecNum;
	}

	public void setFitness(double fitness, int o) {
		this.fitnesses[o] = fitness;
	}

	public double getFitness(int o) {
		return fitnesses[o];
	}

	public void setSize(int newDataSize){
		ruleNum = micRules.size();
		ruleLength = ruleLengthCalc();
		this.DataSize = newDataSize;
	}

	//NSGAII
	public void setRank(int r) {
		rank = r;
	}

	public int getRank() {
		return rank;
	}

	public void setCrowding(double crow) {
		crowding = crow;
	}

	public double getCrowding() {
		return crowding;
	}

	public void setFirstObj(double firstobj){
		this.firstobj[0] = firstobj;
		for(int i=1; i<fitnesses.length;i++){
			this.firstobj[i] = fitnesses[i];
		}
	}

	public double getFirstObj(int num){
		return firstobj[num];
	}

	double out2obje(int way){
		if(way == 4){
			return (double)(getRuleLength() / getRuleNum());
		}else if(way == 3){
			return (double)(getRuleNum() + getRuleLength());
		}else if(way == 2){
			return (double)(getRuleNum() * getRuleLength());
		}else if(way == 1){
			return (double)(getRuleLength());
		}else {
			return (double)(getRuleNum());
		}
	}

	/************************************************************************************************************/

	public void evaluationRule(DataSetInfo trainDataInfo, int objectives, int way, ForkJoinPool forkJoinPool) {

		if (getRuleNum() != 0) {
			//各種方ごとの計算
			double ans = 0;

			boolean isRulePara = Consts.IS_RULE_PARALLEL;
			if(isRulePara){
				ans = calcMissPatternsWithRule(trainDataInfo);
			}
			else{
				ans = calcMissPatterns(trainDataInfo, forkJoinPool);
			}

			double acc = ans / trainDataInfo.getDataSize();
			setMissRate( acc * 100.0 );
			setNumAndLength();

			//各目的数ごとの目的関数値
			if (objectives == 1) {
				double fitness = Consts.W1 * getMissRate() + Consts.W2 * getRuleNum() + Consts.W3 * getRuleLength();
				setFitness(fitness, 0);
			}
			else if (objectives == 2) {
				setFitness( (getMissRate() ), 0 );
				setFitness( (out2obje(way) ), 1 );
			}
			else if (objectives == 3) {
				setFitness( getMissRate(), 0 );
				setFitness( getRuleNum(), 1 );
				setFitness( getRuleLength(), 2 );
			}
			else {
				System.out.println("not be difined");
			}
			if(getRuleLength() == 0){
				for (int o = 0; o < objectives; o++) {
					setFitness(100000, o);
				}
			}
		}
		else {
			setMissRate(100);
			for (int o = 0; o < objectives; o++) {
				setFitness(100000, o);
			}
		}

	}

	/************************************************************************************************************/
	//server unit 用
	public void evaluationRule(DataSetInfo trainDataInfo, ForkJoinPool forkJoinPool) {
		int way = Consts.SECOND_OBJECTIVE_TYPE;
		int objectives = fitnesses.length;

		if (getRuleNum() != 0) {
			//各種方ごとの計算
			double ans = 0;
//			boolean doHeuris = Consts.DO_HEURISTIC_GENERATION;
//			if(doHeuris){
//				ans = calcAndSetMissPatterns(trainDataInfo, forkJoinPool);
//			}
//			else{
//				ans = calcMissPatterns(trainDataInfo, forkJoinPool);
//			}
			ans = calcMissPatterns(trainDataInfo, forkJoinPool);

			double acc = ans / trainDataInfo.getDataSize();
			setMissRate( acc * 100.0 );
			setNumAndLength();

			//各目的数ごとの目的関数値
			if (objectives == 1) {
				double fitness = Consts.W1 * getMissRate() + Consts.W2 * getRuleNum() + Consts.W3 * getRuleLength();
				setFitness(fitness, 0);
			}
			else if (objectives == 2) {
				setFitness( (getMissRate() ), 0 );
				setFitness( (out2obje(way) ), 1 );
			}
			else if (objectives == 3) {
				setFitness( getMissRate(), 0 );
				setFitness( getRuleNum(), 1 );
				setFitness( getRuleLength(), 2 );
			}
			else {
				System.out.println("not be difined");
			}
			if(getRuleLength() == 0){
				for (int o = 0; o < objectives; o++) {
					setFitness(100000, o);
				}
			}
		}
		else {
			setMissRate(100);
			for (int o = 0; o < objectives; o++) {
				setFitness(100000, o);
			}
		}

	}

	/************************************************************************************************************/
	//ルールで並列化する場合
	public int calcMissPatternsWithRule(DataSetInfo dataSetInfo) {

		//初期化
		int ruleNum = micRules.size();
		for(int i=0; i<ruleNum; i++){
			micRules.get(i).clearFitness();
		}
		missPatterns.clear();
		MissPatNum = 0;

		int dataSize = dataSetInfo.getDataSize();
		int ans = -1;
		for (int p = 0; p < dataSize; p++){
			ans = calcWinClassPalwithRule( dataSetInfo.getPattern(p) );
			if ( ans != dataSetInfo.getPattern(p).getConClass() ){
				missPatterns.add(p);
				MissPatNum++;
			}
		}

		return MissPatNum;
	}

	public int calcMissPatterns(DataSetInfo dataSetInfo, ForkJoinPool forkJoinPool) {

		try{
			MissPatNum = forkJoinPool.submit( () ->
				(int)dataSetInfo.getPattern().parallelStream()
				.filter( line -> calcWinClassPal(line) != line.getConClass() )
				.count()
				).get();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return MissPatNum;
	}

	/************************************************************************************************************/
	//ルールで並列化する場合
	public int calcWinClassPalwithRule(Pattern line){

		int answerClass = 0;
		int winClassIdx = 0;

		int ruleSize = micRules.size();
		boolean canClassify = true;
		double maxMul = 0.0;
		for(int r=0; r<ruleSize; r++){

			double multiValue = micRules.get(r).getCf() * micRules.get(r).calcAdaptationPure(line);

			if (maxMul < multiValue){
				maxMul = multiValue;
				winClassIdx = r;
				canClassify = true;
			}
			else if( maxMul == multiValue && micRules.get(r).getConc() != micRules.get(winClassIdx).getConc() ){
				canClassify = false;
			}

		}
		if( canClassify && maxMul != 0.0 ){
			answerClass = micRules.get(winClassIdx).getConc();
			micRules.get(winClassIdx).addFitness();
		}
		else{
			answerClass = -1;
		}

		return answerClass;
	}

	/************************************************************************************************************/
	//データで並列化する場合
	public int calcWinClassPal(Pattern line){

		int answerClass = 0;
		int winClassIdx = 0;

		int ruleSize = micRules.size();
		boolean canClassify = true;
		double maxMul = 0.0;
		for(int r=0; r<ruleSize; r++){

			double multiValue = micRules.get(r).getCf() * micRules.get(r).calcAdaptationPure(line);

			if (maxMul < multiValue){
				maxMul = multiValue;
				winClassIdx = r;
				canClassify = true;
			}
			else if( maxMul == multiValue && micRules.get(r).getConc() != micRules.get(winClassIdx).getConc() ){
				canClassify = false;
			}

		}
		if( canClassify && maxMul != 0.0 ){
			answerClass = micRules.get(winClassIdx).getConc();
		}
		else{
			answerClass = -1;
		}

		return answerClass;
	}

	/************************************************************************************************************/
	public void setTestMissRate(double m){
		testMissRate = m;
	}

	public double getTestMissRate(){
		return testMissRate;
	}

	public void setMissRate(double m) {
		missRate = m;
	}

	public void setNumAndLength(){
		removeRule();
		ruleNum = micRules.size();
		ruleLength = ruleLengthCalc();
	}

	public ArrayList<Rule> getRules(){
		return micRules;
	}


}

