package au.com.southsky.jfreesane;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Represents a SANE device within a session. SANE devices are obtained from a {@link SaneSession}.
 *
 * <p>
 * Not thread-safe.
 *
 * @author James Ring (sjr@jdns.org)
 */
public class SaneDevice implements Closeable {
  private final SaneSession session;
  private final String name;
  private final String vendor;
  private final String model;
  private final String type;

  private SaneDeviceHandle handle;

  private Map<String, SaneOption> optionTitleMap = null;

  SaneDevice(SaneSession session, String name, String vendor, String model, String type) {
    this.session = session;
    this.name = name;
    this.vendor = vendor;
    this.model = model;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public String getVendor() {
    return vendor;
  }

  public String getModel() {
    return model;
  }

  public String getType() {
    return type;
  }

  public boolean isOpen() {
    return handle != null;
  }

  public void open() throws IOException {
    Preconditions.checkState(!isOpen(), "device is already open");
    handle = session.openDevice(this);
  }

  /**
   * Acquires a single image from the Sane daemon.
   *
   * @return a {@link BufferedImage} representing the image obtained from Sane
   * @throws IOException
   *           if an error occurred while talking to the backend
   * @throws SaneException
   *           if an application-level error was returned by the Sane daemon
   */
  public BufferedImage acquireImage() throws IOException, SaneException {
    Preconditions.checkState(isOpen(), "device is not open");
    return session.acquireImage(handle);
  }

  @Override
  public void close() throws IOException {
    if (!isOpen()) {
      throw new IOException("device is already closed");
    }

    session.closeDevice(handle);
    handle = null;
  }

  @Override
  public String toString() {
    return "SaneDevice [name=" + name + ", vendor=" + vendor + ", model=" + model + ", type="
        + type + "]";
  }

  public SaneDeviceHandle getHandle() {
    return handle;
  }

  /**
   * Implements SANE_NET_GET_OPTION_DESCRIPTORS
   *
   * @return List of SaneOptions
   * @throws IOException
   */
  public List<SaneOption> listOptions() throws IOException {
    if (optionTitleMap == null) {
      optionTitleMap = Maps.uniqueIndex(SaneOption.optionsFor(this),
          new Function<SaneOption, String>() {
            @Override
            public String apply(SaneOption input) {
              return input.getName();
            }
          });
    }

    // Maps.uniqueIndex guarantees the order of optionTitleMap.values()
    return ImmutableList.copyOf(optionTitleMap.values());
  }

  public SaneOption getOption(String title) throws IOException {
    listOptions();
    return optionTitleMap.get(title);
  }

  public SaneSession getSession() {
    return session;
  }

  /**
   * Informs this device that its options are stale (e.g. when the server tells us we need to reload
   * options after an option was set).
   */
  void invalidateOptions() {
    optionTitleMap = null;
  }
}
