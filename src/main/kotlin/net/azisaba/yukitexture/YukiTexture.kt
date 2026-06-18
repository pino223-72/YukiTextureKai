package net.azisaba.yukitexture

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.result.Result
import net.azisaba.yukitexture.command.ReloadTextureCommand
import net.azisaba.yukitexture.command.TextureCommand
import net.azisaba.yukitexture.listener.TextureListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
//import io.papermc.paper.resource.ResourcePackInfo
//import io.papermc.paper.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import org.apache.commons.codec.digest.DigestUtils
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import org.bukkit.ChatColor as CC

class YukiTexture : JavaPlugin() {
    private val prefix = "${CC.GRAY}[${CC.RED}$name${CC.GRAY}]${CC.RESET}"

    lateinit var tex: String
    private var sha1: String? = null
    var jedisBox: JedisBox? = null
    private var config: YamlConfiguration? = null

    fun getTextureConfig(reload: Boolean = false): YamlConfiguration {
        if (reload || config == null) {
            val file = dataFolder.resolve("texture.yml")
            if (!file.isFile) saveResource(file.name, true)
            config = YamlConfiguration.loadConfiguration(file)
            return config!!
        }
        return config!!
    }

    fun reloadTex(sender: CommandSender? = null) {
        val yaml = getTextureConfig(true)
        tex = yaml.getString("url") ?: ""
        if (tex.isNotBlank()) logger.info("リソースパックのURLを $tex に設定しました。")
        sha1 = null
        sender?.sendMessage("$prefix ${CC.GREEN}リソースパックのURLを再読み込みしました。")
    }

    fun applyTex(player: Player) {
        if (tex.isBlank()) return

        if (true || sha1 === null) {
            val (_, response, result) = FuelManager()
                .addRequestInterceptor { next: (Request) -> Request ->
                    { req: Request ->
                        player.sendActionBar(Component.text("${req.url.host} に接続中..."))
                        next(req)
                    }
                }
                .get(tex)
                .responseProgress { readBytes, totalBytes ->
                    val percent = readBytes.toFloat().div(totalBytes).times(100)
                    player.sendActionBar(Component.text("リソースパックをダウンロード中... ($percent %)"))
                }
                .response()

            val joinedHeaders = response.headers.entries.joinToString("\n") {
                "${CC.AQUA}${it.key}: ${CC.RESET}${it.value.joinToString(" ")}"
            }
            player.sendMessage(
                Component.text("$prefix レスポンスは ")
                    .append(
                        Component.text("${response.statusCode} (${response.responseMessage})")
                            .hoverEvent(HoverEvent.showText(Component.text("${CC.YELLOW}URL: ${CC.RESET}${response.url}\n$joinedHeaders")))
                    )
                    .append(Component.text("です。"))
            )
            if (result is Result.Failure) {
                result.getException().printStackTrace()
                return
            }
            sha1 = DigestUtils.sha1Hex(result.get())
        }

        val packInfo = ResourcePackInfo.resourcePackInfo()
            .uri(URI.create(tex))
            .hash(sha1!!)
            .build()

        val request = ResourcePackRequest.resourcePackRequest()
            .packs(packInfo)
            .required(false)
            .prompt(Component.text("リソースパックを適用しています..."))
            .build()

        player.sendResourcePacks(request)

        player.sendMessage(
            Component.text(prefix)
                .append(
                    Component.text("${CC.GREEN}完了しました。")
                        .hoverEvent(HoverEvent.showText(Component.text("SHA-1: $sha1")))
                )
        )
    }

    override fun onEnable() {
        reloadTex()
        val yaml = getTextureConfig()
        val redis = yaml.getConfigurationSection("redis") ?: error("redis section is missing")
        val host = redis.getString("host", "localhost")!!
        val port = redis.getInt("port", 6379)
        val user = redis.getString("user")
        val password = redis.getString("password")
        try {
            logger.info("Trying $host:$port...")
            jedisBox = JedisBox(host, port, user, password)
            jedisBox?.getJedisPool()?.getResource()?.use { it.get("something") }
            logger.info("Redisに接続しました。")
        } catch (e: Exception) {
            logger.warning("Redisに接続できませんでした。データベースなしで続行します。")
            e.printStackTrace()
            jedisBox = null
        }

        getCommand("tex")?.setExecutor(TextureCommand(this))
        getCommand("reloadtex")?.setExecutor(ReloadTextureCommand(this))
        server.pluginManager.registerEvents(TextureListener(this), this)
    }

    override fun onDisable() {
        server.messenger.unregisterOutgoingPluginChannel(this)
        server.messenger.unregisterIncomingPluginChannel(this)
    }
}