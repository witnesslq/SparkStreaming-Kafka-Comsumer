package com.juanpi.bi.transformer

import com.juanpi.bi.hiveUDF.GetPageID
import play.api.libs.json.Json

/**
  * Created by gongzi on 2016/9/28.
  */
object checkEvent {

  def testcid(server_jsonstr: String): Any = {
    if (server_jsonstr.contains("cid")) {
      val js_server_jsonstr = Json.parse(server_jsonstr)
      val cid = (js_server_jsonstr \ "cid").asOpt[String].getOrElse("")
      println(cid)
    }
  }

  def main(args: Array[String]) {

    val line = """{"activityname":"click_cube_goods","app_name":"zhe","app_version":"4.1.1","c_label":"C3","c_server":"{\"gid\":\"C3\",\"ugroup\":\"222\"}","cube_position":"1_1","deviceid":"49A3FDC8-2501-4250-8A06-BAF463A008E6","endtime":"1475052337104","endtime_origin":"1475052335999","extend_params":"","ip":"180.156.82.107","jpid":"ba67668beaf7eb8c00cc854dff02e3490b6a890c","location":"","os":"iOS","os_version":"9.3.2","page_extends_param":"345","pagename":"page_tab","pre_extends_param":"316","pre_page":"page_tab","result":"1","server_jsonstr":"{\"pit_info\":\"goods::21761493::1_1\",\"ab_attr\":\"9\",\"cid\":345,\"_t\":1475052281,\"_gsort_key\":\"DEFAULT_SORT_221_20160928_16_276\",\"_pit_type\":3}","session_id":"1448354623610_zhe_1475052328304","source":"","starttime":"1475052337104","starttime_origin":"1475052335999","ticks":"1448354623610","to_switch":"0","uid":"0","utm":"101431"}"""

//    (for_pageid:page_tab312, ,page_type_id:0, ,page_level_id:0, ,page_value:312, ,f_page_extend_params:312, ,d_page_id:0, ,d_page_value:, ,for_eventid:click_navigationall, ,d_event_id:0, ,event_type_id0, ,event_id:-1, ,event_value:all)
//    page_id=-1, 原始数据为：{"activityname":"click_navigation","app_name":"zhe","app_version":"4.1.0","c_label":"C2","c_server":"{\"gid\":\"C2\",\"ugroup\":\"225_326_331_236\"}","cube_position":"","deviceid":"868486025096837","endtime":"1475909201430","endtime_origin":"1475909201083","extend_params":"all","ip":"111.120.0.173","jpid":"ffffffff-ad86-eaa4-82aa-d66a12b51283","location":"贵州省","os":"android","os_version":"5.1.1","page_extends_param":"312","pagename":"page_tab","pre_extends_param":"","pre_page":"page_center","result":"1","server_jsonstr":"{}","session_id":"1467455392756_zhe_1475909161336","source":"","starttime":"1475909201430","starttime_origin":"1475909201083","ticks":"1467455392756","to_switch":"0","uid":"36937523","utm":"101221"}
    val line7 =
      """{"activityname":"click_cube_block","app_name":"zhe","app_version":"4.1.1","c_label":"C3","c_server":"{\"gid\":\"C3\",\"ugroup\":\"223\"}","cube_position":"3_3","deviceid":"861648033674882","endtime":"1476086816371","endtime_origin":"1476086816177","extend_params":"","ip":"223.9.148.253","jpid":"ffffffff-bce4-c856-ffff-ffffac625205","location":"山西省","os":"android","os_version":"5.1.1","page_extends_param":"1244549_1271871","pagename":"page_home_brand_in","pre_extends_param":"312","pre_page":"page_tab","result":"1","server_jsonstr":"{\"pit_info\":\"ad_id::152::block_id::1395::img_id::3145::3_3\",\"cid\":\"312\",\"_t\":1476084924}","session_id":"1472858003123_zhe_1476086798406","source":"","starttime":"1476086816371","starttime_origin":"1476086816177","ticks":"1472858003123","to_switch":"0","uid":"26286494","utm":"101224"}""".stripMargin

    // base 层
    val row = Json.parse(line7)
    val pagename = (row \ "pagename").asOpt[String].getOrElse("").toLowerCase()
    val page_extends_param = (row \ "page_extends_param").asOpt[String].getOrElse("")
    // f -> fct
    val f_page_extend_params = pageAndEventParser.getExtendParams(pagename, page_extends_param)

    val server_jsonstr = (row \ "server_jsonstr").asOpt[String].getOrElse("")
    val deviceid = (row \ "deviceid").asOpt[String].getOrElse("")
    val cube_position = (row \ "cube_position").asOpt[String].getOrElse("")
    val activityname = (row \ "activityname").asOpt[String].getOrElse("").toLowerCase()
    val extend_params = (row \ "extend_params").asOpt[String].getOrElse("")
    val app_version = (row \ "app_version").asOpt[String].getOrElse("0")
    // t -> tmp
    val t_extend_params = eventParser.getExtendParamsFromBase(activityname, extend_params, app_version)

    // fct 层
    val cid = pageAndEventParser.getJsonValueByKey(server_jsonstr, "cid")

    if(!cid.isEmpty) {
      println(cid.toInt)
    }

    val forEventid = eventParser.getForEventId(cid, activityname, t_extend_params)
    val eventForPageId = eventParser.getForPageId(cid, f_page_extend_params, pagename)
    val f_extend_params = eventParser.getForExtendParams(activityname, t_extend_params, cube_position, server_jsonstr)
    val (pit_type, gsort_key) = pageAndEventParser.getGsortPit(server_jsonstr)

    val d_page_id = 254
//    val pPageId = pageAndEventParser.getPageId(d_page_id, f_page_extend_params)
    val ePageId = pageAndEventParser.getPageId(d_page_id, f_page_extend_params)
    val event_value = eventParser.getEventValue(10, activityname, f_extend_params, server_jsonstr)

    val d_event_id = 433
    val event_id = eventParser.getEventId(d_event_id, app_version) + ""
//    val event_id = eventParser.getEventId(d_event_id, app_version) + ""

    val forLevelId = if(d_page_id == 254 && f_page_extend_params.nonEmpty) {
        433
    } else 0

    val flag = eventParser.filterOutlierPageId(pagename, cid, f_page_extend_params)

    println(flag)

    println("pagename:" + pagename, " page_extends_param:" + page_extends_param, " f_page_extend_params:" + f_page_extend_params, " f_extend_params:" + f_extend_params)
    println("eventParser.getForPageId:" + eventForPageId)
    println("server_jsonstr:" + server_jsonstr, "deviceid:" + deviceid)
    println("cid:" + cid, " activityname:" + activityname, " t_extend_params:" + t_extend_params,
      " cube_position:" + cube_position, " extend_params:" + extend_params, " app_version:" + app_version , " ,forEventid:" + forEventid, " event_id:" + event_id)
    println(" ePageId:" + ePageId)
    println("event_value:" + event_value)
  }
}
