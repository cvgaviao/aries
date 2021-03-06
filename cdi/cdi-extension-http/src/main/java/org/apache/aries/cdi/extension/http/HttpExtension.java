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

package org.apache.aries.cdi.extension.http;

import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.service.cdi.CdiConstants.CDI_CAPABILITY_NAME;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.servlet.WeldInitialListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class HttpExtension implements Extension {

	public HttpExtension(Bundle bundle) {
		_bundle = bundle;
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
		processWebClasses();

		BeanManagerImpl beanManagerImpl = ((BeanManagerProxy)beanManager).delegate();

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, getSelectedContext());
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, Boolean.TRUE.toString());
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE - 100);

		_registrations.add(
			_bundle.getBundleContext().registerService(
				LISTENER_CLASSES, new WeldInitialListener(beanManagerImpl), properties));
	}

	private void processWebClasses() {
		// TODO Auto-generated method stub
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		for (ServiceRegistration<?> registration : _registrations) {
			registration.unregister();
		}

		_registrations.clear();
	}

	private Map<String, Object> getAttributes() {
		BundleWiring bundleWiring = _bundle.adapt(BundleWiring.class);

		List<BundleWire> wires = bundleWiring.getRequiredWires(EXTENDER_NAMESPACE);

		Map<String, Object> cdiAttributes = Collections.emptyMap();

		for (BundleWire wire : wires) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(EXTENDER_NAMESPACE);

			if (extender.equals(CDI_CAPABILITY_NAME)) {
				BundleRequirement requirement = wire.getRequirement();
				cdiAttributes = requirement.getAttributes();
				break;
			}
		}

		return cdiAttributes;
	}

	private String getSelectedContext() {
		if (_contextSelect != null) {
			return _contextSelect;
		}

		return _contextSelect = getSelectedContext0();
	}

	private String getSelectedContext0() {
		Map<String, Object> attributes = getAttributes();

		if (attributes.containsKey(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT)) {
			return (String)attributes.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
		}

		Dictionary<String,String> headers = _bundle.getHeaders();

		if (headers.get(WEB_CONTEXT_PATH) != null) {
			return CONTEXT_PATH_PREFIX + headers.get(WEB_CONTEXT_PATH) + ')';
		}

		return DEFAULT_CONTEXT_FILTER;
	}

	private static final String CONTEXT_PATH_PREFIX = "(osgi.http.whiteboard.context.path=";
	private static final String DEFAULT_CONTEXT_FILTER = "(osgi.http.whiteboard.context.name=default)";
	private static final String[] LISTENER_CLASSES = new String[] {
		ServletContextListener.class.getName(),
		ServletRequestListener.class.getName(),
		HttpSessionListener.class.getName()
	};
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

	private final Bundle _bundle;
	private String _contextSelect;
	private List<ServiceRegistration<?>> _registrations = new CopyOnWriteArrayList<>();

}
