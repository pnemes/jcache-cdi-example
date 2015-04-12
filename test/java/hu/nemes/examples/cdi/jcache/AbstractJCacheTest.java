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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;
import javax.cache.spi.CachingProvider;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJCacheTest<K, V> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractJCacheTest.class);

	private static CacheManager cacheManager;

	private final Cache<K, V> cache;

	private final Map<EventType, Collection<CacheEntryEvent<? extends K, ? extends V>>> cacheEvents;

	private final Collection<CacheEntryListenerConfiguration<K, V>> activeCacheEntryListenerConfigurations;

	protected AbstractJCacheTest(String cacheName) {
		this(cacheName, EventType.CREATED, EventType.UPDATED, EventType.REMOVED, EventType.EXPIRED);
	}

	protected AbstractJCacheTest(String cacheName, EventType... eventTypesToListenFor) {
		cacheEvents = new EnumMap<>(EventType.class);
		cache = cacheManager.getCache(cacheName);
		activeCacheEntryListenerConfigurations = new ArrayList<>();

		final Consumer<Iterable<CacheEntryEvent<? extends K, ? extends V>>> listener = iterable -> iterable.forEach(
				event -> {
					if (LOG.isTraceEnabled()) {
						LOG.trace("{}: an entry has been {}.", event.getSource().getName(), event.getEventType());
					}
					cacheEvents.get(event.getEventType()).add(event);
				});

		Factory<? extends CacheEntryListener<? super K, ? super V>> listenerFactory;
		for (final EventType eventTypeToListenFor : eventTypesToListenFor) {

			// create the specific listenerFactory for the eventType
			switch (eventTypeToListenFor) {
			case CREATED:
				listenerFactory = (Factory<CacheEntryCreatedListener<K, V>>) (() -> listener::accept);
				break;

			case EXPIRED:
				listenerFactory = (Factory<CacheEntryExpiredListener<K, V>>) (() -> listener::accept);
				break;

			case REMOVED:
				listenerFactory = (Factory<CacheEntryRemovedListener<K, V>>) (() -> listener::accept);
				break;

			case UPDATED:
				listenerFactory = (Factory<CacheEntryUpdatedListener<K, V>>) (() -> listener::accept);
				break;

			default:
				throw new IllegalArgumentException(String.format("Unknown EventType: %s!", eventTypeToListenFor));
			}
			// initialize the backing collection for the specific eventType
			cacheEvents.put(eventTypeToListenFor, new ArrayList<>());

			// add the listenerConfiguration to the list which will be used to register/deregister them
			activeCacheEntryListenerConfigurations.add(
					new MutableCacheEntryListenerConfiguration<>(listenerFactory, null, true, false));
		}
	}

	@BeforeClass
	public static final void setupCacheManager() {
		// FIXME: this only works if the annotation on the method does not have a custom CacheResolverFactory set.
		final CachingProvider provider = Caching.getCachingProvider();
		cacheManager = provider.getCacheManager();
	}

	@Before
	public final void setupCache() {
		activeCacheEntryListenerConfigurations.forEach(cache::registerCacheEntryListener);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Starting cache size for {} is {}.",
					cache.getName(),
					StreamSupport.stream(cache.spliterator(), false).count());
		}
	}

	@After
	public final void teardownCache() {
		activeCacheEntryListenerConfigurations.forEach(cache::deregisterCacheEntryListener);
		if (LOG.isTraceEnabled()) {
			LOG.trace("Ending cache size for {} is {}.",
					cache.getName(),
					StreamSupport.stream(cache.spliterator(), false).count());
		}
		cache.clear();
		cacheEvents.values().forEach(Collection::clear);
	}

	@AfterClass
	public static final void teardownCacheManager() {
		cacheManager = null;
	}

	protected final Collection<CacheEntryEvent<? extends K, ? extends V>> getCacheEvents(EventType eventType) {
		return cacheEvents.get(eventType);
	}
}
