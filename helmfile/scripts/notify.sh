#!/usr/bin/env bash

status=${1}
release=${2}
slack_url_var=${3}
slack_url=${!slack_url_var}
commit_author=${UPSTREAM_COMMIT_AUTHOR:-$CI_COMMIT_AUTHOR}
pipeline_triggerer=${UPSTREAM_USER_NAME:-$GITLAB_USER_NAME}


if [ $SLACK_NOTIFICATIONS_ENABLED  == "false" ]; then
    echo "Notifications are disabled. Skipping."
    exit 0
fi

if [ $status  == "success" ]; then
    echo "Release ${release} was successful. Skipping notification."
    exit 0
fi

echo Release ${release} failed. Sending notification to ${slack_url_var}

get_notification_message() {
  cat <<EOF
{
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "plain_text",
        "text": "Can somebody take a look? Thanks :heart:",
        "emoji": true
      }
    }
  ],
  "attachments": [{
    "color": "#FF0707",
    "title": "Release ${release} failed on ${Environment} :firefine:",
    "fields": [
      {
        "value": "*Environment:* ${Environment}"
      },
      {
        "value": "*Service:* ${release}"
      },
      {
        "value": "*Commit author:* ${commit_author}"
      },
      {
        "value": "*Triggered by:* ${pipeline_triggerer}"
      },
      {
        "value": "*Job URL:* ${CI_JOB_URL}"
      }
    ]
	}]
}
EOF
}

curl -X POST -H 'Content-type: application/json' --data "$(get_notification_message)" ${slack_url}
