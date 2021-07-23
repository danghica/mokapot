package xyz.acygn.mokapot.test.util;

public class ClassWithSlowConstructor {
	public ClassWithSlowConstructor() {
		for (int i = 1; i <= 10000; i++) {
			String s = new String();
		}
	}
}
