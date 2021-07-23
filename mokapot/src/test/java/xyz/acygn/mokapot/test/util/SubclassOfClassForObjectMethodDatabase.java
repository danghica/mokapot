package xyz.acygn.mokapot.test.util;

public final class SubclassOfClassForObjectMethodDatabase extends ClassForObjectMethodDatabase implements InterfaceForObjectMethodDatabase {
	int something = 1;
	String somethingElse;
	@Override
	public Integer returnSomething() {
		return 9;
	}
	
	public void uselessMethod() {
		return;
	}
}
