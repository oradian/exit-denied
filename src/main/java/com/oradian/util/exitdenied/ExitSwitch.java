package com.oradian.util.exitdenied;

import java.util.function.IntConsumer;

@DoNotInstrument
public final class ExitSwitch {
	private ExitSwitch() {};

	private static volatile IntConsumer delegate = null;

	public static void setExit(final IntConsumer delegate) {
		ExitSwitch.delegate = delegate;
	}

	public static void exit(int status) {
		if (delegate == null) {
			System.exit(status);
		} else {
			delegate.accept(status);
		}
	}
}
