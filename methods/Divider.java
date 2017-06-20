package methods;

import gbml.DataSetInfo;

public class Divider {

	static MersenneTwisterFast uniqueRnd;
	static int dpop;

	public Divider(MersenneTwisterFast rnd , int dpop){
		Divider.uniqueRnd = new MersenneTwisterFast( rnd.nextInt() );
		Divider.dpop = dpop;
	}

	public Divider(int dpop){
		Divider.dpop = dpop;
	}

	public DataSetInfo[] letsDivide(DataSetInfo dataSetInfo) {

		int partitionNum = dpop;
		int classNum = dataSetInfo.getCnum();
		int dataSize = dataSetInfo.getDataSize();

		//ここから
		//各クラスのサイズ
		int[] eachClassSize = new int[classNum];
		for (int i = 0; i < dataSize; i++) {
			eachClassSize[dataSetInfo.getPattern(i).getConClass()]++;
		}

		//それぞれの分割データセットにおける各クラスのパターンの数
		int[][] classDividedSize = new int[classNum][partitionNum];
		int remainAddPoint = 0;
		for (int c = 0; c < classNum; c++) {
			for (int i = 0; i < partitionNum; i++) {
				classDividedSize[c][i] = eachClassSize[c] / partitionNum;
			}
			int remain = eachClassSize[c] % partitionNum;
			for (int i = 0; i < remain; i++) {
				int point = remainAddPoint % partitionNum;
				classDividedSize[c][point]++;
				remainAddPoint++;
			}
		}

		//それぞれの分割データセットの大きさ
		int[] eachDataSize = new int[partitionNum];
		for (int c = 0; c < classNum; c++) {
			for (int i = 0; i < partitionNum; i++) {
				eachDataSize[i] += classDividedSize[c][i];
			}
		}

		//それぞれの分割データにクラスごとにデータを割当てていく
		DataSetInfo[] divideDatas = new DataSetInfo[partitionNum+1];
		for(int d=0; d<partitionNum; d++){
			divideDatas[d] = new DataSetInfo(eachDataSize[d], dataSetInfo.getNdim(), classNum);
		}

		//一番うしろはデータ全体
		divideDatas[partitionNum] = dataSetInfo;

		//まずクラスでソート
		dataSetInfo.sortPattern();

		//各クラスごとに順番に各データに格納（シャローコピー）
		int index = 0;
		for(int c=0; c<classNum; c++){
			for(int d=0; d<partitionNum; d++){
				for(int p=0; p<classDividedSize[c][d]; p++){
					divideDatas[d].addPattern( dataSetInfo.getPattern(index++) );
				}
			}
		}

		return divideDatas;
	}



}

