package jp.co.aeoncredit.coupon.batch.dto;

import javax.ws.rs.QueryParam;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.dto.DTOBase;

/**
 * テスト端末登録・解除API(TestDeviceAddOrRemove)のInput DTOクラス。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestDeviceAddOrRemoveInputDTO extends DTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** process(process) */
	private String process;

	/** deviceType(deviceType) */
	@QueryParam("device_type")
	@JsonProperty("device_type")
	private String deviceType;

	/** popinfoId(popinfoId) */
	@QueryParam("popinfo_id")
	@JsonProperty("popinfo_id")
	private String popinfoId;

	/**
	 * 処理名(process)を取得する。
	 * 
	 * @return 処理名(process)
	 */
	public String getProcess() {
		return process;
	}

	/**
	 * 処理名(process)を設定する。
	 * 
	 * @param process 処理名(process)
	 */
	public void setProcess(String process) {
		this.process = process;
	}

	/**
	 * deviceType(deviceType)を取得する。
	 * 
	 * @return deviceType(deviceType)
	 */
	public String getDeviceType() {
		return deviceType;
	}

	/**
	 * deviceType(deviceType)を設定する。
	 * 
	 * @param deviceType deviceType(deviceType)
	 */
	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	/**
	 * popinfoId(popinfoId)を取得する。
	 * 
	 * @return popinfoId(popinfoId)
	 */
	public String getPopinfoId() {
		return popinfoId;
	}

	/**
	 * popinfoId(popinfoId)を設定する。
	 * 
	 * @param popinfoId popinfoId(popinfoId)
	 */
	public void setPopinfoId(String popinfoId) {
		this.popinfoId = popinfoId;
	}
}