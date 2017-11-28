package methods;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import gbml.Consts;

public class Output {

	Output(){}

	//******************************************************************************//

	//データファイル名作成
	public static void makeFileName(String dataName, String traFiles[][], String tstFiles[][]){

		for(int rep_i=0; rep_i<traFiles.length; rep_i++){
			for(int cv_i=0; cv_i<traFiles[0].length; cv_i++){
				 traFiles[rep_i][cv_i] = makeFileNameOne(dataName, cv_i, rep_i, true);
				 tstFiles[rep_i][cv_i] = makeFileNameOne(dataName, cv_i, rep_i, false);
			}
		}

	}

	public static String makeFileNameOne(String dataName, int cv_i, int rep_i, boolean isTra){

		String fileName = "";
		if(isTra){
			fileName = dataName + "/a" + Integer.toString(rep_i) + "_" + Integer.toString(cv_i) + "_" +dataName + "-10tra.dat";
		}else{
			fileName = dataName + "/a" + Integer.toString(rep_i) + "_" + Integer.toString(cv_i) + "_" +dataName + "-10tst.dat";
		}
		return fileName;

	}

	public static String makeFileNameOne(String dataName, String location, int cv_i, int rep_i, boolean isTra){

		String fileName = "";
		if(isTra){
			fileName = location + dataName + "/a" + Integer.toString(rep_i) + "_" + Integer.toString(cv_i) + "_" +dataName + "-10tra.dat";
		}else{
			fileName = location + dataName + "/a" + Integer.toString(rep_i) + "_" + Integer.toString(cv_i) + "_" +dataName + "-10tst.dat";
		}

		return fileName;

	}

	//単一用
	public static void writeln(String fileName, String st){

		try {
			FileWriter fw = new FileWriter(fileName, true);
			PrintWriter pw = new PrintWriter( new BufferedWriter(fw) );
			pw.println(st);
			pw.close();
	    }
		catch (IOException ex){
			ex.printStackTrace();
	    }
	}

	public static void writeln(String fileName, double st){

		try {
			FileWriter fw = new FileWriter(fileName, true);
			PrintWriter pw = new PrintWriter( new BufferedWriter(fw) );
			pw.println(st);
			pw.close();
	    }
		catch (IOException ex){
			ex.printStackTrace();
	    }
	}

	//配列用
	public static void writeln(String fileName, String array[]){

		try {
			FileWriter fw = new FileWriter(fileName, true);
			PrintWriter pw = new PrintWriter( new BufferedWriter(fw) );
			for(int i=0; i<array.length; i++){
				 pw.println(array[i]);
			}
			pw.close();
	    }
		catch (IOException ex){
			ex.printStackTrace();
	    }
	}

	//数値配列用
	public static void writeln(String fileName, Double array[]){

		try {
			FileWriter fw = new FileWriter(fileName, true);
			PrintWriter pw = new PrintWriter( new BufferedWriter(fw) );
			for(int i=0; i<array.length; i++){
				 pw.println(array[i]);
			}
			pw.close();
	    }
		catch (IOException ex){
			ex.printStackTrace();
	    }
	}

	//パラメータ出力用
	public static void writeSetting(String name ,String dir ,String st){

		String sep = File.separator;
		String fileName = dir + sep + name + ".txt";

		writeln(fileName, st);
	}

	public static String makeDirName(String dataname, int executors, int exeCores, int preDiv, int seed){

		String path = "";
		String sep = File.separator;
		path = System.getProperty("user.dir");
		path += sep + Consts.ROOTFOLDER +"_"+ dataname + "_e" + executors + "_c" + exeCores + "_p" + preDiv + "_" + seed;

		return path;
	}

	public static String makeDir(String dataname, int executors, int exeCores, int preDiv, int seed){

		String path = "";
		String sep = File.separator;
		path = System.getProperty("user.dir");
		path += sep + Consts.ROOTFOLDER +"_"+ dataname + "_e" + executors + "_c" + exeCores + "_p" + preDiv + "_" + seed;
		File newdir = new File(path);
		newdir.mkdir();

		return path;
	}

	public static void makeDirRule(String dir){

		String sep = File.separator;
		String path;
		path = dir + sep + Consts.RULESET;
		File newdir = new File(path);
		newdir.mkdir();

		path = dir + sep + Consts.VECSET;
		File newdir2 = new File(path);
		newdir2.mkdir();

		path = dir + sep + Consts.SOLUTION;
		File newdir3 = new File(path);
		newdir3.mkdir();

		path = dir + sep + Consts.OTHERS;
		File newdir4 = new File(path);
		newdir4.mkdir();
	}

}
