package jp.co.aeoncredit.coupon.batch.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test {

	public static void main(String[] args) throws IOException{
		// TODO 自動生成されたメソッド・スタブ

		Practice pc = new Practice();

		System.out.println("数字を入力してください");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int kind = Integer.parseInt(br.readLine());
		
		pc.addCoin(kind);
		
		System.out.println(pc.getCount(kind));
		
	/**	
		int amount;
		int count;
		
		Practice pc = new Practice();
		
		for(int i = 0; i < 10; i++) {
			pc.addCoin(10);
		}
		
		for(int j = 0 ; j < 10 ; j++) {
			pc.addCoin(500);
		}
		
		count = pc.getCount(10);
		
		amount = pc.getAmount(10)+pc.getAmount(500);
		
		System.out.println("合計は"+ count + "で、"+ amount + "円です");
		
		**/
		
	}

}
