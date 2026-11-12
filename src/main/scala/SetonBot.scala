import java.io.PrintWriter
import java.io.FileWriter
import java.io.File
import scala.io.Source
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap
import scala.util.Random
import scala.util.control.Breaks._
import java.text._
import java.net.URL
import java.net.MalformedURLException
import java.nio.charset.UnmappableCharacterException
import java.nio.charset.MalformedInputException
import java.util.Calendar
import java.util.concurrent.TimeUnit
import fafbot._

object bot {
    def main(args:Array[String]):Unit ={
        // Nick, SASL credentials, server and channels come from setoner.conf (see setoner.conf.example)
        val config = BotConfig.load()
        val channels = config.channels

        val bot = new FunBot(config){
            val pingList = new HashSet[String]()
			val ladderList = new HashSet[String]()
			val tmmList = new HashSet[String]()
			val galaList = new HashSet[String]()
			val bhList = new HashSet[String]()
			val quoteList = Source.fromFile(this.filePrefix+"-quotelist.txt").getLines.toArray
			val tauntList = Source.fromFile(this.filePrefix+"-tauntList.txt").getLines.toArray
            this.helpCommand = "!setoner"
            val rand = new Random()
            this.initPingList()
			// this.initASFList()  -- removed with the redacted ASF watchlist code
			this.initLadderList()
			this.initTMMList()
			this.initGalaList()
			this.initBhList()
			//this.initquoteList()
			val prevPreaches = new Queue[String]()
            val prevWisdoms = new Queue[String]()
			val prevBalances = new Queue[String]()
			val prevExcuses = new Queue[String]()
			val prevGyms = new Queue[String]()
            // Initialise our last 8 things not to repeat
            for(i <- 0 until 12){ prevPreaches.enqueue(""); prevWisdoms.enqueue(""); prevBalances.enqueue("") }
			for(i <- 0 until 5){prevExcuses.enqueue("")}
			for(i <- 0 until 15){prevGyms.enqueue("")}
			
			import lists.Lists
			
			// Rating lookups need an authenticated FAF API client (QAI has one, see its !player command).
			// Until Setoner gets its own, the optional minimum-rating argument of the !to ... calls is ignored.
			object Leaderboards {
			  def getGRating(username: String): Int = Int.MaxValue
			  def getLRating(username: String): Int = Int.MaxValue
			}
			
			def convertInt(s:String):Int = {
				try {
					s.toInt
				} catch {
					case e: Exception => 0
				}
			}
			
			def checkStatus(user:String):String = {
				val chan = this.channels("#aeolus")
				var status = "offline"
				if(chan.users.contains(user)) {
					status = "online"
				}
				return status
			}
			
			def updatePingList():Unit = {
				val writer = new PrintWriter(new File(this.filePrefix+"-pinglist.txt"))
				for(name <- pingList)
					writer.write(name+"\r\n")
				writer.close()
			}

			def updateGalaList():Unit = {
				val writer = new PrintWriter(new File(this.filePrefix+"-galalist.txt"))
				for(name <- galaList)
					writer.write(name+"\r\n")
				writer.close()
			}
			
			def updateBhList():Unit = {
				val writer = new PrintWriter(new File(this.filePrefix+"-bhlist.txt"))
				for(name <- bhList)
					writer.write(name+"\r\n")
				writer.close()
			}
				
			def updateLadderList():Unit = {
					val writer = new PrintWriter(new File(this.filePrefix+"-ladderlist.txt"))
					for(name <- ladderList)
						writer.write(name+"\r\n")
					writer.close()
			}
			def updateTMMList():Unit = {
					val writer = new PrintWriter(new File(this.filePrefix+"-tmmlist.txt"))
					for(name <- tmmList)
						writer.write(name+"\r\n")
					writer.close()
			}
			
			/*
									val hashuser = new HashSet[String]()
									hashuser.add(name)
									usernames += (id -> hashuser)
									for (nick <- foundnicknames){
									usernames(id).add(nick)
			*/

			def initPingList():Unit = {
					try {
						Source.fromFile(this.filePrefix+"-pinglist.txt").getLines().foreach(pingList.add(_))
					}
					catch {
						case _ : java.io.FileNotFoundException => {
							printLine(String.format("File not found: %s, initialising with an empty pinglist", this.filePrefix+"-pinglist.txt"))
						}
						case _ : Throwable => { printLine("Something bad just happened, might want to do some debugging!") }
					}
				}
				
			def initGalaList():Unit = {
					try {
						Source.fromFile(this.filePrefix+"-galalist.txt").getLines().foreach(galaList.add(_))
					}
					catch {
						case _ : java.io.FileNotFoundException => {
							printLine(String.format("File not found: %s, initialising with an empty pinglist", this.filePrefix+"-galalist.txt"))
						}
						case _ : Throwable => { printLine("Something bad just happened, might want to do some debugging!") }
					}
				}
				
			def initBhList():Unit = {
					try {
						Source.fromFile(this.filePrefix+"-bhlist.txt").getLines().foreach(bhList.add(_))
					}
					catch {
						case _ : java.io.FileNotFoundException => {
							printLine(String.format("File not found: %s, initialising with an empty pinglist", this.filePrefix+"-bhlist.txt"))
						}
						case _ : Throwable => { printLine("Something bad just happened, might want to do some debugging!") }
					}
				}
				
			def initLadderList():Unit = {
					try {
						Source.fromFile(this.filePrefix+"-ladderlist.txt").getLines().foreach(ladderList.add(_))
					}
					catch {
						case _ : java.io.FileNotFoundException => {
							printLine(String.format("File not found: %s, initialising with an empty ladderlist", this.filePrefix+"-ladderlist.txt"))
						}
						case _ : Throwable => { printLine("Something bad just happened, might want to do some debugging!") }
					}
				}
				
			def initTMMList():Unit = {
					try {
						Source.fromFile(this.filePrefix+"-tmmlist.txt").getLines().foreach(tmmList.add(_))
					}
					catch {
						case _ : java.io.FileNotFoundException => {
							printLine(String.format("File not found: %s, initialising with an empty ladderlist", this.filePrefix+"-tmmlist.txt"))
						}
						case _ : Throwable => { printLine("Something bad just happened, might want to do some debugging!") }
					}
				}

            def getTaunt(caller:String):String = {
				val taunts = Array(
                                (100 , s"Across the desolate reaches of space comes the call, the call To Setons!  $caller summons you!"),
                                (100 , s"Across the deep blue sea, through the endless forest, down from the skyhigh mountains there comes the call: To Setons!"),
                                (30  , s"$caller started a lobby sim, pls join."),
                                (30  , s"I choose YOU to be my next victims at Setons Clutch!"),
                                (30  , s"The holy trinity of ras, salt and mex awaits you soldier at the holy place called Setons Clutch."),
                                (30  , s"$caller is dropping big salty tears unless you join their sentons game!"),
                                (60  , s"From a land of wealth beyond measure, $caller demands your prescence! T3 mexes await!"))
                var sum = 0
                for ((i,taunt) <- taunts)
                    sum+=i

                for ((i,taunt) <- taunts){
                    if(rand.nextInt(sum)<i) return taunt
                    else sum-=i
                }
                return s"$caller"
            }
			
			def getGalaTaunt(caller:String):String = {
			val galataunts = Array(
				(30 , s"$caller summons the Knights of Seton's to the round table of lobbysim!"),
				(10 , s"$caller has spotted a fire breathing Soul Ripper roaming free in Seton's Clutch, Knights of Seton's are called upon to save the people of #aeolus!"),
				(10 , s"Knights, prepare your transports! $caller has announced a prestigious jousting session to be held at Seton's Clutch!"),
				(10 , s"A defenseless T3 Air HQ is under siege at Seton's Clutch, and only the bravest Knights of Seton's can save it!"),
				(10 , s"$caller is hosting a tournament where only the finest Knights of Seton's have been granted permission to participate, be there or renounce your privileges!"),
				(10 , s"$caller is inviting all Knights to the holiest place of RTS to defend their honour!"),
				(10 , s"Knights of Seton's! Rally to me! The hour is upon us!"),
				(10 , s"Once more unto the breach, my Knights, once more; Or we will blast away the Clutch with our death nukes!"),
				(10 , s"Warning! This is not a drill! Will all Knights enter the nearest quantum gateway for immediate transportation! Our King $caller has urgent need to satisfy his unquenchable addiction."),
				(1, s"An ancient relic dating back to the lost times when the Dark Knight Blackheartius reigned and RAS was good has been located, and $caller needs urgent assistance to retrieve it!")
			)
                var sum = 0
                for ((i,taunt) <- galataunts)
                    sum+=i

                for ((i,taunt) <- galataunts){
                    if(rand.nextInt(sum)<i) return taunt
                    else sum-=i
                }
                return s"$caller"
            }
			
			def getBhTaunt(caller:String):String = {
			val bhtaunts = Array(
				(10 , s"Feel the beauty of proper balance. Join Heartler and his Sentons NOW! "),
				(10 , s"Sentons, Sentons, Sentons. "),
				(10 , s"Stop joining noob lobbies and join the only properly balanced lobby. BHalance. "),
				(10 , s"Do you remember back then in 2013 when we tryharded Sentons? We can bring that time back, right now, with BHalance."),
				(10 , s"One Lobby, One Map, One Balance. You know which. Join now. "),
				(10 , s"Heartler thinks that even YOU are someone that he can use in his lobby. Join Sentons with the best balance FAF has to offer."),
				(10 , s"Tired of factories having less HP than a titan? Join BHalance on Sentons, only today, only now. "),
				(10 , s"Heartler is home from gym and his biceps is fully pumped. The only thing that is necessary now is a nice game of Sentons. "),
				(10 , s"Hello, I would love you to join my wonderful Sentons lobby. The only no-insult, no-bullying and big biceps lobby on FAF."),
				(10 , s"We both know that your real life is worse than Sentons. Therefore the logical action is to join RIGHT NOW!"),
				(10 , s"Sitting in #aeolus? Feeling tired of life? The solution is here: Sentons with Heartler and his balance. "),
				(10 , s"Heartlers Sentons - Lose 3 hours of real life for 2 ragequits and 1 disconnect. Join the fun now! "),
				(10 , s"Sentons - You cant escape your addiction anyway. Shorten your suffering and join now.")
			)
                var sum = 0
                for ((i,taunt) <- bhtaunts)
                    sum+=i

                for ((i,taunt) <- bhtaunts){
                    if(rand.nextInt(sum)<i) return taunt
                    else sum-=i
                }
                return s"$caller"
            }

            //Now for our chat functions
            def signup(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!setons")){
                    //add some funky admin command
                    if(m.toLowerCase.startsWith("!setons ") && admins.contains(n)){
                        val names = m.drop(8).split(" ")
                        for(name <- names){
                            if(name.length > 0)
                                pingList.add(name)
								sendAction(channel,String.format("%s added to setons pinglist by admin override.",name))
						}
                        updatePingList()
						return true
                    } else {
                        pingList.add(n)
                        updatePingList()
						sendAction(channel,String.format("%s added to setons pinglist.",n))
                    }
                    return true
                }
                return false
            }
            this.addFunction("!setons",signup(_,_,_),0L,true)

			def addgalaplayer(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!dubsquire") && n.equals("galacticfear")){
					val names = m.drop(10).split(" ")
					for(name <- names){
						if(name.length > 0){
							galaList.add(name)
							updateGalaList()
						}
					}
					sendAction(channel,String.format("%s got dubbed as Seton's Knight(s) by the King of Seton's!",names.mkString(" ")))
                    return true
                }
                return false
            }
            this.addFunction("!dubplayer",addgalaplayer(_,_,_),0L,true)
			
			def addbhplayer(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!bhalance") && n.equals("Blackheart")){
					val names = m.drop(9).split(" ")
					for(name <- names){
						if(name.length > 0){
							bhList.add(name)
							updateBhList()
						}
					}
					sendAction(channel,String.format("%s, you have been bhalanced by Blackheart and received a certified registration.",names.mkString(" ")))
                    return true
                }
                return false
            }
            this.addFunction("!bhalance",addbhplayer(_,_,_),0L,true)

            def unsignup(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!unsetons")){
                    if(m.toLowerCase.startsWith("!unsetons ") && admins.contains(n)){
                        val names = m.drop(10).split(" ")
                        for(name <- names){
                            pingList.remove(name)
							sendAction(channel,String.format("%s removed from setons pinglist by admin override.",name))
							return true
						}
                        updatePingList()
                    } else if(pingList.remove(n))
                        updatePingList()
						sendAction(channel,String.format("%s removed from setons pinglist.",n))
                    return true
                }
                return false
            }
            this.addFunction("!UnSetons",unsignup(_,_,_),0L,true)
			
			def signupLadder(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!signupladder")){
                    //add some funky admin command
                    if(m.toLowerCase.startsWith("!signupladder ") && admins.contains(n)){
                        val names = m.drop(14).split(" ")
                        for(name <- names)
                            if(name.length > 0)
                                ladderList.add(name)
                        updateLadderList()
                    } else {
                        ladderList.add(n)
                        updateLadderList()
						sendAction(channel,String.format("%s added to ladder pinglist.",n))
                    }
                    return true
                }
                return false
            }
            this.addFunction("!signupladder",signupLadder(_,_,_),0L,true)

            def unsignupLadder(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!unladder")){
                    if(m.toLowerCase.startsWith("!unladder ") && admins.contains(n)){
                        val names = m.drop(9).split(" ")
                        for(name <- names)
                            ladderList.remove(name)
                        updateLadderList()
                    } else if(ladderList.remove(n))
                        updateLadderList()
						sendAction(channel,String.format("%s removed from ladder pinglist.",n))
                    return true
                }
                return false
            }
            functions.add("!unladder",unsignupLadder(_,_,_),0L,true)
			
			def signuptmm(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!tmm")){
                    //add some funky admin command
                    if(m.toLowerCase.startsWith("!tmm ") && admins.contains(n)){
                        val names = m.drop(4).split(" ")
                        for(name <- names)
                            if(name.length > 0)
                                tmmList.add(name)
                        updateTMMList()
                    } else {
                        tmmList.add(n)
                        updateTMMList()
						sendAction(channel,String.format("%s added to TMM pinglist.",n))
                    }
                    return true
                }
                return false
            }
            this.addFunction("!tmm",signuptmm(_,_,_),0L,true)

            def unsignuptmm(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!untmm")){
                    if(m.toLowerCase.startsWith("!untmm ") && admins.contains(n)){
                        val names = m.drop(4).split(" ")
                        for(name <- names)
                            tmmList.remove(name)
                        updateTMMList()
                    } else if(tmmList.remove(n))
                        updateTMMList()
						sendAction(channel,String.format("%s removed from TMM pinglist.",n))
                    return true
                }
                return false
            }
            functions.add("!untmm",unsignuptmm(_,_,_),0L,true)

            def callToSetons(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!to setons")){
                    if(!this.channels.contains(channel))
                        printLine("wtf?? (might have got this in pm)")
                    else if(!pingList.contains(n))
                        sendAction(channel,String.format("believes %s needs to sell their soul to use this command...",n))
                    else {
						//Debug
						//printLine(String.format("CHANNEL USERLIST -> %s", this.channels(channel).users))
						val arr = m.split(" ", 3);
						var ratinglimit = -1
						if (arr.length > 2) {
							try {
								ratinglimit = arr(2).toInt
							 } catch {
								case e: NumberFormatException => {
										ratinglimit = -1
										sendLine(channel,String.format("Proper command format: !to setons minimumrating"))
									}	  
								}

						}
                        var victims = ""
                        val c = this.channels(channel)
						//Debug
						//printLine(String.format("CHANNEL USERLIST -> %s", this.channels(channel).users))
                        for(name <- pingList) {
							var rating = 5000
							if(c.users.contains(name) && (!name.equals(n))) {
								if (ratinglimit != -1) {
									rating = Leaderboards.getGRating(name)
								}
									if (rating>ratinglimit)
										victims = victims + (" " + name)
									}
						}
                        if(victims.length > 0) {
                            sendLine(channel,getTaunt(n))
                            sendLine(channel,victims)
							//Update userlist for next to setons ping...
							this.channels(channel).users.clear()
							writeLine(String.format("NAMES #aeolus"))
						}
                        else
                            sendAction(channel,String.format("thinks %s needs to get some setons mates to ping first...",n))
                    }
                    return true
                }
                return false
            }
            functions.add("!To Setons",callToSetons(_,_,_),30L,true)
			
			def callToSSetons(n:String,channel:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!to superior setons") && n.equals("galacticfear")){
					val arr = m.split(" ", 4);
					var ratinglimit = -1
					if (arr.length > 3) {
						try {
							ratinglimit = arr(3).toInt
						 } catch {
							case e: NumberFormatException => {
									ratinglimit = -1
									sendLine(channel,String.format("Proper command format: !to setons minimumrating"))
								}	  
							}

					}
					var knights = ""
					val c = this.channels(channel)
					//Debug
					//printLine(String.format("CHANNEL USERLIST -> %s", this.channels(channel).users))
					for(name <- galaList) {
						var rating = 5000
						if(c.users.contains(name) && (!name.equals(n))) {
							if (ratinglimit != -1) {
								rating = Leaderboards.getGRating(name)
							}
								if (rating>ratinglimit)
									knights = knights + (" " + name)
								}
					}
					if(knights.length > 0) {
						sendLine(channel,getGalaTaunt(n))
						sendLine(channel,knights)
						//Update userlist for next to setons ping...
						this.channels(channel).users.clear()
						writeLine(String.format("NAMES #aeolus"))
					}
					else
						sendAction(channel,String.format("No Knights were found, they must be busy drinking and fighting elsewhere..."))
					return true
                }
                return false
            }
            functions.add("!to superior setons",callToSSetons(_,_,_),30L,true)
			
			def callToBHSetons(n:String,channel:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!to sentons") && n.equals("Blackheart")){
					val arr = m.split(" ", 4);
					var ratinglimit = -1
					if (arr.length > 3) {
						try {
							ratinglimit = arr(3).toInt
						 } catch {
							case e: NumberFormatException => {
									ratinglimit = -1
									sendLine(channel,String.format("Proper command format: !to sentons minimumrating"))
								}	  
							}

					}
					var bhalancees = ""
					val c = this.channels(channel)
					//Debug
					//printLine(String.format("CHANNEL USERLIST -> %s", this.channels(channel).users))
					for(name <- bhList) {
						var rating = 5000
						if(c.users.contains(name) && (!name.equals(n))) {
							if (ratinglimit != -1) {
								rating = Leaderboards.getGRating(name)
							}
								if (rating>ratinglimit)
									bhalancees = bhalancees + (" " + name)
								}
					}
					if(bhalancees.length > 0) {
						sendLine(channel,getBhTaunt(n))
						sendLine(channel,bhalancees)
						//Update userlist for next to setons ping...
						this.channels(channel).users.clear()
						writeLine(String.format("NAMES #aeolus"))
					}
					else
						sendAction(channel,String.format("No bhlance certified players found."))
					return true
                }
                return false
            }
            functions.add("!to sentons",callToBHSetons(_,_,_),30L,true)
			
			def tikapuu(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!to ladder")){
                    if(!this.channels.contains(channel))
                        printLine("wtf?? (might have got this in pm)")
                    else if(!ladderList.contains(n))
                        sendAction(channel,String.format("believes %s needs to climb the ladder of enlightenment to use this command...",n))
                    else {
						val arr = m.split(" ", 3);
						var ratinglimit = -1
						if (arr.length > 2) {
							try {
								ratinglimit = arr(2).toInt
							 } catch {
								case e: NumberFormatException => {
									ratinglimit = -1
									sendLine(channel,String.format("Proper command format: !to ladder minimumrating"))
									}	  
								}

						}
                        var victims = ""
                        val c = this.channels(channel)
                        for(name <- ladderList) {
							var rating = 5000
							if(c.users.contains(name) && (!name.equals(n))) {
								if (ratinglimit != -1) {
									rating = Leaderboards.getLRating(name)
								}
									if (rating>ratinglimit)
										victims = victims + (" " + name)
									}
						}
                        if(victims.length > 0) {
                            sendLine(channel,String.format("%s summons fellow ladder players to become their next victim!",n))
                            sendLine(channel,victims)
							this.channels(channel).users.clear()
							writeLine(String.format("NAMES #aeolus"))
						}
                        else
                            sendAction(channel,String.format("thinks %s needs to get some ladder mates to ping first...",n))
                    }
                    return true
                }
                return false
            }
            functions.add("!To Ladder",tikapuu(_,_,_),30L,true)
			
			def calltotmm(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!to tmm")){
                    if(!this.channels.contains(channel))
                        printLine("wtf?? (might have got this in pm)")
                    else if(!tmmList.contains(n))
                        sendAction(channel,String.format("believes %s needs to signup for this function...",n))
                    else {
						val arr = m.split(" ", 3);
						var ratinglimit = -1
						if (arr.length > 2) {
							try {
								ratinglimit = arr(2).toInt
							 } catch {
								case e: NumberFormatException => {
									ratinglimit = -1
									sendLine(channel,String.format("Proper command format: !to tmm minimumrating"))
									}	  
								}

						}
                        var victims = ""
                        val c = this.channels(channel)
                        for(name <- tmmList) {
							var rating = 5000
							if(c.users.contains(name) && (!name.equals(n))) {
								if (ratinglimit != -1) {
									rating = Leaderboards.getLRating(name)
								}
									if (rating>ratinglimit)
										victims = victims + (" " + name)
									}
						}
                        if(victims.length > 0) {
                            sendLine(channel,String.format("%s summons fellow TMM players!",n))
                            sendLine(channel,victims)
							this.channels(channel).users.clear()
							writeLine(String.format("NAMES #aeolus"))
						}
                        else
                            sendAction(channel,String.format("thinks %s needs to get some TMM mates to ping first...",n))
                    }
                    return true
                }
                return false
            }
            functions.add("!to TMM",calltotmm(_,_,_),30L,true)

            def commandments(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.split(" ")(0).equals("!commandments")){
                    sendAction(channel,String.format("sternly reads the Ten Commandments to %s, as laid out in the holy book of Nepalmoses.",n))
                    for(c <- Lists.commandments)
                        sendPM(n,c)
                    return true
                }
                return false
            }
            this.addFunction("!commandments",commandments(_,_,_),15L,true)	
			
			def threaten(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!threaten")){
					if(!pingList.contains(n) && !promotees.contains(n)) {
						sendAction(channel,String.format("You %s don't meet up to my standards. Only the chosen few have access to this command.",n))
						return false
					}
					val arr = m.split(" ", 2);
						if(arr.length < 2) {
							sendLine(channel,String.format("User not found or no recipient defined."))
							return false
						}
					val victim = arr(1)
					if(!pingList.contains(victim) && !promotees.contains(victim)) {
							sendAction(channel,String.format("%s's mind is too fragile for these holy words and thus must be protected from them.",victim))
							return false
						}
                    sendAction(channel,String.format("threatens %s on behalf of %s",victim,n))
                    for(t <- Lists.threats)
						sendPM(victim,t)
                    return true
                }
                return false
            }
            this.addFunction("!threaten",threaten(_,_,_),15L,true)
			
			def sentonment(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!sentonment")){
					if(!pingList.contains(n) && !promotees.contains(n)) {
						sendAction(channel,String.format("You %s don't meet up to my standards. Only the chosen few have access to this command.",n))
						return true
					}
						val arr = m.split(" ", 2);
							if(arr.length < 2) {
								sendLine(channel,String.format("User not found or no recipient defined."))
								return false
							}
						val victim = arr(1)
						if(!pingList.contains(victim) && !promotees.contains(victim)) {
							sendAction(channel,String.format("%s's mind is too fragile for these holy words and thus must be protected from them.",victim))
							return false
						}
						sendAction(channel,String.format("%s has a talk with %s...",n,victim))
						for(t <- Lists.sentonments)
							sendPM(victim,t)
						return true
                }
                return false
            }
            this.addFunction("!sentonment",sentonment(_,_,_),15L,true)
			
			def lifestory(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!lifestory")){
                    sendAction(channel,String.format("shares his life story with %s",n))
                    for(t <- Lists.storylines)
						sendPM(n,t)
                    return true
                }
                return false
            }
            this.addFunction("!lifestory",lifestory(_,_,_),15L,true)

			def feedbh(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!feedbh")){
					val salmiakkis = Array(
						"https://images-na.ssl-images-amazon.com/images/I/51p-RAwMAOL._SY450_.jpg",
						"https://images.alko.fi/images/cs_srgb,f_auto,t_medium/cdn/154656/koskenkorva-salmiakki-plastic-bottle.jpg",
						"https://images-na.ssl-images-amazon.com/images/I/311stHAj%2BjL.jpg",
						"https://images-na.ssl-images-amazon.com/images/I/712cIiZNnmL._SL1371_.jpg",
						"https://wine-searcher1.freetls.fastly.net/images/labels/96/49/koskenkorva-salmiakki-liqueur-finland-10429649.jpg",
						"https://images-na.ssl-images-amazon.com/images/I/41s-24QOGwL.jpg",
						"http://salmiakki.fi/wp-content/uploads/2017/02/2017-Salmiakki-Finlandia-Haganol-jyvat-nominaatti-300x200.jpg",
						"http://folkloresuomifinland.fi/wp-content/uploads/sites/146/2017/08/salmiakki12.jpg",
						"https://cloetta.studio.crasman.fi/pub/web/images/tuotekuvat/PNG-kuva_1009065_Suosikit+230g+Suolainen+Salmiakki.png?c=system_640x",
						"https://upload.wikimedia.org/wikipedia/commons/b/b7/Halva_-_V%C3%A4kev%C3%A4_Salmiakki.jpg",
						"http://www.dlc.fi/~marian1/gourmet/pic/saltliqr.jpg",
						"https://upload.wikimedia.org/wikipedia/commons/thumb/c/c8/%C3%84ss%C3%A4_Mix_salmiak.jpg/250px-%C3%84ss%C3%A4_Mix_salmiak.jpg",
						"https://upload.wikimedia.org/wikipedia/commons/thumb/5/51/Swedish_salty_liquorice.jpg/1200px-Swedish_salty_liquorice.jpg",
						"https://www.apteekki.fi/media/1-apteekki.fi/terveydeksi/uutiset/cache/uutisen_kuva_350x233px_yskaan_ja_seeparanraidoiksi-350x233.jpg",
						"https://candyhero.com/__public/images/products/c-gallery-9-53/3982.jpg",
						"https://i.ytimg.com/vi/DaKFr0zTzcY/maxresdefault.jpg",
						"http://www.candygurus.com/wp-content/uploads/IMG_7847.jpg",
						"http://www.ingman.fi/Resources/Images/ingman-salmiakki-1-litra-300x3001607-1319356.png",
						"https://cdn-a.verkkokauppa.com/images/60/2_325358-2184x1760.jpeg",
						"https://i.imgur.com/Pvy7Nvk.jpg"
					)
				val salmiakki = salmiakkis(Random.nextInt(salmiakkis.length))
					sendLine("Blackheart",String.format("Your todays meal: %s",salmiakki))
                    return true
                }
                return false
            }
            this.addFunction("!feedbh",feedbh(_,_,_),120L,true)

            def preach(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!preach")){
						if(!pingList.contains(n) && !promotees.contains(n)){
							sendAction(channel,String.format("does not find %s worthy of the holy words in the Styr'an...",n))
							return false
						}
					val newPreaches = Lists.preaches.filter(!prevPreaches.contains(_))
                    val preach = newPreaches(rand.nextInt(newPreaches.length))
                    sendAction(channel,String.format("opens the holy Styr'an, so that %s may spread the holy spirit of Sentons amongst the ignorant:",n))
                    sendLine(channel,preach)
					prevPreaches.enqueue(preach); prevPreaches.dequeue
                    return true
                }
                return false
            }
            this.addFunction("!preach",preach(_,_,_),60L,true)
			
			def sharewisdom(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!sharewisdom")){
						if(!pingList.contains(n) && !promotees.contains(n)){
							sendAction(channel,String.format("does not find %s worthy of the wisdom from the ones enligthened by the Sentons...",n))
							return false
						}
					val quoteList = Source.fromFile(this.filePrefix+"-quotelist.txt").getLines.toArray.filter(!prevWisdoms.contains(_))
                    val wisdom = quoteList(rand.nextInt(quoteList.length))
                    sendAction(channel,String.format("Shares the collected wisdom of fellow enlightened Setoneers among the peasants:",n))
                    sendLine(channel,wisdom)
					prevWisdoms.enqueue(wisdom); prevWisdoms.dequeue
                    return true
                }
                return false
            }
            this.addFunction("!sharewisdom",sharewisdom(_,_,_),60L,true)
			
			def excuse(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!luxcuse")){
					val excuseList = Source.fromFile(this.filePrefix+"-excuselist.txt").getLines.toArray.filter(!prevExcuses.contains(_))
                    val selectedexcuse = excuseList(rand.nextInt(excuseList.length))
                    sendAction(channel,String.format("Spins the luxcusewheel...",n))
					Thread.sleep(2000)
					sendAction(channel,String.format("tik tik tik",n))
					Thread.sleep(2000)
                    sendLine(channel,String.format("Todays luxclusive luxcuse: %s",selectedexcuse))
					prevExcuses.enqueue(selectedexcuse); prevExcuses.dequeue
                    return true
                }
                return false
            }
            this.addFunction("!luxcuse",excuse(_,_,_),60L,true)
			
			def report(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!report")){
                    sendLine(channel,String.format("Report your issue to moderators here: %s","https://www.faforever.com/account/report"))
                    return true
                }
                return false
            }
            this.addFunction("!report",report(_,_,_),60L,true)

            def choose(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!choose ")){
                    val options = m.split(" ").drop(1).map(_.stripSuffix(",").stripPrefix(",")).filter(!_.toLowerCase().equals("or")).filter(_.length()>0)
                    val len = options.length
					for(o <- options){
						for (w <- Lists.riggedwords){
							if (o.toLowerCase.contains(w)) {
								sendLine(channel,String.format("I choose... %s ",o))
								return true
							}
						}
					}
					sendLine(channel,String.format("I choose... %s ",options(rand.nextInt(len))))
					return true
                }
                return false
            }
            this.addFunction("!choose",choose(_,_,_),10L,true)

			def grondcheck(n:String,channel:String,m:String):Boolean = {
                if(m.toLowerCase.startsWith("!grondcheck")){
					val arr = m.split(" ", 2);
					var user = n
						if(arr.length > 1) {
							user = arr(1)
						}
						if(user.toLowerCase.contains("grond")){
							sendLine(channel,String.format("GROND! GROND! GROND!"))
						} else {
							sendLine(channel,String.format("GROND not found, weak!"))
						}
                    return true
                }
                return false
            }
            this.addFunction("!grondcheck",grondcheck(_,_,_),60L,true)
			
			def balanceriot(n:String,channel:String,m:String):Boolean = {
				var toRiot = false
					if(m.toLowerCase.startsWith("!balanceriot")){
						toRiot = true
					} else {
						for (t <- Lists.balancetriggers)
							if(m.toLowerCase.contains(t)){
								if(math.random < 0.04) {
									toRiot = true
							}
						}
					}
				if (toRiot) {
						val balanceList = Source.fromFile(this.filePrefix+"-balancelist.txt").getLines.toArray.filter(!prevBalances.contains(_))
						val rant = balanceList(rand.nextInt(balanceList.length))
						sendAction(channel,String.format("starts a balance riot!",n))
						sendLine(channel,rant)
						prevBalances.enqueue(rant); prevBalances.dequeue
                    return true
                } else {
					return false
				}
            }
            this.addFunction("!balanceriot",balanceriot(_,_,_),60L,true)
			
			def gymquote(n:String,channel:String,m:String):Boolean = {
				var toGym = false
					if(m.toLowerCase.startsWith("!biceps")){
						toGym = true
					} else {
						for (t <- Lists.gymtriggers)
							if(m.toLowerCase.contains(t)){
								if(math.random < 0.08) {
									toGym = true
							}
						}
					}
				if (toGym) {
						val gymList = Source.fromFile(this.filePrefix+"-gymlist.txt").getLines.toArray.filter(!prevGyms.contains(_))
						val gymstuff = gymList(rand.nextInt(gymList.length))
						//sendAction(channel,String.format("starts a balance riot!",n))
						sendLine(channel,gymstuff)
						prevGyms.enqueue(gymstuff); prevGyms.dequeue
                    return true
                } else {
					return false
				}
            }
            this.addFunction("!biceps",gymquote(_,_,_),60L,true)
			
			def gogym(n:String,c:String,m:String):Boolean = {
				var lifters = ""
				
				if(m.toLowerCase.startsWith("!gym")){
				val victims = m.split(" ").map(_.stripSuffix(",").stripPrefix(",")).filter(_.length()>0)
				if(c matches ".*#.*"){
				val chan = this.channels(c)
					for (v <- victims) {
						if(chan.users.contains(v))
							lifters = lifters + (" " + v)
						}
					}
					if(lifters.length > 0) {
						sendAction(c,String.format("whips %s on behalf of %s, GO TO GYM NOW! PUMP THAT IRON! GROW THE BICEPS! EAT THAT PROTEIN!",lifters,n))
					} else {
						sendAction(c,String.format("something might be wrong, %s doesn't seem to have any gym buddies :/",n))
					}
					return true
                }
                return false
            }
            this.addFunction("!gym",gogym(_,_,_),60L,true)
			
			def undress(n:String,c:String,m:String):Boolean = {
                var undressSelf = true
				var strippers = ""
				
				if(m.toLowerCase.contains("undress")){
				val victims = m.split(" ").map(_.stripSuffix(",").stripPrefix(",")).filter(_.length()>0)
				if(c matches ".*#.*"){
				val chan = this.channels(c)
					for (v <- victims) {
						if(chan.users.contains(v))
							strippers = strippers + (" " + v)
						}
					if (strippers.length > 0){
						undressSelf = false
					}
					}
					if(undressSelf) {
						sendAction(c,String.format("carefully, slowly and sensually undresses himself for %s",n))
					} else {
						sendAction(c,String.format("carefully, slowly and sensually undresses %s",strippers))
					}
						return true
                }
                return false
            }
            this.addFunction("!undress",undress(_,_,_),60L,true)
			
			def refaf(n:String,c:String,m:String):Boolean = {
                var noNick = true
				var refaffers = ""
				val refaflink = "https://youtu.be/4Cx5KwqS8Lw"
				if(m.toLowerCase.startsWith("!refaf")){
				val victims = m.split(" ").map(_.stripSuffix(",").stripPrefix(",")).filter(_.length()>0)
				if(c matches ".*#.*"){
				val chan = this.channels(c)
					for (v <- victims) {
						if(chan.users.contains(v))
							refaffers = refaffers + (" " + v)
						}
					if (refaffers.length > 0){
						noNick = false
					}
				}
					if(noNick) {
						sendAction(c,String.format("Refaf explained: %s",refaflink))
					} else {
						sendAction(c,String.format("%s demands you %s to refaf! How to: %s",n,refaffers,refaflink))
					}
						return true
                }
                return false
            }
            this.addFunction("!refaf",refaf(_,_,_),60L,true)
			
			def savemsg(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!offlinemessage")){
				val arr = m.split(" ")
				if(arr.length < 2) {
						sendLine(c,String.format("No user defined. Proper format: !offlinemessage Recipient your message."))
						return true
					}
				if(arr.length < 3) {
						sendLine(c,String.format("No message defined. Proper format: !offlinemessage Recipient your message."))
						return true
					}
				val user = arr(1)
				if(checkStatus(user).equals("online")) {
					sendLine(c,String.format("%s is online you sillywilly, message them yourself.",user))
					return true
				}
				var msg = ""
				for (word <- arr.drop(2))
					{
						msg = msg + word + " "
					}
				val timestamp = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
				val saveinfo = String.format("%s§%s§%s§%s",timestamp,user,n,msg.mkString)
				saveMsg(saveinfo)
					sendAction(c,String.format("Your message to %s has been saved, he/she will receive it when next time online.",user))
					return true
                }
                return false
            }
            this.addFunction("!offlinemessage",savemsg(_,_,_),5L,true)
			
			def bawssemble(n:String,c:String,m:String):Boolean = {
				var pingers = ""
					if(m.toLowerCase.startsWith("!bawssemble")){
						if(c.equals("#BAW_clan")){
						val noping = Array ("&AeonCommander","Giebmasse_irc","TheSetoner")
						val chan = this.channels(c)
							for (v <- chan.users) {
								if(chan.users.contains(v) && !noping.contains(v))
									pingers = pingers + (" " + v)
							}
								sendAction(c,String.format("BAWtobots ASSEMBLE! %s",pingers))
								return true
						}
					}
                return false
            }
            this.addFunction("!bawssemble",bawssemble(_,_,_),60L,true)
			
			def autism(n:String,c:String,m:String):Boolean = {
				var pingers = ""
					if(m.toLowerCase.contains("autism")){
					if(c.equals("#COM_clan")){
						val noping = Array ("&AeonCommander","Giebmasse_irc","TheSetoner")
						val chan = this.channels("#COM_clan")
							for (v <- chan.users) {
								if(chan.users.contains(v) && !noping.contains(v))
									pingers = pingers + (" " + v)
							}
								sendAction("#COM_clan",String.format("*SCREEEEEEEEEEEEEEECH* AUTISM RANGERS ASSEMBLE %s",pingers))
								return true
						}
					}
                return false
            }
            this.addFunction("!autism",autism(_,_,_),180L,true)
			
			def forthequeen(n:String,c:String,m:String):Boolean = {
				var pingers = ""
					if(m.toLowerCase.startsWith("!forthequeen")){
						if(c.equals("#GB_clan")){
						val noping = Array ("&AeonCommander","Giebmasse_irc","TheSetoner","AeonCommander")
						val chan = this.channels(c)
							for (v <- chan.users) {
								if(chan.users.contains(v) && !noping.contains(v))
									pingers = pingers + (" " + v)
							}
								sendAction(c,String.format("Oi lads, stop taking the piss, our Queen needs us! %s",pingers))
								return true
						}
					}
                return false
            }
            this.addFunction("!forthequeen",forthequeen(_,_,_),60L,true)
			
			def noobsgather(n:String,c:String,m:String):Boolean = {
				var pingers = ""
					if(m.toLowerCase.startsWith("!noobsgather")){
						if(c.equals("#SNF_clan")){
						val noping = Array ("&UEFCommander","Giebmasse_irc","Giebmasse_irc2","TheSetoner","UEFCommander")
						val chan = this.channels(c)
							for (v <- chan.users) {
								if(chan.users.contains(v) && !noping.contains(v))
									pingers = pingers + (" " + v)
							}
								sendAction(c,String.format("%s has a special mission for you, only the best and highly trained special forces noobs qualify, saddle up and join the journey: %s",n,pingers))
								return true
						}
					}
                return false
            }
            this.addFunction("!noobsgather",noobsgather(_,_,_),60L,true)
			
			def bawtraining(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!bawtraining")){
					if(c.equals("#BAW_clan")){
						val cal = Calendar.getInstance()
						val day = cal.get(Calendar.DAY_OF_WEEK)
						val hour = (cal.get(Calendar.HOUR_OF_DAY))-2 //Finland UTC+2
						val min = cal.get(Calendar.MINUTE)
						//val date = (day, hour, min)
						val trainings = Array(Array(2,20,0),Array(4,21,0),Array(7,13,0))
						val times = new ListBuffer[Long]()
						println (String.format("%sD %sH %sM",day.toString,hour.toString,min.toString))
						for (date <- trainings) {
							val cal2 = Calendar.getInstance()
							if(date(0)>=day){
								cal2.add(Calendar.DATE,(date(0)-day))
							} else {
								cal2.add(Calendar.DATE,(7-day)+date(0)) 
							}
							cal2.set(Calendar.HOUR_OF_DAY, date(1))
							cal2.set(Calendar.MINUTE, date(2))
							//
							val day2 = cal2.get(Calendar.DAY_OF_WEEK)
							val hour2 = cal2.get(Calendar.HOUR_OF_DAY)
							val min2 = cal2.get(Calendar.MINUTE)
							println (String.format("%sD %sH %sM",day2.toString,hour2.toString,min2.toString))
							//
							
							val difference = (cal2.getTimeInMillis()-cal.getTimeInMillis())
							println (String.format("DIFF: %s",difference.toString))
							if(difference>=0){
								times += difference
							}
						}
						val nextTraining = times.toList.min
						val seconds = TimeUnit.MILLISECONDS.toSeconds(nextTraining)- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(nextTraining))
						val minutes = TimeUnit.MILLISECONDS.toMinutes(nextTraining)- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(nextTraining))
						val hours = TimeUnit.MILLISECONDS.toHours(nextTraining)
						
						sendLine(c,String.format("Time until next training: %s hours and %s minutes.",hours.toString,minutes.toString))
						return true
					}
					return false
				}
				return false
			}
			this.addFunction("!bawtraining",bawtraining(_,_,_),60L,true)
			
			def time(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!time")){
					val msgparts = m.split(" ", 2);
					var offset = 0
					if (msgparts.length > 1) {
							try {
								offset = msgparts(1).toInt
							 } catch {
								case e: NumberFormatException => {
									offset = 0
									sendLine(c,String.format("Proper command format: !time [offset in hours]"))
								}	  
							}
						}
					val timestamp: Long = (System.currentTimeMillis / 1000)
					val utcstamp: Long = timestamp - 10800 + (offset*3600)
					var filler = " "
					if (offset > 0) {
						filler = " +"
					}
					val epoch = timestamp.toString
					val date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new java.util.Date (utcstamp*1000))
					sendAction(c,String.format("looks at his watch, epoch time (UTC%s%s) seems to be %s, for you humans that means %s",filler,offset.toString,epoch,date))
					return true
				}
				return false
			}
			this.addFunction("!time",time(_,_,_),30L,true)

			def salt(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!salt")){
					sendLine(c,String.format("A brief peek to the blooming industry over at Setons Clutch: %s","https://youtu.be/3KquFZYi6L0"))
					return true
				}
				return false
			}
			this.addFunction("!salt",salt(_,_,_),30L,true)

			def mapstats(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!mapstats")){
					sendLine(c,String.format("Check out these awesome map stats: %s","http://softled.northeurope.cloudapp.azure.com/maps?map=560"))
					return true
				}
				return false
			}
			this.addFunction("!mapstats",mapstats(_,_,_),30L,true)
			
			def gms(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!gms")){
					sendLine(c,String.format("Gentlemen's Setons(TM) rules: https://goo.gl/eEEviG"))
					return true
				}
				return false
			}
			this.addFunction("!gms",gms(_,_,_),30L,true)
			
			def gameTaunt(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!gametaunt")){
						val words = m.split(" ", 2);
						var tauntnumber = 0
						if (words.length > 1) {
							try {
								tauntnumber = words(1).toInt
							 } catch {
								case e: NumberFormatException => {
									tauntnumber = 0
									sendLine(c,String.format("Proper command format: !gametaunt [number between 1-116]"))
									return false
									}	  
								}
								if (tauntnumber > 0 && tauntnumber < 117){
									sendAction(c,String.format("%s",tauntList(tauntnumber-1)))
									return true
								} else {
									sendLine(c,String.format("Only taunts between 1 and 98 are available, try again if you dare."))
									return false
								}

						}
					sendLine(c,String.format("Proper command format: !gametaunt [number between 1-98]"))
					return false
				}
				return false
			}
			this.addFunction("!gameTaunt",gameTaunt(_,_,_),60L,true)
			
			def sexytime(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!sexytime")){
					if(n.equals("Doobers") || n.equals("Giebmasse_irc")){
						val arr = m.split(" ")
						if(arr.length < 2) {
							sendLine(c,String.format("%s teabags everyone in %s",n,c))
							return true
						} else {
							val user = arr(1)
							if(checkStatus(user).equals("online")) {
								val st = Lists.sexytimes(rand.nextInt(Lists.sexytimes.length))
								sendLine(c,String.format(st,n,user))
								return true
							}
								return false
						}
					}
					return false
				}
				return false
			}
            this.addFunction("!sexytime",sexytime(_,_,_),60L,true)

			def fix(n:String,c:String,m:String):Boolean = {
				if(m.toLowerCase.contains("!fix")){
						sendLine(c,String.format("1. Restart FAF client 2. Clear game files 3. Turn your lights on and off"))
						return true
				}
				return false
			}
			this.addFunction("!fix",fix(_,_,_),30L,true)

			def tutorials(n:String,channel:String,m:String):Boolean = {
				var send = false
                if(m.toLowerCase.startsWith("!tutorials")){
                    send = true
                }
				val triggers = Array ("tutorial","i'm new","im new","new player","guide","i am new","first time on","beginner help","noob help","hello world")
				for (trigger <- triggers){
					if(m.toLowerCase.contains(trigger)){
						send = true
					}
				
				}
				if(send){
					sendLine(channel,String.format("Check out these helpful videos and tutorials: %s","https://goo.gl/MTuQtk"))
					return true
				}
                return false
            }
            this.addFunction("!tutorials",tutorials(_,_,_),360L,true)
			
			def getPage(url:String):String = {
				try {
					val connection = new URL(url.toString).openConnection
					connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36")
					val s = (Source.fromInputStream(connection.getInputStream).getLines.mkString.replaceAll("[\n\r]", ""))
					return s
				} catch {
				case _: UnmappableCharacterException => {
						try {
							val connection = new URL(url.toString).openConnection
							connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36")
							val s = (Source.fromInputStream(connection.getInputStream)("UTF-8").getLines.mkString.replaceAll("[\n\r]", ""))
							return s
						} catch {
							case _: MalformedInputException => {
								return url
							}
							case _ : Throwable => { return url }
						}
					}
				}
			}
			
			def urlTitle(n:String,channel:String,m:String):Boolean = {
				if(!m.toLowerCase.startsWith("!")){
					val parts = m.split(" ")
					var foundUrls = 0
					var urls = ""
					// Attempt to convert each item into an URL.
					for(item <- parts){
						if (foundUrls < 5){
							try {
								val url = new URL(item)
								//Don't step on QAI's toes
								if(url.toString.contains("youtube.com") && channel.equals("#aeolus")){
									return true
								}
								val s = getPage(url.toString)
								//val title = "<title>.*</title>".r.findAllIn(s)
								
								var start = s.indexOf("<title>")
								var chars = 7
								if(start.equals(-1)){
									start = s.indexOf("""<title ng-bind="title">""")
									chars = 23
								}
								if(start.equals(-1)){
									start = s.indexOf("""<title id="pageTitle">""")
									chars = 22
								}
								if(start.equals(-1)){
									return true
								}
								val end = s.indexOf("</title>")
								var title = s.slice(start,end).drop(chars)
								if(title.length>400){
									title = title.take(400)
								}
								//sendLine(channel,String.format("%s",title))
								urls += " " + title
								foundUrls += 1
							} catch {
							case e: MalformedURLException => {
									// If there was an URL that was not it!...
								}
							}
						}
					}
					sendLine(channel,String.format("%s",urls))
				}
				return true
			}
			this.addFunction("!urltitle",urlTitle(_,_,_),0L,false)

			def daves(n:String,channel:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!finddave")){
					val chan = this.channels("#aeolus")
					val daves = new HashSet[String]()
					for(user <- chan.users){
						if(user.toLowerCase.startsWith("dave")){
							daves.add(user)
						}
					}
					if(daves.size>0){
						sendLine(channel,String.format("Oh no, It seems Dave is missing!"))
						sendAction(channel,String.format("Hands out missing person flyers: https://puu.sh/w2Kcv/ce221b0c97.jpg"))
						sendAction(channel,String.format("Finds a few Daves: %s",daves.mkString(" ")))
					} else {
						sendAction(channel,String.format("Hands out missing person flyers: https://puu.sh/w2Kcv/ce221b0c97.jpg"))
						sendLine(channel,String.format("Sadly no Daves were found :/"))
					}
					return true
				}
				return false
			}
			this.addFunction("!finddave",daves(_,_,_),60L,true)
			
			def findMods():String = {
				val chan = this.channels("#aeolus")
				chan.mods.clear()
				writeLine(String.format("NAMES #aeolus"))
				Thread.sleep(1000)
				val mods = chan.mods
				var modsstring = ""
				if(mods.size>0){
					val toRemove = "@&~".toSet
					modsstring = mods.mkString(" ").filterNot(toRemove)
				}
				modsstring
			}
			
			def mods(n:String,channel:String,m:String):Boolean = {
				if(m.toLowerCase.startsWith("!mods")){
					val modstring = findMods()
					if(modstring.length>0){
						sendLine(n,String.format("Currently online moderators: %s",modstring))
					} else {
						sendLine(n,String.format("No moderators online currently. If you thought about rioting, remember Gieb has logs of everything!"))
					}
					return true
				}
				return false
			}
			this.addFunction("!mods",mods(_,_,_),20L,true)
			
			def gloves(n:String,channel:String,m:String):Boolean = {
				if(m.toLowerCase.contains("gloves")){
					val chan = this.channels("#aeolus")
						if(chan.users.contains("Farmsletje")){
							sendLine("Farmsletje",String.format("Someone has cold hands and is in need of some gloves, be a good boy and help them https://www.youtube.com/watch?v=L6ODA0nfIIE"))
							sendLine("#Com_clan",String.format("%s has cold hands in %s, please urge Farmsletje to help them out!",n,channel))
							return true
						}
						return false
				}
				return false
			}
			this.addFunction("!gloves",gloves(_,_,_),60L,true)
			
			/////////////
			
		}
        bot.initChannels(channels)
        bot.run()
		
    }
}
