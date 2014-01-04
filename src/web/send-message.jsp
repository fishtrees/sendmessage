<%@ page import="java.util.*,
                 org.jivesoftware.openfire.XMPPServer,
                 org.jivesoftware.util.*,
                 com.dinodirect.openfire.plugin.SendMessagePlugin"
    errorPage="error.jsp"
%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%-- Define Administration Bean --%>
<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager"  />
<c:set var="admin" value="${admin.manager}" />
<% admin.init(request, response, session, application, out ); %>

<%  // Get parameters
    boolean save = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    String secret = ParamUtils.getParameter(request, "secret");
    boolean enabled = ParamUtils.getBooleanParameter(request, "enabled");
    String allowedIPs = ParamUtils.getParameter(request, "allowedIPs");

    SendMessagePlugin plugin = (SendMessagePlugin) XMPPServer.getInstance().getPluginManager().getPlugin("sendmessage");

    // Handle a save
    Map errors = new HashMap();
    if (save) {
        if (errors.size() == 0) {
            plugin.setEnabled(enabled);
        	plugin.setSecret(secret);
            plugin.setAllowedIPs(StringUtils.stringToCollection(allowedIPs));
            response.sendRedirect("send-message.jsp?success=true");
            return;
        }
    }

    secret = plugin.getSecret();
    enabled = plugin.isEnabled();
    allowedIPs = StringUtils.collectionToString(plugin.getAllowedIPs());
%>

<html>
    <head>
        <title>Send Message Properties</title>
        <meta name="pageID" content="send-message"/>
    </head>
    <body>


<p>
Use the form below to enable or disable the Send Message and configure the secret key.
By default the Send Message plugin is <strong>disabled</strong>, which means that
HTTP requests to the service will be ignored.
</p>

<%  if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
        <td class="jive-icon-label">
            Send Message properties edited successfully.
        </td></tr>
    </tbody>
    </table>
    </div><br>
<% } %>

<form action="send-message.jsp?save" method="post">

<fieldset>
    <legend>Send Message</legend>
    <div>

    <p>However, the presence of this service exposes a security risk. Therefore,
    a secret key is used to validate legitimate requests to this service. Moreover,
    for extra security you can specify the list of IP addresses that are allowed to
    use this service. An empty list means that the service can be accessed from any
    location. Addresses are delimited by commas.
    </p>
    <ul>
        <input type="radio" name="enabled" value="true" id="rb01"
        <%= ((enabled) ? "checked" : "") %>>
        <label for="rb01"><b>Enabled</b> - Send Message requests will be processed.</label>
        <br>
        <input type="radio" name="enabled" value="false" id="rb02"
         <%= ((!enabled) ? "checked" : "") %>>
        <label for="rb02"><b>Disabled</b> - Send Message requests will be ignored.</label>
        <br><br>

        <label for="text_secret">Secret key:</label>
        <input type="text" name="secret" value="<%= secret %>" id="text_secret">
        <br><br>

        <label for="text_secret">Allowed IP Addresses:</label>
        <textarea name="allowedIPs" cols="40" rows="3" wrap="virtual"><%= ((allowedIPs != null) ? allowedIPs : "") %></textarea>
    </ul>
    </div>
</fieldset>

<br><br>

<input type="submit" value="Save Settings">
</form>


</body>
</html>