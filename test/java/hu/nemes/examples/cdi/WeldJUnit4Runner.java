/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Peter Nemes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hu.nemes.examples.cdi;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class WeldJUnit4Runner extends BlockJUnit4ClassRunner {

	/*
	 * Static initialized run after JUnit setup, but before any of the tests (and Weld setup).
	 */
	static {
		/**
		 * From 2.2.5.Final Weld archive isolation is turned on by default, we want to disable this,
		 * and use the flat structure so we don't have to reference each cache intercepter independently
		 * from our beans.xml file (they are referenced from their own beans.xml file in their own file,
		 * and with isolation turned off they get merged)
		 * @see https://docs.jboss.org/weld/reference/latest/en-US/html/environments.html#_bean_archive_isolation
		 */
		System.setProperty(Weld.ARCHIVE_ISOLATION_SYSTEM_PROPERTY, Boolean.FALSE.toString());

		/**
		 * Unfortunately cache-annotations-ri-cdi uses java.utils.logging, so we want to route that to SLF4J.
		 */
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private final Class<?> klass;
	private final Weld weld;
	private WeldContainer container;

	public WeldJUnit4Runner(final Class<?> klass) throws InitializationError {
		super(klass);
		this.klass = klass;
		weld = new Weld();
	}

	@Override
	protected Statement classBlock(final RunNotifier notifier) {
		final Statement statement = super.classBlock(notifier);
		return new Statement() {
			/**
			 * For every class we want an isolated weld container to be initialized.
			 */
			@Override
			public void evaluate() throws Throwable {
				try {
					container = weld.initialize();
					statement.evaluate();
				}
				finally {
					container = null;
					weld.shutdown();
				}
			}

		};
	}

	@Override
	protected Object createTest() throws Exception {
		return container.instance().select(klass).get();
	}
}
