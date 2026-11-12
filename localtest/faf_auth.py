# Stand-in for FAF's auth.sh -> faf-user-service /irc/ergochat/login.
# Same rules as ErgochatController: passphrase must be "type:value"; "static" checks a fixed user list.
import json, sys, os
STATIC = {"thesetoner": "testpass", "tester1": "testpass", "tester2": "testpass", "giebmasse": "testpass", "tester4": "testpass", "tester5": "testpass"}
req = json.loads(sys.stdin.readline())
with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "auth.log"), "a") as f:
    f.write(json.dumps({k: v for k, v in req.items() if k != "passphrase"}) + "\n")
name = req.get("accountName", "")
parts = req.get("passphrase", "").split(":")
if len(parts) != 2:
    out = {"success": False, "accountName": name, "error": "passphrase does not follow [type:password] specification"}
elif parts[0] == "static":
    ok = STATIC.get(name.lower()) == parts[1]
    out = {"success": ok, "accountName": name, "error": None if ok else f"Username or password does not match for static user {name}"}
else:
    out = {"success": False, "accountName": name, "error": f"unknown authentication type {parts[0]}"}
print(json.dumps(out)); sys.stdout.flush()
