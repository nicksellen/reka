package reka;

import static reka.util.Util.unchecked;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import reka.ApplicationManager.EventType;
import reka.api.data.Data;

public class EventLogger {

	private static final byte NEWLINE = '\n';
	private static final byte SPACE = ' ';

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss.SSSZ");
	private static final Runnable setdate = () -> date = sdf.format(new Date()).toString().getBytes(StandardCharsets.UTF_8);
	private static final ScheduledExecutorService e = Executors.newSingleThreadScheduledExecutor();
	private static volatile byte[] date;

	static {
		setdate.run();
		e.scheduleWithFixedDelay(setdate, 1, 1, TimeUnit.SECONDS);
	}
	
	private final Object lock = new Object();
	
	private final File file;
	private final FileOutputStream out;
	private final FileChannel fc;
	
	private final AtomicLong eventid;
	
	public EventLogger(String path) {
		try {
			synchronized (lock) {
				file = new File(path);
				out = new FileOutputStream(file, true);
				fc = out.getChannel();
				fc.lock(); // TODO: doesn't seem to do what I'm expecting!
				eventid = new AtomicLong(findPreviousEventId());
			}
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	private long findPreviousEventId() {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			long size = file.length();
			int len = (int) Math.min(size, 1024);
			
			int pos = (int) size;
			boolean done = false;
			byte[] buf = new byte[len];
			
			int eventidOffset = 28 + 1;
			
			int lastlineEnd = -1;
			int lastlineStart = -1;
			
			while (!done) {
				pos = (int) pos - len;
				raf.seek(pos);
				int read = raf.read(buf);
				if (read <= 0) break;
				for (int i = read - 1; i >= 0; i--) {
					if (buf[i] == NEWLINE) {
						if (lastlineEnd == -1) {
							lastlineEnd = pos + i;
						} else {
							lastlineStart = pos + i + 1 + eventidOffset;
							int lastlineLen = lastlineEnd - lastlineStart;
							byte[] lastline = new byte[lastlineLen];
							raf.seek(lastlineStart);
							raf.read(lastline);
							long eid = -1;
							for (int j = 0; j < lastline.length; j++) {
								if (lastline[j] == SPACE) {
									byte[] n = new byte[j];
									System.arraycopy(lastline, 0, n, 0, n.length);
									eid = Long.valueOf(new String(n, StandardCharsets.UTF_8));
									break;
								}
							}
							return eid;
						}
					}
				}
			}
			return -1;
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	public long nextEventId() {
		return eventid.incrementAndGet();
	}
	
	public void write(long eid, EventType type, Data data) {
		try {
			synchronized (lock) {
				out.write(date);
				out.write(SPACE);
				out.write(Long.toString(eid).getBytes(StandardCharsets.UTF_8));
				out.write(SPACE);
				out.write(type.toString().getBytes(StandardCharsets.UTF_8));
				out.write(SPACE);
				out.write(data.toJson().getBytes(StandardCharsets.UTF_8));
				out.write(NEWLINE);
			}
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
}
