package org.mltooling.core.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DateUtils {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

  static final List<SimpleDateFormat> sdfPatterns =
      new ArrayList<SimpleDateFormat>() {

        {
          add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
          add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'"));
        }
      };
  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  private DateUtils() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static Date parseISO8601Date(String dateStr) {
    if (dateStr != null && !dateStr.equals("")) {
      Date date;
      Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime(dateStr);
      if (cal != null) {
        date = cal.getTime();
        return date;
      }
    }
    return null;
  }

  public static Date parseDate(DateFormat dateFormat, String dateStr) {
    if (dateStr != null && !dateStr.equals("")) {
      Date date;
      try {
        date = dateFormat.parse(dateStr);
        return date;
      } catch (ParseException ex) {
        log.warn("Date (" + dateStr + ") could not be parsed " + ex);
      }
    }
    return null;
  }

  public static Date tryParsing(String dateStr) {
    for (SimpleDateFormat sdf : sdfPatterns) {
      try {
        return sdf.parse(dateStr);
      } catch (ParseException e) {
      }
    }

    return new Date();
  }

  public static String getTimestamp() {
    return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
  }
  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
