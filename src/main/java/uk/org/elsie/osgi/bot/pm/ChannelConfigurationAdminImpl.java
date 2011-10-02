package uk.org.elsie.osgi.bot.pm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import uk.org.elsie.osgi.bot.Channel;
import uk.org.elsie.osgi.bot.IrcEventConstants;
import uk.org.elsie.osgi.bot.IrcSessionService;
import uk.org.elsie.osgi.bot.PluginProvider;
import uk.org.elsie.osgi.bot.PropertiesUtil;

@Plugin(name="channel configuration admin plugin")
public class ChannelConfigurationAdminImpl implements PluginProvider {

	private static Log log = LogFactory.getLog(PrivmsgPluginCommandRunnerImpl.class);
	
	private IrcSessionService ircSession;
	private ConfigurationAdmin configAdmin;
	private Map<String, Object> properties = Collections.emptyMap();
	
	public void activate(BundleContext context, Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public void modified(Map<String, Object> properties) {
		this.properties = PropertiesUtil.propertiesAsMap(properties);
	}
	
	public void deactivate(BundleContext context) {
		
	}
	
	public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}
	
	public void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = null;
	}
	
	public void setIrcSession(IrcSessionService connectionService) {
		this.ircSession = connectionService;
	}
	
	public void unsetIrcSession(IrcSessionService connection) {
		this.ircSession = null;
	}
	
	public Configuration getChannelConfig(String channelName) throws Exception {
		Configuration cs[] = configAdmin.listConfigurations("(&(" + IrcEventConstants.IRC_CHANNEL + "=" + channelName + ")(service.factoryPid=" + Channel.class.getName() + "))");
		if(cs != null && cs.length > 0) {
			return cs[0];
		} else {
			return null;
		}
	}

	@Command(name="join", help="joins a channel")
	public void join(@PrivmsgProperty(name=IrcEventConstants.RETURN_TARGET) String target, @PrivmsgArgs String[] params) throws Exception {
		String channelName = params[0];
		Configuration c = getChannelConfig(channelName);
		if(c == null) {
			c = configAdmin.createFactoryConfiguration(Channel.class.getName());
			Hashtable<String, String> props = new Hashtable<String, String>();
			props.put(IrcEventConstants.IRC_CHANNEL, channelName);
			c.update(props);
			log.info("created channel config for " + channelName);
		} else {
			log.warn("channel config already exists for " + channelName);
		}
	}
	
	@Command(name="part", help="parts a channel")
	public void part(@PrivmsgProperty(name=IrcEventConstants.RETURN_TARGET) String target, @PrivmsgArgs String[] params) throws Exception {
		String channelName = params[0];
		Configuration c = getChannelConfig(channelName);
		if(c != null) {
			c.delete();
			log.info("removed channel config for " + channelName);
		} else {
			log.warn("channel config doesn't exist for " + channelName);
		}
	}

	@Override
	public String help() {
		return "commands to tell elsie to join and part channels";
	}
}
