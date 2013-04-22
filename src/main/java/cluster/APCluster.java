package cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import edu.bit.dlde.math.BaseMatrix;
import edu.bit.dlde.math.MatrixCompute;
import edu.bit.dlde.math.VectorCompute;

/*
 * data format:  
 * id id similarity
 * 
 * for example
 * 1001 1002 0.54
 */
public class APCluster {

	private int runs = 100 ;//how many runs
	private double lambda = 0.5;//damped paramter
	private int N = 0 ;//matrix's cols count
	private double LOWENOUGH = 0.0000000001;
	
	public APCluster(){
		tempHash = new HashMap<Integer, Integer>();
		valueStore = new HashMap<String,Double>();
		superhash = new SuperHash<Integer,Integer>();
	}
	
	private BaseMatrix identityMatrix;// i(x,y) in
	private BaseMatrix responsibilityMatrix; // r(x,y) responsibility from x to y
	private BaseMatrix availableMatrix; // a(x,y) available from x to y
	private BaseMatrix similarityMatrix; //s(x,y) means similarity between x and y 
	
	private void init(){
		
//		loadData(rawList);//init N and store the raw value
		
		if(N == 0){
			System.out.println("error, please input the similarity matrix or load raw data");
		}
		
		clusterResults = new HashMap<Integer,List<Integer>>();
		availableMatrix = new BaseMatrix(N,N);
		responsibilityMatrix = new BaseMatrix(N,N);
		similarityMatrix = new BaseMatrix(N,N);
		identityMatrix = new BaseMatrix(N,N);

		initSimilarityMatrix();
		initResponsibilityMatrix();
		
	}
	SuperHash<Integer,Integer> superhash;
	private HashMap<String,Double> valueStore;//store the raw data value
	private HashMap<Integer, Integer> tempHash;//make sure the true size of matrix
	/**
	 * 粗粒度数据，a b 0.1
	 * 需加工才可用
	 * @param rawList
	 * Apr 17, 2013
	 */
	private void loadData(List<RawData> rawList){
		//init N
		List<Double> simList = new ArrayList<Double>();
		for(RawData data : rawList){
			int a = data.getA();
			int b = data.getB();
			int indexA = prepareMatrix(a);
			int indexB = prepareMatrix(b);
			valueStore.put(keyFormat(indexA, indexB), data.getSim());
			valueStore.put(keyFormat(indexB, indexA), data.getSim());
			superhash.put(a, indexA);
			superhash.put(b, indexB);
			simList.add(data.getSim());//for init s(k,k)
		}
		Collections.sort(simList);
		double medianForSimKK = simList.get(simList.size()/2);
		System.out.println("initlize s(k,k): "+ medianForSimKK);
		System.out.println("N: "+N);
		for(int i = 0 ; i < N ; i ++){
			valueStore.put(keyFormat(i,i), medianForSimKK);
		}
		outptMatrixFile(null);
	}
	/**
	 * 细粒度数据  1 2 0.4
	 * 直接可用
	 * @param rawList
	 * Apr 17, 2013
	 */
	private void loadSimilarity(List<RawData> rawList){
		//init N
		List<Double> simList = new ArrayList<Double>();
		for(RawData data : rawList){
			int a = data.getA();
			int b = data.getB();
			valueStore.put(keyFormat(a, b), data.getSim());
			valueStore.put(keyFormat(b, a), data.getSim());
			simList.add(data.getSim());//for init s(k,k)
		}
		Collections.sort(simList);
		double medianForSimKK = simList.get(simList.size()/2);
		System.out.println("initlize s(k,k): "+ medianForSimKK);
		N = (int) Math.sqrt(simList.size());
		System.out.println("N: "+N);
		for(int i = 0 ; i < N ; i ++){
			valueStore.put(keyFormat(i,i), medianForSimKK);
		}
	}
	/**
	 * bakup the matrix col index and real value
	 */
	private void outptMatrixFile(String filePath){// just output the number map
		filePath = (filePath == null || filePath.length() == 0) ? "matrix.numbermap" : filePath; //make sure the filepath is ok
		FileWriter fw;
		try {
			fw = new FileWriter(new File(filePath));
			for(Entry<Integer,Integer> entry : tempHash.entrySet()){
				fw.write(entry.getKey()+" "+entry.getValue()+"\n");
			}
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * prepare the matrix's index
	 */
	private int prepareMatrix(int a){
		int index = 0;
		if(tempHash.get(a) == null){
			tempHash.put(a, N);
			index = N;
			N ++ ;
		}
		return index;
	}
	public static String keyFormat(int x, int y){
		return x+"-"+y;
	}
	private void initSimilarityMatrix(){
		for(int row = 0; row < N; row ++){
			for(int col = 0 ; col < N ; col ++){
				Double sim = valueStore.get(keyFormat(row,col));
				double value = (sim == null )? 0 : sim.doubleValue();
				similarityMatrix.setValue(row, col, value);
			}
		}
	}
	private void initResponsibilityMatrix(){
		
	}
	
	private double getR_I_K(BaseMatrix available, int i ,int k){// max{a(i,k')+s(i,k')}
		double[] row = available.getRow(i);
		
		double[] srow = similarityMatrix.getRow(i);
		
		double[] res = VectorCompute.plus(row, srow); 
		
		double[] a_i_k_except = VectorCompute.except(res, k);
		
		double max =  VectorCompute.max(a_i_k_except);
		
		double s_i_k = similarityMatrix.getValue(i, k);
		
		double r_i_k = s_i_k - max;
		
		return r_i_k;
	}
	private double getA_I_K(BaseMatrix responsibility, int i , int k ){
		double[] r_k_k = responsibility.getRow(k);
		double[] r_i_except_k_except = VectorCompute.except(r_k_k, new int[]{i,k});
		double sum = 0.0;
		for(double r : r_i_except_k_except){
			double max = max(0,r);
			sum = sum + max;
		}
		double res = responsibility.getValue(k, k) + sum;
		double min = min(0,res);
		return min;
	}
	private double getA_K_K(BaseMatrix responsibility, int k ){
		double[] r_k_k = responsibility.getRow(k);
		double[] r_i_except_k = VectorCompute.except(r_k_k,k);
		double sum = 0.0;
		for(double r : r_i_except_k){
			double max = max(0,r);
			sum = sum + max;
		}
		return sum;
	}
	private double max(double a, double b){
		return a>b?a:b;
	}
	private double min(double a, double b){
		return a<b?a:b;
	}
	public void showInit(){
		System.out.println("-----similarityMatrix-----");
		System.out.println(similarityMatrix.toString());
		System.out.println(similarityMatrix.toMatlab());
		System.out.println("-----responsibilityMatrix-----");
		System.out.println(responsibilityMatrix.toString());
		System.out.println("-----availableMatrix-----");
		System.out.println(availableMatrix.toString());
		System.out.println("-----identityMatrix-----");
		System.out.println(identityMatrix.toString());
	}
	public void train(){
		for(int run = 0; run < runs; run ++){
//			System.out.println("-----The "+run+" run-----");
			BaseMatrix responsibilityTempMatrix = responsibilityMatrix.clone();
			BaseMatrix availableTempMatrix = availableMatrix.clone();
			/*
			 * compute responsibility
			 */
			for(int row = 0; row < responsibilityMatrix.getRows(); row ++){
				for(int col = 0; col < responsibilityMatrix.getCols(); col ++){
					double r_i_k = getR_I_K(availableTempMatrix, row, col);
					responsibilityMatrix.setValue(row, col, r_i_k);
				}
			}
			responsibilityMatrix = MatrixCompute.plus(responsibilityMatrix.dot(1- lambda), responsibilityTempMatrix.dot(lambda));
			/*
			 * compute available
			 */
			for(int row = 0 ; row < availableMatrix.getRows(); row ++){
				for(int col = 0; col < availableMatrix.getCols(); col ++){
					if(row == col){
						double a_k_k = getA_K_K(responsibilityMatrix,row);
						availableMatrix.setValue(row, col, a_k_k);
					}else{
						double a_i_k = getA_I_K(responsibilityMatrix, row, col);
						availableMatrix.setValue(row, col, a_i_k);
					}
				}
			}
			/*
			 * update matrix
			 */
			availableMatrix = MatrixCompute.plus(availableMatrix.dot(1- lambda), availableTempMatrix.dot(lambda));
			identityMatrix = MatrixCompute.plus(responsibilityMatrix, availableMatrix);
		}
		findIdentity();
	}
	
	/**
	 * debug用，查看收敛结果
	 * @param flag
	 * Apr 17, 2013
	 */
	private void showResults(boolean flag){
		System.out.println("-----results-----");
		System.out.println("-----responsibilityMatrix final-----");
		System.out.println(responsibilityMatrix.toString());
		System.out.println("-----availableMatrix final-----");
		System.out.println(availableMatrix.toString());
		System.out.println("-----identityMatrix final-----");
		System.out.println(identityMatrix.toString());
		showCluster(flag);
	}
	
	/**
	 * 打印出来聚类结果
	 * @param flag
	 * Apr 17, 2013
	 */
	private void showCluster(boolean flag){
		for(Entry<Integer,List<Integer>> entry: clusterResults.entrySet()){
			int center = flag ? superhash.iget(entry.getKey()) : entry.getKey();
			System.out.print("center: "+center);
			System.out.print(" node: ");
			List<Integer> array = entry.getValue();
			for(int point : array){
				int p = flag ? superhash.iget(point) : point;
				System.out.print(p +" , ");
			}
			System.out.println();
		}
	}
	private HashMap<Integer,List<Integer>> clusterResults;
	/**
	 * 从identityMatrix中找出聚类结果
	 * Apr 17, 2013
	 */
	private void findIdentity(){
		List<Integer> exes = new ArrayList<Integer>();//存放聚类中心点
		for(int row = 0 ; row < identityMatrix.getRows(); row ++){
			double idValue = identityMatrix.getValue(row,row);
			if(idValue > LOWENOUGH){
				exes.add(row);
				System.out.println(row+" identity: "+idValue);
			}
		}
		/*
		 * 找出各个点的聚类中心点
		 */
		HashMap<Integer,Integer> temp = new HashMap<Integer,Integer>();
		for(int row = 0 ; row < identityMatrix.getRows(); row ++){
			double max = Integer.MIN_VALUE;
			int exeTrue = 0;
			for(int exeTemp : exes){
				double current = identityMatrix.getValue(row, exeTemp);
				if(current - max > 0){
					max = current;
					exeTrue = exeTemp;
				}
			}
			System.out.println(row+" -> "+exeTrue);
			temp.put(row, exeTrue);
		}
		for(Entry<Integer,Integer> entry:temp.entrySet()){
			int point = entry.getKey();
			int exe = entry.getValue();
			List points = null;
			if(clusterResults.containsKey(exe)){
				points = clusterResults.get(exe);
			}else{
				points = new ArrayList<Integer>();
			}
			points.add(point);
			clusterResults.put(exe, points);
		}
	}
	public int getRuns() {
		return runs;
	}
	public void setRuns(int runs) {
		this.runs = runs;
	}
	public static void main(String[] args){
//		String filePath = "src/main/resources/raw_data_big.txt";
		String filePath = "simi.txt";
		List<RawData> rawList = RawData.readFromFile(filePath);
		APCluster cluster = new APCluster();
		cluster.loadSimilarity(rawList);
//		cluster.loadData(rawList);
		cluster.init();
		cluster.setRuns(10);
		cluster.showInit();
		cluster.train();
		cluster.showResults(false);
	}
	
}
/*
 * has 1 hashmap
 */
class SuperHash<T,Y>{
	private HashMap<T,Y> hashMap;
	private HashMap<Y,T> mapHash;
	
	public SuperHash(){
		hashMap = new HashMap<T,Y>();
		mapHash = new HashMap<Y,T>();
	}
	
	public void put(T t,Y y){
		hashMap.put(t, y);
		mapHash.put(y, t);
	}
	/*
	 * get value from superhash by {key} id
	 */
	public Y get(T t){
		return hashMap.get(t);
	}
	/*
	 * get value from superhash by {value} id
	 */
	public T iget(Y y){
		return mapHash.get(y);
	}
	
	public void clean(){
		hashMap = null;
		mapHash = null;
		hashMap = new HashMap<T,Y>();
		mapHash = new HashMap<Y,T>();
	}
	
}
class RawData{
	private int a;
	private int b;
	private double sim;
	public int getA() {
		return a;
	}
	public void setA(int a) {
		this.a = a;
	}
	public int getB() {
		return b;
	}
	public void setB(int b) {
		this.b = b;
	}
	public double getSim() {
		return sim;
	}
	public void setSim(double sim) {
		this.sim = sim;
	}
	public String toString(){
		return a+" "+b+" "+sim;
	}
	public static List<RawData> readFromFile(String filePath){
		List<RawData> rawList = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			String line;
			RawData data;
			while((line = br.readLine()) !=null){
				if(rawList == null){
					rawList = new ArrayList<RawData>();
				}
				String[] temp = line.split(" ");
				data = new RawData();
				data.setA(Integer.parseInt(temp[0]));
				data.setB(Integer.parseInt(temp[1]));
				data.setSim(Double.parseDouble(temp[2]));
				rawList.add(data);
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println(new File(filePath).getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rawList;
	}
}
