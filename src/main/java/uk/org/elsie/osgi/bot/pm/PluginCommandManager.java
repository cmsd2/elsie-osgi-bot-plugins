package uk.org.elsie.osgi.bot.pm;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import uk.org.elsie.osgi.bot.PluginProvider;

public interface PluginCommandManager {

	public PluginProvider getPlugin(String command);

	public Method getCommand(PluginProvider plugin, String command);

	public Collection<String> getCommands();
	
	public List<PluginProvider> getPlugins();
	
	public Collection<String> getPluginCommands(PluginProvider plugin);

}