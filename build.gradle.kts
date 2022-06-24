plugins {
  val kotlinVersion = "1.6.21"
  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  
  id("net.mamoe.mirai-console") version "2.11.1"
  id("io.github.985892345.mirai-hotfix") version "1.0"
}

group = "com.ndhzs"
version = "1.0"

repositories {
  maven("https://maven.aliyun.com/repository/public")
  mavenCentral()
}

// 这里给全部源集设置依赖
dependencies {
  implementation("com.github.985892345:mirai-hotfix:0.1.3")
}

hotfix {
  // 这里用于设置自定义源集并引入依赖
  createHotfix("hotfix-study") {
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.qcloud:cos_api:5.6.73")
  }
}

