package jp.co.aeoncredit.coupon.batch.form;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.aeoncredit.coupon.batch.dto.DeliveryRequestListDTO;
import jp.co.aeoncredit.coupon.constants.CouponType;

/**
 * B18P0006 配信依頼リスト
 * @author m-omori
 *
 */
public class DeliveryRequestListForm {
	
	/** 改行コード */
	public static final String LINE_SEPARATOR = "\r\n";
	
	/** 文字コード */
	public static final Charset CHARSET = Charset.forName("SJIS");
	
	/** ヘッダ有無 */
	public static final boolean HAS_HEADER = true;
	
	/** 「""」編集有無（囲み文字） */
	public static final char ENCLOSING_CHARACTOR = '\"';
	
	/** パスポートクーポンのヘッダ項目リスト */
	private static final String[] HEADER_LIST_PASSPORT = {
		"会員番号", "家族コード", "共通内部ID"	
	};
	
	/** ターゲットクーポンのヘッダ項目リスト */
	private static final String[] HEADER_LIST_TARGET = {
			"共通内部ID"
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
	
	/**
	 * 共通内部ID
	 * 配信対象者の20ケタの共通内部ID。
	 */
	private String commonInsideId;

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

	public String getCommonInsideId() {
		return commonInsideId;
	}

	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}
	
	/**
	 * CSV出力用に文字列のリストに変換する
	 * @param couponType ... クーポン種別
	 * @return
	 */
	public List<String> toList(CouponType couponType) {
		
		List<String> result = new ArrayList<>();

		if ( couponType == CouponType.TARGET) {
			// ターゲットクーポン
			result.add(this.commonInsideId);
		} else if ( couponType == CouponType.PASSPORT ) {
			// パスポートクーポン
			result.add(this.customerId);
			result.add(this.familyCode);
			result.add(this.commonInsideId);
		}
		
		return result;
		
	}
	
	/**
	 * データレコードの一行分の文字列を取得する
	 * @param couponType クーポン種別
	 * @return データレコードの文字列（"123456789012", ...  のようにダブルクォーテーションで囲まれたカンマ区切りの文字列）
	 */
	public String toString(CouponType couponType) {
		List<String> dataList = toList(couponType);
		List<String> quotedList = new ArrayList<>();
		dataList.stream().map(el -> ENCLOSING_CHARACTOR + el + ENCLOSING_CHARACTOR).forEach(el -> quotedList.add(el));
		return String.join(",", quotedList);			
	}
	
	/**
	 * ヘッダ行を文字列で取得する
	 * @param couponType クーポン種別
	 * @return ヘッダ行の文字列（"会員番号","家族コード","共通内部ID" のようにダブルクォーテーションで囲まれたカンマ区切りの文字列）
	 */
	public String getHeaderString(CouponType couponType) {
		List<String> quotedList = new ArrayList<>();
		List<String> headerList = Arrays.asList(getHeader(couponType));
		headerList.stream().map(el -> ENCLOSING_CHARACTOR + el + ENCLOSING_CHARACTOR).forEach(el -> quotedList.add(el));
		return String.join(",", quotedList);			
	}

	/**
	 * DTOに格納された値をセットする
	 * @param deliveryResultDTO
	 */
	public void setValueOf(DeliveryRequestListDTO deliveryResultDTO) {
		
		this.customerId = deliveryResultDTO.getCustomerId();
		this.familyCode = deliveryResultDTO.getFamilyCode();
		this.commonInsideId = deliveryResultDTO.getCommonInsideId();
		
	}
	
	/**
	 * ヘッダの配列を取得する
	 * @param couponType
	 * @return
	 */
	public static String[] getHeader(CouponType couponType) {
		
		if (couponType == CouponType.TARGET) {
			return HEADER_LIST_TARGET;
		} else {
			return HEADER_LIST_PASSPORT;
		}
	}
	
}
