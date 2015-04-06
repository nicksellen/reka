package reka.email.util;

import static com.google.common.base.Preconditions.checkArgument;
import static reka.data.content.Contents.binary;
import static reka.data.content.Contents.utf8;
import static reka.util.Path.path;
import static reka.util.Path.PathElements.name;
import static reka.util.Path.PathElements.nextIndex;
import static reka.util.Util.unchecked;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;

import reka.data.Data;
import reka.data.MutableData;
import reka.data.memory.MutableMemoryData;
import reka.util.Path;

public class EmailToDataConverter {

	private static final Set<String> ignoreHeaders = new HashSet<>();
	
	static {
		// these get pulled out as top-level values, not left in the headers bit
		ignoreHeaders.add("To");
		ignoreHeaders.add("From");
		ignoreHeaders.add("Subject");
	}
	
	private EmailToDataConverter() { }

	public static Data convert(Message msg) {
		checkArgument(msg instanceof MimeMessage, "must be a %s", MimeMessage.class.getName());
		
		MimeMessage message = (MimeMessage) msg;
		
		try {
			InputStream stream = msg.getInputStream();
		    
			MutableData emailData = MutableMemoryData.create();
			
			ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
			try {
			    Thread.currentThread().setContextClassLoader(Multipart.class.getClassLoader());
				
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

			} finally {
			    Thread.currentThread().setContextClassLoader(originalClassloader);
			}
			
			return emailData.immutable();
			
		} catch (Exception e) {
			throw unchecked(e);
		}
	}
	
}
