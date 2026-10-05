#!/usr/bin/env bash
# Fills in any secret in services/.env that is still missing or still a placeholder.
#
# Why this exists: the order service authenticates to Keycloak as the confidential
# client ecom-order-service. A confidential client needs a shared secret, and Keycloak
# needs it at realm-import time to create the client. Committing a working secret
# would mean publishing a credential that grants SERVICE-role tokens to anyone who
# clones the repo. So the realm import carries a "${ORDER_SERVICE_CLIENT_SECRET}"
# placeholder, Keycloak substitutes the environment variable on boot, and the value
# itself only ever lives in .env, which is gitignored.
#
# Run once before `docker compose up`. Safe to re-run: existing values are left alone.
set -euo pipefail

cd "$(dirname "$0")"

ENV_FILE=".env"
EXAMPLE_FILE=".env.example"
GENERATED=0

if [ ! -f "$ENV_FILE" ]; then
    if [ ! -f "$EXAMPLE_FILE" ]; then
        echo "error: neither .env nor .env.example exists, nothing to do" >&2
        exit 1
    fi
    cp "$EXAMPLE_FILE" "$ENV_FILE"
    echo "created .env from .env.example"
fi

# Prints nothing and returns 1 when the key is absent or still a "change-me" placeholder.
needs_value() {
    local key="$1" current
    current="$(grep -E "^${key}=" "$ENV_FILE" | head -1 | cut -d= -f2- || true)"
    [ -z "$current" ] || [ "${current#change-me}" != "$current" ]
}

generate_hex() {
    # Hex only, so the value needs no quoting in .env, no escaping in the realm import
    # and no URL-encoding when it is posted to the token endpoint.
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -hex 32
    else
        head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
    fi
}

set_value() {
    local key="$1" value="$2"
    if grep -qE "^${key}=" "$ENV_FILE"; then
        # Value chosen so the delimiter is unambiguous regardless of what is in the secret.
        sed -i "s|^${key}=.*|${key}=${value}|" "$ENV_FILE"
    else
        printf '%s=%s\n' "$key" "$value" >>"$ENV_FILE"
    fi
}

if needs_value ORDER_SERVICE_CLIENT_SECRET; then
    SECRET="$(generate_hex)"
    set_value ORDER_SERVICE_CLIENT_SECRET "$SECRET"
    echo "generated ORDER_SERVICE_CLIENT_SECRET (32 bytes hex, stored only in .env)"
    GENERATED=1
fi

if [ "$GENERATED" -eq 0 ]; then
    echo "no changes needed, .env already has usable values"
fi

echo
echo "The order service reads this from .env at boot and Keycloak substitutes it into the"
echo "realm import. Both sides must agree, which they do because they read the same file."
echo
echo "If you ever rotate it, delete the Keycloak data volume as well, otherwise the already"
echo "imported realm keeps the old secret and the order service will fail to get a token:"
echo "  docker compose down -v && ./init-secrets.sh && docker compose up -d"