package com.juanpi.bi.transformer

import com.juanpi.bi.bean.{Event, Page, PageAndEvent, User}
import com.juanpi.bi.sc_utils.DateUtils
import com.juanpi.hive.udf.GetGoodsId
import play.api.libs.json.{JsValue, Json}

import scala.collection.mutable
/**
  * 解析逻辑的具体实现
  */
class PageinfoTransformer {

  // 返回解析的结果
  def logParser(line: String,
                dimPage: mutable.HashMap[String, (Int, Int, String, Int)],
                dimEvent: mutable.HashMap[String, (Int, Int)],
                fCate: mutable.HashMap[String, String]): (String, String, Any) = {

    //play
    val row = Json.parse(line.replaceAll("null", """\\"\\""""))

    if (row != null) {
      // 解析逻辑
      var gu_id = ""
      val ticks = (row \ "ticks").asOpt[String].getOrElse("")
      val jpid = (row \ "jpid").asOpt[String].getOrElse("")
      val deviceId = (row \ "deviceid").asOpt[String].getOrElse("")
      val os = (row \ "os").asOpt[String].getOrElse("")

      val startTime = (row \ "starttime_origin").asOpt[String].getOrElse("")
      val endtime_origin = (row \ "endtime_origin").asOpt[String].getOrElse("")
      val endTime = pageAndEventParser.getEndTime(startTime, endtime_origin)

      try
      {
        gu_id = pageAndEventParser.getGuid(jpid, deviceId, os)
      } catch {
        //使用模式匹配来处理异常
        case ex:Exception => { println(ex.getStackTraceString)}
          println("=======>> Event: getGuid Exception!!" + "\n======>>异常数据:" + row)
      }

      val ret = if(gu_id.nonEmpty) {
        try {
          val res = parse(row, dimPage, fCate)
          val partitionStr = DateUtils.dateGuidPartitions(endTime.toLong, gu_id)
          (partitionStr, "page", res)
        } catch {
          //使用模式匹配来处理异常
          case ex:Exception => {
            println(ex.getStackTraceString)
            ex.printStackTrace()
          }
            println("=======>> Page: parse Exception!!" + "======>>异常数据:" + row)
            ("", "", None)
        }
      } else {
        println("=======>> Page: GU_ID IS NULL!!" + "======>>异常数据:" + row)
        ("", "", None)
      }
      ret
    } else {
      println("=======>> Page: ROW IS NULL!!" + "======>>异常数据:" + row)
      ("", "", None)
    }
  }

  private def parse(row: JsValue,
                    dimpage: mutable.HashMap[String, (Int, Int, String, Int)],
                    fCate: mutable.HashMap[String, String]): (User, PageAndEvent, Page, Event) = {
    // mb_pageinfo
//    val ticks = (row \ "ticks").asOpt[String].getOrElse("")
    val session_id = (row \ "session_id").asOpt[String].getOrElse("")
    val pageName = (row \ "pagename").asOpt[String].getOrElse("").toLowerCase()
//    val startTime = (row \ "starttime").asOpt[String].getOrElse("0")
//    val endTime = (row \ "endtime").asOpt[String].getOrElse("0")
    val prePage = (row \ "pre_page").asOpt[String].getOrElse("")
    val uid = (row \ "uid").asOpt[String].getOrElse("0")
    val extendParams = (row \ "extend_params").asOpt[String].getOrElse("")
    val appName = (row \ "app_name").asOpt[String].getOrElse("")
    val appVersion = (row \ "app_version").asOpt[String].getOrElse("")
//    val os_version = (row \ "os_version").asOpt[String].getOrElse("")
    val os = (row \ "os").asOpt[String].getOrElse("")
    val utm = (row \ "utm").asOpt[String].getOrElse("0")
    val source = (row \ "source").asOpt[String].getOrElse("")
//    修正过的时间
    val startTime = (row \ "starttime_origin").asOpt[String].getOrElse("")
    val endtime_origin = (row \ "endtime_origin").asOpt[String].getOrElse("")

    val endTime = if (endtime_origin.isEmpty) {
      startTime
    } else {
      endtime_origin
    }

    val pre_extend_params = (row \ "pre_extend_params").asOpt[String].getOrElse("")
    val url = (row \ "wap_url").asOpt[String].getOrElse("")
    val urlref = (row \ "wap_pre_url").asOpt[String].getOrElse("")
    val deviceid = (row \ "deviceid").asOpt[String].getOrElse("")
    val jpid = (row \ "jpid").asOpt[String].getOrElse("")
    val ip = (row \ "ip").asOpt[String].getOrElse("")
    val to_switch = (row \ "to_switch").asOpt[String].getOrElse("0")
    val location = (row \ "location").asOpt[String].getOrElse("")
    val ctag = (row \ "c_label").asOpt[String].getOrElse("")
    val server_jsonstr = (row \ "server_jsonstr").asOpt[String].getOrElse("")

    // =========================================== base to base log ===========================================  //
    val site_id = pageAndEventParser.getSiteId(appName)
    val ref_site_id = site_id
    val gu_id = pageAndEventParser.getGuid(jpid, deviceid, os)
    val terminal_id = pageAndEventParser.getTerminalId(os)

    val gu_create_time = ""

    // =========================================== base to dw ===========================================  //
    // 用户画像中定义的
    var gid = ""
    var uGroup = ""

    val c_server = (row \ "c_server").asOpt[String].getOrElse("")
    if(c_server.nonEmpty)
    {
      val js_c_server = Json.parse(c_server)
      gid = (js_c_server \ "gid").asOpt[String].getOrElse("0")
      uGroup = (js_c_server \ "ugroup").asOpt[String].getOrElse("0")
    }

    // mb_pageinfo -> mb_pageinfo_log
    val fct_extendParams = pageAndEventParser.getExtendParams(pageName, extendParams)
    val fct_preExtendParams = pageAndEventParser.getExtendParams(pageName, pre_extend_params)

    // for_pageid 判断
    val forPageId = pageParser.forPageId(pageName, fct_extendParams, server_jsonstr)
    val forPrePageid = pageParser.forPageId(prePage, fct_preExtendParams, server_jsonstr)

    val (d_page_id: Int, page_type_id: Int, d_page_value: String, d_page_level_id: Int) = dimpage.get(forPageId).getOrElse(0, 0, "", 0)
    val page_id = pageAndEventParser.getPageId(d_page_id, url)
    val page_value = pageParser.getPageValue(d_page_id, url, fct_extendParams, page_type_id, d_page_value)

    // ref_page_id
    val (d_pre_page_id: Int, d_pre_page_type_id: Int, d_pre_page_value: String, d_pre_page_level_id: Int) = dimpage.get(forPrePageid).getOrElse(0, 0, "", 0)
    val ref_page_id = pageAndEventParser.getPageId(d_pre_page_id, urlref)
    val ref_page_value = pageParser.getPageValue(d_pre_page_id, fct_preExtendParams, urlref, d_pre_page_type_id, d_pre_page_value)

    val parsed_source = pageAndEventParser.getSource(source)
    val shop_id = pageAndEventParser.getShopId(d_page_id, fct_extendParams)
    val ref_shop_id = pageAndEventParser.getShopId(d_pre_page_id, fct_preExtendParams)

    val forLevelId = if(d_page_id == 254 && fct_extendParams.nonEmpty){fCate.get(fct_extendParams).getOrElse("0")} else "0"

    val page_level_id = pageAndEventParser.getPageLevelId(d_page_id, fct_extendParams, d_page_level_id, forLevelId)

    // WHEN p1.page_id = 250 THEN getgoodsid(NVL(split(a.extendParams1,'_')[2],''))
    val hot_goods_id = if(d_page_id == 250 && fct_extendParams.nonEmpty && fct_extendParams.contains("_") && fct_extendParams.split("_").length > 2)
    {
      new GetGoodsId().evaluate(fct_extendParams.split("_")(2))
    }
    else {
      ""
    }

    val page_lvl2_value = pageParser.getPageLvl2Value(d_page_id, fct_extendParams, server_jsonstr, url)

    val ref_page_lvl2_value = pageParser.getPageLvl2Value(d_pre_page_id, fct_preExtendParams, server_jsonstr, urlref)

    val pit_type = 0
    val (sortdate, sorthour, lplid, ptplid) = ("", "", "", "")

    val jpk = 0
    val table_source = "mb_page"
    // 最终返回值
    val event_id, event_value, rule_id, test_id, select_id, event_lvl2_value, loadTime, ug_id = ""

    val (date, hour) = DateUtils.dateHourStr(endTime.toLong)

    val user = User.apply(gu_id, uid, utm, gu_create_time, session_id, terminal_id, appVersion, site_id, ref_site_id, ctag, location, jpk, uGroup, date, hour)
    val pe = PageAndEvent.apply(page_id, page_value, ref_page_id, ref_page_value, shop_id, ref_shop_id, page_level_id, startTime, endTime, hot_goods_id, page_lvl2_value, ref_page_lvl2_value, pit_type, sortdate, sorthour, lplid, ptplid, gid, table_source)
    val page = Page.apply(parsed_source, ip, url, urlref, deviceid, to_switch)
    val event = Event.apply(event_id, event_value, event_lvl2_value, rule_id, test_id, select_id, loadTime, ug_id)

    if (-1 == page_id) {
      println("for_pageid:" + forPageId, " ,page_type_id:" + page_type_id, " ,page_level_id:" + page_level_id,
        " ,pageName:" + pageName, " ,fct_extendParams:" + fct_extendParams,
        " ,page_value:" + page_value, " ,fct_extendParams:" + fct_extendParams,
        " ,d_page_id:" + d_page_id, " ,d_page_value:" + d_page_value)
      println("page_id=-1===>原始数据为：" + row)
    }

    (user, pe, page, event)
  }
}

// for test
object PageinfoTransformer{

  def main(args: Array[String]) {

    val pp = new PageinfoTransformer
    val liuliang =
      """
        |{"app_name":"zhe","app_version":"3.4.6","c_label":"C3","c_server":"{\"gid\":\"C3\",\"ugroup\":\"143_223_112_142\"}","deviceid":"867568022962029","endtime":"1468929132822","endtime_origin":"1468929131796","extend_params":"1","gj_ext_params":"past_zhe,1580540_1500762_13504152,past_zhe,crazy_zhe","gj_page_names":"page_tab,page_home_brand_in,page_tab,page_tab","ip":"119.109.179.179","jpid":"ffffffff-bc21-7da8-ffff-ffffe4de7969","location":"辽宁省","os":"android","os_version":"4.4.4","pagename":"page_tab","pre_extend_params":"past_zhe","pre_page":"page_tab","server_jsonstr":"{\"ab_info\":{\"rule_id\":\"\",\"test_id\":\"\",\"select\":\"\"},\"ab_attr\":\"7\"}","session_id":"1468155168409_zhe_1468929047609","source":"","starttime":"1468929130209","starttime_origin":"1468929129183","ticks":"1468155168409","to_switch":"0","uid":"40102432","utm":"104954","wap_pre_url":"","wap_url":""}
        |""".stripMargin


    val pl = liuliang.replaceAll("null", """\\"\\"""").replaceAll("\\\\n\\s+", "\\\\")
//    println(pl)

    try{
      val line = Json.parse(pl)
    }catch{
      //使用模式匹配来处理异常
      case ex:IllegalArgumentException=>println(ex.getMessage())
      case ex:RuntimeException=>{ println("ok")}
      case ex:StringIndexOutOfBoundsException=>println("Invalid Index")
      case ex:Exception => println(ex.getStackTraceString, "\n======>>异常数据:" + pl)
    }
  }
}