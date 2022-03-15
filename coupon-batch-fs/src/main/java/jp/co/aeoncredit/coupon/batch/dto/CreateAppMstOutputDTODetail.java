package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg登録API(CreateAppMst).DetailのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAppMstOutputDTODetail extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** エラー対象(Loc) */
	@JsonProperty("loc")
	private List<String> loc;

	/** エラーメッセージ(Msg) */
	@JsonProperty("msg")
	private String msg;

	/** エラー種類(Type) */
	@JsonProperty("type")
	private String type;

	/**
	 * エラー対象(loc)を取得する。
	 * 
	 * @return エラー対象(loc)
	 */
	public List<String> getLoc() {
		return loc;
	}

	/**
	 * エラー対象(loc)を設定する。
	 * 
	 * @param loc エラー対象(loc)
	 */
	public void setLoc(List<String> loc) {
		this.loc = loc;
	}

	/**
	 * エラーメッセージ(msg)を取得する。
	 * 
	 * @return エラーメッセージ(msg)
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * エラーメッセージ(msg)を設定する。
	 * 
	 * @param msg エラーメッセージ(msg)
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

	/**
	 * エラー種類(type)を取得する。
	 * 
	 * @return エラー種類(type)
	 */
	public String getType() {
		return type;
	}

	/**
	 * エラー種類(type)を設定する。
	 * 
	 * @param type エラー種類(type)
	 */
	public void setType(String type) {
		this.type = type;
	}

}
