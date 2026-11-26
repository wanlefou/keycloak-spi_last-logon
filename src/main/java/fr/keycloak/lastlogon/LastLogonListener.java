package fr.keycloak.lastlogon;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

final class LastLogonListener implements EventListenerProvider {
    private static final String ATTRIBUTE = "lastlogon";
    private final KeycloakSession session;

    LastLogonListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.LOGIN || event.getError() != null
                || event.getUserId() == null || event.getTime() <= 0) {
            return;
        }

        RealmModel realm = session.getContext().getRealm();
        if (realm == null || !realm.getId().equals(event.getRealmId())) {
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null || user.getServiceAccountClientLink() != null
                || user.getFederationLink() != null || user.getId().startsWith("f:")) {
            return;
        }

        if (event.getTime() > previousLogon(user)) {
            user.setSingleAttribute(ATTRIBUTE, Long.toString(event.getTime()));
        }
    }

    private static long previousLogon(UserModel user) {
        String value = user.getFirstAttribute(ATTRIBUTE);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }
}
