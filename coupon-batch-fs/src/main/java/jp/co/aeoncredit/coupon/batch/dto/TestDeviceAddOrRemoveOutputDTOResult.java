package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * テスト端末登録・解除API(TestDeviceAddOrRemove).ResultのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestDeviceAddOrRemoveOutputDTOResult extends MapiOutputDTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;
}
