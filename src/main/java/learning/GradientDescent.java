package learning;

/**
 * @author ChenJie
 * 123            250
 * 150            320
 * 87             160
 * 102            220
 */
public class GradientDescent {
	
	public int[] data={123,150,87,102};
	
	public int[] label={250,320,160,220};
	
	public float w = 0f;
	
	private float a = 0.00001f; //learning rate
	private int round = 5000; //learning round
	
	// y = wx
	public float function(int x){
		return w * x;
	}
	
	private void train(){
		for(int i = 0; i < round ; i ++){
			int index = 0 ;
			float error = 0f;
			for(index = 0 ; index < data.length; index++){
				int x = data[index];
				int y = label[index];
				float h = function(x);
				float q =( h - y ) * x; 
				w = w - a * q;
				
				error += (y - h) ;	
			}
			System.out.println("round:"+i +",error:"+error);
		}
		System.out.println("w: "+ w);
	}
	
	public void test(){
		int index = 0;
		for(index = 0 ; index < data.length ; index ++){
			int x = data[index];
			int y = label[index];
			float h = function(data[index]);
			float error = y - h ;
			System.out.println("data:"+x+",label:"+y+",predict:"+h+",error:"+error);
		}
	}

	/**
	 * @param args
	 * Jan 30, 2013
	 */
	public static void main(String[] args) {
		GradientDescent gd = new GradientDescent();
		gd.train();
		gd.test();
	}

}
