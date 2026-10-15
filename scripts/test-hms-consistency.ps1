param(
    [string]$Tests = 'CommandConsistencyTest,FinancialConfigurationTest,OperationsTest',
    [string]$MavenCacheVolume = 'hms_hotel_hms_maven_cache',
    [switch]$ContainerBuild
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$project = 'hms-consistency-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
$compose = Join-Path $repo 'docker-compose.test.yml'
$image = 'maven:3.9-eclipse-temurin-21'

# Existing images and dependencies only. Never inherit the application's .env.
docker image inspect $image --format '{{.Id}}'
if ($LASTEXITCODE -ne 0) { throw 'Existing Maven/Java 21 image is required.' }
docker volume inspect $MavenCacheVolume --format '{{.Name}}'
if ($LASTEXITCODE -ne 0) { throw 'Existing Maven dependency cache is required.' }
try {
    docker compose -p $project -f $compose up -d --pull never --wait --wait-timeout 60
    if ($LASTEXITCODE -ne 0) { throw 'Disposable test services failed to start.' }
    $postgres = "$project-test-postgres-1"
    $inspection = docker inspect $postgres | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or -not $inspection[0].HostConfig.Tmpfs.PSObject.Properties['/var/lib/postgresql/data']) {
        throw 'PostgreSQL is not using the required disposable tmpfs.'
    }
    $identity = docker exec $postgres psql -U hms_test -d hms_test -Atc 'select current_database(), current_user'
    if ($LASTEXITCODE -ne 0 -or $identity.Trim() -ne 'hms_test|hms_test') {
        throw 'Unexpected test database identity.'
    }
    Write-Host "Verified $postgres / hms_test / hms_test; credentials from docker-compose.test.yml; isolated network $project`_default."
    if ($ContainerBuild) {
        # Classloading across a Windows bind mount can take minutes. Copy only source and
        # pom.xml into this disposable container, not .env files or existing build outputs.
        $buildScript = @'
set -eu
mkdir -p /tmp/hms-test-api
cp /workspace/apps/api/pom.xml /tmp/hms-test-api/pom.xml
cp -R /workspace/apps/api/src /tmp/hms-test-api/src
cd /tmp/hms-test-api
if mvn -o "-Dtest=$HMS_TEST_SELECTION" test; then
  result=0
else
  result=$?
fi
mkdir -p /workspace/apps/api/target/surefire-reports
if [ -d target/surefire-reports ]; then
  cp -R target/surefire-reports/. /workspace/apps/api/target/surefire-reports/
fi
exit "$result"
'@
        docker run --rm --pull never --network "$project`_default" `
            -v "${MavenCacheVolume}:/root/.m2" -v "${repo}:/workspace" `
            -e HMS_TEST_DB_URL=jdbc:postgresql://test-postgres:5432/hms_test `
            -e HMS_TEST_DB_USER=hms_test -e HMS_TEST_DB_PASSWORD=hms_test_only `
            -e HMS_TEST_REDIS_HOST=test-redis -e "HMS_TEST_SELECTION=$Tests" `
            $image sh -c $buildScript
    } else {
        docker run --rm --pull never --network "$project`_default" `
        -v "${MavenCacheVolume}:/root/.m2" -v "${repo}:/workspace" -w /workspace/apps/api `
        -e HMS_TEST_DB_URL=jdbc:postgresql://test-postgres:5432/hms_test `
        -e HMS_TEST_DB_USER=hms_test -e HMS_TEST_DB_PASSWORD=hms_test_only `
        -e HMS_TEST_REDIS_HOST=test-redis $image mvn -o "-Dtest=$Tests" test
    }
    $testExit = $LASTEXITCODE
    if ($testExit -ne 0) { throw "Maven tests failed (exit $testExit). See apps/api/target/surefire-reports." }
} finally {
    # This unique project contains only this invocation's temporary services.
    docker compose -p $project -f $compose down
}
