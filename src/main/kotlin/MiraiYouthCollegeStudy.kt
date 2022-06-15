package com.ndhzs

import com.ndhzs.hotfix.HotfixKotlinPlugin
import com.ndhzs.hotfix.handler.suffix.jar.JarEntrance
import com.ndhzs.hotfix.handler.suffix.jar.JarHotfixUser
import com.ndhzs.hotfix.handler.suffix.jar.getEntrance
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription

/**
 * 催大家交青年大学习的 mirai 插件
 *
 * 注意：
 * - 请把同学都添加为好友，不然临时聊天很容易被封号
 * - 由于无法直接给好友传文件，所以只能先上传压缩包到 COS 对象存储后给出下载地址
 */
object MiraiYouthCollegeStudy : HotfixKotlinPlugin(
  JvmPluginDescription(
    id = "com.ndhzs.mirai-younger-study",
    name = "催同学交青年大学习",
    version = "1.0",
  ) {
    author("985892345")
  },
  hotfixCommandName = "younger"
) {
  override fun onHotfixEnable() {
    super.onHotfixEnable()
    StudyCommand.register()
  }

  override fun onHotfixDisable() {
    super.onHotfixDisable()
    StudyCommand.unregister()
  }
}

object StudyCommand : CompositeCommand(
  MiraiYouthCollegeStudy, "younger",
  description = "青年大学习指令"
), JarHotfixUser {

  private val connect: IStudy?
    get() = getEntrance(IStudy::class.java)

  private suspend inline fun CommandSender.getStudy(func: IStudy.() -> Unit) {
    val connect = connect
    if (connect != null) {
      func.invoke(connect)
    } else {
      sendMessage("未实现 IStudy 接口，请检查热修是否成功！")
    }
  }
  
  override fun onRemoveEntrance(entrance: JarEntrance): Boolean {
    return true
  }
  
  /**
   * /younger list (t|f|all)
   *
   * 查看已发列表
   */
  @SubCommand
  suspend fun CommandSender.list(s: String) {
    getStudy {
      listStu(s)
    }
  }
  
  /**
   * /younger inform
   *
   * 通知没发截图的发截图
   */
  @Description("通知没发截图的发截图")
  @SubCommand
  suspend fun CommandSender.inform(fullName: String? = null) {
    getStudy {
      informStu(fullName)
    }
  }
  
  /**
   * /younger download
   *
   * 下载
   */
  @Description("下载所有截图")
  @SubCommand
  suspend fun CommandSender.download() {
    getStudy {
      downloadZip()
    }
  }
  
  /**
   * /younger add <fullName> <config>
   *
   * 添加其他人
   */
  @Description("添加其他人")
  @SubCommand
  suspend fun CommandSender.add(fullName: String, vararg configs: String) {
    getStudy {
      addStu(fullName, configs)
    }
  }
  
  /**
   * /younger remove <fullName>
   *
   * 删除某人
   */
  @Description("删除某人")
  @SubCommand
  suspend fun CommandSender.remove(fullName: String) {
    getStudy {
      removeStu(fullName)
    }
  }
  
  /**
   * /younger set <fullName> <boolean>
   *
   * 设置某人是否完成截图
   */
  @Description("设置某人是否完成截图")
  @SubCommand
  suspend fun CommandSender.set(fullName: String, boolean: Boolean) {
    getStudy {
      setStu(fullName, boolean)
    }
  }
  
  /**
   * /younger set <fullName> <boolean>
   *
   * 删除某人图片
   */
  @Description("删除某人图片")
  @SubCommand
  suspend fun CommandSender.delete(fullName: String) {
    getStudy {
      deleteImg(fullName)
    }
  }
}