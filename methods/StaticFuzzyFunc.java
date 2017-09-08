package methods;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import gbml.Consts;
import gbml.DataSetInfo;
import gbml.Pattern;

public class StaticFuzzyFunc {

	public StaticFuzzyFunc(){}

	static int KK[] = {1,2,2,3,3,3,4,4,4,4,5,5,5,5,5,6,6,6,6,6,6,7,7,7,7,7,7,7};					//メンバシップの時のK
	static int kk[] = {1,1,2,1,2,3,1,2,3,4,1,2,3,4,5,1,2,3,4,5,6,1,2,3,4,5,6,7};					//メンバシップの時のk

	int LargeK[];
	int SmallK[];

	static double w[]  = {100.0, 1.0, 1.0};							//ウェイト

	/************************************************************************************************************/
	//ファジィ分割の初期化
	public void KKkk(int maxFnum){
		int arrayNum = 0;
		for(int i=0; i<maxFnum; i++){
			arrayNum += (i+1);
		}
		LargeK = new int[arrayNum];
		SmallK = new int[arrayNum];

		int num = 0;
		for(int i=0; i<maxFnum; i++){
			for(int j=0; j<(i+1); j++){
				LargeK[num] = i+1;
				SmallK[num] = j+1;
				num++;
			}
		}
	}
	/************************************************************************************************************/
	//結論部の計算

	//信頼度  （データから直接なので，データが大きいと結構重い処理（O[n]
	public static double[] calcTrust(DataSetInfo dataSetInfo, int[] rule, int Cnum, ForkJoinPool forkJoinPool){

		ArrayList<Double> part = new ArrayList<Double>();

		Optional<Double> partSum = null;
		for(int c=0; c<Cnum; c++){
			final int CLASSNUM = c;
			partSum = null;
			try {
				partSum = forkJoinPool.submit( () ->
					dataSetInfo
					.getPattern().parallelStream()
					.filter(s -> s.getConClass() == CLASSNUM )
					.map( s -> menberMulPure(s, rule) )
					.reduce( (l,r) -> l+r )
					).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}

			part.add( partSum.orElse(0.0) );
		}

		double all = 0.0;
		for (int c = 0; c < part.size(); c++) {
			all += part.get(c);
		}

		double[] trust = new double[Cnum];
		if( all != 0.0 ){
			for (int c=0; c<Cnum; c++){
				trust[c] = part.get(c) / all;
			}
		}

		return trust;
	}

	//結論部クラス
	public static int calcConclusion(double trust[], int Cnum){

		int ans = 0;
		double max = 0.0;
		for (int i = 0; i < Cnum; i++){
			if (max < trust[i]){
				max = trust[i];
				ans = i;
			}
			else if(max == trust[i]){
				ans = -1;
			}
		}

		return ans;
	}

	//ルール重み
	public static double calcCf(int conCla, double trust[], int Cnum){

		double ans = 0.0;
		if(conCla == -1 || trust[conCla] <= 0.5){
			ans = 0;
		}else{
			double sum = 0.0;
			for (int i = 0; i < Cnum; i++){
					sum += trust[i];
			}
			ans = trust[conCla] + trust[conCla] - sum;
		}

		return ans;
	}

	//適合度

	public static double menberMulPure(Pattern line, int rule[]){

		double ans = 1.0;
		int Ndim = rule.length;
		for(int i=0; i<Ndim; i++){
			try{
				ans *= calcMenbership(rule[i], line.getDimValue(i));
			}catch (Exception e) {
				//calcMenbership(rule[i], 0.0);
			}
		}

		return ans;
	}

	//メンバシップ値
	public static double calcMenbership(int num, double x){

		double uuu = 0.0;

		if(num == 0){
			uuu = 1.0;
		}
		else if(num > 0){
			double a = (double)(kk[num]-1) / (double)(KK[num]-1);
			double b = 1.0 / (double)(KK[num]-1);

			uuu = 1.0 - ( Math.abs(x - a) / b );

			if(uuu < 0.0){
				uuu = 0.0;
			}
		}
		else{
			if(num == x){
				uuu = 1.0;
			}else{
				uuu = 0;
			}
		}

		return uuu;
	}

	/************************************************************************************************************/
	//１つのパターンからルール生成

	//HDFS使わない場合
	public static int[] selectSingle(Pattern line, int Ndim, MersenneTwisterFast rnd){

		int rule[] = new int[Ndim];
		boolean isProb = Consts.IS_PROBABILITY_DONT_CARE;
		double dcRate;
		if(isProb){
			dcRate = Consts.DONT_CAlE_RT;
		}
		else{
			dcRate = (double)(((double)Ndim - (double)Consts.ANTECEDENT_LEN)/(double)Ndim);
		}

		double[] membershipValueRulet = new double[Consts.FUZZY_SET_NUM];
		for (int n = 0; n < Ndim; n++) {
			if (rnd.nextDouble() < dcRate) {
				rule[n] = 0;
			}
			else {

				if( line.getDimValue(n) < 0 ){
					rule[n] = (int) line.getDimValue(n);
				}else{
					double sumMembershipValue = 0.0;
					membershipValueRulet[0] = 0.0;
					for (int f = 0; f < Consts.FUZZY_SET_NUM; f++) {
						sumMembershipValue += calcMenbership( f+1, line.getDimValue(n) );
						membershipValueRulet[f] = sumMembershipValue;
					}
					double rr = rnd.nextDouble() * sumMembershipValue;
					for (int f = 0; f < Consts.FUZZY_SET_NUM; f++) {
						if (rr < membershipValueRulet[f]) {
							rule[n] = f + 1;
							break;
						}
					}	//for f
				}

			}
		}	//for n

		//TODO
		//rule = new int[Ndim];
		//rule[0] = 1;
		//rule[1] = 2;

		return rule;
	}

	//ランダムにルール生成
	public static int[] selectRnd(int Ndim, MersenneTwisterFast rnd){

		int rule[] = new int[Ndim];
		boolean isProb = Consts.IS_PROBABILITY_DONT_CARE;
		double dcRate;
		if(isProb){
			dcRate = Consts.DONT_CAlE_RT;
		}
		else{
			dcRate = (double)(((double)Ndim - (double)Consts.ANTECEDENT_LEN)/(double)Ndim);
		}

		for (int n = 0; n < Ndim; n++) {
			if (rnd.nextDouble() < dcRate) {
				rule[n] = 0;
			} else {
				rule[n] = rnd.nextInt(Consts.FUZZY_SET_NUM) + 1;
			}
		}

		return rule;
	}

	/************************************************************************************************************/
	//適応度関数
	public static double fitness(double f1, double f2, double f3){
		return (double)(w[0] *  f1) + (double)(w[1] * f2) + (double)(w[2] * f3);
	}
	/************************************************************************************************************/
}
