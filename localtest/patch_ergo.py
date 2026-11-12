# Turns FAF's production ergo config (FAForever/gitops-stack apps/ergochat/config/ircd.yaml)
# into one that runs locally: localhost listeners, SASL still required, FAF's auth script
# swapped for faf_auth.py, no OAuth/MySQL.
# usage: python patch_ergo.py <faf ircd.yaml> <output ircd.yaml> <dir holding faf_auth.py>
import sys
src,dst,run=sys.argv[1:4]
run=run.replace(chr(92),'/')
s=open(src,encoding='utf-8').read()
def rep(a,b,count=1):
    global s
    assert s.count(a)>=count,(a,s.count(a))
    s=s.replace(a,b)
rep('{{ .Values.baseDomain }}','faforever.com',2)
a=s.index('    ":6697":'); b=s.index('    # Example of a Unix domain socket')
s=s[:a]+s[b:]
rep('    ":6667":','    "127.0.0.1:16667":')
rep('    ":8097":','    "127.0.0.1:18097":')
rep('''    exempted:
      - "localhost"
      # - '10.10.0.0/16'
''','''    exempted: []
''')
rep('command: "/ircd/auth.sh"','command: "'+sys.executable.replace(chr(92),'/')+'"')
rep('args: [ "http://faf-user-service:8080/irc/ergochat/login" ]','args: [ "-I", "'+run+'/faf_auth.py" ]')
i=s.index('  oauth2:\n    enabled: true'); s=s[:i]+s[i:].replace('enabled: true','enabled: false',1)
rep('lock-file: "/data/ircd.lock"','lock-file: "ircd.lock"')
rep('path: "/data/ircd.db"','path: "ircd.db"')
i=s.index('  mysql:
'); s=s[:i]+s[i:].replace('enabled: true','enabled: false',1)
i=s.index('  sqlite:
'); s=s[:i]+s[i:].replace('enabled: false','enabled: true',1)
open(dst,'w',encoding='utf-8').write(s)
