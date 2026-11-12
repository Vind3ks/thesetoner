# TheSetoner

The Setons bot for `#aeolus` on [FAForever](https://www.faforever.com): Setons, ladder and TMM pinglists, game calls, preaching, quotes and other channel fun.

It runs alongside FAF's [QAI](https://github.com/FAForever/faf-qai) bot and is deployed the same way: a Docker image configured with `IRC_*` environment variables, logging into FAF's IRC as a fixed user. QAI handles the factual lookups (`!player`, `!ratings`, `!streams`, `!seen`, `!help`), so Setoner doesn't duplicate them and keeps its help under `!setoner`.

## Commands

`!setoner` lists the commands, and `!setoner help <command>` explains one.

| Group | Commands |
|---|---|
| Pinglists and game calls | `!setons` / `!unsetons` / `!to setons`, `!signupladder` / `!unladder` / `!to ladder`, `!tmm` / `!untmm` / `!to tmm` |
| Lore | `!preach`, `!commandments`, `!sentonment`, `!lifestory`, `!threaten <nick>` |
| Fun | `!choose a or b`, `!sharewisdom`, `!luxcuse`, `!balanceriot`, `!biceps`, `!gym`, `!undress`, `!refaf`, `!salt`, `!gametaunt <1-98>`, `!finddave`, `!grondcheck` |
| Info | `!time [offset]`, `!report`, `!fix`, `!tutorials`, `!gms`, `!mapstats`, `!mods`, `!offlinemessage <nick> <text>`, page titles for pasted links |
| Clan or person specific | `!bawssemble`, `!bawtraining` (#BAW_clan), `!forthequeen` (#GB_clan), `!noobsgather` (#SNF_clan), `!bhalance`, `!to sentons`, `!feedbh`, `!dubplayer`, `!to superior setons`, `!sexytime`, gloves |

**Admins** are listed in `TheSetoner-admins.txt`. In a channel they can use `!setons <nick>`, `!promote`, `!demote` and `!dedemote <nick>`. In a private message to the bot they can use `say <channel> <text>`, `action`, `notice`, `join` and `names`.

## Configuration

These settings use the same names as QAI's:

| Variable | Meaning | Default |
|---|---|---|
| `IRC_SERVER` | Server to connect to | `wss://chat.faforever.com` |
| `IRC_USER` | Nick and IRC account | `TheSetoner` |
| `IRC_PASS` | The account's fixed-user password (sent as `static:<password>`) | required |
| `IRC_CHANNELS` | Channels, comma separated | `aeolus` |
| `IRC_DEBUG` | `true` logs raw server lines | off |

When running locally, the same keys can also go in a `setoner.conf` file (see `setoner.conf.example`).

The bot keeps its lists in `TheSetoner-*.txt` files in its working directory. In Docker that is `/data`, a volume that gets seeded from the image on first start.

## How it logs in

FAF's IRC server (ergo) only accepts clients that log in with SASL, and it requires the nick to match the account. The bot connects over WebSocket, does SASL PLAIN as `IRC_USER` with `static:<IRC_PASS>`, and FAF's user service checks that against its list of fixed users. That is the same mechanism QAI uses.

## Building and running

- Run locally: `sbt run`, with a `setoner.conf` in place. You need JDK 17+ and sbt.
- Build the image: `docker build -t faf-setoner .`
- CI (`.github/workflows/build.yml`) builds the image on every push. From the FAForever repo it pushes `faforever/faf-setoner:<branch>` for main and develop, and `:<tag>` for tags.
- `localtest/` runs FAF's ergo config on your machine, so the bot can be tested without any FAF credentials.

The files for deploying it on FAF are in `deploy/gitops-stack/`.
