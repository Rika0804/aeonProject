package jp.co.aeoncredit.coupon.batch.dto;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ibm.jp.awag.common.validator.Number;

public class DeliveryListGetInputDTO {
	
	/** count(count) */
	@Number(type = Number.Type.INTEGER)	
	@JsonProperty("count")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String count;
	
	/** page(page) */
	@Number(type = Number.Type.INTEGER)			
	@JsonProperty("page")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String page;

	/**
	 * count(count)を取得する。
	 * @return count(count)
	 */
	public String getCount() {
		return count;
	}

	/**
	 * count(count)を設定する。
	 * @param count count(count)
	 */
	public void setCount(String count) {
		this.count = count;
	}

	/**
	 * page(page)を取得する。
	 * @return page(page)
	 */
	public String getPage() {
		return page;
	}

	/**
	 * page(page)を設定する。
	 * @param page page(page)
	 */
	public void setPage(String page) {
		this.page = page;
	}

}
