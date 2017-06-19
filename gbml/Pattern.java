package gbml;

import java.io.Serializable;

public class Pattern implements Serializable{

	//コンストラクタ
	Pattern(){}

	public Pattern(double[] pattern){
		int Ndim = pattern.length - 1;
		x = pattern;
		conClass = (int)pattern[Ndim];
	}

	/******************************************************************************/

	double[] x;
	int conClass;

	/******************************************************************************/

	public double getDimValue(int i){
		return x[i];
	}

	public int getConClass(){
		return conClass;
	}

}
