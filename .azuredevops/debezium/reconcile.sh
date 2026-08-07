#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  reconcile.sh <dev|hml|prod> [--dry-run]

Behavior:
  - Loads .env at repository root when present.
  - Loads environment defaults from .azuredevops/config/<env>/debezium.defaults.env.
  - Applies namespace, secrets and DebeziumServer manifests idempotently.
EOF
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" || $# -lt 1 ]]; then
  usage
  exit 0
fi

ENVIRONMENT="$1"
shift

DRY_RUN=false
if [[ ${1:-} == "--dry-run" ]]; then
  DRY_RUN=true
fi

case "$ENVIRONMENT" in
  dev|hml|prod) ;;
  *)
    echo "Invalid environment: $ENVIRONMENT"
    usage
    exit 1
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

GLOBAL_ENV_FILE="$REPO_ROOT/.env"
DEFAULTS_FILE="$REPO_ROOT/.azuredevops/config/$ENVIRONMENT/debezium.defaults.env"

if [[ -f "$GLOBAL_ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$GLOBAL_ENV_FILE"
  set +a
fi

if [[ ! -f "$DEFAULTS_FILE" ]]; then
  echo "Defaults file not found: $DEFAULTS_FILE"
  exit 1
fi

export ENVIRONMENT
set -a
# shellcheck disable=SC1090
source "$DEFAULTS_FILE"
set +a

: "${DEBEZIUM_NAMESPACE:=debezium}"
: "${DEBEZIUM_SERVER_NAME:=postgres-cdc-credit-consent-$ENVIRONMENT}"
: "${DEBEZIUM_VERSION:=3.6}"

export DEBEZIUM_NAMESPACE
export DEBEZIUM_SERVER_NAME
export DEBEZIUM_VERSION

required_vars=(
  DEBEZIUM_DATABASE_HOST
  DEBEZIUM_DATABASE_PORT
  DEBEZIUM_DATABASE_NAME
  DEBEZIUM_DATABASE_USER
  DEBEZIUM_DATABASE_PASSWORD
  DEBEZIUM_BOOTSTRAP_SERVERS
  DEBEZIUM_API_KEY
  DEBEZIUM_API_SECRET
  DEBEZIUM_SCHEMA_REGISTRY_URL
  DEBEZIUM_SCHEMA_REGISTRY_KEY
  DEBEZIUM_SCHEMA_REGISTRY_SECRET
  DEBEZIUM_PUBLICATION_NAME
  DEBEZIUM_SLOT_NAME
  DEBEZIUM_TOPIC_PREFIX
  DEBEZIUM_SCHEMA_INCLUDE_LIST
  DEBEZIUM_TABLE_INCLUDE_LIST
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required variable: $var_name"
    exit 1
  fi
  if [[ "${!var_name}" == *'$('*')'* ]]; then
    echo "Variable looks unresolved from Azure DevOps substitution: $var_name=${!var_name}"
    exit 1
  fi
done

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl not found in PATH"
  exit 1
fi

if ! command -v envsubst >/dev/null 2>&1; then
  echo "envsubst not found in PATH (install gettext)"
  exit 1
fi

if ! kubectl get crd debeziumservers.debezium.io >/dev/null 2>&1; then
  echo "DebeziumServer CRD not found. Install Debezium Operator before reconcile."
  exit 1
fi

render_dir="$(mktemp -d)"
trap 'rm -rf "$render_dir"' EXIT

render_template() {
  local input_file="$1"
  local output_file="$2"
  envsubst \
    '${DEBEZIUM_NAMESPACE} ${DEBEZIUM_SERVER_NAME} ${DEBEZIUM_VERSION} ${DEBEZIUM_BOOTSTRAP_SERVERS} ${DEBEZIUM_DATABASE_HOST} ${DEBEZIUM_DATABASE_PORT} ${DEBEZIUM_DATABASE_NAME} ${DEBEZIUM_PUBLICATION_NAME} ${DEBEZIUM_SLOT_NAME} ${DEBEZIUM_TOPIC_PREFIX} ${DEBEZIUM_SCHEMA_INCLUDE_LIST} ${DEBEZIUM_TABLE_INCLUDE_LIST} ${DEBEZIUM_DATABASE_USER} ${DEBEZIUM_DATABASE_PASSWORD} ${DEBEZIUM_API_KEY} ${DEBEZIUM_API_SECRET} ${DEBEZIUM_SCHEMA_REGISTRY_URL} ${DEBEZIUM_SCHEMA_REGISTRY_KEY} ${DEBEZIUM_SCHEMA_REGISTRY_SECRET}' \
    < "$input_file" > "$output_file"
}

render_template "$SCRIPT_DIR/namespace.yaml.tmpl" "$render_dir/namespace.yaml"
render_template "$SCRIPT_DIR/postgres-secret.yaml.tmpl" "$render_dir/postgres-secret.yaml"
render_template "$SCRIPT_DIR/confluent-secret.yaml.tmpl" "$render_dir/confluent-secret.yaml"
render_template "$SCRIPT_DIR/debezium-server.yaml.tmpl" "$render_dir/debezium-server.yaml"

apply_manifest() {
  local manifest="$1"
  if [[ "$DRY_RUN" == "true" ]]; then
    kubectl apply --dry-run=client -f "$manifest" >/dev/null
  else
    kubectl apply -f "$manifest"
  fi
}

apply_manifest "$render_dir/namespace.yaml"
apply_manifest "$render_dir/postgres-secret.yaml"
apply_manifest "$render_dir/confluent-secret.yaml"
apply_manifest "$render_dir/debezium-server.yaml"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "Dry-run completed successfully for environment: $ENVIRONMENT"
  exit 0
fi

echo "Waiting for DebeziumServer readiness..."
for _ in $(seq 1 30); do
  status="$(kubectl get debeziumserver "$DEBEZIUM_SERVER_NAME" -n "$DEBEZIUM_NAMESPACE" -o jsonpath='{range .status.conditions[*]}{.type}={.status}{"\n"}{end}' 2>/dev/null || true)"
  if echo "$status" | grep -Eq 'Ready=True|Available=True'; then
    echo "DebeziumServer is ready: $DEBEZIUM_SERVER_NAME"
    exit 0
  fi
  sleep 10
done

echo "DebeziumServer did not become ready within timeout"
kubectl get debeziumserver "$DEBEZIUM_SERVER_NAME" -n "$DEBEZIUM_NAMESPACE" -o yaml || true
exit 1
