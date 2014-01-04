package com.dinodirect.openfire.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.StringUtils;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.Message;

/**
 * Plugin that allows sending offline files via HTTP requests.
 *
 * @author Yu Lin
 */
public class SendMessagePlugin implements Plugin, Component, PropertyEventListener {

    private static final Logger Log = LoggerFactory.getLogger(SendMessagePlugin.class);
    private static final String SERVICE_NAME = "com.dinodirect.openfire.plugin.sendMessage";

    private UserManager userManager;
    private XMPPServer server;
    private ComponentManager componentManager;
    private PluginManager pluginManager;

    private String secret;
    private boolean enabled;
    private Collection<String> allowedIPs;

    
    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        server = XMPPServer.getInstance();
        userManager = server.getUserManager();
        pluginManager = manager;

        componentManager = ComponentManagerFactory.getComponentManager();

        secret = JiveGlobals.getProperty("plugin.sendmessage.secret", "");
        // If no secret key has been assigned to the user service yet, assign a random one.
        if (secret.equals("")) {
            secret = StringUtils.randomString(8);
            setSecret(secret);
        }

        // See if the service is enabled or not.
        enabled = JiveGlobals.getBooleanProperty("plugin.sendmessage.enabled", false);
        // Get the list of IP addresses that can use this service. An empty list means that this filter is disabled.
        allowedIPs = StringUtils.stringToCollection(JiveGlobals.getProperty("plugin.sendmessage.allowedIPs", ""));
        // Register as a component.
        try {
            componentManager.addComponent(SERVICE_NAME, this);
        } catch (ComponentException e) {
            Log.error(e.getMessage(), e);
        }
        // Listen to system property events
        PropertyEventDispatcher.addListener(this);
    }

    
    public void destroyPlugin() {
        userManager = null;
        // Stop listening to system property events
        PropertyEventDispatcher.removeListener(this);
    }

    
    public String getName() {
        return pluginManager.getName(this);
    }

    
    public String getDescription() {
        return pluginManager.getDescription(this);
    }

    
    public void processPacket(Packet packet) {
        // do nothing
    }

    
    public void initialize(JID jid, ComponentManager cm) throws ComponentException {
        // do nothing
    }

    
    public void start() {
        // do nothing
    }

    
    public void shutdown() {
        // do nothing
    }

    public void send(String fromUserName, String fromResource, String toUserName, String content) throws UserNotFoundException, ComponentException {

        if (!enabled) {
            throw new java.lang.IllegalStateException("plugin is disabled.");
        }
        if (null == content || "".equals(content)) {
            return;
        }
        // check if user exists
        getUser(fromUserName);
        getUser(toUserName);

        JID fromJID = server.createJID(fromUserName, fromResource);
        JID toJID = server.createJID(toUserName, null);
        Message msg = new Message();
        msg.setFrom(fromJID);
        msg.setTo(toJID);
        msg.setBody(content);

        componentManager.sendPacket(this, msg);
    }

    /**
     * Returns the the requested user or <tt>null</tt> if there are any problems
     * that don't throw an error.
     *
     * @param username the username of the local user to retrieve.
     * @return the requested user.
     * @throws UserNotFoundException if the requested user does not exist in the
     * local server.
     */
    private User getUser(String username) throws UserNotFoundException {
        JID targetJID = server.createJID(username, null);
        // Check that the sender is not requesting information of a remote server entity
        if (targetJID.getNode() == null) {
            // Sender is requesting presence information of an anonymous user
            throw new UserNotFoundException("Username is null");
        }
        return userManager.getUser(targetJID.getNode());
    }

    /**
     * Returns the secret key that only valid requests should know.
     *
     * @return the secret key.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the secret key that grants permission to use the sendmessage.
     *
     * @param secret the secret key.
     */
    public void setSecret(String secret) {
        JiveGlobals.setProperty("plugin.sendmessage.secret", secret);
        this.secret = secret;
    }

    public Collection<String> getAllowedIPs() {
        return allowedIPs;
    }

    public void setAllowedIPs(Collection<String> allowedIPs) {
        JiveGlobals.setProperty("plugin.sendmessage.allowedIPs", StringUtils.collectionToString(allowedIPs));
        this.allowedIPs = allowedIPs;
    }

    /**
     * Returns true if the user service is enabled. If not enabled, it will not
     * accept requests to create new accounts.
     *
     * @return true if the user service is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the user service. If not enabled, it will not accept
     * requests to create new accounts.
     *
     * @param enabled true if the user service should be enabled.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        JiveGlobals.setProperty("plugin.sendmessage.enabled", enabled ? "true" : "false");
    }

    
    public void propertySet(String property, Map<String, Object> params) {
        if (property.equals("plugin.sendmessage.secret")) {
            this.secret = (String) params.get("value");
        } else if (property.equals("plugin.sendmessage.enabled")) {
            this.enabled = Boolean.parseBoolean((String) params.get("value"));
        } else if (property.equals("plugin.sendmessage.allowedIPs")) {
            this.allowedIPs = StringUtils.stringToCollection((String) params.get("value"));
        }
    }

    
    public void propertyDeleted(String property, Map<String, Object> params) {
        if (property.equals("plugin.sendmessage.secret")) {
            this.secret = "";
        } else if (property.equals("plugin.sendmessage.enabled")) {
            this.enabled = false;
        } else if (property.equals("plugin.sendmessage.allowedIPs")) {
            this.allowedIPs = Collections.emptyList();
        }
    }

    
    public void xmlPropertySet(String property, Map<String, Object> params) {
        // Do nothing
    }

    
    public void xmlPropertyDeleted(String property, Map<String, Object> params) {
        // Do nothing
    }
}
