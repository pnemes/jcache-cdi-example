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
package hu.nemes.examples.cdi.jcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hu.nemes.examples.cdi.WeldJUnit4Runner;

import java.util.Collection;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(WeldJUnit4Runner.class)
public class CachedBeanTest extends AbstractJCacheTest<Integer, Double> {

	private static final Logger LOG = LoggerFactory.getLogger(CachedBeanTest.class);

	@Inject
	private CachedBean cachedBean;

	@Inject
	private CallthroughBean callthroughBean;

	public CachedBeanTest() {
		super(CachedBean.CACHE_NAME);
	}

	@Test
	public void testDirectCallIsCached() {
		testCachedCall(cachedBean::sqrt);
	}

	@Test
	public void testIndirectCallIsCached() {
		testCachedCall(callthroughBean::sqrt);
	}

	private final void testCachedCall(IntToDoubleFunction call) {

		// wrap for logging
		final IntToDoubleFunction f = i -> {
			final double result = call.applyAsDouble(i);
			if (LOG.isTraceEnabled()) {
				LOG.trace("[RESULT] f({}) = {}" , i, result);
			}
			return result;
		};

		// get the CREATED events
		final Collection<CacheEntryEvent<? extends Integer, ? extends Double>> events = super.getCacheEvents(EventType.CREATED);

		// it should be never null, only empty
		assertNotNull(events);
		assertTrue(events.isEmpty());

		// call the function with [0, 25), verifying every
		// time that there is a new CREATED event type emitted
		final double[] results = IntStream
				.range(0, 25)
				.sequential()
				.mapToDouble(i -> {
					assertEquals(events.size(), i);
					final double result = f.applyAsDouble(i);
					assertEquals(events.size(), i + 1);
					return result;
				})
				.toArray();

		// the first and the second call for with the same values,
		// should return the same result (delta = 0)
		IntStream.range(0, 25).forEach(i -> assertEquals(results[i], f.applyAsDouble(i), 0));
	}
}
