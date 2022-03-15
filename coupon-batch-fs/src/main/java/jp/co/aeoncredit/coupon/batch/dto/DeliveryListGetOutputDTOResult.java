/** Generated by AWAG */

package jp.co.aeoncredit.coupon.batch.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 配信一覧取得 API(DeliveryListGet).ResultのOutput DTOクラス。
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryListGetOutputDTOResult extends MapiOutputDTOBase {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/** id(Id) */
	@JsonProperty("id")
	private String id;

	/** type(Type) */
	@JsonProperty("type")
	private String type;

	/** content_type(ContentType) */
	@JsonProperty("content_type")
	private String contentType;

	/** platform(Platform) */
	@JsonProperty("platform")
	private List<String> platform;

	/** popup(Popup) */
	@JsonProperty("popup")
	private String popup;

	/** popup_items(PopupItems) */
	@JsonProperty("popup_items")
	private DeliveryListGetOutputDTOPopupItems popupItems;

	/** title(Title) */
	@JsonProperty("title")
	private String title;

	/** content(Content) */
	@JsonProperty("content")
	private String content;

	/** delivery_type(DeliveryType) */
	@JsonProperty("delivery_type")
	private String deliveryType;

	/** url(Url) */
	@JsonProperty("url")
	private String url;

	/** category(Category) */
	@JsonProperty("category")
	private String category;

	/** canceled(Canceled) */
	@JsonProperty("canceled")
	private String canceled;

	/** send_time(SendTime) */
	@JsonProperty("send_time")
	private String sendTime;

	/** period(Period) */
	@JsonProperty("period")
	private List<DeliveryListGetOutputDTOPeriod> period;

	/** wifissid(Wifissid) */
	@JsonProperty("wifissid")
	private List<DeliveryListGetOutputDTOWifissid> wifissid;

	/** bluetooth(Bluetooth) */
	@JsonProperty("bluetooth")
	private List<DeliveryListGetOutputDTOBluetooth> bluetooth;

	/** status(Status) */
	@JsonProperty("status")
	private String status;

	/** info_status(InfoStatus) */
	@JsonProperty("info_status")
	private String infoStatus;

	/** sent(Sent) */
	@JsonProperty("sent")
	private DeliveryListGetOutputDTOSent sent;

	/** open(Open) */
	@JsonProperty("open")
	private DeliveryListGetOutputDTOOpen open;

	/** view(View) */
	@JsonProperty("view")
	private DeliveryListGetOutputDTOView view;

	/** click(Click) */
	@JsonProperty("click")
	private DeliveryListGetOutputDTOClick click;

	/**
	 * id(id)を取得する。
	 * 
	 * @return id(id)
	 */
	public String getId() {
		return id;
	}

	/**
	 * id(id)を設定する。
	 * 
	 * @param id id(id)
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * type(type)を取得する。
	 * 
	 * @return type(type)
	 */
	public String getType() {
		return type;
	}

	/**
	 * type(type)を設定する。
	 * 
	 * @param type type(type)
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * content_type(content_type)を取得する。
	 * 
	 * @return content_type(content_type)
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * content_type(content_type)を設定する。
	 * 
	 * @param contentType content_type(content_type)
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * platform(platform)を取得する。
	 * 
	 * @return platform(platform)
	 */
	public List<String> getPlatform() {
		return platform;
	}

	/**
	 * platform(platform)を設定する。
	 * 
	 * @param platform platform(platform)
	 */
	public void setPlatform(List<String> platform) {
		this.platform = platform;
	}

	/**
	 * popup(popup)を取得する。
	 * 
	 * @return popup(popup)
	 */
	public String getPopup() {
		return popup;
	}

	/**
	 * popup(popup)を設定する。
	 * 
	 * @param popup popup(popup)
	 */
	public void setPopup(String popup) {
		this.popup = popup;
	}

	/**
	 * popup_items(popup_items)を取得する。
	 * 
	 * @return popup_items(popup_items)
	 */
	public DeliveryListGetOutputDTOPopupItems getPopupItems() {
		return popupItems;
	}

	/**
	 * popup_items(popup_items)を設定する。
	 * 
	 * @param popupItems popup_items(popup_items)
	 */
	public void setPopupItems(DeliveryListGetOutputDTOPopupItems popupItems) {
		this.popupItems = popupItems;
	}

	/**
	 * title(title)を取得する。
	 * 
	 * @return title(title)
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * title(title)を設定する。
	 * 
	 * @param title title(title)
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * content(content)を取得する。
	 * 
	 * @return content(content)
	 */
	public String getContent() {
		return content;
	}

	/**
	 * content(content)を設定する。
	 * 
	 * @param content content(content)
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * delivery_type(delivery_type)を取得する。
	 * 
	 * @return delivery_type(delivery_type)
	 */
	public String getDeliveryType() {
		return deliveryType;
	}

	/**
	 * delivery_type(delivery_type)を設定する。
	 * 
	 * @param deliveryType delivery_type(delivery_type)
	 */
	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	/**
	 * url(url)を取得する。
	 * 
	 * @return url(url)
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * url(url)を設定する。
	 * 
	 * @param url url(url)
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * category(category)を取得する。
	 * 
	 * @return category(category)
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * category(category)を設定する。
	 * 
	 * @param category category(category)
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * canceled(canceled)を取得する。
	 * 
	 * @return canceled(canceled)
	 */
	public String getCanceled() {
		return canceled;
	}

	/**
	 * canceled(canceled)を設定する。
	 * 
	 * @param canceled canceled(canceled)
	 */
	public void setCanceled(String canceled) {
		this.canceled = canceled;
	}

	/**
	 * send_time(send_time)を取得する。
	 * 
	 * @return send_time(send_time)
	 */
	public String getSendTime() {
		return sendTime;
	}

	/**
	 * send_time(send_time)を設定する。
	 * 
	 * @param sendTime send_time(send_time)
	 */
	public void setSendTime(String sendTime) {
		this.sendTime = sendTime;
	}

	/**
	 * period(period)を取得する。
	 * 
	 * @return period(period)
	 */
	public List<DeliveryListGetOutputDTOPeriod> getPeriod() {
		return period;
	}

	/**
	 * period(period)を設定する。
	 * 
	 * @param period period(period)
	 */
	public void setPeriod(List<DeliveryListGetOutputDTOPeriod> period) {
		this.period = period;
	}

	/**
	 * wifissid(wifissid)を取得する。
	 * 
	 * @return wifissid(wifissid)
	 */
	public List<DeliveryListGetOutputDTOWifissid> getWifissid() {
		return wifissid;
	}

	/**
	 * wifissid(wifissid)を設定する。
	 * 
	 * @param wifissid wifissid(wifissid)
	 */
	public void setWifissid(List<DeliveryListGetOutputDTOWifissid> wifissid) {
		this.wifissid = wifissid;
	}

	/**
	 * bluetooth(bluetooth)を取得する。
	 * 
	 * @return bluetooth(bluetooth)
	 */
	public List<DeliveryListGetOutputDTOBluetooth> getBluetooth() {
		return bluetooth;
	}

	/**
	 * bluetooth(bluetooth)を設定する。
	 * 
	 * @param bluetooth bluetooth(bluetooth)
	 */
	public void setBluetooth(List<DeliveryListGetOutputDTOBluetooth> bluetooth) {
		this.bluetooth = bluetooth;
	}

	/**
	 * status(status)を取得する。
	 * 
	 * @return status(status)
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * status(status)を設定する。
	 * 
	 * @param status status(status)
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * info_status(info_status)を取得する。
	 * 
	 * @return info_status(info_status)
	 */
	public String getInfoStatus() {
		return infoStatus;
	}

	/**
	 * info_status(info_status)を設定する。
	 * 
	 * @param infoStatus info_status(info_status)
	 */
	public void setInfoStatus(String infoStatus) {
		this.infoStatus = infoStatus;
	}

	/**
	 * sent(sent)を取得する。
	 * 
	 * @return sent(sent)
	 */
	public DeliveryListGetOutputDTOSent getSent() {
		return sent;
	}

	/**
	 * sent(sent)を設定する。
	 * 
	 * @param sent sent(sent)
	 */
	public void setSent(DeliveryListGetOutputDTOSent sent) {
		this.sent = sent;
	}

	/**
	 * open(open)を取得する。
	 * 
	 * @return open(open)
	 */
	public DeliveryListGetOutputDTOOpen getOpen() {
		return open;
	}

	/**
	 * open(open)を設定する。
	 * 
	 * @param open open(open)
	 */
	public void setOpen(DeliveryListGetOutputDTOOpen open) {
		this.open = open;
	}

	/**
	 * view(view)を取得する。
	 * 
	 * @return view(view)
	 */
	public DeliveryListGetOutputDTOView getView() {
		return view;
	}

	/**
	 * view(view)を設定する。
	 * 
	 * @param view view(view)
	 */
	public void setView(DeliveryListGetOutputDTOView view) {
		this.view = view;
	}

	/**
	 * click(click)を取得する。
	 * 
	 * @return click(click)
	 */
	public DeliveryListGetOutputDTOClick getClick() {
		return click;
	}

	/**
	 * click(click)を設定する。
	 * 
	 * @param click click(click)
	 */
	public void setClick(DeliveryListGetOutputDTOClick click) {
		this.click = click;
	}
}