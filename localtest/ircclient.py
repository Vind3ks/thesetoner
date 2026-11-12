import socket, base64, time, threading, sys, json
class C:
    def __init__(self, nick, pw="static:testpass", port=16667):
        self.nick=nick; self.lines=[]; self.lock=threading.Lock()
        self.s=socket.create_connection(("127.0.0.1",port))
        self.buf=b""
        self.w("CAP LS 302"); self.w(f"NICK {nick}"); self.w(f"USER {nick} 0 * :{nick}")
        self.w("CAP REQ :sasl"); self.w("AUTHENTICATE PLAIN")
        t=threading.Thread(target=self.rd,daemon=True); t.start()
        end=time.time()+5
        while time.time()<end and not any(" AUTHENTICATE +" in l or l.startswith("AUTHENTICATE +") for l in self.lines): time.sleep(.05)
        self.w("AUTHENTICATE "+base64.b64encode(f"\0{nick}@FAF\0{pw}".encode()).decode())
        self.wait(" 903 ",5); self.w("CAP END"); self.wait(" 001 ",5)
    def w(self,l): self.s.sendall((l+"\r\n").encode())
    def rd(self):
        while True:
            d=self.s.recv(65536)
            if not d: break
            self.buf+=d
            while b"\r\n" in self.buf:
                l,self.buf=self.buf.split(b"\r\n",1); l=l.decode(errors="replace")
                if l.startswith("PING"): self.w("PONG "+l[5:])
                with self.lock: self.lines.append(l)
    def wait(self,needle,t):
        end=time.time()+t
        while time.time()<end:
            with self.lock:
                if any(needle in l for l in self.lines): return True
            time.sleep(.05)
        return False
    def mark(self):
        with self.lock: return len(self.lines)
    def since(self,m):
        with self.lock: return self.lines[m:]
