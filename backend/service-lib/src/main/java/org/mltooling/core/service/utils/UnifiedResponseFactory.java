package org.mltooling.core.service.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.mltooling.core.api.format.ErrorMessageFormat;
import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.utils.LogUtils;
import org.mltooling.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifiedResponseFactory {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(UnifiedResponseFactory.class);

  private static final String CONTENT_TYPE_CHARSET = "; charset=utf-8";

  // ================ Public Methods ====================================== //
  public static Response getResponse(UnifiedFormat unifiedFormat) {
    return getResponse(unifiedFormat, new ResponseOptions());
  }

  public static Response getResponse(UnifiedFormat unifiedFormat, ResponseOptions responseOptions) {
    if (unifiedFormat == null) {
      return getEmptyResponse();
    }

    String responseBody = unifiedFormat.toJson();

    if (StringUtils.isNullOrEmpty(responseBody)) {
      return getEmptyResponse();
    }

    return buildResponse(
        unifiedFormat.getStatus(), responseBody, ApiUtils.CONTENT_TYPE_JSON, responseOptions);
  }

  public static Response getResponse(String response, String responseType) {
    if (StringUtils.isNullOrEmpty(response)) {
      return getEmptyResponse();
    }

    return buildResponse(Response.Status.OK.getStatusCode(), response, responseType);
  }

  public static Response getResponse(String response) {
    return getResponse(response, ApiUtils.CONTENT_TYPE_JSON);
  }

  public static Response getErrorResponse(Response.Status status, String message) {
    String responseMessage = "";
    if (message != null) {
      responseMessage = message;
    }

    return buildResponse(
        status.getStatusCode(),
        new ErrorMessageFormat(status.getStatusCode(), responseMessage).toJson(),
        ApiUtils.CONTENT_TYPE_JSON);
  }

  public static Response getErrorResponse(String message) {
    return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
  }

  public static Response getErrorResponse(Exception ex) {
    String responseMessage = LogUtils.getStackTrace(ex);
    return getErrorResponse(responseMessage);
  }

  public static Response getFileDownloadResponse(
      InputStream in, ContentDisposition contentDisposition) {
    return Response.ok(
            new StreamingOutput() {

              public void write(OutputStream output) throws IOException {

                byte[] buf = new byte[16384];
                try {
                  int len = in.read(buf);
                  while (len != -1) {
                    output.write(buf, 0, len);
                    len = in.read(buf);
                  }
                } catch (Exception e) {
                  log.error("Error while streaming file for download", e);
                } finally {
                  // Minio Documentation for getObject states that the stream must be closed
                  in.close();
                }
              }
            })
        .header("Content-Disposition", contentDisposition)
        .build();
  }

  private Response.ResponseBuilder getCookieResponse(
      UnifiedFormat unifiedFormat, NewCookie cookie) {
    Response.ResponseBuilder responseBuilder = Response.fromResponse(getResponse(unifiedFormat));
    return responseBuilder.cookie(cookie);
  }

  @Deprecated
  public static Response getItemCreatedResponse() {
    return buildResponse(Response.Status.CREATED.getStatusCode());
  }

  public static Response getEmptyResponse() {
    return buildResponse(Response.Status.NO_CONTENT.getStatusCode());
  }

  public static Response getPlainResponse(String text) {
    if (StringUtils.isNullOrEmpty(text)) {
      return getEmptyResponse();
    }

    return buildResponse(Response.Status.OK.getStatusCode(), text, ApiUtils.CONTENT_TYPE_PLAIN);
  }

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Private Methods ===================================== //
  private static byte[] compress(String text) {
    ByteArrayOutputStream gzipByteStream = new ByteArrayOutputStream();
    try {
      GZIPOutputStream gzip = new GZIPOutputStream(gzipByteStream);
      gzip.write(text.getBytes(Charset.forName("UTF-8")));
      gzip.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return gzipByteStream.toByteArray();
  }

  private static Response buildResponse(int status) {
    return Response.status(status).build();
  }

  private static Response buildResponse(int status, String text, String responseType) {
    return buildResponse(status, text, responseType, new ResponseOptions());
  }

  private static Response buildResponse(
      int status, String responseBody, String responseType, ResponseOptions responseOptions) {
    if (StringUtils.isNullOrEmpty(responseBody)) {
      return getEmptyResponse();
    }

    Response.ResponseBuilder responseBuilder =
        Response.status(status)
            .header(ApiUtils.CONTENT_TYPE_HEADER, responseType + CONTENT_TYPE_CHARSET);

    byte[] textBytes = responseBody.getBytes(Charset.forName("UTF-8"));
    if (responseOptions.gzipCompressed) {
      textBytes = compress(responseBody);
      responseBuilder.header(HttpHeaders.CONTENT_ENCODING, "gzip");
    }

    responseBuilder.entity(textBytes);

    return responseBuilder.build();
  }

  // ================ Getter & Setter ===================================== //
  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //

  public static class ResponseOptions {

    private boolean gzipCompressed = false;

    public ResponseOptions() {}

    public boolean isGzipCompressed() {
      return isGzipCompressed();
    }

    public ResponseOptions withGzipCompressed(boolean gzipCompressed) {
      this.gzipCompressed = gzipCompressed;
      return this;
    }
  }
}
