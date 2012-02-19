package org.mobicents.servlet.sip.restcomm.dao.mybatis;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.Sid;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumberDao;

@ThreadSafe public final class MybatisIncomingPhoneNumberDao implements IncomingPhoneNumberDao {
  private static final String namespace = "org.mobicents.servlet.sip.restcomm.dao.IncomingPhoneNumberDao.";
  private final SqlSessionFactory sessions;
  
  public MybatisIncomingPhoneNumberDao(final SqlSessionFactory sessions) {
    super();
    this.sessions = sessions;
  }
  
  @Override public void addIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
    final SqlSession session = sessions.openSession();
    try {
      session.insert(namespace + "addIncomingPhoneNumber", toMap(incomingPhoneNumber));
    } finally {
      session.close();
    }
  }

  @Override public IncomingPhoneNumber getIncomingPhoneNumber(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getIncomingPhoneNumber", sid.toString());
      if(result != null) {
        return toIncomingPhoneNumber(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }
  
  @Override public IncomingPhoneNumber getIncomingPhoneNumber(final String phoneNumber) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> result = (Map<String, Object>)session.selectOne(namespace + "getIncomingPhoneNumberByValue", phoneNumber);
      if(result != null) {
        return toIncomingPhoneNumber(result);
      } else {
        return null;
      }
    } finally {
      session.close();
    }
  }

  @Override public List<IncomingPhoneNumber> getIncomingPhoneNumbers(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> results = (List<Map<String, Object>>)session.selectList(namespace + "getIncomingPhoneNumbers", accountSid.toString());
      final List<IncomingPhoneNumber> incomingPhoneNumbers = new ArrayList<IncomingPhoneNumber>();
      if(results != null && !results.isEmpty()) {
        for(final Map<String, Object> result : results) {
          incomingPhoneNumbers.add(toIncomingPhoneNumber(result));
        }
      }
      return incomingPhoneNumbers;
    } finally {
      session.close();
    }
  }

  @Override public void removeIncomingPhoneNumber(final Sid sid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(namespace + "removeIncomingPhoneNumber", sid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void removeIncomingPhoneNumbers(final Sid accountSid) {
    final SqlSession session = sessions.openSession();
    try {
      session.delete(namespace + "removeIncomingPhoneNumbers", accountSid.toString());
    } finally {
      session.close();
    }
  }

  @Override public void updateIncomingPhoneNumber(final IncomingPhoneNumber incomingPhoneNumber) {
    final SqlSession session = sessions.openSession();
    try {
      session.update(namespace + "updateIncomingPhoneNumber", toMap(incomingPhoneNumber));
    } finally {
      session.close();
    }
  }
  
  private IncomingPhoneNumber toIncomingPhoneNumber(final Map<String, Object> map) {
    final Sid sid = new Sid((String)map.get("sid"));
    final DateTime dateCreated = new DateTime((Date)map.get("date_created"));
    final DateTime dateUpdated = new DateTime((Date)map.get("date_updated"));
    final String friendlyName = (String)map.get("friendly_name");
    final Sid accountSid = new Sid((String)map.get("account_sid"));
    final String phoneNumber = (String)map.get("phone_number");
    final String apiVersion = (String)map.get("api_version");
    final Boolean hasVoiceCallerIdLookup = (Boolean)map.get("voice_caller_id_lookup");
    final URI voiceUrl = URI.create((String)map.get("voice_url"));
    final String voiceMethod = (String)map.get("voice_method");
    final URI voiceFallbackUrl = URI.create((String)map.get("voice_fallback_url"));
    final String voiceFallbackMethod = (String)map.get("voice_fallback_method");
    final URI statusCallback = URI.create((String)map.get("status_callback"));
    final String statusCallbackMethod = (String)map.get("status_callback_method");
    final Sid voiceApplicationSid = new Sid((String)map.get("voice_application_sid"));
    final URI smsUrl = URI.create((String)map.get("sms_url"));
    final String smsMethod = (String)map.get("sms_method");
    final URI smsFallbackUrl = URI.create((String)map.get("sms_fallback_url"));
    final String smsFallbackMethod = (String)map.get("sms_fallback_method");
    final Sid smsApplicationSid = new Sid((String)map.get("sms_application_sid"));
    final URI uri = URI.create((String)map.get("uri"));
    return new IncomingPhoneNumber(sid, dateCreated, dateUpdated, friendlyName, accountSid, phoneNumber, apiVersion, hasVoiceCallerIdLookup,
        voiceUrl, voiceMethod, voiceFallbackUrl, voiceFallbackMethod, statusCallback, statusCallbackMethod, voiceApplicationSid,
        smsUrl, smsMethod, smsFallbackUrl, smsFallbackMethod, smsApplicationSid, uri);
  }
  
  private Map<String, Object> toMap(final IncomingPhoneNumber incomingPhoneNumber) {
    final Map<String, Object> map = new HashMap<String, Object>();
    map.put("sid", incomingPhoneNumber.getSid().toString());
    map.put("date_created", incomingPhoneNumber.getDateCreated().toDate());
    map.put("date_updated", incomingPhoneNumber.getDateUpdated().toDate());
    map.put("friendly_name", incomingPhoneNumber.getFriendlyName());
    map.put("account_sid", incomingPhoneNumber.getAccountSid().toString());
    map.put("phone_number", incomingPhoneNumber.getPhoneNumber());
    map.put("api_version", incomingPhoneNumber.getApiVersion());
    map.put("voice_caller_id_lookup", incomingPhoneNumber.hasVoiceCallerIdLookup());
    map.put("voice_url", incomingPhoneNumber.getVoiceUrl().toString());
    map.put("voice_method", incomingPhoneNumber.getVoiceMethod());
    map.put("voice_fallback_url", incomingPhoneNumber.getVoiceFallbackUrl().toString());
    map.put("voice_fallback_method", incomingPhoneNumber.getVoiceFallbackMethod());
    map.put("status_callback", incomingPhoneNumber.getStatusCallback().toString());
    map.put("status_callback_method", incomingPhoneNumber.getStatusCallbackMethod());
    map.put("voice_application_sid", incomingPhoneNumber.getVoiceApplicationSid().toString());
    map.put("sms_url", incomingPhoneNumber.getSmsUrl().toString());
    map.put("sms_method", incomingPhoneNumber.getSmsMethod());
    map.put("sms_fallback_url", incomingPhoneNumber.getSmsFallbackUrl().toString());
    map.put("sms_fallback_method", incomingPhoneNumber.getSmsFallbackMethod());
    map.put("sms_application_sid", incomingPhoneNumber.getSmsApplicationSid().toString());
    map.put("uri", incomingPhoneNumber.getUri().toString());
    return map;
  }
}