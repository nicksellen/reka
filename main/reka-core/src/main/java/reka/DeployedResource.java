package reka;

public interface DeployedResource {
	void undeploy(int version);
	void freeze(int version);
}
