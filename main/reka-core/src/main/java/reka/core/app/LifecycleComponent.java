package reka.core.app;

public interface LifecycleComponent {
	void undeploy();
	Runnable pause();
}
