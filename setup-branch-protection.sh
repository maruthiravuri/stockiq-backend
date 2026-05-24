#!/bin/bash
# setup-branch-protection.sh
# Run once after initial repo creation to configure branch protection rules
# Usage: PAT=<your-token> ./setup-branch-protection.sh

PAT="${PAT:?PAT environment variable required}"
ORG="maruthiravuri"
REPOS=("stockiq-frontend" "stockiq-backend")

for REPO in "${REPOS[@]}"; do
  echo "Configuring branch protection for $REPO..."

  # Protect main branch
  curl -s -X PUT \
    -H "Authorization: token $PAT" \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/repos/$ORG/$REPO/branches/main/protection" \
    -d '{
      "required_status_checks": {
        "strict": true,
        "contexts": ["Build & Test"]
      },
      "enforce_admins": false,
      "required_pull_request_reviews": {
        "dismiss_stale_reviews": true,
        "require_code_owner_reviews": true,
        "required_approving_review_count": 1
      },
      "restrictions": null,
      "required_linear_history": true,
      "allow_force_pushes": false,
      "allow_deletions": false
    }' | python3 -c "import sys,json; d=json.load(sys.stdin); print('main:', d.get('url','OK') or d.get('message','error'))"

  # Protect develop branch
  curl -s -X PUT \
    -H "Authorization: token $PAT" \
    -H "Accept: application/vnd.github.v3+json" \
    "https://api.github.com/repos/$ORG/$REPO/branches/develop/protection" \
    -d '{
      "required_status_checks": {
        "strict": true,
        "contexts": ["Build & Test"]
      },
      "enforce_admins": false,
      "required_pull_request_reviews": {
        "dismiss_stale_reviews": false,
        "require_code_owner_reviews": false,
        "required_approving_review_count": 1
      },
      "restrictions": null,
      "allow_force_pushes": false,
      "allow_deletions": false
    }' | python3 -c "import sys,json; d=json.load(sys.stdin); print('develop:', d.get('url','OK') or d.get('message','error'))"

  echo "Done: $REPO"
done
echo "Branch protection configured for all repos."
