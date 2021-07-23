package xyz.acygn.mokapot.test.util;

public class ClassWithGeneric <T> {
	public static <K> boolean areSame(Class<K> a, Class<K> b) {
		return a == b;
	}
}
