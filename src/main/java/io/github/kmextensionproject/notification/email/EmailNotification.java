package io.github.kmextensionproject.notification.email;

import static io.github.kmextensionproject.notification.base.NotificationResult.failure;
import static io.github.kmextensionproject.notification.base.NotificationResult.success;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Logger.getLogger;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import io.github.kmextensionproject.notification.base.Message;
import io.github.kmextensionproject.notification.base.Notification;
import io.github.kmextensionproject.notification.base.NotificationResult;
import io.github.kmextensionproject.notification.base.Recipient;

/**
 * To use this class properly, user must export these environment variables:
 * <ul>
 *  <li>${mail_smtp_host} - your/delegated SMTP server</li>
 *  <li>${mail_smtp_port} - SMTP port</li>
 *  <li>${mail_app_address} - may be a username of your account or email</li>
 *  <li>${mail_app_password} - your password or token generated for application</li>
 * </ul>
 *
 * @author mkrajcovic
 */
public class EmailNotification implements Notification {

	private static final Logger LOG = getLogger(EmailNotification.class.getName());

	private static final Pattern PORT_PATTERN = Pattern.compile("[0-9]{2,4}");
	private static final Pattern RFC_5322 = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

	private Properties smtpProperties;
	private String senderAddress;
	private String senderPassword;

	// based on proper configuration
	private boolean sendingEnabled = true;

	public EmailNotification() {
		smtpProperties = initSystemProperties();
		senderAddress = getSystemProperty("mail_app_address", RFC_5322);
		senderPassword = getSystemProperty("mail_app_password", null);
	}

	private Properties initSystemProperties() {
		Properties sysProps = System.getProperties();
		sysProps.put("mail.smtp.host", getSystemProperty("mail_smtp_host", null));
		sysProps.put("mail_smtp_port", getSystemProperty("mail_smtp_port", PORT_PATTERN));
		sysProps.put("mail.smtp.ssl.enable", "true");
        sysProps.put("mail.smtp.auth", "true");
		return sysProps;
	}

	private String getSystemProperty(String key, Pattern validationPattern) {
		String value = System.getenv(key);
		if (isNull(value)) {
			LOG.warning(() -> "${" + key + "} environment varible must be set to use Email notifications");
			disableSending();
		} else if (nonNull(validationPattern) && !validationPattern.matcher(value).matches()) {
			LOG.warning(() -> "${" + key + "} environment varible must comply to pattern " + validationPattern.pattern());
			disableSending();
		}
		return value;
	}

	/**
	 * HTML tags can be used to enrich the content of the message body.<br>
	 * The message body is not mandatory unlike subject and recipient's email
	 * address, when the notification will not be sent if they are missing.
	 */
	@Override
	public NotificationResult sendNotification(Message message, Recipient recipient) {
		requireNonNull(message, "message cannot be null");
		requireNonNull(recipient, "recipient cannot be null");

		if (!sendingEnabled) {
			return failure("Can not send an email - missing proper configuration, check logs for missing variables");
		} else if (isBlank(message.getSubject()) || isBlank(recipient.getEmail())) {
			return failure("Can not send an email - message subject or recipient's email address is missing");
		}
		try {
			MimeMessage mimeMsg = new MimeMessage(createSession());
			mimeMsg.setFrom(new InternetAddress(senderAddress));
			mimeMsg.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient.getEmail()));
			mimeMsg.setSubject(message.getSubject());
			mimeMsg.setText(withHtmlLineBreaks(message.getBody()), "UTF-8", "html");

			Transport.send(mimeMsg);
			return success("Email sent successfully to " + recipient.getEmail());
		} catch (MessagingException mex) {
			return failure("Could not send an email to " + recipient.getEmail(), mex);
		}
	}

	private boolean isBlank(String value) {
		return isNull(value) || value.trim().isEmpty();
	}

	private Session createSession() {
		return Session.getInstance(smtpProperties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderAddress, senderPassword);
            }
        });
	}

	private String withHtmlLineBreaks(String message) {
		return isBlank(message) ? message : message.replace("\n", "<br>");
	}

	private void disableSending() {
		sendingEnabled = false;
	}

}