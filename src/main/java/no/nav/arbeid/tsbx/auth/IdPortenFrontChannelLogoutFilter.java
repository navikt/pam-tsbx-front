package no.nav.arbeid.tsbx.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Checks if local session should be invalidated due to a previously received front channel logout signal.
 */
public class IdPortenFrontChannelLogoutFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(IdPortenFrontChannelLogoutFilter.class);

    private final  UserSession sessionProvider;

    private final IdPortenFrontChannelLogoutEventStore frontChannelLogoutEventStore;

    public IdPortenFrontChannelLogoutFilter(UserSession sessionProvider, IdPortenFrontChannelLogoutEventStore frontChannelLogoutEventStore) {
        this.sessionProvider = sessionProvider;
        this.frontChannelLogoutEventStore = frontChannelLogoutEventStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        sessionProvider.authenticatedUser().map(au -> au.idPortenSession().idPortenSessionIdentifier())
                .ifPresent(sid -> {
                    if (frontChannelLogoutEventStore.timeOfLogout(sid).isPresent()) {
                        // If we have a registered id-porten logout, then invalidate local session before normal request handling
                        LOG.info("Invalidating session for sid {} due to front channel logout", sid);
                        sessionProvider.invalidate();
                        frontChannelLogoutEventStore.remove(sid);
                    }
                });

        filterChain.doFilter(request, response);
    }

}
