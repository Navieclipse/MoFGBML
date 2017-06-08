package methods;

public class CommandLineFunc {

	public static void lessArgs(String[] args, int argsLen){

		if (args.length < argsLen){

			System.out.println("=======Check args=======");
			System.out.println("0 Calclation Type: int");
			System.out.println("1 DataName: String");
			System.out.println("2 Generations: int");
			System.out.println("3 Population: int");
			System.out.println("4 Objectives: int");
			System.out.println("5 EmoAlgolithm: int (0:NSGAII, 1:WS, 2:TCH, 3:PBI, 4:IPBI, 5:AOF");
			System.out.println("6 CV: int");
			System.out.println("7 Repeat times: int");
			System.out.println("8 Seed: int");
			System.out.println("9 One: boolean");

			System.out.println("=======Case: Single node=======");
			System.out.println("3 DivideNum: int");

			System.out.println("=======Case: Apache Spark=======");
			System.out.println("12 isDitributed: boolean");
			System.out.println("11 PartitionNum	int");
			System.out.println("11 HDFS Folder Name String");
			System.out.println("9 master Node Name :String");
			System.out.println("10 AppName: String");
			System.out.println("10 No. of executor: int");
			System.out.println("10 No. of exes'cores: int");

			System.out.println("=======Case: Simple Socket=======");
			System.out.println("12 isDitributed: boolean");
			System.out.println("11 PartitionNum: int");
			System.out.println("11 HDFS Folder Name: String");
			System.out.println("11 Port's number: int");
			System.out.println("11 Thread's number: int");
			System.out.println("11 Nodes' names: String");

			System.out.println("========================");

			System.exit(-1);

		}
	}

}
