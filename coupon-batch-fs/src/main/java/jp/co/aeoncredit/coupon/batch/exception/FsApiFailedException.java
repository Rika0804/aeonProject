package jp.co.aeoncredit.coupon.batch.exception;

/**
 * FS API連携に失敗した際にthrowされる例外
 * @author m-omori
 *
 */
public class FsApiFailedException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * コンストラクタ
	 * @param statusCode
	 */
	public FsApiFailedException(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public FsApiFailedException() {
		this.statusCode = 0;
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
