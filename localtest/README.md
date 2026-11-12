# Local test server

This runs the bot against a local copy of FAF's IRC server, so it can be tested without real FAF credentials.

1. Download [ergo](https://github.com/ergochat/ergo/releases) (v2.19.1 was used) and unzip it into a folder outside the repo.
2. Fetch FAF's config: `https://raw.githubusercontent.com/FAForever/gitops-stack/develop/apps/ergochat/config/ircd.yaml`
3. Copy `faf_auth.py` into the ergo folder, then build the local config and start the server:
   ```
   python patch_ergo.py ircd.yaml <ergo dir>/ircd.yaml <ergo dir>
   cd <ergo dir>
   ergo initdb --conf ircd.yaml
   ergo run --conf ircd.yaml
   ```
   It listens on `127.0.0.1:16667` (plain IRC) and `ws://127.0.0.1:18097` (WebSocket).
4. Point the bot at it. `setoner.conf`:
   ```
   IRC_USER=TheSetoner
   IRC_PASS=testpass
   IRC_SERVER=ws://127.0.0.1:18097
   IRC_CHANNELS=aeolus,paul
   ```
   Then run `sbt run`.
5. `python runtests.py . results.json` logs in test users and runs every chat command.

`faf_auth.py` uses the same rules as FAF's user service for `static:` passwords. The test accounts are in its `STATIC` list.

The real server keeps every user "always-on". Someone who disconnects stays in the channel, so the bot only sees a JOIN the first time an account ever joins. Use a fresh account name to test anything that fires on join.
