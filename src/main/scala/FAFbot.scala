package fafbot

import java.net._
import java.io._
import scala.io.Source
import java.text._
import scala.collection.mutable._
import scala.util.Random
import scala.util.control.Breaks._
import lists.Lists
import java.nio.charset.StandardCharsets

/*
Should:
    Login to IRC server
    Authenticate if challenged
    Join designated channels
    Receive user input for join, quit, stop commands
    Maintain channel state, e.g user list
    Handle all IRC commands
*/
class FafBot(val config:BotConfig){
    val name:String = config.nick

    var initialised:Boolean = false

    // Opened in run(), so constructing the bot doesn't connect yet.
    var transport:IrcTransport = null

    val channels:HashMap[String, Channel] = new HashMap[String, Channel]()
	
	val rickrolls = Array (
								"We're no strangers to love - You know the rules and so do I - A full commitment's what I'm thinking of - You wouldn't get this from any other guy",
								"I just wanna tell you how I'm feeling - Gotta make you understand",
								"Never gonna give you up",
								"Never gonna let you down",
								"Never gonna run around and desert you"
								)
	var partsJoins = true

    val serverReadThread:Thread = new Thread(new Runnable() {
        def run():Unit = {
            var line = readLine()
            while(line != null){
                if(config.debug) printLine("RAW <- " + line)
                try {
                    handleLine(line)
                } catch {
                    // One odd line shouldn't take the whole bot down.
                    case e : Throwable => printLine(String.format("Error handling line [%s]: %s", line, e.toString))
                }
                line = readLine()
            }
            // No in-process reconnect: exit non-zero so the start script restarts us.
            printLine("Disconnected from server, exiting")
            System.exit(1)
        }
    })

    def handleLine(line:String):Boolean = {
		//testing
		//printLine("RAW INPUT: " + line)
        if(line.startsWith("PING ")){
            writeLine("PONG "+line.drop(5))
            return true
        }
        else {
            val commands = line.split(':')(1).split(" ")
			//printLine("COMMANDS " + commands.mkString)
            if((commands.length == 5) && commands(3).equals("=")){
                // this looks like they're giving us a userlist
                val cname = commands(4)
                if(!channels.contains(cname))
                    return true
                val channel = this.channels(cname)
                val names = line.split(':')(2).split(" ").map(_.stripPrefix("@").stripPrefix("&").stripPrefix("~"))
				val lazynames2 = line.split(':')(2).split(" ")
                //printLine(line)
                for(name <- names){
                    channel.users.add(name)
				}
				for(mod <- lazynames2) {
					if (mod.contains("@") || mod.contains ("&")){
						if (mod.contains("AeonCommander") || mod.contains("QAI")){
							} else {
								channel.mods.add(mod)
							}
					}
				}
                return true
            }
            else if(commands.length > 2){
                var cname = commands(2)
                if(channels.contains(cname)){
                    if(!commands(1).equals("PRIVMSG")){
                        channels(cname).handleLine(line)
                        return true
                    } else return false
                } else {
                    // This might be a private message to us
                    if(cname.equals(name)){
                        handlePriv(line)
                        return true
                    }
                }
            } /*else if(commands(1).equals("JOIN")) {
                var cname = line.drop(1).dropWhile(_!=':').drop(1)
                if(channels.contains(cname)){
                    channels(cname).handleLine(line)
                    return true
                } else return false
            }*///This blocks JOIN from going further
        }
        return false
    }

    def handlePriv(line:String):Boolean = return false

    def initChannels(channels:Array[String]):Unit = {
        for(n <- channels)
            this.channels.put(n,new Channel(n,this))
        initialised = true
    }

    /*
     * Registers with the server using IRCv3 capability negotiation and SASL PLAIN.
     * FAF's ergo server requires SASL; NickServ IDENTIFY no longer works there.
     * Returns once the server welcomes us (001), throws if authentication fails.
     */
    def login():Unit = {
        writeLine("CAP LS 302")
        writeLine("NICK " + this.name)
        writeLine("USER " + this.name + " 0 * :" + this.name)

        var offered = ""
        var saslRequested = false
        while(true){
            val line = readLine()
            if(line == null)
                throw new IOException("Connection closed during login")
            printLine("LOGIN <- " + line)

            val parts = line.split(" ")
            // Numerics and commands sit after the optional ":prefix"
            val cmdIdx = if(line.startsWith(":")) 1 else 0
            val cmd = if(parts.length > cmdIdx) parts(cmdIdx) else ""

            if(cmd == "PING"){
                writeLine("PONG " + parts.drop(cmdIdx + 1).mkString(" "))
            }
            else if(cmd == "CAP" && parts.length > cmdIdx + 2 && parts(cmdIdx + 2) == "LS"){
                // CAP * LS * :caps...  (a "*" before the list means more lines follow)
                val more = parts.length > cmdIdx + 3 && parts(cmdIdx + 3) == "*"
                offered += " " + line.drop(1).dropWhile(_ != ':').drop(1)
                if(!more){
                    val caps = offered.trim.split(" ").map(_.takeWhile(_ != '='))
                    if(caps.contains("sasl")){
                        saslRequested = true
                        writeLine("CAP REQ :sasl")
                    } else {
                        throw new IOException("Server does not offer SASL, cannot log in")
                    }
                }
            }
            else if(cmd == "CAP" && parts.length > cmdIdx + 2 && parts(cmdIdx + 2) == "ACK" && saslRequested){
                writeLine("AUTHENTICATE PLAIN")
            }
            else if(cmd == "CAP" && parts.length > cmdIdx + 2 && parts(cmdIdx + 2) == "NAK"){
                throw new IOException("Server refused the sasl capability")
            }
            else if(cmd == "AUTHENTICATE" && parts.length > cmdIdx + 1 && parts(cmdIdx + 1) == "+"){
                sendSaslPlain()
            }
            else if(cmd == "903"){
                printLine("SASL authentication successful")
                writeLine("CAP END")
            }
            else if(Set("902","904","905","906","908").contains(cmd)){
                throw new IOException("SASL authentication failed: " + line)
            }
            else if(cmd == "433" || cmd == "432"){
                throw new IOException("Nick " + this.name + " rejected: " + line)
            }
            else if(cmd == "ERROR"){
                throw new IOException("Server error during login: " + line)
            }
            else if(cmd == "001"){
                return
            }
        }
    }

    // AUTHENTICATE payload: base64(authzid NUL authcid NUL password), sent in 400-byte chunks
    private def sendSaslPlain():Unit = {
        val raw = ("\u0000" + config.saslUser + "\u0000" + config.saslPassword).getBytes(StandardCharsets.UTF_8)
        val encoded = java.util.Base64.getEncoder.encodeToString(raw)
        val chunks = encoded.grouped(400).toList
        for(c <- chunks)
            writeRaw("AUTHENTICATE " + c)
        if(encoded.length % 400 == 0)
            writeRaw("AUTHENTICATE +")
        printLine("AUTHENTICATE <credentials hidden>")
    }

    def joinChannels():Unit = {
        for((n,c) <- channels)
            writeLine("JOIN "+n)
    }

    def readLine():String = {
        val line = transport.readLine()
        if(line == null) null else normaliseIncoming(line.trim)
    }

    // ergo sends "JOIN #chan" where older servers sent "JOIN :#chan". The parsing below
    // was written for the latter, so put the colon back.
    def normaliseIncoming(line:String):String = {
        val parts = line.split(" ")
        if(parts.length == 3 && parts(1) == "JOIN" && !parts(2).startsWith(":"))
            parts(0) + " JOIN :" + parts(2)
        else line
    }

    // Lots of code sends "PRIVMSG target some text" without the ':' before the text.
    // Strict servers like ergo then deliver only the first word, so add it here.
    def normaliseOutgoing(line:String):String = {
        val parts = line.split(" ", 3)
        if(parts.length == 3 && (parts(0) == "PRIVMSG" || parts(0) == "NOTICE") && !parts(2).startsWith(":"))
            parts(0) + " " + parts(1) + " :" + parts(2)
        else line
    }

    def writeLine(line:String):Unit = synchronized {
        printLine(this.name, line.takeWhile(_!=' '), line.dropWhile(_!=' '))
        transport.writeLine(normaliseOutgoing(line))
    }

    // Like writeLine but never echoes the content to the console (for credentials)
    private def writeRaw(line:String):Unit = synchronized {
        transport.writeLine(line)
    }

    def printLine(sender:String, action:String, message:String):Unit = {
        if(action.equals("PONG")) return
        val line = DateFormat.getInstance.format(java.util.Calendar.getInstance.getTime).padTo(18, ' ') +
        action.padTo(10, ' ') + sender + ":" + message
        println(line)
    }

    def printLine(message:String):Unit = {
        val line = DateFormat.getInstance.format(java.util.Calendar.getInstance.getTime).padTo(18, ' ') +
            ("BOT_INFO").padTo(10, ' ') + message
        println(line)
    }

    def run():Unit = {
        if(!initialised) throw new RuntimeException(
                    "Please initialise with initChannels(channels:Array[(String,Boolean)]) before starting.")


        printLine("Connecting to " + config.server)
        transport = IrcTransport.open(config.server)

        // CAP/SASL registration, then join channels
        login()
        joinChannels()

        printLine("Login complete")

        // handle server output
        serverReadThread.start()

        // handle user input from the console; with no console attached, just keep running
        val userInput = new BufferedReader (new InputStreamReader (System.in))
        var input = userInput.readLine
        while(input != null && handleUserInput(input)){
            input = userInput.readLine
        }
        if(input == null){
            serverReadThread.join()
        }
        writeLine("QUIT :Styr be with you")
        transport.close()
        System.exit(0)
    }

    def handleUserInput(input:String):Boolean = {
        if(input.toLowerCase.equals("stop"))
            return false
        else if(input.toLowerCase.startsWith("say ")){
            writeLine(String.format("PRIVMSG%s",input.drop(3)))
			}
		else if(input.toLowerCase.startsWith("action ")){
			val inputList = input.split(" ")
			writeLine(String.format("PRIVMSG %s \u0001ACTION %s\u0001", inputList.apply(1), inputList.drop(2).mkString(" ")))
			}
		else if(input.toLowerCase.startsWith("notice ")){
			writeLine(String.format("NOTICE%s",input.drop(6)))
			}
		else if(input.toLowerCase.equals("names")){
		//test clear names
			this.channels("#aeolus").users.clear()
			writeLine(String.format("NAMES #aeolus"))
        }
		else if(input.toLowerCase.equals("rickroll")){
			for (r <- rickrolls)
				writeLine(String.format("NOTICE #aeolus %s",r))
        }
		else if(input.toLowerCase.startsWith("join ")){
            for(channel <- input.split(" ")){
                if(!channels.contains(channel)){
                    writeLine(String.format("JOIN %s",channel))
                    channels.put(channel, new Channel(channel,this))
                }
            }
        }
        return true
    }

    class Channel(val channelName:String, val bot:FafBot){
        var users:HashSet[String] = new HashSet[String]()
		var mods:HashSet[String] = new HashSet[String]()

        def handleLine(line:String):Unit = {
            val command = line.split(" ")(1)
            if(command.equals("KICK")){
                val uname = line.split(" ")(3)
                val cname = line.split(" ")(2)
                if(!cname.equals(this.channelName))
                    return
                if(users.contains(uname))
                    users.remove(uname)
                if(bot.name.equals(uname)){
                    // We just got kicked, this is war.
                    users = new HashSet[String]()
                    Thread.sleep(500)
                    bot.writeLine("JOIN "+this.channelName)
                }
            } /*else if(command.equals("JOIN")) {
				val user = line.split("!")(0).drop(1)
				val channel = line.split(" ")(2).drop(1)
                users.add(line.split("!")(0).drop(1))
				if(partsJoins) {
					printLine((String.format("%s JOINS %s",user,channel)))
					//checkandsendOfflinemsg(user)
				}
            } else if(command.equals("PART") || command.equals("QUIT")) {
				val user = line.split("!")(0).drop(1)
				val channel = line.split(" ")(2).drop(1)
                users.remove(line.split("!")(0).drop(1))
				if(partsJoins) {
					printLine((String.format("%s %sS %s",user,command,channel)))
				}
            } else println("Unrecognised command: " + line)*/
        }
    }
}

class FunBot(config:BotConfig) extends FafBot(config) {
    // Data files keep their original names even if the bot runs under another nick
    val filePrefix = "TheSetoner"
    val functions = new HashSet[(String,(String,String,String) => Boolean,Long,Boolean)]()
	val injected = new HashSet[(String,String,String,Boolean) => Unit]()
	
    val lastTimes = new HashMap[((String,String,String) => Boolean,String),Long]()
    var helpCommand = "!hlp"

    val spammers:HashSet[(String,Long)] = new HashSet[(String,Long)]()
	val trackedUsers:HashSet[(String,Long)] = new HashSet[(String,Long)]()
    val demoteesFile = "./"+this.filePrefix+"-demotees.txt"
    val demotees:HashSet[String] = new HashSet[String]()
    val promoteesFile = "./"+this.filePrefix+"-promotees.txt"
    val promotees:HashSet[String] = new HashSet[String]()
    val adminsFile = "./"+this.filePrefix+"-admins.txt"
    val admins:HashSet[String] = new HashSet[String]()
	val offlineMsgFile = "./"+this.filePrefix+"-offlinemsgs.txt"
	val offlineMsgs:HashSet[String] = new HashSet[String]()

    var muted = false
    for(user <- getLines(demoteesFile))
        demotees.add(user)
    for(user <- getLines(promoteesFile))
        promotees.add(user)
    for(user <- getLines(adminsFile))
        admins.add(user)
	for(msg <- getLines(offlineMsgFile))
		offlineMsgs.add(msg)

    /* Parse a line, return (Sender, Command, Channel, Message)
    */
    def parseLine(line:String):(String, String, String, String) = {
        // line starts with ':', and the sender is delimeted by ' '
        val sender = line.drop(1).split(' ')(0)
        // command is the second argument on the line
        val command = line.split(' ')(1)
        // channel is the third argument on the line
        val channel = line.split(' ')(2)
        // Drop ':', sender, ' ', command, ' ', channel, " :"
        val message = line.drop(1 + sender.length + 1 + command.length + 1 + channel.length + 2)
        // and return
        (sender, command, channel, message)
    }
	
	def splitMessage(msg:String):List[String] = {
		val messages = msg.grouped(430).toList
		messages
	}
	
	def isAllDigits(x:String) = {
		x forall Character.isDigit
	}

    def sendLine(channel:String, message:String):Unit = {
        if(this.muted) return
        if(!channels.contains(channel)) {
			//Not a channel, maybe user?
			sendPM(channel,message)
            println(String.format("We're not in %s!",channel))
		} else {
			val msgs = splitMessage(message)
			for (msg <- msgs){
				this.writeLine(String.format("PRIVMSG %s %s", channel, msg))
				Thread.sleep(2000)
			}
		}
    }

    def sendPM(recipient:String, message:String):Unit = {
        if(this.muted) return
        if(channels.contains(recipient)) {
            println(String.format("%s is a channel!",recipient))
        } else {
			val msgs = splitMessage(message)
			for (msg <- msgs){
				this.writeLine(String.format("PRIVMSG %s %s", recipient, msg))
				Thread.sleep(400)
			}
		}
    }

    def sendAction(channel:String, message:String):Unit = {
        if(this.muted) return
        if(!channels.contains(channel)) {
			//Not a channel, maybe user?
			sendPM(channel,message)
            println(String.format("We're not in %s!",channel)) 
		} else {
			val msgs = splitMessage(message)
			for (msg <- msgs){
				this.writeLine(String.format("PRIVMSG %s \u0001ACTION %s\u0001", channel, msg))
				Thread.sleep(400)
			}
		}
    }

    def promote(user:String):Unit = {
        printLine(String.format("%s promoted", user))
        if(demotees.remove(user)){
            updateFile(demoteesFile,demotees.toArray)
        }
        else if(!promotees.contains(user)){
            promotees.add(user)
            updateFile(promoteesFile,promotees.toArray)
        }
    }
	
	def dedemote(user:String):Unit = {
        printLine(String.format("%s dedemote", user))
        if(demotees.remove(user)){
            updateFile(demoteesFile,demotees.toArray)
        }
    }

    def demote(user:String):Unit = {
        printLine(String.format("%s demoted", user))
        if(promotees.remove(user))
            updateFile(promoteesFile,promotees.toArray)
        else if(!demotees.contains(user)){
            demotees.add(user)
            updateFile(demoteesFile,demotees.toArray)
        }
    }
	
    def addSpammer(user:String, duration:Int):Unit = {
        printLine(String.format("%s marked as spammer for %s minutes", user, duration.toString))
			val currentTime: Long = (System.currentTimeMillis / 1000)
			val endTime: Long = currentTime+(duration*60)
			spammers+=(user -> endTime)
			//sendLine("#paul",String.format("addSpammer launched. %s",user))
    }
	
	def removeTrack():Unit = {
		//sendLine("#paul",String.format("removeTrack launched."))
		//sendLine("#paul",String.format("trackedUsers: %s",trackedUsers.toString))
		for ((user, stamp) <- trackedUsers) {
			//How long the tracking system should remember user using a command
			val trackingTime = 300
			if((stamp+trackingTime)<(System.currentTimeMillis/1000)){
				trackedUsers -= (user -> stamp)
			}
		}
		//sendLine("#paul",String.format("spammers: %s",spammers.toString))
		for ((user, stamp) <- spammers) {
			if((stamp)<System.currentTimeMillis/1000){
				spammers -= (user -> stamp)
			}
		}
		//sendLine("#paul",String.format("trackedUsers: %s",trackedUsers.toString))
		//sendLine("#paul",String.format("spammers: %s",spammers.toString))
	}
	
	def addTrack(user:String):Unit = {
		trackedUsers += (user -> (System.currentTimeMillis/1000))
	}
	
	def amountTrack(user:String):Int = {
		removeTrack()
			var count = 0
			for((name, stramp) <- trackedUsers){
				if(user.equals(name)){
					count+=1
				}
			}
			count
	}
	
	def saveMsg(message:String):Unit = {
		offlineMsgs.add(message)
		updateFile(offlineMsgFile,offlineMsgs.toArray)
	}
	
	def checkandsendOfflinemsg(recipient:String):Unit = {
	//timestpam,recpi,sender,msg
		for (element <- offlineMsgs) {
		val arr = element.split("§")
		val to = arr(1)
		val msg = arr(3)
		val time = arr(0)
		val sender = arr(2)
		if (to.contains(recipient)) {
				Thread.sleep(6000)
				sendPM(to,String.format("%s left you a message when you were offline (%s UTC): %s",sender,time,msg))
				offlineMsgs.remove(element)
				updateFile(offlineMsgFile,offlineMsgs.toArray)
			}
		}
	}

    def getLines(filepath:String):Array[String] = {
        try {
            val lines = Source.fromFile(filepath).getLines.toArray
            return lines
        } catch {
            case _ : Throwable => return new Array[String](0)
        }
    }

    def updateFile(filepath:String, values:Array[String]):Unit = {
        val writer = new PrintWriter(new File(filepath))
        for(v <- values)
            writer.write(v+"\r\n") // Remember our newline characters
        writer.close()
    }
	
	def checkFunctions(line:String):Boolean = {
		//Quitting players removed from channel userlists, could maybe be triggered from t._2 instead?
		val command = line.split(" ")(1)
		if (command.equals("PART") || command.equals("QUIT")) {
			val user = line.split("!")(0).drop(1)
			for ((chan,x) <- this.channels) {
				if (this.channels(chan).users.contains(user))
				this.channels(chan).users.remove(user)
			}
			printLine((String.format("%s %sS",user,command)))
			return true
		}
		//parseline returns (sender, command, channel, message)
        val t = parseLine(line)
        var name = t._1
		//printLine(t._1 + t._2 + t._3 + t._4)
		// Break out the users nick
        if(t._1.contains('!'))
            name = t._1.split('!')(0)
		//printLine("checkFunctions mode "+ t._2)
		if (!t._1.contains('!') || name.equals(this.name) || name.equals("NickServ")) {
			return true
		}
		
		//Joining players added to channel userlists
		if (t._2.equals("JOIN")) {
			val channel = line.split(" ")(2).drop(1)
			val userpayload = line.split(" ")(0).drop(1)
			//REDACTED COMSEC
			checkandsendOfflinemsg(name)
			this.channels(channel).users.add(name)
			//handle channel users
			return true
		}

		var channel = t._3
        if(!channels.contains(channel)){
            // We're not even in this channel, so something is wrong
            // Probably some info message, lets just drop it.
            // It's probably fine, whats the worst that could happen?
			// Second check to allow PM's getting through, dirty but it has worked so far?
			if(!channel.equals(this.name))
            return true
        }
		
		//Ignore demoted users
        if(demotees.contains(name)) return true
		//Ignore spammers
		for((user,stamp) <- spammers) {
			if(user.equals(name)) {
				//sendLine(name,String.format("You were placed in the time-out zone, you've been a naughty boy."))
				//Check the spam timer
				removeTrack()
				return true
			}
		}

        if(t._4.startsWith("!promote ")){
            if(admins.contains(name)){
					promote(t._4.split(" ")(1))
					sendLine(channel,String.format("User %s promoted.",t._4.split(" ")(1)))
				}
            return true
        }
        if(t._4.startsWith("!demote ")){
            if(admins.contains(name)){
					demote(t._4.split(" ")(1))
					sendLine(channel,String.format("User %s demoted.",t._4.split(" ")(1)))
				}
            return true
        }
		if(t._4.startsWith("!dedemote ")){
            if(admins.contains(name)){
					dedemote(t._4.split(" ")(1))
					sendLine(channel,String.format("User %s dedemoted.",t._4.split(" ")(1)))
				}
            return true
        }
		

		
		//this is PM
		if (channel.equals(this.name)) {
			channel = name
			//add admin control via PM, redirect to handleUserInput
			if(admins.contains(name)) {
			val commands = Array ("say ", "rickroll", "action ", "notice ", "names", "join ")
			for (com <- commands) {
				if(t._4.startsWith(com)) {
						handleUserInput(t._4)
						return true
					}
				}
			}
		}
		
		// Help. QAI already answers !help and !commands in #aeolus, so Setoner uses its own name:
		//   !setoner / !setoner help        -> list of commands
		//   !setoner help <command>         -> what that command does
		val helpWords = t._4.trim.split("\\s+")
		if(helpCommand != null && helpWords(0).equalsIgnoreCase(helpCommand)){
			if(helpWords.length >= 3 && helpWords(1).equalsIgnoreCase("help")){
				val wanted = "!" + helpWords(2).toLowerCase.stripPrefix("!")
				Lists.commandhelp.find(h => h.toLowerCase.takeWhile(_ != '-').trim == wanted) match {
					case Some(helpLine) => sendLine(channel, helpLine)
					case None => sendLine(channel, String.format("No help for %s. %s help lists the commands.", wanted, helpCommand))
				}
			} else {
				val commands = functions.toList.map(_._1).filter(_ != null).sortBy(_.toLowerCase).mkString(" ")
				sendLine(channel, String.format("Setoner commands: %s | %s help <command> for details", commands, helpCommand))
			}
			return true
		}

		//Only regular chat at this point, show it
		printLine((String.format("CHAT: %s: %s: %s",channel,name,t._4)))
		
		
		//TODO, the tracking/spam system can be made more compact, send data to the count checker and perform actions there etc.
		// Check registered functions for one that matches
        class Runner(val bot:FunBot) extends Runnable(){
            def run():Unit = {
                for((commandname,functionname,cooldown,monitoring) <- bot.functions){
				//Fucking hacky method, don't trigger this on regular messages...
					if(!commandname.equals("processUser")){
						if(monitoring){
							//for anti-spam, check if command matches
							if (t._4.toLowerCase.split(" ")(0).startsWith(commandname.toLowerCase)){
							//Clear old tracking data and spammers
							val trackingHits = amountTrack(name)
								if(trackingHits > 3) {
									addSpammer(name,30)
									sendLine("#aeolus",String.format("User %s was placed in the time-out zone, he's been a naughty boy.",name))
									return
								}
							}
							if(bot.lastTimes.contains((functionname,channel)) && (System.currentTimeMillis - bot.lastTimes((functionname,channel)) <= (cooldown*1000L))){
								/* We're in cooldown on this command */
								if (t._4.toLowerCase.split(" ")(0).startsWith(commandname.toLowerCase)){
									val timeleft:Long = ((cooldown*1000L)-(System.currentTimeMillis - bot.lastTimes((functionname,channel))))/1000L
									sendLine(name,String.format("The command %s you tried to use is on cooldown in the channel %s, still %s seconds left.",commandname,channel,timeleft.toString))
									if(channel.equals("#aeolus")){
										addTrack(name)
										val trackingHits = amountTrack(name)
										if(trackingHits == 3){
											sendLine("#aeolus",String.format("%s please don't spam me anymore.",name))
										}
									}
								}
							}
							//launches function in if clause, returns true -> timer, false no timer
							else if(functionname(name,channel,t._4)){
								//Add channel to timestamp
								if(channel.equals("#aeolus") && t._4.toLowerCase.split(" ")(0).startsWith(commandname.toLowerCase)){
									addTrack(name)
									val trackingHits = amountTrack(name)
									if(trackingHits == 3){
										sendLine("#aeolus",String.format("%s please don't spam me anymore.",name))
									}
								}
								bot.lastTimes.put((functionname,channel),System.currentTimeMillis)
								return
							}
						} else {
							//Just run the function
							functionname(name,channel,t._4)
						}
					}
				}
            }
        }
		
		val handleThread = new Thread(new Runner(this))
		handleThread.start()
		//sendLine("#test3","Runner started")
        return true
	}


    override def handleLine(line:String):Boolean = {
        if(super.handleLine(line))
            return true	
		val result = checkFunctions(line)
		return result
    }

    override def handlePriv(line:String):Boolean = {
        if(super.handlePriv(line))
            return true
        printLine("PM: " + line)
		checkFunctions(line)
        return false
    }

    override def handleUserInput(input:String):Boolean = {
        if(!super.handleUserInput(input))
            return false
        if(input.toLowerCase.startsWith("admin add ")){
            for(name <- input.drop(10).split(" "))
                admins.add(name)
            updateFile(adminsFile,admins.toArray)
        } else if(input.toLowerCase.startsWith("admin remove ")){
            for(name <- input.drop(13).split(" "))
                admins.remove(name)
            updateFile(adminsFile,admins.toArray)
        }
        return true
    }
	//val injected = new HashSet[(String,String,String,Boolean) => Unit]()
	def injectFunction(f:((String,String,String,Boolean) => Unit)):Unit = {
		this.injected.add(f)
	}
    def addFunction(s:String,f:((String,String,String) => Boolean),t:Long,m:Boolean):Unit = this.functions.add(s,f,t,m)
	//val functions = new HashSet[(String,(String,String,String) => Boolean,Long,Boolean)]()
}
