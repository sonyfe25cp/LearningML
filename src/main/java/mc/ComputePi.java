package mc;
/**
 * 蒙特卡罗方法计算pi
 * @author Sonyfe25cp
 * 2014-1-15
 */
public class ComputePi {

	public static void main(String[] args) {
		int[] ranges = {100, 1000, 10000, 100000, 1000000, 1000000000};
		for(int range : ranges){
			computePi(range);
		}
	}
	
	public static void  computePi(int range){
		int r = 1;
		int sum = 0;
		for(int i = 0; i < range; i ++){
			double x = Math.random();
			double y = Math.random();
			if(x*x + y*y < r){
				sum ++;
			}
		}
		double pi = (double)(4*sum)/range;
		System.out.println("sum : "+sum);
		System.out.println("total : "+range);
		System.out.println("pi : "+pi);
		System.out.println("distance : "+ (Math.PI-pi));
		System.out.println("******************");
	}

}
