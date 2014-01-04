package com.dinodirect.openfire.plugin.sendMessage;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.XMPPServer;
import com.dinodirect.openfire.plugin.SendMessagePlugin;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.ComponentException;

/**
 * Servlet that send a XMPP message from administrator. The request <b>MUST</b>
 * include the <b>secret</b> parameter. This parameter will be used to
 * authenticate the request. If this parameter is missing from the request then
 * an error will be logged and no action will occur.
 *
 * @author Yu Lin
 */
public class SendMessageServlet extends HttpServlet {

    private static final Logger Log = LoggerFactory.getLogger(SendMessageServlet.class);
    private SendMessagePlugin plugin;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        plugin = (SendMessagePlugin) XMPPServer.getInstance().getPluginManager().getPlugin("sendmessage");

        // Exclude this servlet from requiring the user to login
        AuthCheckFilter.addExclude("sendMessage/sendmessage");
    }

    @Override
    public void destroy() {
        super.destroy();
        // Release the excluded URL
        AuthCheckFilter.removeExclude("sendMessage/sendmessage");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Printwriter for writing out responses to browser
        PrintWriter out = response.getWriter();
        String ipAddress = request.getRemoteAddr();
        if (!checkIPAddressAllowed(ipAddress)) {
            Log.warn("User service rejected service to IP address: " + ipAddress);
            replyError(ErrorCode.NotAllowedIPAddress, "forbidden", response, out);
            return;
        }
        // Check that our plugin is enabled.
        if (!plugin.isEnabled()) {
            Log.warn("User service plugin is disabled: " + request.getQueryString());
            replyError(ErrorCode.SendMessageDisabled, "disabled", response, out);
            return;
        }

        String secret = request.getParameter("secret");
        // Check this request is authorised
        if (secret == null || !secret.equals(plugin.getSecret())) {
            Log.warn("An unauthorised request was received.");
            replyError(ErrorCode.NotAuthorized, "unauthorised", response, out);
            return;
        }

        String fromUserName = request.getParameter("fromUserName");
        String fromResource = request.getParameter("fromResource");
        String toUserName = request.getParameter("toUserName");
        String content = request.getParameter("content");

        if (fromUserName == null) {
            replyError(ErrorCode.InvalidArgument, "invalid argumnet: fromUserName", response, out);
            return;
        }
        if (fromResource == null) {
            replyError(ErrorCode.InvalidArgument, "invalid argumnet: fromResource", response, out);
            return;
        }
        if (toUserName == null) {
            replyError(ErrorCode.InvalidArgument, "invalid argumnet: toUserName", response, out);
            return;
        }
        if (content == null) {
            replyError(ErrorCode.InvalidArgument, "invalid argumnet: content", response, out);
            return;
        }
        try {
            plugin.send(fromUserName, fromResource, toUserName, content);
            replyOK(response, out);
        } catch (UserNotFoundException ex) {
            replyError(ErrorCode.UserNotFound, "user not found, " + ex.getMessage(), response, out);
        } catch (ComponentException ex) {
            replyError(ErrorCode.SendMessageFailed, "SendMessageFailed", response, out);
        }
    }

    private boolean checkIPAddressAllowed(String ipAddress) {
        if (!plugin.getAllowedIPs().isEmpty()) {
            if (!plugin.getAllowedIPs().contains(ipAddress)) {
                return false;
            }
        }
        return true;
    }

    private void replyOK(HttpServletResponse response, PrintWriter out) {
        response.setContentType("text/json");
        out.print("{\"message\":\"OK\",");
        out.print("\"code\":0}");
        out.flush();
    }

    private void replyError(ErrorCode code, String message, HttpServletResponse response, PrintWriter out) {
        response.setContentType("text/json");
        out.print("{\"message\":\"" + message + "\",");
        out.print("\"code\":" + code.getValue() + "}");
        out.flush();
    }

    enum ErrorCode {

        None(0),
        InvalidArgument(400000),
        NotAuthorized(401000),
        SendMessageFailed(500001),
        SendMessageDisabled(403001),
        NotAllowedIPAddress(403002),
        UserNotFound(404001);

        private final int value;

        ErrorCode(int aValue) {
            value = aValue;
        }

        public int getValue() {
            return value;
        }

    }
}
