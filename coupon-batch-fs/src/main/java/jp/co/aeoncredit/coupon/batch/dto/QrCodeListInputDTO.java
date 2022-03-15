package jp.co.aeoncredit.coupon.batch.dto;

import java.sql.Timestamp;

/**
 * B18BC002 QRコードリスト作成 のinput DTOクラス
 * @author m-omori
 *
 */
public class QrCodeListInputDTO {
	
	/** QRコードID */
	private String qrCodeId;
	
	/** FSクーポンUUID */
	private String uuid;
	
	/**
	 * 有効日（開始）
	 */
	private Timestamp validStartDate;

	/**
	 * 有効日（終了）
	 */
	private Timestamp validEndDate;
	
	public String getQrCodeId() {
		return qrCodeId;
	}

	public void setQrCodeId(String qrCodeId) {
		this.qrCodeId = qrCodeId;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Timestamp getValidStartDate() {
		return validStartDate;
	}

	public void setValidStartDate(Timestamp validStartDate) {
		this.validStartDate = validStartDate;
	}

	public Timestamp getValidEndDate() {
		return validEndDate;
	}

	public void setValidEndDate(Timestamp validEndDate) {
		this.validEndDate = validEndDate;
	}

	@Override
	public String toString() {
		return "qrCodeId:" + this.qrCodeId + ", fsCouponUuid:" + this.uuid + ", validStartDate:" + this.validStartDate
				+ ", validEndDate:" + this.validEndDate;

	}
}
