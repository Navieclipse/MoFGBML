package gbml;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

import methods.MersenneTwisterFast;
import methods.StaticFuzzyFunc;

public class Rule  implements Serializable{

		/******************************************************************************/
		//コンストラクタ

		public Rule(){}

		public Rule(Rule rule){
			this.rnd = rule.rnd;

			this.Ndim = rule.Ndim;
			this.Cnum = rule.Cnum;
			this.DataSize = rule.DataSize;
			this.TstDataSize = rule.TstDataSize;

			this.rule = Arrays.copyOf(rule.rule, rule.Ndim);

			this.conclution = rule.conclution;
			this.cf = rule.cf;
			this.ruleLength = rule.ruleLength;
		}

		Rule(MersenneTwisterFast rnd, int Ndim, int Cnum, int DataSize, int TstDataSize){
			this.rnd = new MersenneTwisterFast(rnd.nextInt());
			this.Ndim = Ndim;
			this.Cnum = Cnum;
			this.DataSize = DataSize;
			this.TstDataSize = TstDataSize;
		}

		public void copyRule(Rule rule){
			this.rnd = rule.rnd;

			this.Ndim = rule.Ndim;
			this.Cnum = rule.Cnum;
			this.DataSize = rule.DataSize;
			this.TstDataSize = rule.TstDataSize;

			this.rule = Arrays.copyOf(rule.rule, rule.Ndim);

			this.conclution = rule.conclution;
			this.cf = rule.cf;
			this.ruleLength = rule.ruleLength;
		}

		/******************************************************************************/
		//ランダム
		MersenneTwisterFast rnd;

	    //学習用
		int Ndim;												//次元
		int Cnum;												//クラス数
		int DataSize;											//パターン数
		int TstDataSize;										//パターン数

		//基本値
		int rule[];	//ルールの前件部
		int conclution;	//ルールの結論部クラス
		double cf; //ルール重み
		int ruleLength;	//ルール長

		/******************************************************************************/
		//method

		//ルール作成
		public void setMic(){
			rule = new int[Ndim];
		}

		public void setMic(int Dim){
			Ndim = Dim;
			rule = new int[Dim];
		}

		public void setTest(int size){
			this.TstDataSize = size;
		}

		//HDFS使わない場合
		public void makeRuleSingle(Pattern line, MersenneTwisterFast rnd2){
			rule = StaticFuzzyFunc.selectSingle(line, Ndim, rnd2);
		}

		//HDFS使わない場合
		public void calcRuleConc(DataSetInfo trainData, ForkJoinPool forkJoinPool){

			double[] trust = StaticFuzzyFunc.calcTrust(trainData, rule, Cnum, forkJoinPool);
			conclution = StaticFuzzyFunc.calcConclusion(trust, Cnum);
			cf = StaticFuzzyFunc.calcCf(conclution, trust, Cnum);

	        ruleLength = ruleLengthCalc();
		}

		public void makeRuleRnd1(MersenneTwisterFast rnd2){
			rule = StaticFuzzyFunc.selectRnd(Ndim, rnd2);
		}

		public void makeRuleRnd2(){
	    	conclution = rnd.nextInt(Cnum);
	    	cf = rnd.nextDouble();
	        ruleLength = ruleLengthCalc();
		}

		public void makeRuleNoCla(int[] noClass){
	    	conclution = noClass[rnd.nextInt(noClass.length)];
	    	cf = rnd.nextDouble();
	        ruleLength = ruleLengthCalc();
		}

		public void makeRuleCross(int ansCla, double cf){
	    	conclution = ansCla;
	    	this.cf = cf;
	        ruleLength = ruleLengthCalc();
		}

		public int getRuleLength(){
			return ruleLength;
		}

		//HDFS使わない場合
		public double calcAdaptationPure(Pattern line){
	    	return  StaticFuzzyFunc.menberMulPure(line, rule);
		}

		public double getCf(){
			return cf;
		}

		public int getConc(){
			return conclution;
		}

		public int getLength(){
			return ruleLength;
		}

		public void setLength(int Length){
			ruleLength = Length;
		}

		public int ruleLengthCalc(){
			int ans = 0;
			for(int i=0; i<Ndim; i++){
				if(rule[i]!=0){
					ans++;
				}
			}
			return ans;
		}

		public void setRule(int num, int ruleN){
			rule[num] = ruleN;
		}

		public int getRule(int num){
			return rule[num];
		}

		public void mutation(int i, MersenneTwisterFast rnd2){

			int v;
			do {
				v = rnd2.nextInt(Consts.FUZZY_SET_NUM + 1);
			} while (v == rule[i]);
			rule[i] = v;

		    cf = rnd.nextDouble();

		    ruleLength = ruleLengthCalc();

		}

		public int getNdim(){
			return Ndim;
		}

}
