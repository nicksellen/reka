package reka.git;

import static java.lang.String.format;
import static reka.api.Path.path;
import static reka.api.Path.PathElements.index;
import static reka.api.Path.PathElements.name;
import static reka.util.Util.unchecked;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reka.api.data.MutableData;
import reka.core.data.memory.MutableMemoryData;

public class GitServer {
	
	private static final Logger log = LoggerFactory.getLogger(GitServer.class);
	
	private final int port;
	private final String basepath;
	
	public static void main(String[] args) {
		new GitServer(9001, "/tmp/repos").start();
	}
	
	public GitServer(int port, String basepath) {
		this.port = port;
		this.basepath = basepath;
	}
	
	private static Pattern reponamePattern = Pattern.compile("[^\\/]+$");
	
	private void start() {
		
		Server server = new Server(port);
		
	    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
 
        GitServlet servlet = new GitServlet();
        
        servlet.setReceivePackFactory(new RekaReceivePackFactory());
        
        ServletHolder h = new ServletHolder(servlet);
        h.setInitParameter("base-path", basepath);
        h.setInitParameter("export-all", "true");
        context.addServlet(h,"/*");
		
		try {
			server.start();
		} catch (Exception e) {
			throw unchecked(e);
		}
	}
	
	private static void listCommits(Repository repo, ReceivePack rp, ObjectId from, ObjectId to) {
		try {
			
			Git git = new Git(repo);
			
			RevWalk walk = new RevWalk(repo);
			RevCommit toCommit = walk.parseCommit(to);
			walk.markStart(toCommit);
			
			if (from.equals(ObjectId.zeroId())) {
				for (RevCommit parent : toCommit.getParents()) {
					walk.markUninteresting(parent);
				}
			} else {
				walk.markUninteresting(walk.parseCommit(from));
			}
			
			walk.forEach(commit -> {
				
				MutableData data = MutableMemoryData.create();
				
				PersonIdent author = commit.getAuthorIdent();
				
				if (author != null) {
					data.putString(path("author", "name"), author.getName());
					data.putString(path("author", "email"), author.getEmailAddress());
					data.putLong(path("author", "when"), author.getWhen().getTime());
				}

				PersonIdent committer = commit.getCommitterIdent();

				if (committer != null) {
					data.putString(path("committer", "name"), committer.getName());
					data.putString(path("committer", "email"), committer.getEmailAddress());
					data.putLong(path("committer", "when"), committer.getWhen().getTime());
				}
				
				data.putInt("when", commit.getCommitTime());
				data.putString("summary", commit.getShortMessage());
				data.putString("message", commit.getFullMessage());
				data.putString("commit", commit.getName());
				
				diff(repo, git, commit, data);
				
				log.debug("commit : {}", data.toPrettyJson());
				
				rp.sendMessage(format("processed commit %s", data.toPrettyJson()));
				
			});
			
		} catch (IOException e) {
			throw unchecked(e);
		}
	}
	
	private static void diff(Repository repo, Git git, RevCommit commit, MutableData data) {
		
		AtomicInteger changeIndex = new AtomicInteger();
		
		for (RevCommit parent : commit.getParents()) {
			try {
				
				ObjectReader reader = repo.newObjectReader();
				
				CanonicalTreeParser oldTree = new CanonicalTreeParser();
				oldTree.reset(reader, parent.getTree().getId());

				CanonicalTreeParser newTree = new CanonicalTreeParser();
				newTree.reset(reader, commit.getTree().getId());
				
				git.diff().setOldTree(oldTree).setNewTree(newTree).call().forEach(e -> { 
					
					reka.api.Path base = path(name("changes"), index(changeIndex.getAndIncrement()));
					
					data.putString(base.add("type"), e.getChangeType().name());
					
					boolean oldId = e.getOldId().prefixCompare(ObjectId.zeroId()) != 0;
					boolean newId = e.getNewId().prefixCompare(ObjectId.zeroId()) != 0;
					boolean oldPath = !e.getOldPath().equals("/dev/null");
					boolean newPath = !e.getNewPath().equals("/dev/null");
					
					if (oldId && newId && !e.getOldId().equals(e.getNewId())) {
						data.putString(base.add("old-id"), e.getOldId().name());
						data.putString(base.add("new-id"), e.getNewId().name());
					} else if (oldId) {
						data.putString(base.add("id"), e.getOldId().name());
					} else if (newId) {
						data.putString(base.add("id"), e.getNewId().name());
					}
					
					if (oldPath && newPath && !e.getOldPath().equals(e.getNewPath())) {
						data.putString(base.add("old-path"), e.getOldPath());
						data.putString(base.add("new-path"), e.getNewPath());
					} else if (oldPath) {
						data.putString(base.add("path"), e.getOldPath());
					} else if (newPath) {
						data.putString(base.add("path"), e.getNewPath());
					}
					
				});
				
			} catch (Exception e) {
				throw unchecked(e);
			}
		
		}
	}
	
	private class RekaReceivePackFactory implements ReceivePackFactory<HttpServletRequest> {

		@Override
		public ReceivePack create(HttpServletRequest req, Repository repo)
				throws ServiceNotEnabledException,
				ServiceNotAuthorizedException {
			
			ReceivePack receive = new ReceivePack(repo);
        	
        	String path = req.getPathInfo();
        	Matcher m = reponamePattern.matcher(path);
        	
        	String reponame;
        	if (m.find()) {
        		reponame = m.group();
        	} else {
        		receive.sendError(format("don't know which repo this is for [%s]", path));
        		return receive;
        	}
        	
        	receive.setPostReceiveHook((rp, cmds) -> {
        		
            	rp.sendMessage(format("reka received your push to [%s]", reponame));
            	
        		cmds.forEach(cmd -> {
        			
        			MutableData data = MutableMemoryData.create();
        			data.putString("type", cmd.getType().name());
        			data.putString("ref", cmd.getRefName());
        			data.putString("repo", reponame);
        			
        			try {
						data.putString("branch", repo.getBranch());
					} catch (Exception e) {
						e.printStackTrace();
					}
        			
        			if (cmd.getMessage() != null) {
        				data.putString("message", cmd.getMessage());
        			}

					boolean oldId = !cmd.getOldId().equals(ObjectId.zeroId());
					boolean newId = !cmd.getNewId().equals(ObjectId.zeroId());
					
					if (oldId && newId && !cmd.getOldId().equals(cmd.getNewId())) {
	        			data.putString("new-id", cmd.getNewId().name());
        				data.putString("old-id", cmd.getOldId().name());
					} else if (oldId) {
        				data.putString("id", cmd.getOldId().name());
					} else if (newId) {
	        			data.putString("id", cmd.getNewId().name());
					}
        			
        			rp.sendMessage(format("received %s", data.toPrettyJson()));
        			
        			switch (cmd.getType()) {
        			case DELETE: break;
        			default: listCommits(repo, rp, cmd.getOldId(), cmd.getNewId());
        			}
        			
        		});
        	});
        	
        	return receive;
		}

	}

}
