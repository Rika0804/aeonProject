package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 登録者情報一覧取得 API(RegisterInfoListGet)のOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterInfoListGetOutputDTO extends MapiOutputDTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** total_results(TotalResults) */
	@JsonProperty("total_results")
	private Integer totalResults;

	/** page(Page) */
	@JsonProperty("page")
	private Integer page;

	/** pages(Pages) */
	@JsonProperty("pages")
	private Integer pages;

	/** result(Result) */
	@JsonProperty("result")
	private List<List<Boolean>> result;

	/**
	 * total_results(total_results)を取得する。
	 * @return total_results(total_results)
	 */
	public Integer getTotalResults() {
		return totalResults;
	}

	/**
	 * total_results(total_results)を設定する。
	 * @param totalResults total_results(total_results)
	 */
	public void setTotalResults(Integer totalResults) {
		this.totalResults = totalResults;
	}

	/**
	 * page(page)を取得する。
	 * @return page(page)
	 */
	@JsonProperty("page")
	public Integer getPage() {
		return page;
	}

	/**
	 * page(page)を設定する。
	 * @param page page(page)
	 */
	public void setPage(Integer page) {
		this.page = page;
	}

	/**
	 * pages(pages)を取得する。
	 * @return pages(pages)
	 */
	public Integer getPages() {
		return pages;
	}

	/**
	 * pages(pages)を設定する。
	 * @param pages pages(pages)
	 */
	public void setPages(Integer pages) {
		this.pages = pages;
	}

	/**
	 * result(result)を取得する。
	 * @return result(result)
	 */
	public List<List<Boolean>> getResult() {
		return result;
	}

	/**
	 * result(result)を設定する。
	 * @param result result(result)
	 */
	public void setResult(List<List<Boolean>> result) {
		this.result = result;
	}

}
