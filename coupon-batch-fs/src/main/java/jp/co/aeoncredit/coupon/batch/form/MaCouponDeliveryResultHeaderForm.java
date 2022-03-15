package jp.co.aeoncredit.coupon.batch.form;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.aeoncredit.coupon.constants.CouponType;

/**
 * B18I0002_MAクーポン配布結果
 * ヘッダ項目
 * @author m-omori
 */
public class MaCouponDeliveryResultHeaderForm {

	// 桁数
	/** レコード種別 */
	private static final int RECORD_TYPE_LENGTH = 1;
	/** シーケンスＮｏ */
	private static final int SEQUENCE_NO_LENGTH = 4;
	/** 送信元システムＩＤ */
	private static final int SENDOR_SYSTEM_ID_LENGTH = 2;
	/** 送信先システムＩＤ */
	private static final int DEST_SYSTEM_ID_LENGTH = 2;
	/** データ抽出日時 */
	private static final int DATA_EXTRACT_DATETIME_LENGTH = 14;
	/** エラーコード */
	private static final int ERROR_CODE_LENGTH = 1;
	/** クーポンID */
	private static final int COUPON_ID_LENGTH = 18;
	/** クーポン種別 */
	private static final int COUPON_TYPE_LENGTH = 1;
	/** クーポン表示期間開始 */
	private static final int COUPON_DISP_PERIOD_FROM_LENGTH = 14;
	/** クーポン表示期間終了 */
	private static final int COUPON_DISP_PERIOD_TO_LENGTH = 14;
	/** クーポン有効期間開始 */
	private static final int COUPON_LIMIT_FROM_LENGTH = 14;
	/** クーポン有効期間終了 */
	private static final int COUPON_LIMIT_TO_LENGTH = 14;
	/** レコード長 */
	private static final int RECORD_LENGTH_LENGTH = 8;

	/**
	 * 桁数リスト
	 */
	public static final int[] LENGTH_LIST = {
			RECORD_TYPE_LENGTH,
			SEQUENCE_NO_LENGTH,
			SENDOR_SYSTEM_ID_LENGTH,
			DEST_SYSTEM_ID_LENGTH,
			DATA_EXTRACT_DATETIME_LENGTH,
			ERROR_CODE_LENGTH,
			COUPON_ID_LENGTH,
			COUPON_TYPE_LENGTH,
			COUPON_DISP_PERIOD_FROM_LENGTH,
			COUPON_DISP_PERIOD_TO_LENGTH,
			COUPON_LIMIT_FROM_LENGTH,
			COUPON_LIMIT_TO_LENGTH,
			RECORD_LENGTH_LENGTH
	};

	/** 文字コード */
	public final Charset CHARSET = Charset.forName("SJIS");
	/** 改行コード */
	public static final String LINE_SEPARATOR_CODE = "CR";

	/**
	 * 改行コードのバイト数を取得する
	 * @return
	 */
	public int getLineSeparatorCodeLength() {
		if (LINE_SEPARATOR_CODE.equals("CR") || LINE_SEPARATOR_CODE.equals("LF")) {
			return 1;
		} else if (LINE_SEPARATOR_CODE.equals("CRLF")) {
			return 2;
		} else {
			return 0;
		}
	}

	/**
	 * レコード長を取得する
	 * @param isContainLineSeparator ... 改行コードを含む場合true
	 * @return
	 */
	public int getLength(boolean isContainLineSeparator) {
		if (isContainLineSeparator) {
			return Arrays.stream(LENGTH_LIST).sum() + getLineSeparatorCodeLength();
		} else {
			return Arrays.stream(LENGTH_LIST).sum();
		}
	}

	/**
	 * レコード種別
	 * 規定値「H」
	 */
	private String recordType = "H";

	/**
	 * シーケンスＮｏ
	 * 数字
	 * 0000～9999の番号（9999の次は0000になる）
	 * ※頭ゼロ埋め
	 */
	private String sequenceNo;

	/**
	 * 送信元システムＩＤ
	 * 規定値「CK」
	 */
	private String sendorSystemId = "CK";

	/**
	 * 送信先システムＩＤ
	 * 規定値「MA」
	 */
	private String destSystemId = "MA";

	/**
	 * データ抽出日時
	 * （YYYYMMDDhhmmss）
	 */
	private String dataExtractDatatime;

	/**
	 * エラーコード
	 * 0 ~ 9
	 */
	private String errorCode;

	/**
	 * クーポンID
	 * （頭スペース埋め）
	 * クーポンIDが存在しない場合は半角スペース埋め
	 */
	private String couponId;

	/**
	 * クーポン種別
	 */
	private CouponType couponType;

	/**
	 * クーポン表示期間開始
	 * （YYYYMMDDhhmmss）または半角スペース埋め
	 */
	private String couponDispPeriodFrom;

	/**
	 * クーポン表示期間終了
	 * （YYYYMMDDhhmmss）または半角スペース埋め
	 */
	private String couponDispPeriodTo;

	/**
	 * クーポン有効期間開始
	 * （YYYYMMDDhhmmss）または半角スペース埋め
	 */
	private String couponLimitFrom;

	/**
	 * クーポン有効期間終了
	 * （YYYYMMDDhhmmss）または半角スペース埋め
	 */
	private String couponLimitTo;

	/**
	 * レコード長
	 * (改行コード、レコードタイプ、フィラーを含む)
	 * ※頭ゼロ埋め
	 */
	private String recordLength = String.format("%08d",
			Arrays.stream(LENGTH_LIST).sum() + getLineSeparatorCodeLength());

	public String getRecordType() {
		return recordType;
	}

	public String getSequenceNo() {
		return sequenceNo;
	}

	public void setSequenceNo(String sequenceNo) {
		this.sequenceNo = sequenceNo;
	}

	public String getSendorSystemId() {
		return sendorSystemId;
	}

	public String getDestSystemId() {
		return destSystemId;
	}

	public String getDataExtractDatatime() {
		return dataExtractDatatime;
	}

	public void setDataExtractDatatime(String dataExtractDatatime) {
		this.dataExtractDatatime = dataExtractDatatime;
	}

	/**
	 * Timestamp形式から指定のフォーマットに変換し、フィールドにセットする
	 * @param dataExtractDatatime
	 */
	public void setDataExtractDatatime(Timestamp dataExtractDatatime) {
		this.dataExtractDatatime = new SimpleDateFormat("yyyyMMddHHmmss").format(dataExtractDatatime);
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getCouponId() {
		return couponId;
	}

	public void setCouponId(String couponId) {
		this.couponId = couponId;
	}

	/**
	 * クーポンIDを空でセットする
	 */
	public void setCouponIdAsEmpty() {
		this.couponId = " ".repeat(COUPON_ID_LENGTH);
	}

	public CouponType getCouponType() {
		return couponType;
	}

	public void setCouponType(CouponType couponType) {
		this.couponType = couponType;
	}

	/**
	 * クーポン種別を空でセットする
	 */
	public void setCouponTypeAsEmpty() {
		this.couponType = null;
	}

	public String getCouponDispPeriodFrom() {
		return couponDispPeriodFrom;
	}

	public void setCouponDispPeriodFrom(String couponDispPeriodFrom) {
		this.couponDispPeriodFrom = couponDispPeriodFrom;
	}

	/**
	 * クーポン表示期間開始を空でセットする
	 */
	public void setCouponDispPeriodFromAsEmpty() {
		this.couponDispPeriodFrom = " ".repeat(COUPON_DISP_PERIOD_FROM_LENGTH);
	}

	public String getCouponDispPeriodTo() {
		return couponDispPeriodTo;
	}

	public void setCouponDispPeriodTo(String couponDispPeriodTo) {
		this.couponDispPeriodTo = couponDispPeriodTo;
	}

	/**
	 * クーポン表示期間終了を空でセットする
	 */
	public void setCouponDispPeriodToAsEmpty() {
		this.couponDispPeriodTo = " ".repeat(COUPON_DISP_PERIOD_TO_LENGTH);
	}

	public String getCouponLimitFrom() {
		return couponLimitFrom;
	}

	public void setCouponLimitFrom(String couponLimitFrom) {
		this.couponLimitFrom = couponLimitFrom;
	}

	/**
	 * クーポン有効期間開始を空でセットする
	 */
	public void setCouponLimitFromAsEmpty() {
		this.couponLimitFrom = " ".repeat(COUPON_LIMIT_FROM_LENGTH);
	}

	public String getCouponLimitTo() {
		return couponLimitTo;
	}

	public void setCouponLimitTo(String couponLimitTo) {
		this.couponLimitTo = couponLimitTo;
	}

	/**
	 * クーポン有効期間終了を空でセットする
	 */
	public void setCouponLimitToAsEmpty() {
		this.couponLimitTo = " ".repeat(COUPON_LIMIT_TO_LENGTH);
	}

	public String getRecordLength() {
		return recordLength;
	}

	public void setRecordLength(String recordLength) {
		this.recordLength = recordLength;
	}

	/**
	 * クーポン種別を文字列に変換する
	 * クーポン種別がセットされていない場合、半角スペース埋め文字列を返却する
	 * @return クーポン種別文字列
	 */
	private String couponTypeToString() {
		if (this.couponType == null) {
			return " ".repeat(COUPON_TYPE_LENGTH);
		} else {
			return this.couponType.getValue();
		}
	}

	/**
	 * ヘッダーレコードのフィールドリストを作成する
	 * @return フィールドリスト
	 */
	public List<String> toHeaderFieldList() {
		// クーポン種別変換
		String couponTypeStr = couponTypeToString();

		List<String> fieldList = new ArrayList<>() {
			{
				add(recordType);
				add(sequenceNo);
				add(sendorSystemId);
				add(destSystemId);
				add(dataExtractDatatime);
				add(errorCode);
				add(couponId);
				add(couponTypeStr);
				add(couponDispPeriodFrom);
				add(couponDispPeriodTo);
				add(couponLimitFrom);
				add(couponLimitTo);
				add(recordLength);
			}
		};
		return fieldList;
	}

}
