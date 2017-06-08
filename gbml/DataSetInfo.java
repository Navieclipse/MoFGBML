package gbml;

import java.util.ArrayList;


public class DataSetInfo {

	//コンストラクタ
	public DataSetInfo(){}

	public DataSetInfo(int Datasize, int Ndim, int Cnum){

		this.DataSize = Datasize;
		this.Ndim = Ndim;
		this.Cnum = Cnum;

	}

	public DataSetInfo(int Ndim, int Cnum, int DataSize, ArrayList<Pattern> patterns){

		this.Ndim = Ndim;
		this.Cnum = Cnum;
		this.DataSize = DataSize;

		this.patterns = patterns;

	}

	/******************************************************************************/

	int Ndim;
	int Cnum;
	int DataSize;

	ArrayList<Pattern> patterns = new ArrayList<Pattern>();

	/******************************************************************************/
	//メソッド

	public void setPattern(ArrayList<Pattern> patterns){
		this.patterns = patterns;
	}

	public void addPattern(Double[] pattern){
		patterns.add(new Pattern(pattern));
	}

	public void setNdim(int num){
		Ndim = num;
	}

	public void setCnum(int num){
		Cnum = num;
	}

	public void setDataSize(int num){
		DataSize = num;
	}

	public ArrayList<Pattern> getPattern(){
		return patterns;
	}

	public Pattern getPattern(int index){
		return patterns.get(index);
	}

	public int getNdim(){
		return Ndim;
	}

	public int getCnum(){
		return Cnum;
	}

	public int getDataSize(){
		return DataSize;
	}

}
