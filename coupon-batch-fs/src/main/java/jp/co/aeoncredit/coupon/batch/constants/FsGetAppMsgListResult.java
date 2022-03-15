package jp.co.aeoncredit.coupon.batch.constants;

import java.util.List;

import jp.co.aeoncredit.coupon.batch.dto.GetAppMsgOutputDTO;

/**
 * FS API連携(アプリ内Msg一覧API
 * 結果格納用クラス
 * @author m-omori
 *
 */
public class FsGetAppMsgListResult {
	
	/**
	 * コンストラクタ
	 * @param nextUrl 次ページのURL
	 * @param appMsgList 取得したアプリ内Msgのリスト
	 */
	public FsGetAppMsgListResult(String nextUrl, List<GetAppMsgOutputDTO> appMsgList) {
		super();
		this.nextUrl = nextUrl;
		this.appMsgList = appMsgList;
	}
	

	/** 次ページのURL */
	private String nextUrl;
	
	/** 取得結果リスト */
	private List<GetAppMsgOutputDTO> appMsgList;

	public String getNextUrl() {
		return nextUrl;
	}

	public void setNextUrl(String nextUrl) {
		this.nextUrl = nextUrl;
	}

	public List<GetAppMsgOutputDTO> getAppMsgList() {
		return appMsgList;
	}

	public void setAppMsgList(List<GetAppMsgOutputDTO> appMsgList) {
		this.appMsgList = appMsgList;
	}
	
	
}
