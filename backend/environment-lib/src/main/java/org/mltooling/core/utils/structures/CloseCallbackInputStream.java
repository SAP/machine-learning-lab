package org.mltooling.core.utils.structures;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CloseCallbackInputStream extends FilterInputStream {

  public interface CloseCallback {

    void close();
  }

  CloseCallback closeCallback;

  public CloseCallbackInputStream(InputStream in, CloseCallback closeCallback) {
    super(in);
    this.closeCallback = closeCallback;
  }

  @Override
  public int read() throws IOException {
    return in.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return in.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return in.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return in.skip(n);
  }

  @Override
  public void mark(int readLimit) {
    in.mark(readLimit);
  }

  @Override
  public void reset() throws IOException {
    in.reset();
  }

  @Override
  public void close() throws IOException {
    in.close();

    if (closeCallback != null) {
      closeCallback.close();
    }
  }
}
