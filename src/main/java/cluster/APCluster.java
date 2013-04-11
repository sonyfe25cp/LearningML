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

/*
 * data format:  
 * id id similarity
 * 
 * for example
 * 1001 1002 0.54
 */
public class APCluster {

	private int runs = 10;//how many runs
	private double lambda = 0.5;//damped paramter
	private int N = 0 ;//matrix's cols count
	private APMatrix matrix;
	private List<RawData> rawList;
	
	public APCluster(List<RawData> rawList){
		if(rawList == null){
			System.out.println("please input the rawData");
		}else{
			this.rawList = rawList;
			matrix = new APMatrix(rawList);
			N = matrix.getN();
		}
	}
	public void showMatrix(){
		System.out.println(matrix);
	}
	
	public void train(){
		for(int run = 0; run < runs; run++){
			List<Exemplar> candidates = matrix.getCandidates();
			HashMap<String,Message> responsibilityMessageMapOld = matrix.getResponsibilityMessageMap();
			HashMap<String,Message> availableMessageMapOld = matrix.getAvailableMessageMap();
			for(int i = 0 ; i < N; i++){
				Exemplar maxE = null;
				double maxId = 0;
				for(Exemplar exemplar : candidates){
					
					double responsibility = matrix.computeResponsibilityIK(i, exemplar.getIndex());
					double available = matrix.computeAvailableIK(i, exemplar.getIndex());
					double id = responsibility + available;
					if(id > maxId){
						maxE = exemplar;
					}
					
					double oldResponsibility = responsibilityMessageMapOld.get(APMatrix.keyFormat(i,exemplar.getIndex())).getValue(); 
					responsibility = lambda * oldResponsibility - (1-lambda) * responsibility; 
					
					double oldAvailable = availableMessageMapOld.get(APMatrix.keyFormat(i,exemplar.getIndex())).getValue();
					available = lambda * oldAvailable - (1-lambda) * available;
					
					int k = exemplar.getIndex();
					matrix.putResponsibilityByXY(k, i, responsibility);
					matrix.putAvailableByXY(k, i, available);
					
				}
				if(maxE != null){
					maxE.addCluster(i);//add i to exemplarMK;
				}
			}
			matrix.showCandidates();
		}
	}
	
	public void setRawList(List<RawData> rawList){
		this.rawList = rawList;
	}
	
	public static void main(String[] args){
		String filePath = "src/main/resources/raw_data.txt";
		List<RawData> rawList = RawData.readFromFile(filePath);
		APCluster cluster = new APCluster(rawList);
		cluster.train();
	}
	
}
class Exemplar{
	private int index;
	private double identity;
	private List<Integer> cluster;
	public void addCluster(int i){
		if(cluster==null){
			cluster = new ArrayList<Integer>();
		}
		cluster.add(i);
	}
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("index:"+index+" identity: "+identity+" ");
		if(cluster!=null){
			sb.append("cluster: ");
			for(int i : cluster){
				sb.append(i+" ");
			}
		}
		return sb.toString();
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public double getIdentity() {
		return identity;
	}
	public void setIdentity(double identity) {
		this.identity = identity;
	}
	public List<Integer> getCluster() {
		return cluster;
	}
	public void setCluster(List<Integer> cluster) {
		this.cluster = cluster;
	}
}
class APMatrix{
	private HashMap<String,Double> valueStore;//store the raw data value
	private HashMap<String,Message> responsibilityMessageMap;
	private HashMap<String,Message> availableMessageMap;
	List<Exemplar> candidates;
	private int N = 0 ;//矩阵长度
	private List<RawData> rawList;
	/**
	 * show all candidates
	 * Apr 11, 2013
	 */
	public void showCandidates(){
		System.out.println("--- show candidates ---");
		for(Exemplar exemplar : candidates){
			System.out.println(exemplar);
			System.out.println("-----------------------");
		}
	}
	/**
	 * add responsibility
	 */
	public void putResponsibilityByXY(int x,int y, double responsibility){
		responsibilityMessageMap.put(keyFormat(x, y),new Message(x,y,responsibility));
	}
	public void putAvailableByXY(int x, int y, double available){
		availableMessageMap.put(keyFormat(x, y), new Message(x, y, available));
	}
	/**
	 * prepare the matrix's value and col and row
	 */
	private void buildSuperHash(){
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
		this.setValueStore(valueStore);
		outptMatrixFile(null);
	}
	SuperHash<Integer,Integer> superhash;
	static HashMap<Integer, Integer> tempHash;//make sure the true size of matrix
	/*
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
	private HashMap<Integer,Double> responsibilityMap;
	private HashMap<Integer,Double> availableMap;
	/**
	 * init the <i,j> apnode
	 * no priori, so identify = responsibility
	 * init the responsibility map and available map and exemplars
	 */
	private void initAPMatrix(){
		for(int k = 0 ; k < N ; k ++){
			for(int i = 0 ; i < N ; i ++){
				availableMessageMap.put(keyFormat(i,k), new Message(k,i,0));
			}
		}
		for(int k = 0 ; k < N ; k ++){
			for(int i = 0 ; i < N ; i ++){
				availableMessageMap.put(keyFormat(i,k), new Message(k,i,0));
				double responsibility;
				if(k!=i)
					responsibility = computeResponsibilityIK(i,k);//a(i,k)=0; r(i,k) <- max{a(i,k'),s(i,k');
				else
					responsibility = computeResponsibilityKK(k);//对矩阵的对角线赋上初值，a(i,k)=0; r(i,k) <- max{a(i,k'),s(i,k');
				addResponsibility(k,responsibility);
				responsibilityMessageMap.put(keyFormat(i, k), new Message(i,k,responsibility));
			}
			Exemplar exemplar = new Exemplar();
			exemplar.setIndex(k);
			double cumulatedResponsibility = responsibilityMap.get(k);
			exemplar.setIdentity(cumulatedResponsibility);
			candidates.add(exemplar); // all point is candicate
		}
	}
	/**
	 * responsibility is from i to k
	 */
	private void addResponsibility(int k, double responsibility){
		double before = 0;
		if(responsibilityMap.containsKey(k)){
			before = responsibilityMap.get(k);
		}
		double newValue = before + responsibility;
		responsibilityMap.put(k, newValue);
	}
	private void addAvailable(int i , double available){
		double before = 0;
		if(availableMap.containsKey(i)){
			before = availableMap.get(i);
		}
		double newValue = before + available;
		availableMap.put(i, newValue);
	}
	
	/**
	 * compute the responsibility for s(i,k)
	 */
	public double computeResponsibilityIK(int i, int k){
		Double simIKTemp = valueStore.get(keyFormat(i,k));
		if(simIKTemp == null){
			return 0.0;
		}
		double simIK = simIKTemp.doubleValue();
		
		List<Double> tempForComputeR = new ArrayList<Double>(); 
		for(int tempK = 0 ; tempK < N ; tempK ++){
			if(k != tempK ){
				double avaITempK = availableMessageMap.get(keyFormat(i,tempK)).getValue();
				Double simITempKTemp = valueStore.get(keyFormat(i,tempK));
				double simITempK = (simITempKTemp == null ? 0.0 : simITempKTemp.doubleValue());
				double sum = avaITempK + simITempK ;
				tempForComputeR.add(sum);
			}
		}
		Collections.sort(tempForComputeR);
		double tempSum = tempForComputeR.get(tempForComputeR.size()-1);//get the max one
		
		return simIK - tempSum;
	}
	/**
	 * compute the responsibility for s(i,k)
	 */
	public double computeResponsibilityKK(int k){
		double simKK = valueStore.get(keyFormat(k,k));
		
		double maxOtherSimIK = 0 ;
		for(Exemplar candidate : candidates){
			if(k == candidate.getIndex()){
				continue;
			}
			Double temp = valueStore.get(keyFormat(k,candidate.getIndex()));
			double simTemp = (temp == null ? 0 : temp.doubleValue());
			maxOtherSimIK = max(maxOtherSimIK , simTemp);
		}
		return simKK - maxOtherSimIK;
	}
	/**
	 * compute the available for a(i,k), from k to i
	 */
	public double computeAvailableIK(int i ,int k){
		Double simIKTemp = valueStore.get(keyFormat(i,k));
		if(simIKTemp == null){
			return 0.0;
		}
		
		double resKK = responsibilityMessageMap.get(keyFormat(k,k)).getValue();
		
		double tempSum = 0;
		for(int tempI = 0 ; tempI < N ; tempI ++){
			if(tempI != i && tempI != k){
				double resIK = responsibilityMessageMap.get(keyFormat(tempI,k)).getValue();
				resIK = resIK > 0 ? resIK : 0;
				tempSum += resIK;
			}
		}
		double tempCount = resKK+tempSum;
		return min(0,tempCount);
	}
	/*
	 * compute the avaiable for a(k,k)
	 */
	public double computeAvailableKK(int k){
		double tempSum = 0;
		for(int tempI = 0 ; tempI < N ; tempI ++){
			if(tempI != k){
				double resIK = responsibilityMessageMap.get(keyFormat(tempI,k)).getValue();
				resIK = resIK > 0 ? resIK : 0;
				tempSum += resIK;
			}
		}
		return tempSum;
	}
	/*
	 * return which one is lower
	 */
	private double min(double a, double b){
		return a>b?b:a;
	}
	/*
	 * return which one is higher
	 */
	private double max(double a, double b){
		return a<b?b:a;
	}
	/*
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
	public static String keyFormat(int x, int y){
		return x+"-"+y;
	}
	public double getSimByXY(int x, int y ){
		return valueStore.get(keyFormat(x,y));
	}
	public APMatrix(List<RawData> rawList){
		this.rawList = rawList;
		init();
		buildSuperHash();
		initAPMatrix();
	}
	public void init(){
		valueStore = new HashMap<String,Double>();
		superhash = new SuperHash<Integer,Integer>();
		tempHash = new HashMap<Integer,Integer>();
		candidates = new ArrayList<Exemplar>();
		availableMessageMap = new HashMap<String, Message>();
		responsibilityMessageMap = new HashMap<String, Message>();
		responsibilityMap = new HashMap<Integer, Double>();
		availableMap = new HashMap<Integer, Double>();
	}
	public HashMap<String, Double> getValueStore() {
		return valueStore;
	}
	public void setValueStore(HashMap<String, Double> valueStore) {
		this.valueStore = valueStore;
	}
	public int getN() {
		return N;
	}
	public List<Exemplar> getCandidates() {
		return candidates;
	}
	public void setCandidates(List<Exemplar> candidates) {
		this.candidates = candidates;
	}
	public HashMap<String, Message> getResponsibilityMessageMap() {
		return responsibilityMessageMap;
	}
	public void setResponsibilityMessageMap(
			HashMap<String, Message> responsibilityMessageMap) {
		this.responsibilityMessageMap = responsibilityMessageMap;
	}
	public HashMap<String, Message> getAvailableMessageMap() {
		return availableMessageMap;
	}
	public void setAvailableMessageMap(HashMap<String, Message> availableMessageMap) {
		this.availableMessageMap = availableMessageMap;
	}
}
/**
 * 
 * message between point and candidate exemplar
 * 
 */
class Message{
	private int from;
	private int to;
	private double value;
	public Message(int from, int to, double value){
		this.from = from;
		this.to = to;
		this.value = value;
	}
	public int getFrom() {
		return from;
	}
	public void setFrom(int from) {
		this.from = from;
	}
	public int getTo() {
		return to;
	}
	public void setTo(int to) {
		this.to = to;
	}
	public double getValue() {
		return value;
	}
	public void setValue(double value) {
		this.value = value;
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
