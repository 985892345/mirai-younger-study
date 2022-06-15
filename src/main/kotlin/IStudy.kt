package com.ndhzs

import com.ndhzs.hotfix.handler.suffix.jar.JarEntrance
import net.mamoe.mirai.console.command.CommandSender

interface IStudy : JarEntrance {
  suspend fun CommandSender.listStu(s: String)
  suspend fun CommandSender.informStu(fullName: String? = null)
  suspend fun CommandSender.downloadZip()
  suspend fun CommandSender.addStu(fullName: String, configs: Array<out String>)
  suspend fun CommandSender.removeStu(fullName: String)
  suspend fun CommandSender.setStu(fullName: String, boolean: Boolean)
  suspend fun CommandSender.deleteImg(fullName: String)
}