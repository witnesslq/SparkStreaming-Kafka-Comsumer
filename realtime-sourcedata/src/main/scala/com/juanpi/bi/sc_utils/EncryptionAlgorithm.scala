package com.juanpi.bi.sc_utils

/**
  * Created by gongzi on 2017/1/14.
  */
object EncryptionAlgorithm {

  /**
    * md5 加密
    * @param text
    * @return
    */
  def md5Hash(text: String) : String = {
    java.security.MessageDigest.getInstance("MD5").digest(text.getBytes()).map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  }
}
