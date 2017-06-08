package methods;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import gbml.Consts;
import gbml.RuleSet;

public class StaticGeneralFunc {

	StaticGeneralFunc(){}

	//******************************************************************************//

	//変数定数出力
	public static String getExperimentSettings(String[] args){

		String allSettings = "";
		String endLine = System.lineSeparator();
		for (int i = 0; i < args.length; i++) {
			allSettings += args[i] + endLine;
		}
		Consts consts = new Consts();
		allSettings += consts.getStaticValues();

		return allSettings;
	}
	//非復元抽出
	public static int [] sampringWithout(int num, int DataSize, MersenneTwisterFast rnd){

		int ans[] = new int[num];
		int i, j, same;

		int[] patternNumber2 = new int[DataSize];
		int generateNumber = num;

		if (DataSize < num){
			for (i = 0; i < DataSize; i++) {
				patternNumber2[i] = i;
			}
			generateNumber = num - DataSize;
		}

	    //非復元抽出
	    for(i=0;i<generateNumber;i++){
	    	same = 0;
	        ans[i] = rnd.nextInt(DataSize);
	        for(j=0;j<i;j++){
	        	if(ans[i] == ans[j]){
	        	   same=1;
	        	}
	        }
	        if(same==1){
	        	i--;
	        }
	    }

	    if(DataSize < num){
	    	int ii = 0;
	    	for(i=generateNumber;i<num;i++){
	    		ans[i] = patternNumber2[ii];
	    		ii++;
	    	}
	    }

	    return ans;
	}

	//非復元抽出
	public static int [] sampringWithout2(int num, int DataSize, MersenneTwisterFast rnd){

		int ans[] = new int[num];

	    //非復元抽出
	    for(int i=0;i<num;i++){
	    	boolean isSame = false;
	        ans[i] = rnd.nextInt(DataSize);
	        for(int j=0; j<i; j++){
	        	if(ans[i] == ans[j]){
	        	   isSame = true;
	        	}
	        }
	        if(isSame){
	        	i--;
	        }
	    }

	    return ans;
	}

	public static int sampringArray(int array[], MersenneTwisterFast rnd){
		return array[rnd.nextInt(array.length)];
	}

	//バイナリトーナメント
	public static int binaryT4(ArrayList<RuleSet> ruleSets, MersenneTwisterFast rnd, int popSize, int objectives){

		int ans = 0;
		int sele1, sele2;

		do{
			sele1 = rnd.nextInt(popSize);
			sele2 = rnd.nextInt(popSize);
			if (objectives == 1) {
				if ( ruleSets.get(sele1).getFitness() < ruleSets.get(sele2).getFitness() ) {
					ans = sele1;
				} else {
					ans = sele2;
				}
			}
			else {
				if ( ruleSets.get(sele1).getRank() > ruleSets.get(sele2).getRank() ) {
					ans = sele2;
				}
				else if ( ruleSets.get(sele1).getRank() < ruleSets.get(sele2).getRank() ) {
					ans = sele1;
				}
				else if ( ruleSets.get(sele1).getRank() == ruleSets.get(sele2).getRank() ) {
					if ( ruleSets.get(sele1).getCrowding() < ruleSets.get(sele2).getCrowding() ) {
						ans = sele2;
					} else {
						ans = sele1;
					}
				}
			}

		} while(ruleSets.get(ans).getRuleNum() == 0);

		return ans;
	}

	public static double distance(RuleSet a, RuleSet b){
		double dis = Math.abs(a.getFitness(1) - b.getFitness(1));
		dis += Math.abs(a.getFitness(0) - b.getFitness(0));
		return dis;
	}

	public static int[] binaryTRand(ArrayList<RuleSet> pitsRules, MersenneTwisterFast rnd, int popSize, int objectives){
		int[] ans = new int[2];
		int sele1, sele2;

		//一個目
		do{
			sele1 = rnd.nextInt(popSize);
			sele2 = rnd.nextInt(popSize);

			if (objectives == 1) {
				if (pitsRules.get(sele1).getFitness() < pitsRules.get(sele2).getFitness()) {
					ans[0] = sele1;
				} else {
					ans[0] = sele2;
				}
			} else {

				if (pitsRules.get(sele1).getRank() > pitsRules.get(sele2).getRank()) {
					ans[0] = sele2;
				}
				else if (pitsRules.get(sele1).getRank() < pitsRules.get(sele2).getRank()) {
					ans[0] = sele1;
				}
				else if (pitsRules.get(sele1).getRank() == pitsRules.get(sele2).getRank()) {

					if (pitsRules.get(sele1).getCrowding() == 100000000){
						ans[0] = sele1;
					}else if (pitsRules.get(sele2).getCrowding() == 100000000){
						ans[0] = sele2;
					}else if (pitsRules.get(sele1).getCrowding() == 0){
						ans[0] = sele2;
					}else if (pitsRules.get(sele2).getCrowding() == 0){
						ans[0] = sele1;
					}else if (rnd.nextBoolean()) {
						ans[0] = sele2;
					} else {
						ans[0] = sele1;
					}
				}
			}

		}while(pitsRules.get(ans[0]).getRuleNum() == 0);


		//二個目
		do{
			sele1 = rnd.nextInt(popSize);
			sele2 = rnd.nextInt(popSize);

			if (objectives == 1) {
				if (pitsRules.get(sele1).getFitness() < pitsRules.get(sele2).getFitness()) {
					ans[1] = sele1;
				} else {
					ans[1] = sele2;
				}
			} else {

				if (pitsRules.get(sele1).getRank() > pitsRules.get(sele2).getRank()) {
					ans[1] = sele2;
				}
				else if (pitsRules.get(sele1).getRank() < pitsRules.get(sele2).getRank()) {
					ans[1] = sele1;
				}
				else if (pitsRules.get(sele1).getRank() == pitsRules.get(sele2).getRank()) {
					if (distance(pitsRules.get(sele1), pitsRules.get(ans[0])) >
						distance(pitsRules.get(sele2), pitsRules.get(ans[0])) )
					{
						ans[1] = sele2;
					} else {
						ans[1] = sele1;
					}
				}
			}

		}while(pitsRules.get(ans[1]).getRuleNum() == 0);

		return ans;
	}

	//marge sort
	public  static void mergeSort(ArrayList<RuleSet> temp, ArrayList<RuleSet> parent, ArrayList<RuleSet> child){

		int parentI = 0;
		int childI = 0;

		int length = child.size() + parent.size();

		for (int k = 0; k < child.size() + length; k++) {
			if(parent.get(parentI).getFitness(0) < child.get(childI).getFitness(0)){
				temp.add(parent.get(parentI));
				parentI++;
			}
			else{
				temp.add(child.get(childI));
				childI++;
			}
		}

	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void shuffle(List<?> list, MersenneTwisterFast rnd) {
		int SHUFFLE_THRESHOLD = 5;
		int size = list.size();
		if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
			for (int i=size; i>1; i--)
				swap(list, i-1, rnd.nextInt(i));
		} else {
			Object arr[] = list.toArray();

            // Shuffle array
			for (int i=size; i>1; i--)
                swap(arr, i-1, rnd.nextInt(i));

			ListIterator it = list.listIterator();
            for (int i=0; i<arr.length; i++) {
                it.next();
                it.set(arr[i]);
            }
        }
    }

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void swap(List<?> list, int i, int j) {
		final List l = list;
		l.set(i, l.set(j, l.get(i)));
	}

	private static void swap(Object[] arr, int i, int j) {
		Object tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

}
