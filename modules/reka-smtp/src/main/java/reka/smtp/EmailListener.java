package reka.smtp;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.api.Path.PathElements.nextIndex;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

public class EmailListener implements SimpleMessageListener {
	
	private final Consumer<Data> consumer;
	
	public EmailListener(Consumer<Data> consumer) {
		this.consumer = consumer;
	}

	@Override
	public boolean accept(String from, String to) {
		return true;
	}

	@Override
	public void deliver(String from, String to, InputStream stream) throws TooMuchDataException, IOException {
		
		Properties props = new Properties();
		Session session = Session.getInstance(props);
		try {
			
			MimeMessage message = new MimeMessage(session, stream);
			
			MimeMessageParser email = new MimeMessageParser(message);
			
			email.parse();
			
			stream.close();
			
			MutableData emailData = MutableMemoryData.create();
			
			for (Address addr : email.getTo()) {
				emailData.putOrAppend(path("to"), utf8(addr.toString()));
			}
			
			for (Address addr : email.getCc()) {
				emailData.putOrAppend(path("cc"), utf8(addr.toString()));
			}
			
			for (Address addr : email.getBcc()) {
				emailData.putOrAppend(path("bcc"), utf8(addr.toString()));
			}
			
			if (email.getReplyTo() != null) {
				emailData.putString("reply-to", email.getReplyTo());
			}
			
			if (email.getFrom() != null) {
				emailData.putString("from", email.getFrom());
			}
			
			for (DataSource att : email.getAttachmentList()) {
				
				emailData.putMap(path(name("attachments"), nextIndex()), map -> {
					map.putString("name", att.getName());
					map.putString("content-type", att.getContentType());
					try {
						map.put("content", binary(att.getContentType(), att.getInputStream()));
					} catch (Exception e) {
						throw unchecked(e);
					}
				});
				
			}
			
			if (email.getPlainContent() != null) {
				emailData.putString(path("content", "plain"), email.getPlainContent());
			}
			
			if (email.getHtmlContent() != null) {
				emailData.putString(path("content", "html"), email.getHtmlContent());
			}
			
			emailData.putString("subject", email.getSubject());
			
			consumer.accept(emailData);
			
		} catch (Exception e) {
			throw unchecked(e);
		}
	}
	
}