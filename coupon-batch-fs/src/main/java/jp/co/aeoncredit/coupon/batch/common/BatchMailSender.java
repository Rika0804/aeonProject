package jp.co.aeoncredit.coupon.batch.common;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;

import com.ibm.jp.awag.common.logic.ServiceCommonException;

/**
 * メール送信用のユーティリティクラス
 *  
 */
@Dependent
public class BatchMailSender {
	
	@Resource(lookup="java:jboss/mail/Default")
	public Session session;
	
	public MimeMessage createMimeMessage() {		
		return new MimeMessage(session);
	}
	
	public void process(Message mes) throws MessagingException{
		Transport.send(mes);
	}
	
}
