package com.juanpi.bi.streaming

/**
 * Created by juanpi on 2015/7/9.
 */
object Config {

  val interval = System.getProperty("spark.mystreaming.batch.interval", "60").toInt

  val dirPrefix = "/user/hadoop/gongzi/"

  val checkpoindDir= dirPrefix + "sparkstreaming/checkpoint"

  // todo 配置 baseDir
  val baseDir = System.getProperty("spark.juanpi.bi.realtime.basedir", dirPrefix + "kafka_realoutput_path_list")

  private val transformerPackage="com.juanpi.bi.transformer"
  val kafkaTopicMap:Map[String, Seq[String]] = Map(
                          "mb_event_hash2" -> List(s"${transformerPackage}.MbEventTransformer"),
                          "mb_pageinfo_hash2" -> List(s"${transformerPackage}.PageinfoTransformer")
                          )

  def getTopicTransformerClass(topic: String): String ={
    kafkaTopicMap.get(topic).get(0)
  }

  def main(args: Array[String]) {

//    println(getTopicTransformerClass("mbevent"))
//    println(getTopicTransformerClass("pageinfo"))
    println(interval)
  }

}
