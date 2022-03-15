package jp.co.aeoncredit.coupon.batch.main;

import java.util.HashMap;

class Practice {


	private int kind;
	private int count500 =0;
	private int count100;
	private int count50;
	private int count10;
	private int count5;
	private int count1;

	
	private HashMap<Integer,Integer>money = new HashMap<>();
	
	public void addCoin(int kind) {
		switch(kind){
		
		case 500:
			count500++;
			money.put(500, count500);
			break;
		
		case 100:
			count100++;
			money.put(100, count100);
			break;
			
		case 50:
			count50++;
			money.put(50,count50);
			break;
			
		case 10:
			count10++;
			money.put(10,count10);
			break;
		
		case 5:
			count5++;
			money.put(5, count5);
			break;
			
		case 1:
			count1++;
			money.put(1, count1);
			break;
	}
}
	public int getCount(int kind) {
		int count = money.get(kind);
		return count;
	}
	
	public int getAmount(int kind) {
		int amount = money.get(kind)*kind;
		return amount;
	}



}



