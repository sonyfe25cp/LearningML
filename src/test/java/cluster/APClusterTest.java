package cluster;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

public class APClusterTest extends TestCase{
	public APClusterTest(){
		init();
	}
	ArrayList<Pair> randomPoints; 
	String[] abc = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u"};
	public void init(){
		int number = 20;
		randomPoints  =  new ArrayList<Pair>();
		for(int i =0; i < number; i++){
			double p = Math.random();
			System.out.println(Math.round(p*20));
			double p1 = Math.random();
			System.out.println(Math.round(p1*20));
			int r1=(int) Math.round(p*20);
			int r2=(int) Math.round(p1*20);
			randomPoints.add(new Pair(abc[r1], abc[r2], r1,r2));
		}
	}
	@Test
	public void testAPCluster() throws IOException{
		FileWriter fw = new FileWriter("data.txt");
		for(int i =0;i<randomPoints.size();i++){
			fw.write(i+" "+randomPoints.get(i).toString());
			fw.write("\n");
		}
		fw.flush();
		fw.close();
		FileWriter fw2 = new FileWriter("simi.txt");
		for(int i =0;i<randomPoints.size();i++){
			Pair p1 = randomPoints.get(i);
			for(int j = 0; j < randomPoints.size(); j++){
				
				Pair p2 = randomPoints.get(j);
				double distance = p2.distance(p1);
				double max = Math.sqrt(2*randomPoints.size()*randomPoints.size());
				distance = max - distance;
				if(i == j){
					distance = 0;
				}
				System.out.println(i+" "+j+" "+distance);
				fw2.write(i+" "+j+" "+distance);
				fw2.write("\n");
			}
		}
		fw2.flush();
		fw2.close();
	}
}
class Pair{
	String a;
	String b;
	long x;
	long y;
	
	public Pair(String a, String b, long x, long y) {
		super();
		this.a = a;
		this.b = b;
		this.x = x;
		this.y = y;
	}

	public String toString(){
		return "("+a+","+b+") == ("+x+","+y+")";
	}
	
	public double distance(Pair pair){
		long x1 = pair.x;
		long y1 = pair.y;
		
		return Math.sqrt((x1-x)*(x1-x)+(y1-y)*(y1-y));
	}
}
