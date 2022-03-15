package jp.co.aeoncredit.coupon.batch.form;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.aeoncredit.coupon.batch.dto.DeliveryPlanListDTO;

/**
 * B18P0013 配信予定リスト（ATM）
 * @author m-omori
 *
 */
public class DeliveryPlanListATMForm {

	/** 改行コード */
	public static final String LINE_SEPARATOR = "\r\n";
	
	/** 文字コード */
	public static final Charset CHARSET = Charset.forName("SJIS");
	
	/** ヘッダ有無 */
	public static final boolean HAS_HEADER = true;
	
	/** 「""」編集有無（囲み文字） */
	public static final char ENCLOSING_CHARACTOR = '\"';
	
	/** ヘッダの項目リスト */
	private static final String[] HEADER_LIST = {
			"会員番号", "家族コード"
	};

	/**
	 * 会員番号
	 * 配信対象者の12ケタの会員番号。
	 * ターゲットクーポンでは出力無
	 */
	private String customerId;
	
	/**
	 * 家族CD
	 * 配信対象者の1ケタの家族コード。
	 * ターゲットクーポンでは出力無
	 */
	private String familyCode;
	

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getFamilyCode() {
		return familyCode;
	}

	public void setFamilyCode(String familyCode) {
		this.familyCode = familyCode;
	}
	
	/**
	 * CSV出力用に文字列のリストに変換する
	 * @return
	 */
	public List<String> toList() {
		
		List<String> result = new ArrayList<>();

		result.add(this.customerId);
		result.add(this.familyCode);
	
		return result;
		
	}

	/**
	 * DTOからフォームにセットする
	 * @param deliveryTarget 配信予定リストのDTO
	 */
	public void setValueOf(DeliveryPlanListDTO deliveryTarget) {
		this.customerId = deliveryTarget.getCustomerId();
		this.familyCode = deliveryTarget.getFamilyCode();
	}

	/**
	 * ヘッダのリストを取得する
	 * @return
	 */
	public static String[] getHeader() {
		return HEADER_LIST;
	}

	/**
	 * ヘッダ文字列を取得する
	 * @return
	 */
	public String getHeaderString() {
		List<String> headerList = Arrays.asList(HEADER_LIST);
		List<String> quotedList = new ArrayList<>();
		headerList.stream().map(el -> ENCLOSING_CHARACTOR + el + ENCLOSING_CHARACTOR).forEach(el -> quotedList.add(el));
		return String.join(",", quotedList);			
	}
	
	@Override
	public String toString() {
		List<String> dataList = toList();
		List<String> quotedList = new ArrayList<>();
		dataList.stream().map(el -> ENCLOSING_CHARACTOR + el + ENCLOSING_CHARACTOR).forEach(el -> quotedList.add(el));
		return String.join(",", quotedList);			
	}
	
}
