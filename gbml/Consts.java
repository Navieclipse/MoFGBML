package gbml;

import java.lang.reflect.Field;

public class Consts {

	//experiment's parameters
	public static final boolean IS_NOT_EQUAL_DIVIDE_NUM = false;	//部分個体群とデータ分割数を一緒にしない

	public static final boolean IS_ISLAND_TIME = true;	//サーバ１台のときに各島の時間もはかる．

	//Parallel parameters
	public static final boolean IS_RULESETS_SORT = false; //評価するときにルール数でソートする
	public static final boolean IS_RULE_PARALLEL = true; //ルールで並列化する（データのパターンでなく）

	//GBML's parameters
    public static final int ANTECEDENT_LEN  = 5;	//ドントケアにしない条件部の数
    public static final double DONT_CAlE_RT = 0.8;	//どんとケア適応確率（合わせ用）
    public static final boolean IS_PROBABILITY_DONT_CARE = false;	//ドントケアを確率で行う
    public static final boolean DO_ADD_RULES = false;	//ミシガン操作時にルールを追加する（置き換えでなく）
    public static final boolean IS_ES_UPDATE = false;	//ES型

    public static final int FUZZY_SET_NUM  = 14;	//ファジィ集合数
    public static final int MAX_FUZZY_DIVIDE_NUM = 5;	//条件部の分割数の最大値

    //TODO
    public static final int INITIATION_RULE_NUM = 30;	//初期ルール数

    public static final int MAX_RULE_NUM  = 60;	//最大ルール数
    public static final int MIN_RULE_NUM = 1;	//最小ルール数
    public static final boolean DO_HEURISTIC_GENERATION = true;	//ヒューリスティック生成法

    //TODO  0.9
    public static final double RULESET_CROSS_RT = 0.9;	//ピッツバーグ交叉確率

    public static final double RULE_CROSS_RT = 0.9;	//ミシガン交叉確率

    //TODO  0.5
    public static final double RULE_OPE_RT = 0.5;	//ミシガン適用確率

    public static final double RULE_CHANGE_RT = 0.2;	//ルール入れ替え割合

	public static final boolean DO_LOG_PER_LOG = false;	//ログでログを出力．

    //MOEAD's parameters
    public static final int VECTOR_DIVIDE_NUM  = 59;	//分割数 //2:209,3:19
    public static final double MOEAD_ALPHA = 0.9;	//参照点のやつ
    public static final double MOEAD_THETA = 5.0;	//シータ
    public static final boolean IS_NEIGHBOR_SIZE = false;	//近傍サイズ 0:個数指定, 1:パーセント指定
    public static final int NEIGHBOR_SIZE_RT = 10;	//近傍サイズ％
    public static final int SELECTION_NEIGHBOR_NUM  = 21;	//選択近傍サイズ
    public static final int UPDATE_NEIGHBOR_NUM  = 1;	//更新近傍サイズ

    public static final int WS  = 1;	//weighted sum
    public static final int TCHEBY = 2;	//Tchebycheff
    public static final int PBI = 3;	//PBI
    public static final int IPBI = 4;	//InvertedPBI
    public static final int SSF = 5;	//special scalarizing function.

    public static final int SECOND_OBJECTIVE_TYPE = 0;	//2目的め 0:rule, 1:length, 2:rule * length, 4:length/rule
    public static final boolean DO_NORMALIZE = false;	//正規化するかどうか
    public static final boolean IS_BIAS_VECTOR = false;	//false: NObiasVector, 1: biasVector

    public static final double IS_FIRST_IDEAL_DOWN = 0.0;	//１目的目のみ下に動かす．（やらない場合は０に）
    public static final boolean IS_WS_FROM_NADIA = false;	//WSをナディアポイントから

    //NSGA2's parameters
    public static final int NSGA2 = 0;	//NSGA2の番号
    public static final int OBJECTIVE_DEGREES = 0;	//目的関数の回転度
    public static final boolean DO_CD_NORMALIZE = false;	//Crowding distanceを正規化する
    public static final boolean HAS_PARENT = false;

    //Other parameters
    public static final int PER_SHOW_GENERATION_NUM  = 100;	//表示する世代
    public static final int WAIT_SECOND = 300000;
    public static final int TIME_OUT_TIME = 30000;
    public static final int SLEEP_TIME = 1000;

    //One objective weights
    public static final int W1 = 100;
    public static final int W2  = 1;
    public static final int W3 = 1;

    public static final int TRAIN = 0;	//学習用
    public static final int TEST = 1;	//評価用

    //Folders' name
    public static final String ROOTFOLDER = "result";
    public static final String RULESET = "ruleset";
    public static final String SOLUTION = "solution";
    public static final String VECSET = "vecset";
    public static final String OTHERS = "write";

    //OS
    public static final int WINDOWS = 0;	//windows
    public static final int UNIX = 1;	//unix

    public String getStaticValues(){

    	StringBuilder sb = new StringBuilder();
    	String sep = System.lineSeparator();
        sb.append("Class: " + this.getClass().getCanonicalName() + sep);
        sb.append("Settings:" + sep);
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                sb.append(field.getName() + " = " + field.get(this) + sep);
            } catch (IllegalAccessException e) {
                sb.append(field.getName() + " = " + "access denied" + sep);
            }
        }

    	return sb.toString();
    }

}
