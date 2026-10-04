"""Offline deployment checks. Docker/SSH/HTTP are never contacted by this harness."""
import json
import hashlib
import os
from pathlib import Path
import stat
import subprocess
import tempfile
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[1]
SHA = "b" * 40

class WorkflowTests(unittest.TestCase):
    def test_applied_migrations_are_immutable(self):
        directory=ROOT/'apps/api/src/main/resources/db/migration'
        for line in (ROOT/'deployment/migrations-v1-v32.sha256').read_text().splitlines():
            expected,name=line.split('  ',1)
            # Git checkouts may use CRLF or LF; preserve SQL content, not checkout policy.
            content=(directory/name).read_bytes().replace(b'\r\n',b'\n')
            self.assertEqual(hashlib.sha256(content).hexdigest(),expected,name+' is immutable; add V33+ instead')
    def test_release_gates_and_exact_sha(self):
        production = yaml.load((ROOT / "apps/api/src/main/resources/application-prod.yml").read_text(), Loader=yaml.BaseLoader)
        for logger in ("org.hibernate.orm.jdbc.error", "org.hibernate.engine.jdbc.spi.SqlExceptionHelper"):
            self.assertEqual(production["logging"]["level"][logger], "OFF")
        workflow = yaml.load((ROOT / ".github/workflows/backend-image.yml").read_text(), Loader=yaml.BaseLoader)
        self.assertIn("main", workflow["on"]["push"]["branches"])
        self.assertNotIn("paths", workflow["on"]["push"])
        jobs = workflow["jobs"]
        deploy = jobs["deploy-production"]
        self.assertEqual(deploy["if"], "github.event_name == 'push' && github.ref == 'refs/heads/main'")
        for event, branch, expected in [("push","main",True),("pull_request","main",False),("push","devs",False),("push","yfeature",False),("workflow_dispatch","main",False)]:
            self.assertEqual(event == "push" and branch == "main", expected)
        self.assertEqual(set(deploy["needs"]), {"test","image"})
        self.assertIn("test",jobs["image"]["needs"])
        self.assertIn("web",jobs["image"]["needs"])
        self.assertEqual(deploy["environment"],"production")
        self.assertEqual(deploy["concurrency"]["cancel-in-progress"],"false")
        self.assertIn("refs/heads/main",workflow["concurrency"]["cancel-in-progress"])
        build=next(step for step in jobs["image"]["steps"] if step.get("uses","").startswith("docker/build-push-action"))
        self.assertEqual(build["with"]["tags"].strip(),"${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}")
        self.assertIn("org.opencontainers.image.revision=${{ github.sha }}",build["with"]["labels"])
        step=deploy["steps"][-1]
        self.assertEqual(step["env"]["RELEASE_SHA"],"${{ github.sha }}")
        self.assertIn("StrictHostKeyChecking=yes",step["run"])
        self.assertNotIn("ssh-keyscan",step["run"])
        subprocess.run(["bash","-n"],input=step["run"],text=True,check=True)
        subprocess.run(["bash","-n",str(ROOT/"deployment/production/deploy.sh")],check=True)

class RemoteScriptTests(unittest.TestCase):
    def run_deploy(self, failure=""):
        self.temp=tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        directory=Path(self.temp.name)
        commands=directory/"commands";commands.mkdir()
        docker=commands/"docker"
        docker.write_text('''#!/usr/bin/env python3
import json,os,sys,pathlib
a=sys.argv[1:]; mode=os.environ.get('TEST_FAILURE','');sha=os.environ['TEST_SHA']
with open(os.environ['TEST_CALLS'],'a') as f: f.write(json.dumps(a)+'\\n')
if a[0]=='logs':
 print('password=SUPER_SECRET SQL /private/path full_account=00001234')
 print('event=unexpected_api_failure requestId=safe-ref exceptionType=java.lang.RuntimeException')
elif a[0]=='image': print('wrong' if mode=='image' else sha)
elif a[0]=='inspect':
 if '.State.Status' in a[2]: print('exited' if mode=='container' else 'running')
 elif '.State.Health' in a[2]: print('healthy')
 else: print('sha256:local-test-image')
''')
        (commands/"curl").write_text('#!/bin/sh\nif [ "$TEST_FAILURE" = public ]; then printf \'{"status":"DOWN"}\'; else printf \'{"status":"UP"}\'; fi\n')
        (commands/"sleep").write_text('#!/bin/sh\nexit 0\n')
        if failure=="lock": (commands/"flock").write_text('#!/bin/sh\nexit 1\n')
        for file in commands.iterdir(): file.chmod(0o700)
        self.original=b'# preserve comments\r\nHMS_IMAGE_TAG='+b'a'*40+b'\r\nPASSWORD=SUPER_SECRET\r\nLITERAL=$(touch injected)\r\n'
        (directory/".env").write_bytes(self.original)
        (directory/"docker-compose.yml").write_text('services: {}\n')
        environment={**os.environ,"PATH":str(commands)+os.pathsep+os.environ["PATH"],"HMS_DEPLOY_DIRECTORY":str(directory),"TEST_SHA":SHA,"TEST_FAILURE":failure,"TEST_CALLS":str(directory/"calls")}
        result=subprocess.run(["bash",str(ROOT/"deployment/production/deploy.sh"),SHA,"https://example.test/actuator/health"],env=environment,capture_output=True,text=True,timeout=20)
        self.assertNotIn("SUPER_SECRET",result.stdout+result.stderr)
        self.assertNotIn("00001234",result.stdout+result.stderr)
        self.assertFalse((directory/"injected").exists())
        return directory,result
    def test_success_changes_only_image_tag_and_recreates_only_api(self):
        directory,result=self.run_deploy()
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        self.assertEqual((directory/".env").read_bytes(),self.original.replace(b'a'*40,SHA.encode()))
        backups=list(directory.glob('.env.backup.*'));self.assertEqual(len(backups),1)
        self.assertEqual(backups[0].read_bytes(),self.original)
        self.assertEqual(stat.S_IMODE(backups[0].stat().st_mode),0o600)
        calls=[json.loads(line) for line in (directory/'calls').read_text().splitlines()]
        self.assertIn(['pull','ghcr.io/k154y/hms-api:'+SHA],calls)
        self.assertIn(['compose','--env-file','.env','-f','docker-compose.yml','up','-d','--force-recreate','--no-deps','api'],calls)
    def test_revision_mismatch_fails_before_env_change(self):
        directory,result=self.run_deploy('image');self.assertNotEqual(result.returncode,0)
        self.assertEqual((directory/'.env').read_bytes(),self.original)
        self.assertIn('safe-ref',result.stdout)
    def test_container_and_public_failures_do_not_rollback(self):
        for failure in ['container','public']:
            directory,result=self.run_deploy(failure);self.assertNotEqual(result.returncode,0)
            self.assertIn(SHA.encode(),(directory/'.env').read_bytes())
            self.assertIn('No automatic',result.stderr)
    def test_lock_failure_prevents_changes(self):
        directory,result=self.run_deploy('lock');self.assertNotEqual(result.returncode,0)
        self.assertEqual((directory/'.env').read_bytes(),self.original)
        self.assertFalse((directory/'calls').exists())

if __name__ == '__main__': unittest.main()
