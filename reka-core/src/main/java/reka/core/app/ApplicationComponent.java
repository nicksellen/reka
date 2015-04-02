package reka.core.app;

public interface ApplicationComponent {
	void undeploy();
	Runnable pause();
}
