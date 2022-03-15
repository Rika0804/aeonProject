package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterInfoListGetInputDTO {

	/**受け取りたいパラメータ名*/
	public String[] fields;

	/**レスポンスの形式*/
	public boolean flat;

	/**結果件数*/
	public Integer count;

	/**ページ番号*/
	public int page;

	/** filter(Filter) */
	private List<RegisterInfoListGetInputDTOFilter> filter;

	/**
	 * fieldsを取得する
	 * @return
	 */
	public String[] getFields() {
		return fields;
	}

	/***
	 * fieldsを設定する
	 * @param fields
	 */
	public void setFields(String[] fields) {
		this.fields = fields;
	}

	/***
	 * flatを取得する
	 * @return
	 */
	public boolean isFlat() {
		return flat;
	}

	/***
	 * flatを設定する
	 * @param flat
	 */
	public void setFlat(boolean flat) {
		this.flat = flat;
	}

	/**
	 * countを取得する
	 * @return
	 */
	public Integer getCount() {
		return count;
	}

	/**
	 * countを設定する
	 * @param count
	 */
	public void setCount(Integer count) {
		this.count = count;
	}

	/**
	 * pageを取得する
	 * @return
	 */
	public int getPage() {
		return page;
	}

	/**
	 * pageを設定する
	 * @param page
	 */
	public void setPage(int page) {
		this.page = page;
	}

	/**
	 * filter(filter)を取得する。
	 * 
	 * @return filter(filter)
	 */
	public List<RegisterInfoListGetInputDTOFilter> getFilter() {
		return filter;
	}

	/**
	 * filter(filter)を設定する。
	 * 
	 * @param filter filter(filter)
	 */
	public void setFilter(List<RegisterInfoListGetInputDTOFilter> filter) {
		this.filter = filter;
	}
}
