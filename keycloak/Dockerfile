FROM quay.io/keycloak/keycloak:24.0.1 as builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure database vendor
ENV KC_DB=postgres


WORKDIR /opt/keycloak
# Build Keycloak with the MySQL database support
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:24.0.1
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Copy the realm configuration file to be imported
COPY fitness-realm-realm.json /opt/keycloak/data/import/fitness-realm-realm.json

# Entrypoint configures keycloak to import the realm and start in optimized mode
# Provide DB connection details at runtime via env variables:
# KC_DB_URL, KC_DB_USERNAME, KC_DB_PASSWORD
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", "--optimized", "--import-realm"]
