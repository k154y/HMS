#!/usr/bin/env bash
set -euo pipefail
umask 077

sha=${1:?A full Git SHA is required}
health_url=${2:?The public HTTPS API health URL is required}
[[ "$sha" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid release SHA.' >&2; exit 1; }
[[ "$health_url" =~ ^https://[a-zA-Z0-9.-]+(:[0-9]+)?/[a-zA-Z0-9_./-]+$ ]] || { echo 'Invalid public health URL.' >&2; exit 1; }

# Directory override is for the isolated deployment test harness only.
cd "${HMS_DEPLOY_DIRECTORY:-/opt/hms/production}"
exec 9>.deployment.lock
flock -w 60 9 || { echo 'Another deployment holds the production lock.' >&2; exit 1; }

safe_logs() {
  # Do not dump arbitrary application/SQL/provider logs into GitHub Actions.
  docker logs --tail 100 hms_production_api 2>&1 | python3 -c '
import re,sys
found=False
for line in sys.stdin:
    match=re.search(r"event=unexpected_api_failure requestId=([A-Za-z0-9._-]{1,64}) exceptionType=([A-Za-z0-9_.$]+)",line)
    if match:
        print("Application failure reference="+match[1]+" type="+match[2]); found=True
    elif "Flyway" in line and any(word in line.lower() for word in ("error","fail","exception")):
        print("Flyway reported a startup/migration failure; inspect protected VPS logs."); found=True
if not found: print("No allowlisted diagnostic events found; inspect protected VPS logs.")
' || true
}
failed() {
  trap - ERR
  echo "Deployment failed for revision $sha. No automatic database or application rollback was attempted." >&2
  safe_logs
  exit 1
}
trap failed ERR

test -f .env
test -f docker-compose.yml
previous=$(python3 - <<'PY'
import pathlib,re
matches=re.findall(rb'^HMS_IMAGE_TAG=([^\r\n]*)',pathlib.Path('.env').read_bytes(),re.M)
if len(matches)!=1: raise SystemExit('Expected exactly one HMS_IMAGE_TAG entry; .env was not modified.')
tag=matches[0].decode().strip().strip('"\x27')
print(tag if re.fullmatch(r'[A-Za-z0-9_.-]{1,128}',tag) else '[nonstandard tag]')
PY
)
echo "Deploying revision $sha; previous image tag: $previous"
backup=".env.backup.$(date -u +%Y%m%dT%H%M%S).$sha"
cp -- .env "$backup"
chmod 600 "$backup"

image="ghcr.io/k154y/hms-api:$sha"
docker pull "$image"
revision=$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$image")
[[ "$revision" == "$sha" ]] || { echo 'Pulled image revision does not match the built SHA.' >&2; false; }

# Preserve every other byte/variable. Never source or print the secrets file.
python3 - "$sha" <<'PY'
import os,pathlib,re,sys,tempfile
path=pathlib.Path('.env');data=path.read_bytes()
updated,count=re.subn(rb'(?m)^HMS_IMAGE_TAG=[^\r\n]*',b'HMS_IMAGE_TAG='+sys.argv[1].encode(),data)
if count!=1: raise SystemExit('Expected exactly one image tag entry.')
fd,name=tempfile.mkstemp(prefix='.env.release.',dir='.')
try:
    with os.fdopen(fd,'wb') as out: out.write(updated);out.flush();os.fsync(out.fileno())
    os.chmod(name,0o600);os.replace(name,path)
finally:
    if os.path.exists(name): os.unlink(name)
PY

docker compose --env-file .env -f docker-compose.yml up -d --force-recreate --no-deps api
deadline=$((SECONDS + 300))
healthy=false
while ((SECONDS < deadline)); do
  state=$(docker inspect --format '{{.State.Status}}' hms_production_api)
  health=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' hms_production_api)
  if [[ "$state" == running && "$health" == healthy ]]; then healthy=true; break; fi
  if [[ "$state" == exited || "$state" == dead || "$health" == missing ]]; then break; fi
  sleep 5
done
[[ "$healthy" == true ]] || { echo 'API did not become healthy within the deployment timeout.' >&2; false; }
running_image=$(docker inspect --format '{{.Image}}' hms_production_api)
running_revision=$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "$running_image")
[[ "$running_revision" == "$sha" ]] || { echo 'Running image revision mismatch.' >&2; false; }

# Bounded retries allow the reverse proxy to reconnect to the recreated API.
public_up=false
for attempt in {1..12}; do
  if curl --fail --silent --show-error --max-time 10 "$health_url" | python3 -c 'import json,sys;sys.exit(0 if json.load(sys.stdin).get("status")=="UP" else 1)' 2>/dev/null; then
    public_up=true; break
  fi
  sleep 5
done
[[ "$public_up" == true ]] || { echo 'Public API health verification failed.' >&2; false; }
echo "Production deployment healthy at revision $sha. Flyway ran through normal application startup."
