package jp.co.aeoncredit.coupon.batch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ログインAPI用リクエストパラメータDTO
 */
public class LoginInputDTO {
	/** ログインユーザID */
	@JsonProperty("user_id")
	private String userId;

	/** パスワード */
	@JsonProperty("password")
	private String password;

	/** 使用可能な回数 */
	@JsonProperty("count_limit")
	private Integer countLimit;

	/** 使用可能な秒数 */
	@JsonProperty("time_limit")
	private Integer timeLimit;

	/**
	 * @return userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId セットする userId
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password セットする password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return countLimit
	 */
	public Integer getCountLimit() {
		return countLimit;
	}

	/**
	 * @param countLimit セットする countLimit
	 */
	public void setCountLimit(Integer countLimit) {
		this.countLimit = countLimit;
	}

	/**
	 * @return timeLimit
	 */
	public Integer getTimeLimit() {
		return timeLimit;
	}

	/**
	 * @param timeLimit セットする timeLimit
	 */
	public void setTimeLimit(Integer timeLimit) {
		this.timeLimit = timeLimit;
	}
}
