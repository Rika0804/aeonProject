package jp.co.aeoncredit.coupon.batch.form;

import java.util.ArrayList;
import java.util.List;

import jp.co.aeoncredit.coupon.constants.FamilyCode;

/**
 * B18I0002_MAクーポン配布結果
 * データレコード項目
 * @author m-omori
 */
public class MaCouponDeliveryResultBodyForm {
	// 桁数
	/** レコード種別 */
	private static final int RECORD_TYPE_LENGTH = 1;
	/** 共通内部ID */
	private static final int COMMON_INSIDE_ID_LENGTH = 20;
	/** 会員番号 */
	private static final int CUSTOMER_ID_LENGTH = 12;
	/** 配布結果 */
	public static final int DELIVERY_RESULT_LENGTH = 1;
	/** 家族コード */
	private static final int FAMILY_CODE_LENGTH = 1;
	/** 枚数フラグ */
	private static final int COUNT_FLAG_LENGTH = 2;
	/** 余白 */
	private static final int MARGIN_LENGTH = 70;

	/**
	 * 桁数リスト
	 */
	public static final int[] LENGTH_LIST = {
			RECORD_TYPE_LENGTH,
			COMMON_INSIDE_ID_LENGTH,
			CUSTOMER_ID_LENGTH,
			DELIVERY_RESULT_LENGTH,
			FAMILY_CODE_LENGTH,
			COUNT_FLAG_LENGTH,
			MARGIN_LENGTH
	};

	/**
	 * レコード種別
	 */
	private String recordType = "G";

	/**
	 * 共通内部ID
	 */
	private String commonInsideId;

	/**
	 * 会員番号
	 */
	private String customerId;

	/**
	 * 配布結果
	 */
	private String deliveryResult;

	/**
	 * 家族コード
	 */
	private FamilyCode familyCode;

	/**
	 * 枚数フラグ
	 */
	private String countFlag;

	/**
	 * 余白
	 */
	private String margin;

	public String getRecordType() {
		return recordType;
	}

	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}

	public String getCommonInsideId() {
		return commonInsideId;
	}

	public void setCommonInsideId(String commonInsideId) {
		this.commonInsideId = commonInsideId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * 会員番号を空でセットする
	 */
	public void setCustomerIdAsEmpty() {
		this.customerId = " ".repeat(CUSTOMER_ID_LENGTH);
	}

	public String getDeliveryResult() {
		return deliveryResult;
	}

	public void setDeliveryResult(String deliveryResult) {
		this.deliveryResult = deliveryResult;
	}

	public FamilyCode getFamilyCode() {
		return familyCode;
	}

	public void setFamilyCode(FamilyCode familyCode) {
		this.familyCode = familyCode;
	}

	public String getCountFlag() {
		return countFlag;
	}

	public void setCountFlag(String countFlag) {
		this.countFlag = countFlag;
	}

	public String getMargin() {
		return margin;
	}

	public void setMargin(String margin) {
		this.margin = margin;
	}

	public void initMargin() {
		this.margin = " ".repeat(MARGIN_LENGTH);
	}
	
	/**
	 * 家族コードを文字列に変換する
	 * 家族コードがセットされていない場合、半角スペース埋め文字列を返却する
	 * @return 家族コード文字列
	 */
	private String familyCodeToString() {
		if (this.familyCode == null) {
			return " ".repeat(FAMILY_CODE_LENGTH);
		} else {
			return this.familyCode.getValue();
		}
	}

	/**
	 * データレコードのフィールドリストを作成する
	 * @return フィールドリスト
	 */
	public List<String> toBodyFieldList() {
		// 家族コード変換
		String familyCodeStr = familyCodeToString();

		List<String> fieldList = new ArrayList<>();
		fieldList.add(recordType);
		fieldList.add(commonInsideId);
		fieldList.add(customerId);
		fieldList.add(deliveryResult);
		fieldList.add(familyCodeStr);
		fieldList.add(countFlag);
		fieldList.add(margin);

		return fieldList;
	}

}
