plugins {
  val kotlinVersion = "1.6.21"
  kotlin("jvm") version kotlinVersion
  kotlin("plugin.serialization") version kotlinVersion
  
  id("net.mamoe.mirai-console") version "2.11.1"
}

group = "com.ndhzs"
version = "1.0"

repositories {
  maven("https://maven.aliyun.com/repository/public")
  maven("https://jitpack.io")
  mavenCentral()
}

//////////////////////////////////////////////////////////////////////
//       这里是固定代码的起始位置，请直接复制到结尾
//////////////////////////////////////////////////////////////////////
/**
 * 该类主要用于给单个源集设置依赖
 */
class HotfixDependencyHandlerScope(
  val sourceSetName: String,
  val dependencies: DependencyHandler
) {
  fun implementation(
    dependencyNotation: Any
  ) = dependencies.add("${sourceSetName}Implementation", dependencyNotation)
  fun implementation(
    dependencyNotation: String,
    dependencyConfiguration: ExternalModuleDependency.() -> Unit
  ) = dependencies.add("${sourceSetName}Implementation", dependencyNotation, dependencyConfiguration)
  fun compileOnly(
    dependencyNotation: Any
  ) = dependencies.add("${sourceSetName}CompileOnly", dependencyNotation)
  fun compileOnly(
    dependencyNotation: String,
    dependencyConfiguration: ExternalModuleDependency.() -> Unit
  ) = dependencies.add("${sourceSetName}CompileOnly", dependencyNotation, dependencyConfiguration)
}

sourceSets {
  main {
    compileClasspath += fileTree(File(projectDir, "src/main/module/java"))
    runtimeClasspath += fileTree(File(projectDir, "src/main/module/java"))
  }
}

/**
 * 创建自定义源集并引入依赖
 * @param sourceSetName 源集名字
 * @param depend 源集依赖，只会给当前添加
 */
fun createHotfix(sourceSetName: String, depend: (HotfixDependencyHandlerScope.() -> Unit)? = null) {
  // 这里会专门生成打热修代码的 sourceSets 文件夹
  sourceSets {
    create(sourceSetName) {
      // 依赖 main 的 output
      compileClasspath += sourceSets.named("main").get().output
      // 依赖 main 的 compileClasspath
      compileClasspath += sourceSets.named("main").get().compileClasspath
      java.srcDirs.forEach { file ->
        file.mkdirs()
        File(file.parentFile, "kotlin").mkdirs()
      }
      resources.srcDirs.forEach { file -> file.mkdirs() }
    }
  }
  /**
   * 给 gradle 新增打热修包的任务
   * 位置在 idea  gradle 侧边栏 Tasks/hotfix/xxx 中
   * 打好的包位置在 build/libs 下
   */
  tasks.register<Jar>(sourceSetName) {
    group = "hotfix"
    exclude("META-INF/**")
    archiveFileName.set("$sourceSetName.jar")
    from(sourceSets.named(sourceSetName).get().output)
    // 增加 runtimeClasspath
    from(sourceSets.named(sourceSetName).get().runtimeClasspath.filter { file ->
      // 去掉与 main 中相同的 runtimeClasspath
      !sourceSets.named("main").get().runtimeClasspath.contains(file) && file.name.endsWith(".jar")
    }.map { file -> zipTree(file) }) {
      this.exclude("module-info.class")
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
  }
  // 设置单个源集的依赖
  dependencies {
    depend?.invoke(HotfixDependencyHandlerScope(sourceSetName, dependencies))
  }
}
//////////////////////////////////////////////////////////////////////
//           这里是固定代码的结尾位置，以上内容请直接复制
//////////////////////////////////////////////////////////////////////




// 这里给全部源集设置依赖
dependencies {
  implementation("com.github.985892345:mirai-hotfix:0.1.3")
}

// 这里用于设置自定义源集并引入依赖
createHotfix("hotfix-study") {
  implementation("com.google.code.gson:gson:2.9.0")
  implementation("com.qcloud:cos_api:5.6.73")
}
