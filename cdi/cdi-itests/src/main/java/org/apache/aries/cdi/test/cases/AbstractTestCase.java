/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.test.cases;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CdiConstants;
import org.osgi.service.cdi.CdiContainer;
import org.osgi.service.cdi.CdiEvent;
import org.osgi.util.tracker.ServiceTracker;

import junit.framework.TestCase;

public class AbstractTestCase extends TestCase {

	@Override
	protected void setUp() throws Exception {
		servicesBundle = bundleContext.installBundle("services-one.jar" , getBundle("services-one.jar"));
		servicesBundle.start();
		cdiBundle = bundleContext.installBundle("basic-beans.jar" , getBundle("basic-beans.jar"));
		cdiBundle.start();
	}

	@Override
	protected void tearDown() throws Exception {
		cdiBundle.uninstall();
		servicesBundle.uninstall();
	}

	void assertPojoExists(BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans(Pojo.class, any);

		assertFalse(beans.isEmpty());
		Iterator<Bean<?>> iterator = beans.iterator();
		Bean<?> bean = iterator.next();
		assertTrue(Pojo.class.isAssignableFrom(bean.getBeanClass()));
		assertFalse(iterator.hasNext());

		bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Pojo pojo = (Pojo)beanManager.getReference(bean, Pojo.class, ctx);
		assertNotNull(pojo);
	}

	InputStream getBundle(String name) {
		Class<?> clazz = this.getClass();

		ClassLoader classLoader = clazz.getClassLoader();

		return classLoader.getResourceAsStream(name);
	}

	Bundle getCdiExtenderBundle() {
		BundleWiring bundleWiring = cdiBundle.adapt(BundleWiring.class);

		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);

		for (BundleWire wire : requiredWires) {
			Map<String, Object> attributes = wire.getCapability().getAttributes();
			String extender = (String)attributes.get(ExtenderNamespace.EXTENDER_NAMESPACE);

			if (CdiConstants.CDI_CAPABILITY_NAME.equals(extender)) {
				return wire.getProvider().getBundle();
			}
		}

		return null;
	}

	ServiceTracker<CdiContainer, CdiContainer> getServiceTracker(long bundleId) throws InvalidSyntaxException {
		Filter filter = bundleContext.createFilter(
			"(&(objectClass=" + CdiContainer.class.getName() + ")(service.bundleid=" + bundleId + ")(" +
				CdiConstants.CDI_CONTAINER_STATE + "=" + CdiEvent.Type.CREATED + "))");

		ServiceTracker<CdiContainer, CdiContainer> serviceTracker = new ServiceTracker<>(bundleContext, filter, null);

		serviceTracker.open();

		return serviceTracker;
	}

	public Bundle installBundle(String url) throws Exception {
		return installBundle(url, true);
	}

	public Bundle installBundle(String bundleName, boolean start) throws Exception {
		Bundle b = bundleContext.installBundle(bundleName, getBundle(bundleName));

		if (start) {
			b.start();
		}

		return b;
	}

	CdiContainer waitForCdiContainer() throws Exception {
		return waitForCdiContainer(bundle.getBundleId());
	}

	CdiContainer waitForCdiContainer(long bundleId) throws Exception {
		return getServiceTracker(bundleId).waitForService(timeout);
	}

	static final AnnotationLiteral<Any> any = new AnnotationLiteral<Any>() {
		private static final long serialVersionUID = 1L;
	};

	static final Bundle bundle = FrameworkUtil.getBundle(CdiBeanTests.class);
	static final BundleContext bundleContext = bundle.getBundleContext();
	static final long timeout = 5000;

	Bundle cdiBundle;
	Bundle servicesBundle;
	CdiContainer cdiContainer;

}