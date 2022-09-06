package cos

import com.qcloud.cos.COSClient
import com.qcloud.cos.ClientConfig
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.exception.CosClientException
import com.qcloud.cos.exception.CosServiceException
import com.qcloud.cos.model.*
import com.qcloud.cos.region.Region
import secrect.COS_secretId
import secrect.COS_secretKey
import java.io.File
import java.net.URL

/**
 * COS 对象存储的管理类
 *
 * @author 985892345 (Guo Xiangrui)
 * @email 2767465918@qq.com
 * @date 2022/4/18 19:46
 */
object COSManger {
  private val cred = BasicCOSCredentials(COS_secretId, COS_secretKey)
  private val region = Region("ap-chengdu")
  private val clientConfig = ClientConfig(region)
  private val cosClient = COSClient(cred, clientConfig)
  private val bucket = cosClient.listBuckets().find {
    it.name.contains("mirai")
  } ?: throw RuntimeException("不存在这个存储桶，请手动添加！")
  const val path = "younger-study/"
  
  fun upload(file: File): String {
    val key = path + file.name
    val putObjectRequest = PutObjectRequest(bucket.name, key, file)
    cosClient.putObject(putObjectRequest)
    return key
  }
  
  fun list(): ObjectListing? {
    val listObjectsRequest = ListObjectsRequest().apply {
      bucketName = bucket.name
      prefix = path
    }
    var objectListing: ObjectListing
    do {
      try {
        objectListing = cosClient.listObjects(listObjectsRequest)
      } catch (e: CosServiceException) {
        e.printStackTrace()
        return null
      } catch (e: CosClientException) {
        e.printStackTrace()
        return null
      }
      val nextMarker = objectListing.nextMarker
      listObjectsRequest.marker = nextMarker
    } while (objectListing.isTruncated)
    return objectListing
  }
  
  fun download(downFile: File, filePath: String) {
    val getObjectRequest = GetObjectRequest(bucket.name, filePath)
    cosClient.getObject(getObjectRequest, downFile)
  }
  
  fun getTemporaryUrl(filePath: String): URL {
    val generatePresignedUrlRequest = GeneratePresignedUrlRequest(bucket.name, filePath)
    return cosClient.generatePresignedUrl(generatePresignedUrlRequest)
  }
  
  fun isContain(key: String): Boolean {
    return cosClient.doesObjectExist(bucket.name, key)
  }
  
  fun shutDown() {
    cosClient.shutdown()
  }
}