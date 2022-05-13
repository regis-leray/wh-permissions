#!/usr/bin/env bash
set -e

NAMESPACE=${1}
xInsertKey=${2}
newRelicAccount=${3:-2666572}
if [[ -z "$xInsertKey" || -z "$NAMESPACE" ]]; then
    echo "Usage: $0 <namespace> <xInsertKey> <newRelicAccount>"
    exit 1
fi

helm ls --namespace="$NAMESPACE" --output json  > service_versions.json

cat service_versions.json | jq --arg NAMESPACE "$NAMESPACE" '. += [{"NAMESPACE": $NAMESPACE}]' | jq '[.[] | .["eventType"] = "UnityVersionsBucket"]' > versions.json

gzip -c versions.json | curl --data-binary @- -X POST -H "Content-Type: application/json" -H "X-Insert-Key: $xInsertKey" -H "Content-Encoding: gzip" https://insights-collector.newrelic.com/v1/accounts/"$newRelicAccount"/events
