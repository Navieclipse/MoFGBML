package methods;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import gbml.DataSetInfo;
import gbml.Pattern;
import gbml.RuleSet;

public class Democracy {

	/*********************************/
	//コンストラクタ
	public Democracy(RuleSet[] bests, int classNum){
		this.bests = bests;
		this.classNum = classNum;
	}

	/*********************************/
	//メンバ変数
	RuleSet[] bests;
	int classNum;

	/*********************************/
	//メンバ関数

	//全ての島のベストの識別器で多数決
	//結論部クラス×ルール重みの和　の最も大きいクラスが最終的な結論クラス
	//それでも決まらない場合は，学習用の誤識別率の合計がが最も低いものを採用
	//それでも決まらない場合は識別不能
	public double calcDemocraticRate(DataSetInfo testDataInfo, ForkJoinPool forkJoinPool){
		double MissPatNum = 0.0;
		try{
			MissPatNum = forkJoinPool.submit( () ->
				(int)testDataInfo.getPattern().parallelStream()
				.filter( line -> calcDemoWinClass(line) != line.getConClass() )
				.count()
				).get();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return MissPatNum;
	}

	int calcDemoWinClass(Pattern line){
		//各島のbest識別器の結論部クラス
		int[] winClasses = new int[bests.length];
		for(int i=0; i<bests.length; i++){
			winClasses[i] = bests[i].calcWinClassPal(line);
		}
		//結論部クラス×学習用誤識別率(各島での部分データへの学習用誤識別率）
		double[] winClassMulRate = new double[classNum];
		for(int i=0; i<winClasses.length; i++){
			if(winClasses[i] != -1){
				winClassMulRate[ winClasses[i] ] += (100.0 - bests[i].getMissRate());
			}
		}
		//最も得票数の多いクラス(合計識別率の大きい)を最終的な結論部にする.
		int winClass = -1;
		double maxMul = 0.0;
		for(int i=0; i<winClassMulRate.length; i++){
			if(winClassMulRate[i] > maxMul){
				maxMul = winClassMulRate[i];
				winClass = i;
			}
		}
		//識別不能の場合は-1を返す,それ以外は0-(クラス数-1)の整数値を返す.
		return winClass;
	}
	/*********************************/
}
