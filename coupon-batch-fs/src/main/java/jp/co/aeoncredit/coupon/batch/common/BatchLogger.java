package jp.co.aeoncredit.coupon.batch.common;

import jp.co.aeoncredit.coupon.batch.constants.BusinessMessageCode;
import jp.co.aeoncredit.coupon.util.BusinessMessage;

/**
 * バッチのロガークラス
 *  
 */
public class BatchLogger {

	static String jobidlog = "";

	/**
	 * コンストラクタ
	 * @param jobid
	 */
	public BatchLogger(String jobid) {
		jobidlog = "[" + jobid + "] ";
	}

	String logmsg = "";
	String logmsgid = "";

	/**
	 * ログメッセージを作成するメソッド
	 * @param msgCode メッセージコード
	 * @param params メッセージ・パラメータ
	 * @return logmsg ログメッセージ
	 */
	public String createMsg(BusinessMessageCode msgCode, Object... params) {
		String msgid = msgCode.toString();
		String msg = BusinessMessage.getMessages(msgid);
		msg = String.format(msg, params);
		return createMsg(msgid, msg);
	}

	/**
	 * ログメッセージを作成するメソッド。
	 * @param msgid メッセージID
	 * @param msg メッセージ 
	 * @return logmsg ログメッセージ
	 */
	public String createMsg(String msgid, String msg) {

		logmsgid = createMsgid(msgid);
		logmsg = jobidlog + logmsgid + msg;

		return logmsg;
	}

	/**
	 * 開始処理ログメッセージを作成するメソッド。
	 * @param msgid メッセージID
	 * @param bacthname バッチ名
	 * @return logmsg ログメッセージ
	 */
	public String createStartMsg(String msgid, String batchname) {

		logmsgid = createMsgid(msgid);
		logmsg = jobidlog + logmsgid + batchname + "バッチが開始しました。";

		return logmsg;
	}

	/**
	 * 終了処理ログメッセージを作成するメソッド。
	 * @param msgid メッセージID
	 * @param bacthname バッチ名
	 * @param status 正常/異常
	 * @return logmsg ログメッセージ
	 */
	public String createEndMsg(String msgid, String batchname, Boolean status) {

		logmsgid = createMsgid(msgid);

		if (status) {
			logmsg = jobidlog + logmsgid + batchname + "バッチが終了しました。";
		} else {
			logmsg = jobidlog + logmsgid + batchname + "バッチが異常終了しました。";
		}

		return logmsg;

	}

	private String createMsgid(String msgid) {

		if (msgid != null && !msgid.equals("")) {
			logmsgid = msgid + ":";
		}

		return logmsgid;
	}

}
