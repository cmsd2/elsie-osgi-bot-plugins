package uk.org.elsie.osgi.bot.pm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import uk.org.elsie.osgi.bot.IrcEventConstants;
import uk.org.elsie.osgi.bot.IrcSessionService;
import uk.org.elsie.osgi.bot.PluginProvider;
import uk.org.elsie.osgi.bot.PropertiesUtil;

public class PrivmsgPluginCommandRunnerImpl implements EventHandler, PrivmsgPluginCommandRunner {

	public static final String PREFIX = "plugin.command.prefix";
	
	private static Log log = LogFactory.getLog(PrivmsgPluginCommandRunnerImpl.class);
	private Map<String, Object> properties = Collections.emptyMap();
	private PluginCommandManager commandManager;
	private IrcSessionService ircSession;
	
	public void activate(BundleContext context, Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public void modified(Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public void deactivate(BundleContext context) {
		
	}
	
	public void setPluginManager(PluginCommandManager commandManager) {
		this.commandManager = commandManager;
	}
	
	public void unsetPluginManager(PluginCommandManager commandManager) {
		this.commandManager = null;
	}
	
	public void setIrcSession(IrcSessionService irc) {
		this.ircSession = irc;
	}
	
	public void unsetIrcSession(IrcSessionService irc) {
		this.ircSession = null;
	}
	
	/* (non-Javadoc)
	 * @see uk.org.elsie.osgi.bot.pm.PrivmsgPluginCommandRunner#getPrefix()
	 */
	@Override
	public String getPrefix() {
		String prefix = (String) properties.get(PREFIX);
		if(prefix == null)
			return "";
		return prefix;
	}
	
	@Override
	public void handleEvent(Event event) {
		if(!event.getTopic().equals(IrcEventConstants.IRC_PRIVMSG_TOPIC))
			return;
		
		String escapedParams = (String) event.getProperty(IrcEventConstants.ESCAPED_PARAMS);
		if(escapedParams == null)
			return;
		
		String parts[] = escapedParams.split(" +");
		if(parts.length == 0)
			return;
		
		String command = parts[0].replaceFirst("^" + getPrefix(), "");
		String[] commandArgs = new String[parts.length - 1];
		StringBuilder commandArgsJoinedSb = new StringBuilder();
		for(int i = 0; i < commandArgs.length; i++) {
			commandArgs[i] = parts[i+1];
			commandArgsJoinedSb.append(commandArgs[i]);
			if(i > 0)
				commandArgsJoinedSb.append(' ');
		}
		String commandArgsJoined = commandArgsJoinedSb.toString();
					
		PluginProvider plugin = commandManager.getPlugin(command);
		if(plugin == null) {
			log.info("no plugin found for command " + command);
			return;
		}
					
		Method m = commandManager.getCommand(plugin, command);
		if(m == null) {
			log.warn("no plugin method found for command " + command);
			return;
		}
		
		Class<?>[] argClasses = m.getParameterTypes();
		Object[] args = new Object[argClasses.length];
		Annotation[][] paramAnnotations = m.getParameterAnnotations();
		
		for(int i = 0; i < args.length; i++) {
			if(argClasses[i].isAssignableFrom(Event.class))
				args[i] = event;
			else if(paramAnnotations.length > 0) {
				for(int j = 0; j < paramAnnotations[i].length; j++) {
					if(PrivmsgProperty.class.isAssignableFrom(paramAnnotations[i][j].getClass())) {
						args[i] = event.getProperty(((PrivmsgProperty)paramAnnotations[i][j]).name());
					} else if(PrivmsgCommand.class.isAssignableFrom(paramAnnotations[i][j].getClass())) {
						args[i] = command;
					} else if(PrivmsgArgs.class.isAssignableFrom(paramAnnotations[i][j].getClass())) {
						if(argClasses[i].isAssignableFrom(String.class))
							args[i] = commandArgsJoined;
						else if(argClasses[i].isAssignableFrom(String[].class))
							args[i] = commandArgs;
					}
				}
			}
		}
		
		try {
			log.info("invoking " + m.toString() + " for " + escapedParams);
			m.invoke(plugin, args);
		} catch (Exception e) {
			log.error("error invoking method " + m.toString() + " for " + escapedParams, e);
			ircSession.reply(event, "error running command " + escapedParams + ": " + e.toString());
		}
	}

}
