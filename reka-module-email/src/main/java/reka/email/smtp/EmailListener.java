package reka.email.smtp;

import static reka.util.Util.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import reka.data.Data;
import reka.email.util.EmailToDataConverter;

public class EmailListener implements SimpleMessageListener {
	
	private static final Set<String> ignoreHeaders = new HashSet<>();
	
	static {
		ignoreHeaders.add("To");
		ignoreHeaders.add("From");
		ignoreHeaders.add("Subject");
	}

	private final Consumer<Data> consumer;

	private volatile BiFunction<String,String,Boolean> acceptor = (from, to) -> true;
	
	public void setAcceptor(BiFunction<String,String,Boolean> acceptor) {
		this.acceptor = acceptor;
	}
	
	public EmailListener(Consumer<Data> consumer) {
		this.consumer = consumer;
	}

	@Override
	public boolean accept(String from, String to) {
		return acceptor.apply(from, to);
	}

	@Override
	public void deliver(String from, String to, InputStream stream) throws TooMuchDataException, IOException {
		try {
			consumer.accept(EmailToDataConverter.convert(new MimeMessage(Session.getInstance(new Properties()), stream)));
		} catch (MessagingException e) {
			throw unchecked(e);
		}
	}
	
}