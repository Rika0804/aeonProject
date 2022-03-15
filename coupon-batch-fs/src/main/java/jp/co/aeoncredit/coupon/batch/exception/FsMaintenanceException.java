package jp.co.aeoncredit.coupon.batch.exception;

import jp.co.aeoncredit.coupon.constants.HTTPStatus;

/**
 * FS API連携にて、FSメンテナンス時にthrowされる例外
 * @author m-omori
 *
 */
public class FsMaintenanceException extends Exception {

	private static final long serialVersionUID = 1L;
	
	/**
	 * コンストラクタ
	 * @param statusCode
	 */
	public FsMaintenanceException(int statusCode) {
		this.statusCode = statusCode;
	}

	public FsMaintenanceException() {
		this.statusCode = HTTPStatus.HTTP_STATUS_MAINTENANCE.getValue();
	}
	
	/** ステータスコード */
	private int statusCode;

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	
}
