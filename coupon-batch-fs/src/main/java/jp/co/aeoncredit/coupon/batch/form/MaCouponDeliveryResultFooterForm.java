package jp.co.aeoncredit.coupon.batch.form;

import java.util.ArrayList;
import java.util.List;

/**
 * B18I0001_MAクーポン配布対象者リスト
 * フッタ項目
 * @author m-omori
 *
 */
public class MaCouponDeliveryResultFooterForm {

	// 桁数
	/** レコード種別 */
	private static final int RECORD_TYPE_LENGTH = 1;
	/** 業務データレコード件数 */
	private static final int RECORD_COUNT_LENGTH = 10;
	/** 余白 */
	private static final int MARGIN_LENGTH = 96;

	/**
	 * 桁数リスト
	 */
	public static final int[] LENGTH_LIST = {
			RECORD_TYPE_LENGTH,
			RECORD_COUNT_LENGTH,
			MARGIN_LENGTH
	};

	/**
	 * レコード種別
	 */
	private String recodType;

	/**
	 * 業務データレコード件数
	 */
	private String recordCount;

	/**
	 * 余白
	 */
	private String margin = " ".repeat(MARGIN_LENGTH);

	public String getRecodType() {
		return recodType;
	}

	public void setRecodType(String recodType) {
		this.recodType = recodType;
	}

	public String getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(String recordCount) {
		this.recordCount = recordCount;
	}

	/**
	 * 数値を頭ゼロ埋めに変換しセットする
	 * @param recordCount
	 */
	public void setRecordCount(int recordCount) {
		String format = "%0" + RECORD_COUNT_LENGTH + "d";
		this.recordCount = String.format(format, recordCount);
	}

	public String getMargin() {
		return margin;
	}

	/**
	 * フッターレコードのフィールドリストを作成する
	 * @return フィールドリスト
	 */
	public List<String> toFooterFieldList() {
		List<String> fieldList = new ArrayList<>() {
			{
				add(recodType);
				add(recordCount);
				add(margin);
			}
		};
		return fieldList;
	}

}
