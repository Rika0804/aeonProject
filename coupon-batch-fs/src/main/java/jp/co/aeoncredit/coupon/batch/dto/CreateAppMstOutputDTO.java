package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * アプリ内Msg登録API(CreateAppMst)のOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateAppMstOutputDTO extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** エラー詳細配列(Detail) */
	@JsonProperty("detail")
	private List<CreateAppMstOutputDTODetail> detail;

	/**
	 * エラー詳細配列(detail)を取得する。
	 * 
	 * @return エラー詳細配列(detail)
	 */
	public List<CreateAppMstOutputDTODetail> getDetail() {
		return detail;
	}

	/**
	 * エラー詳細配列(detail)を設定する。
	 * 
	 * @param detail エラー詳細配列(detail)
	 */
	public void setDetail(List<CreateAppMstOutputDTODetail> detail) {
		this.detail = detail;
	}
}
