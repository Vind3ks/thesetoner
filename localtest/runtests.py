import sys, time, json
sys.path.insert(0, sys.argv[1])
from ircclient import C
log=[]
def ev(who,line): log.append((round(time.time(),1),who,line)); print(who,line,flush=True)
t1=C("tester1"); t2=C("tester2")
for c in (t1,t2):
    c.w("JOIN #paul"); c.w("JOIN #aeolus")
time.sleep(2)
def run(client, target, msg, wait=6):
    m1=t1.mark(); m2=t2.mark()
    ev(">>", f"{client.nick} -> {target}: {msg}")
    client.w(f"PRIVMSG {target} :{msg}")
    end=time.time()+wait
    while time.time()<end: time.sleep(.2)
    for c,m in ((t1,m1),(t2,m2)):
        for l in c.since(m):
            if l.startswith(":TheSetoner!") and (" PRIVMSG " in l or " NOTICE " in l):
                ev("<<"+c.nick, l.split(" ",1)[1])
P="#paul"
tests=[(t1,P,"!setoner"),(t1,P,"!setoner help preach"),(t1,P,"!setoner help !choose"),(t1,P,"!commands"),(t1,P,"!help"),
(t1,P,"!setons"),(t2,P,"!setons"),(t1,P,"!To Setons"),(t1,P,"!To Setons 1500"),(t1,P,"!preach"),(t1,P,"!commandments"),(t1,P,"!threaten tester2"),
(t1,P,"!lifestory"),(t1,P,"!sharewisdom"),(t1,P,"!luxcuse"),(t1,P,"!report"),(t1,P,"!choose red or blue"),(t1,P,"!grondcheck"),
(t1,P,"!balanceriot"),(t1,P,"!biceps"),(t1,P,"!gym"),(t1,P,"!undress"),(t1,P,"!refaf"),(t1,P,"!time"),(t1,P,"!salt"),(t1,P,"!mapstats"),
(t1,P,"!gms"),(t1,P,"!gameTaunt 5"),(t1,P,"!fix"),(t1,P,"!tutorials"),(t1,P,"look at https://example.com"),(t1,P,"!finddave"),(t1,P,"!mods"),
(t1,P,"!signupladder"),(t2,P,"!signupladder"),(t1,P,"!unladder"),(t1,P,"!tmm"),(t1,P,"!untmm"),
(t1,P,"!ratings Brutus5000"),(t1,P,"!globalrating x"),(t1,P,"!alias x"),(t1,P,"!sstreams"),(t1,P,"!ip example.com"),(t1,P,"!wordcount"),(t1,P,"!addtourney x"),
(t1,P,"!UnSetons"),(t1,"TheSetoner","!choose pm or channel"),
]
for c,tg,msg in tests: run(c,tg,msg)
# offline message: tester1 leaves a message for tester3, who joins later
run(t1,P,"!offlinemessage tester5 hello from tester1",4)
t3=C("tester5"); t3.w("JOIN #aeolus"); time.sleep(10)
for l in t3.lines:
    if l.startswith(":TheSetoner!"): ev("<<tester5", l.split(" ",1)[1])
# admin
g=C("Giebmasse"); g.w("JOIN #paul"); time.sleep(2)
run(g,P,"!setons tester3")
run(g,"TheSetoner","say #paul admin say works")
run(g,P,"!promote tester2")
run(g,P,"!demote tester2"); run(t2,P,"!choose x or y"); run(g,P,"!dedemote tester2")
json.dump(log,open(sys.argv[2],"w"),indent=0)
