package reka.app;

public interface ApplicationComponent {
	void undeploy();
	Runnable pause();
}
