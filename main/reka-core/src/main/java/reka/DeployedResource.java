package reka;


public interface DeployedResource {
	void undeploy(int version);
	void pause(int version);
	void resume(int version);
}
