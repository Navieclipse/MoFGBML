package moead;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import gbml.Consts;
import gbml.RuleSet;
import methods.MersenneTwisterFast;

public class Moead implements Serializable{

	private MersenneTwisterFast rnd;
	private MersenneTwisterFast rnd2;

	private int populationSize_;

	double[] z_; // ideal point
	double[] base_z_; // z_ = base_z_ * alpha
	double alpha_;

	double[] n_; // nadir point
	double[] base_n_; // z_ = base_n_ * alpha
	double alpha2_;

	public ArrayList<double[]> lambda_;

	public ArrayList<int[]> neighborhood_;

	int vecNum[];

	int H_; // H : 分割数
	int functionType_;
	int selectNeighborSize_; // 選択近傍サイズ
	int updateNeighborSize_; // 更新近傍サイズ
	int parcentNeighborSize; //　％で近傍サイズ指定

	double epsilon;	//AOFのペナルティの調整パラメータ

	int objective;

	public Moead(int popSize, int H, double alpha, int function, int objective, int seleN,int updaN,
			int patternNum, MersenneTwisterFast rand) {

		this.rnd = rand;
		this.rnd2 = new MersenneTwisterFast(rnd.nextInt());
		this.objective = objective;

		populationSize_ = popSize;

		H_ = H;
		functionType_ = function;
		alpha_ = alpha;

		selectNeighborSize_ = seleN;
		updateNeighborSize_ = updaN;

		boolean isNeighbor = Consts.IS_NEIGHBOR_SIZE;

		if(!isNeighbor){
			parcentNeighborSize = calcNeiPer();
			if(parcentNeighborSize <= 0){
				parcentNeighborSize = 1;
			}
			selectNeighborSize_ = parcentNeighborSize;
			updateNeighborSize_ = parcentNeighborSize;
		}

		vecNum = new int[popSize];
		for (int i = 0; i < popSize; i++) {
			vecNum[i] = i;
		}

		//島モデルの場合は分割後の学習用データのパターン数
		this.epsilon = 1.0 / (double)patternNum;

	}

	public Moead(Moead moe, int popSize, ArrayList<double[]> lambda, int vecNu[]) {

		this.rnd = moe.rnd;
		this.rnd2  = new MersenneTwisterFast(rnd.nextInt());
		this.objective = moe.objective;
		this.functionType_ = moe.functionType_;
		this.alpha_ = moe.alpha_;
		this.selectNeighborSize_ = moe.selectNeighborSize_;
		this.updateNeighborSize_ = moe.updateNeighborSize_;
		populationSize_ = popSize;

		boolean isNeighbor = Consts.IS_NEIGHBOR_SIZE;

		if(!isNeighbor){
			parcentNeighborSize = calcNeiPer();
			if(parcentNeighborSize <= 0){
				parcentNeighborSize = 1;
			}
			selectNeighborSize_ = parcentNeighborSize;
			updateNeighborSize_ = parcentNeighborSize;

		}

		z_ = Arrays.copyOf(moe.z_, moe.z_.length);
		base_z_ = Arrays.copyOf(moe.base_z_, moe.base_z_.length);

		n_ = Arrays.copyOf(moe.n_, moe.n_.length);
		base_n_ = Arrays.copyOf(moe.base_n_, moe.base_n_.length);

		lambda_ = lambda;

		this.vecNum = vecNu;

	}

	public void setPopSize(int popSize){
		this.populationSize_ = popSize;
		vecNum = new int[popSize];
		for (int i = 0; i < popSize; i++) {
			vecNum[i] = i;
		}
		if(objective==2){
			setVecNum(popSize-1);
		}
	}

	public void setVecNum(int vecNum){
		this.H_ = vecNum;
	}

	public void setEpsilon(int patternNum){
		//島モデルの場合は分割後の学習用データのパターン数
		this.epsilon = 1.0 / (double)patternNum;
	}

	public double getEpsilon(){
		return epsilon;
	}

	public void setEmoType(int emoType){
		this.functionType_ = emoType;
	}

	int calcNeiPer(){
		int nei = 0;
		nei = (int)((populationSize_ * (double)Consts.NEIGHBOR_SIZE_RT) / 100.0);
		return nei;
	}

	public void ini(){

		z_ = new double[objective];
		base_z_ = new double[objective];
		for (int i = 0; i < objective; i++) {
			z_[i] = 1.0e+30;
			base_z_[i] = 1.0e+30;
		}

		n_ = new double[objective];
		base_n_ = new double[objective];
		for (int i = 0; i < objective; i++) {
			n_[i] = 1.0e-6;
			base_n_[i] = 1.0e-6;
		}

		boolean isNeighbor = Consts.IS_NEIGHBOR_SIZE;

		if(!isNeighbor){
			parcentNeighborSize = calcNeiPer();
			if(parcentNeighborSize <= 0){
				parcentNeighborSize = 1;
			}
			selectNeighborSize_ = parcentNeighborSize;
			updateNeighborSize_ = parcentNeighborSize;
		}

		if(functionType_ != 5){
			initWeight(H_);
		}else{
			initWeightAOF(populationSize_);
		}

		//近傍構造を計算
		initNeighborhood();
	}

	double imada(double x){

		return ( 4 * (x - 0.5)*(x - 0.5)*(x - 0.5) + 0.5 );

	}

	private void initWeight(int m) {
		this.lambda_ = new ArrayList<double[]>();
		for (int i = 0; i <= m; i++) {
			if (objective == 2) {
				double[] weight = new double[2];

				boolean bias = Consts.IS_BIAS_VECTOR;
				if(bias){
					weight[1] = imada(i / (double) m);
					weight[0] = 1.0 - imada(i / (double) m);
				}else{
					weight[0] = i / (double) m;
					weight[1] = (m - i) / (double) m;
				}
				this.lambda_.add(weight);
			} else if (objective == 3) {
				for (int j = 0; j <= m; j++) {
					if (i + j <= m) {
						int k = m - i - j;
						double[] weight = new double[3];
						weight[0] = i / (double) m;
						weight[1] = j / (double) m;
						weight[2] = k / (double) m;
						this.lambda_.add(weight);
					}
				}
			} else {
				System.out.println("error");
			}
		}
	}

	private void initWeightAOF(int popSize) {

		boolean isInt = Consts.IS_AOF_VECTOR_INT;
		double interval;
		if(isInt){
			interval = 1.0;
		}else{
			interval = Consts.MAX_RULE_NUM / (double)popSize;
		}

		this.lambda_ = new ArrayList<double[]>();

		for (int i = 1; i <= popSize; i++) {
			if (objective == 2) {
				double[] weight = new double[2];
				weight[0] = (double)i * interval;
				weight[1] = (double)i * interval;
				this.lambda_.add(weight);
			} else {
				System.out.println("error");
			}
		}

	}

	private void initNeighborhood() {
		neighborhood_ = new ArrayList<int[]>(populationSize_);

		double[][] distancematrix = new double[populationSize_][populationSize_];
		for (int i = 0; i < populationSize_; i++) {
			distancematrix[i][i] = 0;
			for (int j = i + 1; j < populationSize_; j++) {
				distancematrix[i][j] = Utils.distVector(lambda_.get(i), lambda_.get(j));
				distancematrix[j][i] = distancematrix[i][j];
			}
		}

		for (int i = 0; i < populationSize_; i++) {
			int[] index = Utils.sorting(distancematrix[i]);
			int[] array = new int[populationSize_];
			System.arraycopy(index, 0, array, 0, populationSize_);
			neighborhood_.add(array);
		}
	}

	public void updateReference(RuleSet individual) {
		for (int n = 0; n < objective; n++) {

			if (individual.getFitness(n) < 1) continue;

			if (individual.getFitness(n) < base_z_[n]) {
				base_z_[n] = individual.getFitness(n);
			}
			z_[n] = base_z_[n] * alpha_;

			if(n == 0){
				z_[n] = base_z_[n] - base_z_[n] * Consts.IS_FIRST_IDEAL_DOWN;
			}
		}

		for (int n = 0; n < objective; n++) {

			if (individual.getFitness(n) > 1000) continue;

			if (individual.getFitness(n) > base_n_[n]) {
				base_n_[n] = individual.getFitness(n);
			}
			n_[n] = base_n_[n] * alpha_;

		}
	}

	public int[] matingSelection(int cid, int size) {
		// cid : the id of current subproblem
		// size : the number of selected mating parents
		int pare;
		int[] numOfParents = new int[size];

		// 選択近傍
		for (int i = 0; i < size; i++) {
			pare = rnd2.nextInt(selectNeighborSize_);
			numOfParents[i] = neighborhood_.get(cid)[pare];
		}

		return numOfParents;
	}

	public void updateNeighbors(RuleSet offSpring, ArrayList<RuleSet> population_, int cid, int func) {

		for (int j = 0; j < updateNeighborSize_; j++) {
			int weightIndex = neighborhood_.get(cid)[j];
			RuleSet sol = population_.get(weightIndex);
			double f1 = fitnessFunction(offSpring, lambda_.get(weightIndex), func);
			double f2 = fitnessFunction(sol, lambda_.get(weightIndex), func);
			if (f1 <= f2 && offSpring.getRuleNum() > 0){
				population_.get(weightIndex).replace(new RuleSet(offSpring, vecNum[weightIndex]));
			}
		}

	}

	double normalizationTch(double f , int ob){
		double normalized;

		normalized = (f - z_[ob]) / (n_[ob] - z_[ob]);

		return normalized;
	}

	double normalizationWS(double f , int ob){
		double normalized;

		normalized = f / (n_[ob] - z_[ob]);

		return normalized;
	}

	private double fitnessFunction(RuleSet individual, double[] lambda, int func) {

		boolean doNormalize = Consts.DO_NORMALIZE;
		if (func == Consts.TCHEBY) {
			double maxFun = -1.0 * Double.MAX_VALUE;

			for (int n = 0; n < objective; n++) {
				double diff = 0;
				if(doNormalize){
					diff = Math.abs( normalizationTch(individual.getFitness(n), n) );
				}else{
					diff = Math.abs(individual.getFitness(n) - z_[n]);
				}

				double feval;
				if (lambda[n] == 0) {
					feval = 0.00001 * diff;
				} else {
					feval = diff * lambda[n];
				}
				if (feval > maxFun) {
					maxFun = feval;
				}
			}
			return maxFun;
		}

		else if (func == Consts.WS) {
			boolean isNadia = Consts.IS_WS_FROM_NADIA;
			double sum = 0;
			if(doNormalize){
				for (int n = 0; n < objective; n++) {
					sum += normalizationWS(individual.getFitness(n), n) * lambda[n];
				}
			}else if(isNadia){
				for (int n = 0; n < objective; n++) {
					sum += (n_[n] - individual.getFitness(n)) * lambda[n];
				}
				sum = -sum;
			}
			else{
				for (int n = 0; n < objective; n++) {
					sum += individual.getFitness(n) * lambda[n];
				}
			}
			return sum;
		}

		else if (func == Consts.PBI) {
			double d1, d2;
			double nd = Utils.norm_vector(lambda);
			double[] namda = new double[lambda.length];

			for (int i = 0; i < namda.length; i++) {
				namda[i] = lambda[i] / nd;
			}

			double[] realA = new double[objective];
			double[] realB = new double[objective];

			for (int n = 0; n < realA.length; n++) {
				if(doNormalize){
					realA[n] = normalizationTch(individual.getFitness(n), n);
				}else{
					realA[n] = (individual.getFitness(n) - z_[n]);
				}
			}

			d1 = Math.abs(Utils.prod_vector(realA, namda));

			for (int n = 0; n < realB.length; n++) {
				if(doNormalize){
					realB[n] = normalizationTch(individual.getFitness(n), n) - d1 * namda[n];
				}else{
					realB[n] = (individual.getFitness(n) - z_[n]) - d1 * namda[n];
				}
			}

			d2 = Utils.norm_vector(realB);

			return d1 + Consts.MOEAD_THETA * d2;

		}

		else if (func == Consts.IPBI) {
			double d1, d2;
			double nd = Utils.norm_vector(lambda);
			double[] namda = new double[lambda.length];

			for (int i = 0; i < namda.length; i++) {
				namda[i] = lambda[i] / nd;
			}

			double[] realA = new double[objective];
			double[] realB = new double[objective];

			for (int n = 0; n < realA.length; n++) {
				realA[n] = (n_[n] - individual.getFitness(n));
			}

			d1 = Math.abs(Utils.prod_vector(realA, namda));

			for (int n = 0; n < realB.length; n++) {
				realB[n] = (n_[n] - individual.getFitness(n)) - d1 * namda[n];
			}

			d2 = Utils.norm_vector(realB);

			return -(d1 - Consts.MOEAD_THETA  * d2);

		}

		else if (func == Consts.AOF) {
			double S = individual.getRuleNum();
			double P = S - lambda[1]; //Wi = i ( I = 1, 2, 3, 4, …, N).
			if(P < 0.0)  P = 0.0;
			double SF  = individual.getFitness(0) + 100.0 * P + epsilon * S;    //fi(S) = error(S) + 100 max{ (|S| - Ni), 0} + 1/dataSize*|S|
			return SF;
		}

		else {
			System.out.println("unknown type " + functionType_);
			System.exit(-1);
			return 0;
		}
	}

	public double[] getIdeal(){
		return base_z_;
	}

	public void setIdeal(double[] inputZ){
		base_z_ = Arrays.copyOf(inputZ, objective);
	}

	public double[] getNadia(){
		return base_n_;
	}

}
