#!/usr/bin/env python3
"""Laptop acceptance: drives core, edge and a simulated ESP32 over HTTP. Standard library only.

Usage (see clair-core/docs/RUNBOOK.md):
    python3 clair-core/tools/e2e/laptop_acceptance.py setup      # account, space, pair/claim, first reading
    python3 clair-core/tools/e2e/laptop_acceptance.py commands   # STANDBY / WAKE round trips
    python3 clair-core/tools/e2e/laptop_acceptance.py alerts     # threshold -> ACTIVE -> RESOLVED on the device
    python3 clair-core/tools/e2e/laptop_acceptance.py readings --count 3   # queue readings (core may be down)
    python3 clair-core/tools/e2e/laptop_acceptance.py verify     # exactly one evaluation per queued reading
    python3 clair-core/tools/e2e/laptop_acceptance.py reassign   # unlink, reclaim by another user, no leakage

Environment: CORE_URL (http://localhost:49220), EDGE_URL (http://localhost:5050),
MAILPIT_URL (http://localhost:8025), PROVISIONING_CSV (path written by the demo profile),
E2E_STATE (json file that carries ids between steps), E2E_DEVICE_INDEX (inventory row, default 0).
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import sys
import time
import uuid
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

CORE = os.getenv("CORE_URL", "http://localhost:49220").rstrip("/")
EDGE = os.getenv("EDGE_URL", "http://localhost:5050").rstrip("/")
MAILPIT = os.getenv("MAILPIT_URL", "http://localhost:8025").rstrip("/")
CSV_PATH = os.getenv("PROVISIONING_CSV", "provisioned-devices.csv")
STATE_PATH = os.getenv("E2E_STATE", ".e2e-state.json")
PASSWORD = "ClairE2ePass1!"


class Failure(Exception):
    pass


def http(method, url, body=None, headers=None, params=None, expect=None):
    if params:
        url = url + "?" + urlencode({k: v for k, v in params.items() if v is not None})
    data = json.dumps(body).encode() if body is not None else None
    req = Request(url, data=data, method=method, headers={"Accept": "application/json", **(headers or {})})
    if data is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urlopen(req, timeout=20) as r:
            raw = r.read()
            parsed = json.loads(raw) if raw else None
            status = r.status
    except HTTPError as e:
        raw = e.read()
        try:
            parsed = json.loads(raw) if raw else None
        except ValueError:
            parsed = raw.decode("utf-8", "replace")
        status = e.code
    if expect is not None and status not in (expect if isinstance(expect, (list, tuple)) else [expect]):
        raise Failure(f"{method} {url} -> {status} {parsed}")
    return status, parsed


def wait_for(description, fn, timeout=60, interval=1.0):
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        try:
            result = fn()
            if result:
                return result
            last = result
        except (URLError, HTTPError, OSError, Failure) as exc:
            last = exc
        time.sleep(interval)
    raise Failure(f"timed out waiting for {description} (last: {last})")


def load_state():
    return json.load(open(STATE_PATH)) if os.path.exists(STATE_PATH) else {}


def save_state(state):
    json.dump(state, open(STATE_PATH, "w"), indent=2)


def step(msg):
    print(f"==> {msg}", flush=True)


# ---------------------------------------------------------------- core helpers
def auth(token):
    return {"Authorization": f"Bearer {token}"}


def sign_up_and_in(email):
    step(f"sign up {email}")
    _, initiated = http("POST", f"{CORE}/api/v1/auth/sign-up", {"email": email, "password": PASSWORD}, expect=201)
    session_id = initiated["sessionId"]
    code = wait_for("verification email in Mailpit", lambda: verification_code(email), timeout=30)
    http("POST", f"{CORE}/api/v1/auth/confirm", {"sessionId": session_id, "verificationCode": code}, expect=201)
    _, signed = http("POST", f"{CORE}/api/v1/auth/sign-in", {"email": email, "password": PASSWORD}, expect=200)
    return signed["token"], signed["id"]


def verification_code(email):
    _, listing = http("GET", f"{MAILPIT}/api/v1/messages", params={"limit": 50})
    for message in listing.get("messages", []):
        if any(to.get("Address", "").lower() == email.lower() for to in message.get("To", [])):
            _, full = http("GET", f"{MAILPIT}/api/v1/message/{message['ID']}")
            text = (full.get("Text") or "") + " " + (full.get("HTML") or "")
            found = re.search(r"\b([A-Z0-9]{4}-[A-Z0-9]{4})\b", text)
            if found:
                return found.group(1)
    return None


def create_space(token, org_name, space_name):
    _, org = http("POST", f"{CORE}/api/v1/organizations", {"name": org_name}, headers=auth(token), expect=201)
    _, space = http("POST", f"{CORE}/api/v1/spaces?organizationId={org['id']}", {"name": space_name}, headers=auth(token), expect=201)
    return org["id"], space["id"]


def pair_and_claim(token, hardware_id, space_id):
    _, pairing = http("POST", f"{CORE}/api/v1/devices/pair", {"hardwareId": hardware_id}, headers=auth(token), expect=201)
    _, device = http("POST", f"{CORE}/api/v1/devices/claim", {"claimToken": pairing["claimToken"], "spaceId": space_id},
                     headers=auth(token), expect=200)
    return pairing["deviceId"], device


# ---------------------------------------------------------------- simulated device
class Device:
    def __init__(self, hardware_id, api_key):
        self.hardware_id = hardware_id
        self.api_key = api_key
        self.headers = {"X-Hardware-Id": hardware_id, "X-API-Key": api_key}

    def reading(self, co2=650.0, pm25=8.05):
        reading_id = str(uuid.uuid4())
        body = {
            "deviceId": self.hardware_id,
            "reading_id": reading_id,
            "measured_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
            "timestamp": datetime.now(timezone.utc).strftime("%H:%M:%S"),
            "uptime": "00:10:00",
            "airQuality": {"co2": co2, "temperature": 22.5, "humidity": 45.0},
            "particulateMatter": {"pm1_0": 3.2, "pm2_5": pm25, "pm10": 12.4},
            "connectivity": {"status": "connected", "network": "e2e-wifi", "signalStrength": -50},
            "location": {"country": "PERU"},
            "healthStatus": 100,
            "status": "Optimal",
        }
        return reading_id, body

    def post_reading(self, body, expect=201):
        return http("POST", f"{EDGE}/api/v1/device/telemetry", body, headers=self.headers, expect=expect)

    def pending_commands(self):
        status, payload = http("GET", f"{EDGE}/api/v1/device/commands/pending", headers=self.headers)
        return status, (payload or {}).get("commands", []) if status == 200 else []

    def ack_command(self, command_id, status="EXECUTED"):
        return http("POST", f"{EDGE}/api/v1/device/commands/{command_id}/ack", {"status": status}, headers=self.headers, expect=200)

    def pending_incidents(self):
        status, payload = http("GET", f"{EDGE}/api/v1/alerting/incidents/pending", headers=self.headers)
        return (payload or {}).get("events", []) if status == 200 else []

    def ack_incident(self, row_id):
        return http("POST", f"{EDGE}/api/v1/alerting/incidents/{row_id}/ack", {"id": row_id, "status": "ACKNOWLEDGED"}, headers=self.headers, expect=200)


def inventory():
    with open(CSV_PATH) as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise Failure(f"no devices in {CSV_PATH}")
    return rows


# ---------------------------------------------------------------- scenarios
def cmd_setup(args):
    step("waiting for core and edge")
    wait_for("core health", lambda: http("GET", f"{CORE}/actuator/health")[0] == 200, timeout=120)
    wait_for("edge readiness", lambda: http("GET", f"{EDGE}/ready")[0] == 200, timeout=120, interval=2)
    row = inventory()[int(os.getenv("E2E_DEVICE_INDEX", "0"))]
    device = Device(row["hardware_id"], row["api_key"])
    email = f"e2e-{uuid.uuid4().hex[:8]}@example.com"
    token, user_id = sign_up_and_in(email)
    step("create organization and space")
    org_id, space_id = create_space(token, "E2E Home", "E2E Room")
    step(f"pair and claim {device.hardware_id}")
    device_id, claimed = pair_and_claim(token, device.hardware_id, space_id)
    assert claimed["ownerUserId"] == user_id, "claim must make the caller the owner"
    step("device state: registered (claimed, no reading yet)")
    _, status = http("GET", f"{CORE}/api/v1/devices/{device_id}/status", headers=auth(token), expect=200)
    print("   status:", status["status"], "lastSeenAt:", status.get("lastSeenAt"))
    step("waiting for the edge to learn the device from the roster")
    wait_for("edge accepts device credentials", lambda: device.pending_commands()[0] == 200, timeout=60, interval=2)
    step("device state: reachable (edge accepted an authenticated poll)")
    reading_id, body = device.reading()
    step("first reading through the edge")
    _, stored = device.post_reading(body)
    assert stored["duplicate"] is False and stored["reading_id"] == reading_id, stored
    _, again = device.post_reading(body)
    assert again["duplicate"] is True and again["id"] == stored["id"], "exact retry must return the same row"
    changed = dict(body, airQuality=dict(body["airQuality"], co2=999.0))
    device.post_reading(changed, expect=409)
    step("waiting for the reading to be visible in core")
    latest = wait_for("latest evaluation", lambda: _latest(token, device_id, reading_id), timeout=60, interval=2)
    print("   measuredAt:", latest["measuredAt"], "pm2_5:", latest["particulateMatter"]["pm2_5"])
    assert abs(latest["particulateMatter"]["pm2_5"] - 8.05) < 1e-9, "PM decimals must survive both hops"
    step("device state: recently measured (presence ONLINE in core)")
    wait_for("presence ONLINE", lambda: http("GET", f"{CORE}/api/v1/devices/{device_id}/status", headers=auth(token))[1]["status"] == "ONLINE", timeout=60, interval=2)
    save_state({"email": email, "token": token, "user_id": user_id, "org_id": org_id, "space_id": space_id,
                "device_id": device_id, "hardware_id": device.hardware_id, "api_key": device.api_key,
                "reading_ids": [reading_id]})
    print("SETUP OK")


def _latest(token, device_id, reading_id):
    status, latest = http("GET", f"{CORE}/api/v1/evaluations/devices/{device_id}/latest", headers=auth(token))
    return latest if status == 200 and latest.get("readingId") == reading_id else None


def cmd_commands(args):
    s = load_state(); token = s["token"]; device = Device(s["hardware_id"], s["api_key"]); device_id = s["device_id"]
    for command_type, expected_status in (("STANDBY", "STANDBY"), ("WAKE", "ONLINE")):
        step(f"issue {command_type} from the app")
        _, created = http("POST", f"{CORE}/api/v1/devices/{device_id}/commands", {"type": command_type, "payload": None}, headers=auth(token), expect=201)
        step("device polls the edge for it")
        delivered = wait_for(f"{command_type} delivered", lambda: [c for c in device.pending_commands()[1] if c["commandId"] == created["id"]], timeout=60, interval=2)
        assert delivered[0]["type"] == command_type
        step("device executes and acks; core marks it EXECUTED")
        device.ack_command(created["id"])
        device.ack_command(created["id"])  # a duplicate ack is harmless
        wait_for("core command EXECUTED", lambda: http("GET", f"{CORE}/api/v1/devices/{device_id}/commands/{created['id']}", headers=auth(token))[1]["status"] == "EXECUTED", timeout=60, interval=2)
        wait_for(f"device status {expected_status}", lambda: http("GET", f"{CORE}/api/v1/devices/{device_id}/status", headers=auth(token))[1]["status"] == expected_status, timeout=30, interval=2)
        print(f"   {command_type}: EXECUTED, device {expected_status}")
    print("COMMANDS OK")


def cmd_alerts(args):
    s = load_state(); token = s["token"]; device = Device(s["hardware_id"], s["api_key"]); device_id = s["device_id"]
    step("set a CO2 threshold of 1000 ppm")
    http("POST", f"{CORE}/api/v1/devices/{device_id}/thresholds", {"metric": "CO2", "value": 1000, "enabled": True}, headers=auth(token), expect=[200, 201])
    step("post a reading above the threshold")
    _, body = device.reading(co2=1500.0); device.post_reading(body)
    alert = wait_for("ACTIVE alert in core", lambda: _alert(token, device_id, "ACTIVE"), timeout=60, interval=2)
    step("device polls incidents: expects the ACTIVE transition")
    active = wait_for("ACTIVE transition on device", lambda: [e for e in device.pending_incidents() if e["alert_id"] == alert["id"] and e["status"] == "ACTIVE"], timeout=60, interval=2)
    assert active[0]["metric"] == "CO2"
    device.ack_incident(active[0]["id"])
    step("post a reading back under the threshold")
    _, body = device.reading(co2=500.0); device.post_reading(body)
    wait_for("RESOLVED alert in core", lambda: _alert(token, device_id, "RESOLVED", alert["id"]), timeout=60, interval=2)
    resolved = wait_for("RESOLVED transition on device", lambda: [e for e in device.pending_incidents() if e["alert_id"] == alert["id"] and e["status"] == "RESOLVED"], timeout=60, interval=2)
    assert resolved[0]["sequence"] > active[0]["sequence"], "resolution must carry a higher sequence"
    device.ack_incident(resolved[0]["id"])
    step("an empty poll afterwards changes nothing and offers nothing new")
    assert not [e for e in device.pending_incidents() if e["alert_id"] == alert["id"]]
    step("the edge never acknowledged the alert on the user's behalf")
    _, page = http("GET", f"{CORE}/api/v1/devices/{device_id}/alerts", headers=auth(token), params={"size": 50}, expect=200)
    statuses = {a["id"]: a["status"] for a in page.get("content", page if isinstance(page, list) else [])}
    assert statuses.get(alert["id"]) == "RESOLVED", statuses
    print("ALERTS OK")


def _alert(token, device_id, status, alert_id=None):
    _, page = http("GET", f"{CORE}/api/v1/devices/{device_id}/alerts", headers=auth(token), params={"size": 50})
    items = page.get("content", page if isinstance(page, list) else [])
    for a in items:
        if a["status"] == status and (alert_id is None or a["id"] == alert_id):
            return a
    return None


def cmd_readings(args):
    s = load_state(); device = Device(s["hardware_id"], s["api_key"])
    ids = []
    for i in range(args.count):
        reading_id, body = device.reading(co2=600 + i)
        _, stored = device.post_reading(body)
        assert stored["reading_id"] == reading_id
        ids.append(reading_id)
        time.sleep(0.2)
    s.setdefault("reading_ids", []).extend(ids)
    save_state(s)
    print(f"READINGS QUEUED {len(ids)} (edge accepted them; core may be down)")


def cmd_verify(args):
    s = load_state(); token = s["token"]; device_id = s["device_id"]
    expected = set(s["reading_ids"])
    step(f"waiting for {len(expected)} reading(s) to reach core exactly once")
    def counts():
        _, page = http("GET", f"{CORE}/api/v1/evaluations/devices/{device_id}", headers=auth(token), params={"size": 200})
        items = page.get("content", [])
        seen = {}
        for item in items:
            seen[item["readingId"]] = seen.get(item["readingId"], 0) + 1
        return seen if expected <= set(seen) else None
    seen = wait_for("all readings evaluated", counts, timeout=180, interval=3)
    duplicates = {k: v for k, v in seen.items() if k in expected and v != 1}
    assert not duplicates, f"readings evaluated more than once: {duplicates}"
    print(f"VERIFY OK ({len(expected)} readings, one evaluation each)")


def cmd_reassign(args):
    s = load_state(); token_a = s["token"]; device = Device(s["hardware_id"], s["api_key"]); device_id = s["device_id"]
    step("user A queues a command the device has not fetched yet, then unlinks the device")
    _, stale = http("POST", f"{CORE}/api/v1/devices/{device_id}/commands", {"type": "STANDBY", "payload": None}, headers=auth(token_a), expect=201)
    time.sleep(3)  # let the edge cache it
    http("DELETE", f"{CORE}/api/v1/devices/{device_id}", headers=auth(token_a), expect=204)
    step("user B signs up and claims the same unit")
    token_b, user_b = sign_up_and_in(f"e2e-b-{uuid.uuid4().hex[:8]}@example.com")
    _, space_b = create_space(token_b, "B Home", "B Room")
    wait_for("device unlinked", lambda: http("GET", f"{CORE}/api/v1/devices/{device_id}", headers=auth(token_a))[0] == 404, timeout=30)
    new_device_id, claimed = pair_and_claim(token_b, device.hardware_id, space_b)
    assert new_device_id == device_id and claimed["ownerUserId"] == user_b
    step("A's stale command must never reach the device")
    time.sleep(8)  # roster + command polls
    delivered = [c["commandId"] for c in device.pending_commands()[1]]
    assert stale["id"] not in delivered, f"stale command delivered: {delivered}"
    stale_status, stale_state = http("GET", f"{CORE}/api/v1/devices/{device_id}/commands/{stale['id']}", headers=auth(token_b))
    assert stale_status in (403, 404) or (stale_state or {}).get("status") == "EXPIRED", (stale_status, stale_state)
    step("a late ack for it cannot touch B's assignment")
    device.ack_command(stale["id"])  # edge: unknown/expired -> no forwarding
    step("B sees no reading from before the claim")
    _, page = http("GET", f"{CORE}/api/v1/evaluations/devices/{device_id}", headers=auth(token_b), params={"size": 200}, expect=200)
    assert page.get("content", []) == [], "history from the previous owner leaked"
    latest_status, _ = http("GET", f"{CORE}/api/v1/evaluations/devices/{device_id}/latest", headers=auth(token_b))
    assert latest_status == 404, f"latest reading for the new owner returned {latest_status}"
    step("A can no longer read the device")
    assert http("GET", f"{CORE}/api/v1/evaluations/devices/{device_id}/latest", headers=auth(token_a))[0] == 403, "previous owner can still read"
    step("a new reading is visible to B only")
    reading_id, body = device.reading()
    wait_for("edge accepts device after reassignment", lambda: device.post_reading(body, expect=None)[0] == 201, timeout=60, interval=2)
    wait_for("B sees the new reading", lambda: _latest(token_b, device_id, reading_id), timeout=60, interval=2)
    save_state(dict(s, token=token_b, user_id=user_b, reading_ids=[reading_id]))
    print("REASSIGN OK")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("setup"); sub.add_parser("commands"); sub.add_parser("alerts"); sub.add_parser("verify"); sub.add_parser("reassign")
    readings = sub.add_parser("readings"); readings.add_argument("--count", type=int, default=3)
    args = parser.parse_args()
    try:
        {"setup": cmd_setup, "commands": cmd_commands, "alerts": cmd_alerts, "readings": cmd_readings,
         "verify": cmd_verify, "reassign": cmd_reassign}[args.command](args)
    except (Failure, AssertionError) as exc:
        print(f"FAILED: {exc}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
