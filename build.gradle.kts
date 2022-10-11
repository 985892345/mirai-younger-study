plugins {
  val kotlinVersion = "1.7.20"
  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  id("net.mamoe.mirai-console") version "2.12.3"
  id("io.github.985892345.mirai-hotfix") version "2.0.0"
}

group = "com.ndhzs"
version = "1.0"

repositories {
  mavenCentral()
  maven("https://maven.aliyun.com/repository/public")
}

// 这里给全部源集设置依赖
dependencies {
  shadowLink("io.github.985892345:mirai-hotfix")
}

hotfix {
  // 这里用于设置自定义源集并引入依赖
  createHotfix("hotfix") {
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("com.qcloud:cos_api:5.6.73")
  }
}

