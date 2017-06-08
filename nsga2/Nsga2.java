package nsga2;

import static java.util.Comparator.*;

import java.util.ArrayList;

import gbml.Consts;
import gbml.PopulationManager;
import gbml.RuleSet;
import methods.MersenneTwisterFast;

public class Nsga2 {

	public Nsga2(int objectives, MersenneTwisterFast rand){
		this.objectiveNum = objectives;
		this.rnd = rand;
	}

	//メンバ
	int objectiveNum;

	MersenneTwisterFast rnd;

	ArrayList<RuleSet> rankedList = new ArrayList<RuleSet>();

	//メソッド

	public void populationUpdate(PopulationManager popManaer) {

		popManaer.margeRuleSets.clear();
		int popSize =popManaer.currentRuleSets.size();

		popManaer.margeRuleSets.addAll(popManaer.currentRuleSets);
		popManaer.margeRuleSets.addAll(popManaer.newRuleSets);
		popManaer.currentRuleSets.clear();
		popManaer.newRuleSets.clear();

		//ランクとCD計算
		calcRank(popManaer.margeRuleSets);

		//ランクとCDでソート
		popManaer.margeRuleSets.sort(comparing(RuleSet::getRank).reversed() //打ち消しのリバース
									.thenComparing(RuleSet::getCrowding).reversed());

		//次世代に個体を格納
		for (int i = 0; i < popSize; i++) {
			popManaer.currentRuleSets.add(popManaer.margeRuleSets.get(i));
		}

	}

	public void calcRank(ArrayList<RuleSet> ruleSets) {

		rankedList.clear();
		int[] n_i = new int[ruleSets.size()];
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] S_i = new ArrayList[ruleSets.size()];
		ArrayList<Integer> F_i = new ArrayList<Integer>();

		for (int p = 0; p < ruleSets.size(); p++) {
			n_i[p] = 0;
			S_i[p] = new ArrayList<Integer>();

			for (int q = 0; q < ruleSets.size(); q++) {
				if (p != q) {
					if (isDominate(p, q, ruleSets)) {
						S_i[p].add(q);
					} else if (isDominate(q, p, ruleSets)) {
						n_i[p]++;
					}
				}
			}
			if (n_i[p] == 0) {
				ruleSets.get(p).setRank(0);
				rankedList.add(ruleSets.get(p));
				F_i.add(p);
			}
		}

		int size = ruleSets.size();
		double FirstMax = 0;
		double FirstMin = 100;
		boolean isNormalize = Consts.DO_CD_NORMALIZE;
		ArrayList<Double> firstObj = new ArrayList<Double>();
		ArrayList<Double> nowFirst = new ArrayList<Double>();
		if(isNormalize){
			for(int i=0; i<size; i++){
				nowFirst.add(ruleSets.get(i).getFitness(0));
				if(FirstMax < nowFirst.get(i)){
					FirstMax = nowFirst.get(i);
				}
				else if(FirstMin > nowFirst.get(i)){
					FirstMin = nowFirst.get(i);
				}
			}
			for(int i=0; i<size; i++){
				double afterFirst = ((nowFirst.get(i)-FirstMin)/(FirstMax-FirstMin)) * 99;
				double log10First = Math.log10(afterFirst + 1);
				firstObj.add(log10First);
			}

		}else{
			for(int i=0; i<size; i++){
				firstObj.add(ruleSets.get(i).getFitness(0));
			}
		}

		for(int i=0; i<size; i++){
			ruleSets.get(i).setFirstObj(firstObj.get(i));
		}

		int i = 0;
		calcDistance(rankedList);
		rankedList.clear();
		ArrayList<Integer> Q = new ArrayList<Integer>();
		while (F_i.size() != 0) {
			for (int p = 0; p < F_i.size(); p++) {
				for (int q = 0; q < S_i[F_i.get(p)].size(); q++) {
					n_i[S_i[F_i.get(p)].get(q)] -= 1;
					if (n_i[S_i[F_i.get(p)].get(q)] == 0) {

						ruleSets.get( S_i[F_i.get(p)].get(q) ).setRank(i + 1);
						Q.add(S_i[F_i.get(p)].get(q));
						rankedList.add(ruleSets.get( S_i[F_i.get(p)].get(q) ));

					}
				}
			}
			if (rankedList.size() != 0)
				calcDistance(rankedList);
			rankedList.clear();
			i++;
			F_i.clear();
			for (int k = 0; k < Q.size(); k++) {
				F_i.add(Q.get(k));
			}
			Q.clear();
		}
	}

	boolean isDominate(int p, int q, ArrayList<RuleSet> ruleSets) {
		// minimize, fitness[0] is maximize
		// if p dominate q then true else false
		boolean ans = false;
		int i = 1;
		for (int o = 0; o < objectiveNum; o++) {
			if (i * ruleSets.get(p).getFitness(o) > i * ruleSets.get(q).getFitness(o)) {
				ans = false;
				break;
			}
			else if (i * ruleSets.get(p).getFitness(o) < i * ruleSets.get(q).getFitness(o)) {
				ans = true;
			}
		}
		return ans;
	}

	void calcDistance(ArrayList<RuleSet> ruleSets) {
		int size = ruleSets.size();
		for (int i = 0; i < size; i++) {
			ruleSets.get(i).setCrowding(0);
		}

		for (int o = 0; o < objectiveNum; o++) {

			for (int i = 1; i < size; i++) {
				for (int j = i; j >= 1
						&& ruleSets.get(j - 1).getFirstObj(o) > ruleSets.get(j)
								.getFirstObj(o); j--) {
					RuleSet tmp = ruleSets.get(j);
					ruleSets.set(j, ruleSets.get(j - 1));
					ruleSets.set(j - 1, tmp);
				}
			}

			ruleSets.get(0).setCrowding(Double.POSITIVE_INFINITY);
			//２目的のときにあると，Infinity増える
			//list.get(size - 1).SetCrowding(Double.POSITIVE_INFINITY);

			double min = ruleSets.get(0).getFirstObj(o);
			double max = ruleSets.get(size - 1).getFirstObj(o);
			double maxmin = max - min;
			if (maxmin == 0) {
				for (int i = 1; i < size - 1; i++) {
					double distance = 0;
					ruleSets.get(i).setCrowding(
							ruleSets.get(i).getCrowding() + distance);
				}
			} else {
				for (int i = 1; i < size - 1; i++) {
					double distance = (Math.abs(ruleSets.get(i + 1).getFirstObj(o)
							- ruleSets.get(i - 1).getFirstObj(o)) / maxmin);
					ruleSets.get(i).setCrowding(
							ruleSets.get(i).getCrowding() + distance);
				}
			}
		}

	}

}
