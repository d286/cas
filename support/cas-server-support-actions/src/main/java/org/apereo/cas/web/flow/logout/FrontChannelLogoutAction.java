package org.apereo.cas.web.flow.logout;

import org.apereo.cas.logout.LogoutHttpMessage;
import org.apereo.cas.logout.LogoutManager;
import org.apereo.cas.logout.LogoutRequestStatus;
import org.apereo.cas.logout.slo.SingleLogoutRequest;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.WebUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * Logout action for front SLO : find the next eligible service and perform front logout.
 *
 * @author Jerome Leleu
 * @since 4.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class FrontChannelLogoutAction extends AbstractLogoutAction {

    private final LogoutManager logoutManager;

    @Override
    protected Event doInternalExecute(final HttpServletRequest request, final HttpServletResponse response,
                                      final RequestContext context) {

        val logoutRequests = WebUtils.getLogoutRequests(context);
        val logoutUrls = new HashMap<SingleLogoutRequest, LogoutHttpMessage>();

        if (logoutRequests != null) {
            logoutRequests.stream()
                .filter(r -> r.getStatus() == LogoutRequestStatus.NOT_ATTEMPTED)
                .forEach(r -> {
                    LOGGER.debug("Using logout url [{}] for front-channel logout requests", r.getLogoutUrl().toExternalForm());
                    val logoutMessage = this.logoutManager.createFrontChannelLogoutMessage(r);
                    LOGGER.debug("Front-channel logout message to send is [{}]", logoutMessage);
                    val msg = new LogoutHttpMessage(r.getLogoutUrl(), logoutMessage, true);
                    logoutUrls.put(r, msg);
                    r.setStatus(LogoutRequestStatus.SUCCESS);
                    r.getService().setLoggedOutAlready(true);
                });

            if (!logoutUrls.isEmpty()) {
                context.getFlowScope().put("logoutUrls", logoutUrls);
                return new EventFactorySupport().event(this, CasWebflowConstants.TRANSITION_ID_PROPAGATE);
            }
        }
        return new EventFactorySupport().event(this, CasWebflowConstants.TRANSITION_ID_FINISH);
    }
}
