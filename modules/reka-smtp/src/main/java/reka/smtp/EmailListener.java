package reka.smtp;

import static reka.api.Path.path;
import static reka.api.Path.PathElements.name;
import static reka.api.Path.PathElements.nextIndex;
import static reka.api.content.Contents.binary;
import static reka.api.content.Contents.utf8;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import reka.api.Path;
import reka.api.data.Data;
import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

public class EmailListener implements SimpleMessageListener {
	
	private final Consumer<Data> consumer;
	
	private static final Set<String> ignoreHeaders = new HashSet<>();
	
	static {
		ignoreHeaders.add("To");
		ignoreHeaders.add("From");
		ignoreHeaders.add("Subject");
	}
	
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
			
			MutableData emailData = MutableMemoryData.create();
			
			MimeMessage message = new MimeMessage(session, stream);
			
			Object content = message.getContent();
			
			if (content instanceof SharedByteArrayInputStream) {
				// I *think* the newer jvm DataHandler implementation returns the content stream instead of a string
				// this gets around it. the problem doesn't show up when running inside eclipse wierdly.
				SharedByteArrayInputStream contentStream = (SharedByteArrayInputStream) content;
				message.setContent(IOUtils.toString(contentStream, StandardCharsets.UTF_8), message.getContentType());
			}
			
			MimeMessageParser email = new MimeMessageParser(message);
			
			@SuppressWarnings("unchecked")
			Enumeration<Header> headers = message.getAllHeaders();
			Path headerBase = path("headers");
			
			while (headers.hasMoreElements()) {
				Header header = headers.nextElement();
				String name = header.getName();
				if (ignoreHeaders.contains(name)) continue;
				emailData.putOrAppend(headerBase.add(header.getName()), utf8(header.getValue()));
			}
			
			email.parse();
			
			stream.close();
			
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