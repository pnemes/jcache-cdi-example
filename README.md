
A simple example on how to use javax.cache API together with CDI.

For JUnit testing a custom testRunner is used for initializing Weld.
In a standalone setup, the environment could be started with weld-se,
using the main class org.jboss.weld.environment.se.StartMain.

The javax.cache implementation can be freely changed, here ehcache is used
(with the bridge for javax.cache api, which is org.ehcahe:jcache).

The core of the setup is org.jsr107.ri:cache-annotations-ri-cdi, which
provides the interceptor implementations for the method annotations for
the CDI environment (which is Weld, and it only used explicitly in the
test runner, so without the test any other CDI implementation can be used).
