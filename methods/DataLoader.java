package methods;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gbml.DataSetInfo;

public class DataLoader {

	//データ読み込み改

	public static void inputFile(DataSetInfo data, String fileName){

		List<Double[]> lines = new ArrayList<Double[]>();
		try ( Stream<String> line = Files.lines(Paths.get(fileName)) ) {
		    lines =
		    	line.map(s ->{
		    	String[] numbers = s.split(",");
		    	Double[] nums = new Double[numbers.length];

		    	//値が無い場合の例外処理
		    	for (int i = 0; i < nums.length; i++) {
		    		if (numbers[i].matches("^([1-9][0-9]*|0)(.[0-9]+)?$") ){
		    			nums[i] = Double.parseDouble(numbers[i]);
		    		}else{
		    			nums[i] = 0.0;
		    		}
				}
		    	return nums;
		    })
		    .collect( Collectors.toList() );

		} catch (IOException e) {
		    e.printStackTrace();
		}

		//1行目はデータのパラメータ
		data.setDataSize( lines.get(0)[0].intValue() );
		data.setNdim( lines.get(0)[1].intValue() );
		data.setCnum( lines.get(0)[2].intValue() );
		lines.remove(0);

		//2行目以降は属性値（最終列はクラス）
		lines.stream().forEach(data::addPattern);

	}

	public static void inputFileOneLine(DataSetInfo data, String fileName){

		String line = "";
		try{
			File file = new File(fileName);
			BufferedReader br = new BufferedReader( new FileReader(file) );
			line = br.readLine();
			br.close();
		}
		catch(FileNotFoundException e){
		  System.out.println(e);
		}catch(IOException e){
		  System.out.println(e);
		}
		//1行目はデータのパラメータ
		String[] splitLine = line.split(",");
		data.setDataSize( Integer.parseInt(splitLine[0]) );
		data.setNdim( Integer.parseInt(splitLine[1]) );
		data.setCnum( Integer.parseInt(splitLine[2]) );

	}

}


