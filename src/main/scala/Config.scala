package fafbot

import java.io.{File, FileInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * Bot settings. They use the same names as FAF's QAI bot and are read from environment
 * variables first, then from an optional setoner.conf file (KEY=value lines, same names).
 *
 *   IRC_SERVER    where to connect        default wss://chat.faforever.com
 *   IRC_USER      nick = IRC account      default TheSetoner
 *   IRC_PASS      the account's password  required. FAF fixed-user passwords are sent as
 *                                          "static:<password>"; that prefix is added if missing.
 *   IRC_CHANNELS  channels to join        default aeolus  (comma separated, # optional)
 *   IRC_DEBUG     true = log every line the server sends
 */
case class BotConfig(
  nick: String,
  saslUser: String,
  saslPassword: String,
  server: String,
  channels: Array[String],
  debug: Boolean = false
)

object BotConfig {
  def load(path: String = "setoner.conf"): BotConfig = {
    val props = new Properties()
    val f = new File(path)
    if (f.exists()) {
      val in = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)
      try props.load(in) finally in.close()
    }

    def get(key: String): Option[String] =
      sys.env.get(key).orElse(Option(props.getProperty(key))).map(_.trim).filter(_.nonEmpty)

    val nick = get("IRC_USER").getOrElse("TheSetoner")
    val pass = get("IRC_PASS").getOrElse {
      throw new IllegalStateException(s"IRC_PASS is not set (environment variable, or a line in $path)")
    }
    BotConfig(
      nick = nick,
      saslUser = nick,
      saslPassword = if (pass.startsWith("static:") || pass.startsWith("token:")) pass else "static:" + pass,
      server = get("IRC_SERVER").getOrElse("wss://chat.faforever.com"),
      channels = get("IRC_CHANNELS").getOrElse("aeolus")
        .split("[,\\s]+").filter(_.nonEmpty).map(c => if (c.startsWith("#")) c else "#" + c),
      debug = get("IRC_DEBUG").exists(_.equalsIgnoreCase("true"))
    )
  }
}
