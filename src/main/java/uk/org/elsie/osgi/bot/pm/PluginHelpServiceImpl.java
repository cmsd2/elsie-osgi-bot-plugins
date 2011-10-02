package uk.org.elsie.osgi.bot.pm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleContext;

import uk.org.elsie.osgi.bot.IrcEventConstants;
import uk.org.elsie.osgi.bot.IrcSessionService;
import uk.org.elsie.osgi.bot.PluginProvider;

@Plugin(name="help")
public class PluginHelpServiceImpl implements PluginProvider {

	private IrcSessionService ircSession;
	private PluginCommandManager pluginManager;
	
	public void activate(BundleContext context) {
	}
	
	public void deactivate(BundleContext context) {
	}
	
	public void setPluginCommandRunner(PrivmsgPluginCommandRunner runner) {
	}
	
	public void unsetPluginCommandRunner(PrivmsgPluginCommandRunner runner) {
	}
	
	public void setPluginManager(PluginCommandManager pluginManager) {
		this.pluginManager = pluginManager;
	}
	
	public void unsetPluginManager(PluginCommandManager pluginManager) {
		this.pluginManager = null;
	}
	
	public void setIrcSession(IrcSessionService connectionService) {
		this.ircSession = connectionService;
	}
	
	public void unsetIrcSession(IrcSessionService connection) {
		this.ircSession = null;
	}

	@Command(name="help", help="prints help on commands")
	public void help(@PrivmsgProperty(name=IrcEventConstants.RETURN_TARGET) String target, @PrivmsgArgs String[] params) {
		if(params == null || params.length == 0) {
			helpForPlugins(target, pluginManager.getPlugins());
		} else {
			List<String> commands = new ArrayList<String>();
			for(int i = 0; i < params.length; i++) {
				commands.add(params[i]);
			}
			helpForCommands(target, commands);
		}
	}
	
	public String getPluginHelp(String command) {
		PluginProvider plugin = pluginManager.getPlugin(command);
		if(plugin == null)
			return "No plugin found for " + command;
		Method method = pluginManager.getCommand(plugin, command);
		if(method == null)
			return "Strange. Found plugin " + plugin.getClass().getName() + " but command is missing";
		Command commandAnnotation = method.getAnnotation(Command.class);
		if(commandAnnotation == null)
			return "Strange. Found plugin " + plugin.getClass().getName() + " and command " + command + " but annotation is missing";
		String help = commandAnnotation.help();
		if(help.equals(""))
			return "No help available for " + command;
		else
			return help;
	}
	
	public void helpForPlugins(String target, List<PluginProvider> plugins) {
		for(PluginProvider plugin : plugins) {
			String pluginHelp = plugin.help();
			String pluginName;
			Plugin pluginAnnotation = plugin.getClass().getAnnotation(Plugin.class);
			if(pluginAnnotation != null)
				pluginName = pluginAnnotation.name() + " plugin";
			else
				pluginName = plugin.getClass().getName();
			ircSession.privMsg(target, pluginName);
			if(pluginHelp != null && pluginHelp.length() > 0)
				ircSession.privMsg(target, pluginHelp);
			List<String> commands = new ArrayList<String>();
			commands.addAll(pluginManager.getPluginCommands(plugin));
			Collections.sort(commands);
			helpForCommands(target, commands);
		}
	}

	public void helpForCommands(String target, List<String> commands) {
		for(String command : commands) {
			ircSession.privMsg(target, "  " + command + ": " + getPluginHelp(command));
		}
	}

	@Override
	public String help() {
		return "";
	}
}
