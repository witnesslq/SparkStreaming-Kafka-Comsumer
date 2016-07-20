package com.juanpi.bi.transformer

import java.util.regex.Pattern

import com.alibaba.fastjson.JSON
import com.juanpi.bi.hiveUDF._
import com.juanpi.bi.sc_utils.DateUtils
import com.juanpi.bi.streaming.DateHour
import org.apache.hadoop.hbase.client.{Get, Put, Result}
import play.api.libs.json.{JsValue, Json}
import org.apache.hadoop.hbase.util.Bytes
import com.juanpi.bi.init.InitConfig.initTicksHistory
import com.juanpi.bi.init.InitConfig.{HbaseFamily, dimPages}
/**
  * 解析逻辑的具体实现
  */
class PageinfoTransformer extends ITransformer {

  def parse(row: JsValue): String = {
    // mb_pageinfo
    val ticks = (row \ "ticks").asOpt[String].getOrElse("")
    val session_id = (row \ "session_id").asOpt[String].getOrElse("")
    val pagename = (row \ "pagename").asOpt[String].getOrElse("").toLowerCase()
    val starttime = (row \ "starttime").asOpt[String].getOrElse("0")
    val endtime = (row \ "endtime").asOpt[String].getOrElse(0)
    val pre_page = (row \ "pre_page").asOpt[String].getOrElse("")
    val uid = (row \ "uid").asOpt[String].getOrElse("0")
    val extend_params = (row \ "extend_params").asOpt[String].getOrElse("")
    val app_name = (row \ "app_name").asOpt[String].getOrElse("")
    val app_version = (row \ "app_version").asOpt[String].getOrElse("")
    val os_version = (row \ "os_version").asOpt[String].getOrElse("")
    val os = (row \ "os").asOpt[String].getOrElse("")
    var utm = (row \ "utm").asOpt[String].getOrElse("0")
    val source = (row \ "source").asOpt[String].getOrElse("")
    val starttime_origin = (row \ "starttime_origin").asOpt[String].getOrElse("")
    val endtime_origin = (row \ "endtime_origin").asOpt[String].getOrElse("")
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

    var gid = 0
    var ugroup = 0
    val c_server = (row \ "c_server").asOpt[String].getOrElse("")
    if(!c_server.isEmpty())
    {
        val js_c_server = Json.parse(c_server)
        gid = (js_c_server \ "gid").asOpt[Int].getOrElse(0)
        ugroup = (js_c_server \ "ugroup").asOpt[Int].getOrElse(0)
    }

    // mb_pageinfo -> mb_pageinfo_log
    val extend_params_1 = pageAndEventParser.getExtendParams(pagename, extend_params)
    val pre_extend_params_1 = pageAndEventParser.getExtendParams(pagename, pre_extend_params)

    // TODO GetMbPageId 函数需要更新
    val pageId = GetMbPageId.evaluate(pagename.toLowerCase(), extend_params_1)
    val goodsId = if ((pageId == 158) ||
      (pageId == 159 && (app_version == "3.2.3" || app_version == "3.2.4") && (os.toLowerCase() == "ios"))) {
      extend_params_1
    } else {
      "-1"
    }

    var gu_create_time = ""

    val gu_id = pageAndEventParser.getGuid(jpid, deviceid, os)

    val terminal_id = pageAndEventParser.getTerminalId(os)

    // 查hbase 从 ticks_history 中查找 ticks 存在的记录
    // 查询某条数据
//    val ticks_history = initTicksHistory()
////    val key = new Get(Bytes.toBytes(ticks + app_name))
//    val key = new Get(Bytes.toBytes(gu_id + ":" + utm))
//    val ticks_res = ticks_history.get(key)
//
//    if (!ticks_res.isEmpty) {
////      utm = Bytes.toString(ticks_res.getValue(HbaseFamily.getBytes, "utm".getBytes))
////      gu_create_time
//      gu_create_time = Bytes.toString(ticks_res.getValue(HbaseFamily.getBytes, "starttime".getBytes))
//    }
//    else {
//      // 如果不存在就写入 hbase
//      // 准备插入一条 key 为 id001 的数据
//      val p = new Put((ticks + app_name).getBytes)
//      // 为put操作指定 column 和 value （以前的 put.add 方法被弃用了）
//      p.addColumn(HbaseFamily.getBytes, "utm".getBytes, utm.getBytes)
//      p.addColumn(HbaseFamily.getBytes, "gu_create_time".getBytes, gu_create_time.getBytes)
//      //提交
//      ticks_history.put(p)
//    }

    val logTime = if (starttime.size == 0) {
      0L
    } else {
      starttime.toLong
    }

    val validGoodsId = try {
      if (goodsId.size == 0) {
        "-1"
      } else {
        val goods = goodsId.toInt
        goods.toString
      }
    } catch {
      case ex: NumberFormatException => {
        println("======>> pageinfo解析异常" + ":" + goodsId + ":" + row)
        "-1"
      }
    }

    // gu_id
    val id = if ((uid.length == 0) || uid.equals("0")) {
      gu_id
    } else {
      uid
    }

    val site_id = app_name.toLowerCase match {
      case "jiu" => 2
      case "zhe" => 1
      case _ => -999
    }

    val ref_site_id = site_id

    // session_id 判断
    val sess_id = session_id match
    {
      case ("" | "null" ) => ticks
      case session_id if session_id.length() ==0 => ticks
      case _ => ticks
    }

    // for_pageid 判断
    val for_pageid = forPageId(pagename, extend_params, server_jsonstr)
    val for_pre_pageid = forPageId(pre_page, pre_extend_params, server_jsonstr)
    val p_source = getSource(source)

    val (d_page_id: Int, page_type_id: Int, d_page_value: String, d_page_level_id: Int) = dimPages.get(for_pageid).getOrElse(0, 0, "", 0)
    val page_id = pageAndEventParser.getPageId(d_page_id, extend_params)
    var page_value = pageAndEventParser.getPageValue(d_page_id, extend_params, page_type_id, d_page_value)


    // ref_page_id
    val (d_pre_page_id: Int, d_pre_page_type_id: Int, d_pre_page_value: String, d_pre_page_level_id: Int) = dimPages.get(for_pre_pageid).getOrElse(0, 0, "", 0)
    var ref_page_id = pageAndEventParser.getPageId(d_pre_page_id, pre_extend_params)
    var ref_page_value = pageAndEventParser.getPageValue(d_pre_page_id, pre_extend_params, d_pre_page_type_id, d_pre_page_value)

    val shop_id = getShopId(d_page_id, extend_params)
    val ref_shop_id = getShopId(d_pre_page_id, pre_extend_params)

    val page_level_id = getPageLevelId(d_page_id, extend_params, d_page_level_id)

    // WHEN p1.page_id = 250 THEN getgoodsid(NVL(split(a.extend_params,'_')[2],''))
    val hot_goods_id = if(d_page_id == 250){new GetGoodsId().evaluate(extend_params.split("_")(2))} else ""

    val page_lvl2_value = getPageLvl2Value(d_page_id, extend_params, server_jsonstr)

    val ref_page_lvl2_value = getPageLvl2Value(d_pre_page_id, pre_extend_params, server_jsonstr)

    var pit_type = 0
    var gsort_key = ""
    if(!server_jsonstr.isEmpty())
    {
        val js_server_jsonstr = Json.parse(server_jsonstr)
        pit_type = (js_server_jsonstr \ "_pit_type").asOpt[Int].getOrElse(0)
        gsort_key = (js_server_jsonstr \ "_gsort_key").asOpt[String].getOrElse("")
    }

    val (sortdate, sorthour, lplid, ptplid) = if(!gsort_key.isEmpty) {
        val sortdate = Array(gsort_key.split("_")(3).substring(0, 4),gsort_key.split("_")(3).substring(4, 6),gsort_key.split("_")(3).substring(6, 8)).mkString("-")
        val sorthour = gsort_key.split("_")(4)
        val lplid = gsort_key.split("_")(6)
        val ptplid = gsort_key.split("_")(6)
        (sortdate, sorthour, lplid, ptplid)
      }
    else ("", "", "", "")

    val jpk = 0
    val tab_source = "page"
    // 最终返回值
    val event_id,event_value,rule_id,test_id,select_id,event_lvl2_value,loadTime = ""

    Array(terminal_id,app_version,gu_id,utm,site_id,ref_site_id,uid,session_id,deviceid,page_id,
      page_value,ref_page_id,ref_page_value,page_level_id,page_lvl2_value,ref_page_lvl2_value,jpk,pit_type,sortdate,
      sorthour,lplid,ptplid,gid,ugroup,shop_id,ref_shop_id,starttime,endtime,hot_goods_id,ctag,location,ip,url,urlref,
      to_switch,source,event_id,event_value,rule_id,test_id,select_id,event_lvl2_value,loadTime,gu_create_time,tab_source
//      ,date,hour
    ).mkString("\u0001")

//    Array("2", "2", "4", "5", "7", "8", "10","11","12"
//    ).mkString("\u0001")

  }

  // page level2 value 二级页面值(品牌页：引流款ID等)
  def getPageLvl2Value(x_page_id: Int, x_extend_params: String, server_jsonstr: String): String =
  {
    val page_lel2_value = if(x_page_id == 250) {
      //  WHEN p1.page_id = 250 THEN getgoodsid(NVL(split(a.extend_params,'_')[2],''))
        new GetGoodsId().evaluate(x_extend_params.split("_")(2))
      }
    else if(x_page_id == 154 || x_page_id == 289) {
    //    when P1.page_id in (154,289) and getpageid(a.extend_params) = 10104 then getskcid(a.extend_params)
      val pid = GetPageID.evaluate(x_extend_params)
      if(pid == 10104) {
        new GetSkcId().evaluate(x_extend_params)
      }
      else if(pid == 10102) {
        new GetShopId().evaluate(x_extend_params)
      } else ""
    }
    else if(x_page_id == 169) {
      // -- 'page_temai_orderdetails'
      // WHEN P1.page_id = 169 then get_json_object(a.server_jsonstr,'$.order_status')
      (Json.parse(server_jsonstr) \ "order_status").asOpt[String].getOrElse("")
    }
    else ""
    page_lel2_value
  }

  // page level id
  def getPageLevelId(page_id: Int, extend_params: String, d_page_level_id: Int): Int =
  {
    val page_level_id: Int = if (page_id == 289 || page_id == 154)
    {
      d_page_level_id
    }else if(GetPageID.evaluate(extend_params)== 34 || GetPageID.evaluate(extend_params) == 65) {
      2
    }
    else if( GetPageID.evaluate(extend_params) == 10069){
      3
    }
    else 0
    page_level_id
  }

  def getShopId(x_page_id: Int, extend_params: String): String =
  {
    val shop_id = if(x_page_id == 250)
      new GetGoodsId().evaluate(extend_params.split("_")(1))
    shop_id.toString()
  }

  def forPageId(pagename: String, extend_params: String, server_jsonstr: String): String =
  {

    val for_pageid = pagename.toLowerCase() match
    {
      case a if pagename.toLowerCase() == "page_tab" && isInteger(extend_params) && (extend_params.toInt > 0 && extend_params.toInt < 9999999) => "page_tab"
      case c if pagename.toLowerCase() == "page_tab" && !server_jsonstr.isEmpty() && (Json.parse(server_jsonstr) \ "cid").asOpt[Int].getOrElse(0) < 0 => (pagename+(Json.parse(server_jsonstr) \ "cid").asOpt[String]).toLowerCase()
      case b if pagename.toLowerCase() != "page_tab" => pagename.toLowerCase()
      case _ => (pagename+extend_params).toLowerCase()
    }
    for_pageid
  }

  /*
  * 判断是否为整数
  * @param str 传入的字符串
  * @return 是整数返回true,否则返回false
  */
  def  isInteger(str: String): Boolean =
  {
    val pattern: Pattern = Pattern.compile("^[-\\+]?[\\d]*$")
//    pattern.matcher(str).matches()
    if (!str.isEmpty()) pattern.matcher(str).matches() else false
  }

  def getSource(source: String): String =
  {
    val s = source match {
      case a if a.isEmpty() | a == "null" | !a.contains("push") => "未知"
      case b if b.contains("订单") => "用户个人订单信息推送"
      case c if c.contains("售后", "退货", "退款") => "用户售后信息推送"
      case d if d.contains("你好") => "用户个人消息通知推送"
      case e if e.contains("有货就赶紧抢") => "有货提醒"
      case f if f.contains("收藏的商品") => "用户收藏商品最新消息推送"
      case g if g.contains("订单") => "用户个人订单信息推送"
      case _ => source.substring(6)
    }
    s
  }

  // 返回解析的结果
  def transform(line: String): (String, String) = {

    // fastjson 也可以用。
    // val row = JSON.parseObject(line)

    //play
    val row = Json.parse(line)

    println("======>> row:: " + row)

    if (row != null) {
      // 解析逻辑
      val res = parse(row)

      // for test
//      val res = Array("2", "2", "4", "5", "7", "8", "10","11","12").mkString("\u0001")

      (DateUtils.dateHour((row \ "endtime").as[String].toLong).toString, res)
    } else {
      (DateHour("1970-01-01", "1").toString, line)
    }
  }

}

// for test
object PageinfoTransformer{
  def main(args: Array[String]) {

    val  pp: PageinfoTransformer = new PageinfoTransformer()
    val liuliang =
      """
        |{"app_name":"zhe","app_version":"3.4.7","c_label":"C2","deviceid":"866936020922108","endtime":"1468892508176","endtime_origin":"1468892506863","extend_params":"17","gj_ext_params":"17,http://m.juanpi.com/zhuanti/zyjjgxp?mobile=1&qminkview=1&qmshareview=1,17,17","gj_page_names":"page_tab,page_active,page_tab,page_tab","ip":"115.60.84.175","jpid":"00000000-75d0-04f2-ea1d-6b2a00000030","location":"河南省","os":"android","os_version":"4.4.2","pagename":"page_tab","pre_extend_params":"16314984","pre_page":"page_temai_goods","server_jsonstr":"","session_id":"1468653064917_zhe_1468892173028","source":"","starttime":"1468892453939","starttime_origin":"1468892452626","ticks":"1468653064917","to_switch":"0","uid":"39775699","utm":"101225","wap_pre_url":"","wap_url":""}
        |""".stripMargin

    val line = Json.parse(liuliang.replaceAll("null", """\\"\\""""))
    val p = pp.parse(line)
    println(p)

    println(pp.isInteger(""))

    val row = Json.parse(liuliang)
    println((row \ "uid").asOpt[Int].getOrElse(0))
    var source = "要正怎样,sdfsadfsadfsadfasfdsa"
    println(source.substring(6))

  }
}