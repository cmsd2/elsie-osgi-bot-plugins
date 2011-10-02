package uk.org.elsie.osgi.bot.pm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import uk.org.elsie.osgi.bot.PluginProvider;

public class PluginCommandManagerImpl implements ServiceTrackerCustomizer, PluginCommandManager {

	private static Log log = LogFactory.getLog(PluginCommandManagerImpl.class);
	private ServiceTracker pluginServiceTracker;
	private BundleContext bundleContext;
	private List<PluginProvider> plugins = new ArrayList<PluginProvider>();
	private Map<String, List<PluginProvider>> commandHandlers = new TreeMap<String, List<PluginProvider>>();
	private Map<PluginProvider, Map<String, Method>> commandProviders = new HashMap<PluginProvider, Map<String, Method>>();

	public void activate(BundleContext context) {
		this.bundleContext = context;
		this.pluginServiceTracker = new ServiceTracker(context, PluginProvider.class.getName(), this);
		this.pluginServiceTracker.open();
	}
	
	public void deactivate(BundleContext context) {
		this.pluginServiceTracker.close();
		this.pluginServiceTracker = null;
	}

	@Override
	public Object addingService(ServiceReference reference) {
		PluginProvider plugin = (PluginProvider) bundleContext.getService(reference);
		addPlugin(plugin);
		return plugin;
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		removePlugin((PluginProvider) service);
		bundleContext.ungetService(reference);
	}
	
	protected void addPlugin(PluginProvider plugin) {
		log.info("adding plugin " + plugin.getClass().getName());
		plugins.add(plugin);
		commandProviders.put(plugin, new TreeMap<String, Method>());
		addCommands(plugin);
		log.info("added plugin " + plugin.getClass().getName());
	}

	protected void removePlugin(PluginProvider plugin) {
		log.info("removing plugin " + plugin.getClass().getName());
		plugins.remove(plugin);
		Map<String, Method> pluginCommands = commandProviders.remove(plugin);
		if(pluginCommands != null) {
			for(String command : pluginCommands.keySet()) {
				log.info("unlinking plugin from command " + command);
				commandHandlers.get(command).remove(plugin);
			}
		}
		log.info("removed plugin " + plugin.getClass().getName());
	}
	
	protected void addCommand(PluginProvider plugin, String command, Method method) {
		log.info("linking plugin to command " + command);
		List<PluginProvider> handlers = commandHandlers.get(command);
		if(handlers == null) {
			handlers = new ArrayList<PluginProvider>();
			commandHandlers.put(command, handlers);
		}
		handlers.add(plugin);
		commandProviders.get(plugin).put(command, method);
	}
	
	protected void addCommands(PluginProvider plugin) {
		Method[] methods = plugin.getClass().getMethods();
		for(int i = 0; i < methods.length; i++) {
			Command command = methods[i].getAnnotation(Command.class);
			if(command != null) {
				String name = command.name();
				if(name.equals("")) {
					name = methods[i].getName();
				}
				addCommand(plugin, name, methods[i]);
			}
		}
	}

	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.pm.PluginCommandManager#getPlugin(java.lang.String)
	 */
	@Override
	public PluginProvider getPlugin(String command) {
		List<PluginProvider> handlers = commandHandlers.get(command);
		if(handlers == null || handlers.isEmpty())
			return null;
		else
			return handlers.get(0);
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.pm.PluginCommandManager#getCommand(uk.org.elsie.osgi.bot.PluginProvider, java.lang.String)
	 */
	@Override
	public Method getCommand(PluginProvider plugin, String command) {
		Map<String, Method> methods = commandProviders.get(plugin);
		if(methods == null)
			return null;
		return methods.get(command);
	}
	
	public List<PluginProvider> getPlugins() {
		return Collections.unmodifiableList(plugins);
	}
	
	@Override
	public Collection<String> getCommands() {
		return Collections.unmodifiableCollection(commandHandlers.keySet());
	}
	
	public Collection<String> getPluginCommands(PluginProvider plugin) {
		Map<String, Method> commands = commandProviders.get(plugin);
		if(commands != null)
			return Collections.unmodifiableCollection(commands.keySet());
		else
			return Collections.emptySet();
	}
}
