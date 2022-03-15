package jp.co.aeoncredit.coupon.batch.dto;

import jp.co.aeoncredit.coupon.batch.constants.Constants;

/**
 * B18B0009 配信予定・依頼リスト出力バッチ
 * 配信予定リスト（ATM）DTO
 * @author m-omori
 *
 */
public class DeliveryPlanListDTO {

	/** 取得するカラム数 */
	private static final int COLUMN_NUM = 3;

	/** 会員番号 */
	private String customerId;

	/** 家族コード */
	private String familyCode;

	/** 共通内部ID */
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
	 * DBから取得したデータをDTOに変換する
	 * @param data DBから取得したデータ
	 * @return 変換結果
	 */
	public boolean setValue(Object[] data) {

		if (data.length != COLUMN_NUM) {
			return false;
		}

		this.customerId = (String) data[0];
		this.familyCode = (String) data[1];
		this.commonInsideId = (String) data[2];

		return true;
	}
	/** quotes in data */
	  private static final char DOUBLE_QUOTES = '"';
	  
	@Override
    public String toString() {
    return DOUBLE_QUOTES + customerId + DOUBLE_QUOTES + Constants.COMMA
        + DOUBLE_QUOTES + familyCode + DOUBLE_QUOTES;
    }
}
