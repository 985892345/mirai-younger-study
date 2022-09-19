import com.ndhzs.IStudy
import com.ndhzs.MiraiYouthCollegeStudy.reload
import com.ndhzs.MiraiYouthCollegeStudy.save
import cos.COSManger
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.rootDir
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Face
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.toPlainText
import okhttp3.OkHttpClient
import okhttp3.Request
import secrect.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

@Suppress("SuspendFunctionOnCoroutineScope")
class StudyImpl : IStudy {
  
  private val jobList = arrayListOf<Job>()
  
  private fun Job.collect(): Job {
    jobList.add(this)
    return this
  }
  
  private val group: Group?
    get() = Bot.getInstanceOrNull(Bot_id)?.getGroup(Group_id)
  
  private fun getRootFile(): File {
    return MiraiConsole.rootDir
      .resolve("younger-study")
      .resolve(
        SimpleDateFormat("yy-MM-dd").format(Calendar.getInstance().apply {
          val diff = get(Calendar.DAY_OF_WEEK)
          add(Calendar.DAY_OF_WEEK, -diff + 2)
        }.time)
      )
      .apply { mkdirs() }
  }
  
  override fun CommandSender.onFixLoad() {
    Data.reload()
    launch {
      sendMessage("Younger-Study 加载成功")
    }
    launch {
      while (true) {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance().apply {
          add(Calendar.WEEK_OF_YEAR, 1)
          set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
          set(Calendar.HOUR, 23)
        }
        delay(calendar2.timeInMillis - calendar1.timeInMillis)
        Data.stuByIdMap.forEach {
          it.value.isSent = false
        }
      }
    }.collect()
    GlobalEventChannel.filter { event ->
      event is GroupTempMessageEvent
        && event.group.id == Group_id
        && Data.stuByIdMap.any { it.value.id == event.sender.id }
        || event is FriendMessageEvent
        && Data.stuByIdMap.any { it.value.id == event.sender.id }
    }.subscribeAlways<MessageEvent> {
      val fullName = Data.stuByIdMap.filter {
        it.value.id == sender.id
      }.firstNotNullOfOrNull { it.key }
      if (fullName != null) {
        val img = message[Image]
        if (img != null && !img.isEmoji) {
          val url = img.queryUrl()
          val client = OkHttpClient.Builder().build()
          val request: Request = Request.Builder()
            .url(url)
            .build()
          withContext(Dispatchers.IO) {
            runCatching {
              client.newCall(request).execute()
            }.onFailure {
              it.printStackTrace()
              bot.getFriend(Manager_id)?.sendMessage(
                "姓名：${fullName}\n" +
                  "QQ号：${sender.id}\n" +
                  "图片下载失败，请手动下载！"
              )
              sendMessage(img)
            }.onSuccess {
              val byte = it.body?.bytes()
              if (byte != null) {
                Files.write(getRootFile().resolve("${fullName}.jpg").toPath(), byte, StandardOpenOption.CREATE)
                Data.stuByIdMap[fullName]?.isSent = true
                val list = listOf(
                  "感谢支持工作".toPlainText() + Face(Face.敬礼),
                  "感谢大哥发来的截图".toPlainText() + Face(Face.汪汪),
                  Face(Face.玫瑰),
                  Face(Face.比心),
                  Face(Face.拜谢),
                )
                sender.sendMessage(list.random())
              }
            }
          }
        }
      }
    }.collect()
  }
  
  override fun CommandSender.onFixUnload(): Boolean {
    Data.save()
    COSManger.shutDown()
    launch {
      sendMessage("Younger-Study 卸载成功")
    }
    jobList.forEach {
      if (it is CompletableJob) {
        it.complete()
      } else {
        it.cancel()
      }
    }
    return true
  }
  
  override suspend fun CommandSender.listStu(s: String) {
    Data.stuByIdMap.forEach {
      val friend = group!!.getMember(it.value.id)
      if (friend == null) {
        sendMessage("这个QQ(${it.value.id})号找不到对应的人！\n")
      }
    }
    val joiner = StringJoiner("\n")
    when (s) {
      "t", "true" -> {
        val data = Data.stuByIdMap.filter { it.value.isSent }
        joiner.add("已发截图：${data.size}人")
        data.forEach { joiner.add(it.key) }
      }
      "f", "false" -> {
        val data = Data.stuByIdMap.filter { !it.value.isSent }
        joiner.add("未发截图：${data.size}人")
        data.forEach { joiner.add(it.key) }
      }
      "reset" -> {
        // 重新设置本地数据
        val childFile = getRootFile().listFiles()
        if (childFile != null) {
          joiner.add("重置数据成功！")
          joiner.add("发现以下人已提交截图：${childFile.size}人")
          val nameSet = childFile.mapTo(mutableSetOf()) {
            val name = it.name.substringBeforeLast(".")
            joiner.add(name)
            name
          }
          Data.stuByIdMap.forEach {
            it.value.isSent = nameSet.contains(it.key)
          }
        }
      }
      else -> {
        val edList = mutableListOf<String>()
        val noList = mutableListOf<String>()
        Data.stuByIdMap.forEach {
          if (it.value.isSent) {
            edList.add(it.key)
          } else {
            noList.add(it.key)
          }
        }
        joiner.add("已发截图：${edList.size}人")
        edList.forEach { joiner.add(it) }
        joiner.add("未发截图：${noList.size}人")
        noList.forEach { joiner.add(it) }
      }
    }
    sendMessage(joiner.toString())
  }
  
  private var mInformJob: Job? = null
  
  override suspend fun CommandSender.informStu(fullName: String?) {
    if (fullName == "cancel") {
      if (mInformJob != null) {
        mInformJob?.cancel()
        sendMessage("取消通知成功")
      } else {
        sendMessage("通知已发完或者未发送过通知")
      }
      return
    }
    if (mInformJob != null) {
      sendMessage("上一次通知未结束！")
      return
    }
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    if (hour * 60 + minute !in 8 * 60 .. 23 * 60 + 10) {
      sendMessage("该时段不能通知！")
      return
    }
    val informMap = mutableMapOf<String, Member>()
    Data.stuByIdMap.forEach {
      if (!it.value.isSent && it.key.contains(fullName ?: "")) {
        val member = group?.getMember(it.value.id)
        if (member != null) {
          informMap[it.key] = member
        } else {
          sendMessage("找不到: ${it.key}, ${it.value.id}，是否账号填错或机器人账号填错？")
        }
      }
    }
    if (informMap.isEmpty()) {
      if (fullName == null) {
        sendMessage("已收齐，使用 /younger download 进行下载")
      } else {
        sendMessage("该人已交")
      }
    } else {
      mInformJob = launch {
        val list = listOf(
          "发一下青年大学习截图，谢谢配合".toPlainText() + Face(Face.吃糖),
          "是时候交青年大学习了，兄弟".toPlainText() + Face(Face.敬礼),
          "我又来催你交青年大学习了".toPlainText() + Face(Face.小纠结),
          "交青年大学习了吗".toPlainText() + Face(Face.闭嘴),
          Face(Face.暗中观察) + "青年大学习截图"
        )
        informMap.forEach {
          it.value.sendMessage(list.random())
          delay(Random.nextLong(120 * 1000, 360 * 1000))
        }
        mInformJob = null
      }.collect()
      
      val joiner = StringJoiner("\n")
      joiner.add("已提醒以下人发截图：共${informMap.size}人")
      informMap.forEach { joiner.add(it.key) }
      sendMessage(joiner.toString())
    }
  }
  
  override suspend fun CommandSender.downloadZip() {
    val rawFile = getRootFile().listFiles()
    if (rawFile.isNullOrEmpty()) {
      sendMessage("图片为空！")
      return
    }
    val zipFile = getRootFile().parentFile.resolve("${getRootFile().name}.zip")
    withContext(Dispatchers.IO) {
      ZipOutputStream(zipFile.outputStream()).use { zos ->
        rawFile.forEach { file ->
          val zipEntry = ZipEntry(file.name)
          zos.putNextEntry(zipEntry)
          file.inputStream().use { ins ->
            val array = ByteArray(1024)
            var len: Int
            while (ins.read(array).also { len = it } != -1) {
              zos.write(array, 0, len)
            }
          }
        }
      }
      val filePath = COSManger.upload(zipFile)
      sendMessage("共有${rawFile.size}份\nZip下载链接：\n${COSManger.getTemporaryUrl(filePath)}")
    }
  }
  
  override suspend fun CommandSender.addStu(fullName: String, configs: Array<out String>) {
    val group = group
    if (group == null) {
      sendMessage("群号有问题，添加失败！")
      return
    }
    if (!Data.stuByIdMap.containsKey(fullName)) {
      val newConfig = Config.newInstance(configs, group)
      if (newConfig != null) {
        Data.stuByIdMap[fullName] = newConfig
        sendMessage(
          "请进行检查：\n" +
            "姓名：$fullName\n" +
            "QQ号：${newConfig.id}\n" +
            "是否已发截图：${newConfig.isSent}\n" +
            "若有误，请使用 /remove <fullName> 删除！"
        )
      } else {
        sendMessage("存在错误！")
      }
    } else {
      sendMessage("已存在该人，暂不支持相同名字！")
    }
  }
  
  override suspend fun CommandSender.removeStu(fullName: String) {
    if (Data.stuByIdMap.remove(fullName) != null) {
      sendMessage("删除 $fullName 成功")
    } else {
      sendMessage("不存在该人：$fullName")
    }
  }
  
  override suspend fun CommandSender.setStu(fullName: String, boolean: Boolean) {
    if (Data.stuByIdMap.containsKey(fullName)) {
      Data.stuByIdMap[fullName]?.isSent = boolean
      if (boolean) {
        sendMessage("已将${fullName}设置为已发截图")
      } else {
        sendMessage("已将${fullName}设置为未发截图")
      }
    } else {
      sendMessage("不存在该人：$fullName，请输入全名")
    }
  }
  
  override suspend fun CommandSender.deleteImg(fullName: String) {
    var isContain = false
    Data.stuByIdMap.forEach {
      if (it.key == fullName) {
        isContain = true
        val files = getRootFile().listFiles { _, name ->
          name.contains(fullName)
        }
        if (files.isNullOrEmpty()) {
          sendMessage("未找到此人")
        } else {
          val joiner = StringJoiner("\n")
          joiner.add("删除以下人截图：共${files.size}人")
          files.forEach { file ->
            joiner.add(file.name)
          }
          sendMessage(joiner.toString())
          files.forEach { file ->
            file.delete()
          }
        }
      }
    }
    if (!isContain) {
      sendMessage("未找到此人")
    }
  }
  
  object Data : AutoSavePluginData("younger-study") {
    val stuByIdMap: MutableMap<String, Config> by value {
      putAll(StudentMap.mapValues { Config(it.value) })
    }
  }
  
  @kotlinx.serialization.Serializable
  class Config(
    val id: Long,
    var isSent: Boolean = false
  ) {
    companion object {
      fun newInstance(configs: Array<out String>, group: Group): Config? {
        val judge = arrayOf(
          configs.size == 1 || configs.size == 2,
          configs[0].matches(Regex("[0-9]{7,12}")),
          group.contains(configs[0].toLong()),
          configs.getOrNull(1)?.matches(Regex("(true|false|t|f)")) ?: true
        )
        
        fun String?.toBoolean(): Boolean {
          return when (this) {
            null, "true", "t" -> true
            "false", "f" -> false
            else -> true
          }
        }
        if (judge.all { it }) {
          return Config(configs[0].toLong(), configs.getOrNull(1).toBoolean())
        }
        return null
      }
    }
  }
}

