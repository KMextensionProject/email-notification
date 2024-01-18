package io.github.kmextensionproject.notification.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.kmextensionproject.notification.base.Message;
import io.github.kmextensionproject.notification.base.NotificationResult;
import io.github.kmextensionproject.notification.base.NotificationResult.Status;
import io.github.kmextensionproject.notification.base.Recipient;

class EmailNotificationTest {

	@Test
	void invalidConfigTest() {

		EmailNotification notification = new EmailNotification();
		NotificationResult notifResult = notification.sendNotification(
				new Message("subject"),
				new Recipient().withEmail("mar.krajc@gmail.com"));

		Assertions.assertEquals(Status.FAILURE, notifResult.getStatus());
	}

}
