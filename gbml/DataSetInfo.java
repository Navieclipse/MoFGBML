package gbml;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class DataSetInfo {

	//コンストラクタ
	public DataSetInfo(){}

	public DataSetInfo(int Datasize, int Ndim, int Cnum, int setting, InetSocketAddress[] serverList){

		this.DataSize = Datasize;
		this.Ndim = Ndim;
		this.Cnum = Cnum;

		this.setting = setting;
		this.serverList = serverList;

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

	int setting = 0;
	InetSocketAddress[] serverList = null;

	ArrayList<Pattern> patterns = new ArrayList<Pattern>();

	/******************************************************************************/
	//メソッド

	public int getSetting(){
		return this.setting;
	}

	public InetSocketAddress[] getServerList(){
		return serverList;
	}

	public void setSetting(int setting, InetSocketAddress[] serverList){
		this.setting = setting;
		this.serverList = serverList;
	}

	public void setPattern(ArrayList<Pattern> patterns){
		this.patterns = patterns;
	}

	public void addPattern(double[] pattern){
		patterns.add(new Pattern(pattern));
	}

	public void addPattern(Pattern pattern){
		patterns.add(pattern);
	}

	public void sortPattern(){
		Collections.sort( this.patterns, new patternComparator() );
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

	public class patternComparator implements Comparator<Pattern> {

	    public int compare(Pattern a, Pattern b) {
	        double no1 = a.getConClass();
	        double no2 = b.getConClass();
	        //昇順でソート
	        if (no1 > no2) {
	            return 1;
	        } else if (no1 == no2) {
	            return 0;
	        } else {
	            return -1;
	        }
	    }

	}

}

