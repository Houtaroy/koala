package cn.koala.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.integration.mqtt.core.ConsumerStopAction;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoComponent;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.AbstractMqttMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 * @author shihongjun
 * mqtt消息驱动渠道适配器
 */
@SuppressWarnings("PMD")
public class MqttAdapter extends AbstractMqttMessageDrivenChannelAdapter
  implements MqttCallback, MqttPahoComponent, ApplicationEventPublisherAware {
  /**
   * The default completion timeout in milliseconds.
   */
  public static final long DEFAULT_COMPLETION_TIMEOUT = 30_000L;

  /**
   * The default disconnect completion timeout in milliseconds.
   */
  public static final long DISCONNECT_COMPLETION_TIMEOUT = 5_000L;

  private static final int DEFAULT_RECOVERY_INTERVAL = 10_000;

  private final MqttPahoClientFactory clientFactory;

  private int recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

  private long completionTimeout = DEFAULT_COMPLETION_TIMEOUT;

  private long disconnectCompletionTimeout = DISCONNECT_COMPLETION_TIMEOUT;

  private boolean manualAcks;

  private ApplicationEventPublisher applicationEventPublisher;

  private volatile IMqttClient client;

  private volatile ScheduledFuture<?> reconnectFuture;

  private volatile boolean connected;

  private volatile boolean cleanSession;

  private volatile ConsumerStopAction consumerStopAction;

  /**
   * Use this constructor for a single url (although it may be overridden if the server
   * URI(s) are provided by the {@link MqttConnectOptions#getServerURIs()} provided by
   * the {@link MqttPahoClientFactory}).
   *
   * @param url           the URL.
   * @param clientId      The client id.
   * @param clientFactory The client factory.
   * @param topic         The topic(s).
   */
  public MqttAdapter(String url, String clientId, MqttPahoClientFactory clientFactory,
                     String... topic) {

    super(url, clientId, topic);
    this.clientFactory = clientFactory;
  }

  /**
   * Use this constructor if the server URI(s) are provided by the
   * {@link MqttConnectOptions#getServerURIs()} provided by the
   * {@link MqttPahoClientFactory}.
   *
   * @param clientId      The client id.
   * @param clientFactory The client factory.
   * @param topic         The topic(s).
   * @since 4.1
   */
  public MqttAdapter(String clientId, MqttPahoClientFactory clientFactory,
                     String... topic) {

    super(null, clientId, topic);
    this.clientFactory = clientFactory;
  }


  /**
   * Use this constructor when you don't need additional {@link MqttConnectOptions}.
   *
   * @param url      The URL.
   * @param clientId The client id.
   * @param topic    The topic(s).
   */
  public MqttAdapter(String url, String clientId, String... topic) {
    this(url, clientId, new DefaultMqttPahoClientFactory(), topic);
  }

  /**
   * Set the completion timeout for operations. Not settable using the namespace.
   * Default {@value #DEFAULT_COMPLETION_TIMEOUT} milliseconds.
   *
   * @param completionTimeout The timeout.
   * @since 4.1
   */
  @Override
  public synchronized void setCompletionTimeout(long completionTimeout) {
    this.completionTimeout = completionTimeout;
  }

  /**
   * Set the completion timeout when disconnecting. Not settable using the namespace.
   * Default {@value #DISCONNECT_COMPLETION_TIMEOUT} milliseconds.
   *
   * @param completionTimeout The timeout.
   * @since 5.1.10
   */
  public synchronized void setDisconnectCompletionTimeout(long completionTimeout) {
    this.disconnectCompletionTimeout = completionTimeout;
  }

  /**
   * The time (ms) to wait between reconnection attempts.
   * Default {@value #DEFAULT_RECOVERY_INTERVAL}.
   *
   * @param recoveryInterval the interval.
   * @since 4.2.2
   */
  public synchronized void setRecoveryInterval(int recoveryInterval) {
    this.recoveryInterval = recoveryInterval;
  }

  /**
   * Set the acknowledgment mode to manual.
   *
   * @param manualAcks true for manual acks.
   * @since 5.3
   */
  @Override
  public void setManualAcks(boolean manualAcks) {
    this.manualAcks = manualAcks;
  }

  /**
   * @since 4.2.2
   */
  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    // NOSONAR (inconsistent synchronization)
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public MqttConnectOptions getConnectionInfo() {
    MqttConnectOptions options = this.clientFactory.getConnectionOptions();
    if (options.getServerURIs() == null) {
      String url = getUrl();
      if (url != null) {
        options = MqttUtils.cloneConnectOptions(options);
        options.setServerURIs(new String[]{url});
      }
    }
    return options;
  }

  @Override
  protected void doStart() {
    Assert.state(getTaskScheduler() != null, "A 'taskScheduler' is required");
    try {
      connectAndSubscribe();
    } catch (Exception ex) {
      logger.error(ex, "Exception while connecting and subscribing, retrying");
      scheduleReconnect();
    }
  }

  @Override
  protected synchronized void doStop() {
    cancelReconnect();
    if (this.client != null) {
      try {
        if (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_ALWAYS)
          || (this.consumerStopAction.equals(ConsumerStopAction.UNSUBSCRIBE_CLEAN)
          && this.cleanSession)) {

          this.client.unsubscribe(getTopic());
        }
      } catch (MqttException ex) {
        logger.error(ex, "Exception while unsubscribing");
      }
      try {
        this.client.disconnectForcibly(this.disconnectCompletionTimeout);
      } catch (MqttException ex) {
        logger.error(ex, "Exception while disconnecting");
      }

      this.client.setCallback(null);

      try {
        this.client.close();
      } catch (MqttException ex) {
        logger.error(ex, "Exception while closing");
      }
      this.connected = false;
      this.client = null;
    }
  }

  @Override
  public void addTopic(String topic, int qos) {
    this.topicLock.lock();
    try {
      super.addTopic(topic, qos);
      if (this.client != null && this.client.isConnected()) {
        this.client.subscribe(topic, qos);
      }
    } catch (MqttException e) {
      super.removeTopic(topic);
      throw new MessagingException("Failed to subscribe to topic " + topic, e);
    } finally {
      this.topicLock.unlock();
    }
  }

  /**
   * 添加监听主题
   *
   * @param topic           主题
   * @param qos             QOS
   * @param messageListener 消息监听器
   */
  public void addTopic(String topic, int qos, IMqttMessageListener messageListener) {
    this.topicLock.lock();
    try {
      super.addTopic(topic, qos);
      if (this.client != null && this.client.isConnected()) {
        this.client.subscribe(topic, qos, messageListener);
      }
    } catch (MqttException e) {
      super.removeTopic(topic);
      throw new MessagingException("Failed to subscribe to topic " + topic, e);
    } finally {
      this.topicLock.unlock();
    }
  }

  @Override
  public void removeTopic(String... topic) {
    this.topicLock.lock();
    try {
      if (this.client != null && this.client.isConnected()) {
        this.client.unsubscribe(topic);
      }
      super.removeTopic(topic);
    } catch (MqttException e) {
      throw new MessagingException("Failed to unsubscribe from topic(s) " + Arrays.toString(topic), e);
    } finally {
      this.topicLock.unlock();
    }
  }

  private synchronized void connectAndSubscribe() throws MqttException {
    MqttConnectOptions connectionOptions = this.clientFactory.getConnectionOptions();
    this.cleanSession = connectionOptions.isCleanSession();
    this.consumerStopAction = this.clientFactory.getConsumerStopAction();
    if (this.consumerStopAction == null) {
      this.consumerStopAction = ConsumerStopAction.UNSUBSCRIBE_CLEAN;
    }
    Assert.state(getUrl() != null || connectionOptions.getServerURIs() != null,
      "If no 'url' provided, connectionOptions.getServerURIs() must not be null");
    this.client = this.clientFactory.getClientInstance(getUrl(), getClientId());
    this.client.setCallback(this);
    if (this.client instanceof MqttClient) {
      ((MqttClient) this.client).setTimeToWait(this.completionTimeout);
    }
    this.topicLock.lock();
    String[] topics = getTopic();
    try {
      this.client.connect(connectionOptions);
      this.client.setManualAcks(this.manualAcks);
      int[] requestedQos = getQos();
      int[] grantedQos = Arrays.copyOf(requestedQos, requestedQos.length);
      this.client.subscribe(topics, grantedQos);
      warnInvalidQosForSubscription(topics, requestedQos, grantedQos);
    } catch (MqttException ex) {
      if (this.applicationEventPublisher != null) {
        this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, ex));
      }
      logger.error(ex, () -> "Error connecting or subscribing to " + Arrays.toString(topics));
      if (this.client != null) {
        // Could be reset during event handling before
        this.client.disconnectForcibly(this.disconnectCompletionTimeout);
        try {
          this.client.setCallback(null);
          this.client.close();
        } catch (MqttException e1) {
          // NOSONAR
        }
        this.client = null;
      }
      throw ex;
    } finally {
      this.topicLock.unlock();
    }
    if (this.client.isConnected()) {
      this.connected = true;
      String message = "Connected and subscribed to " + Arrays.toString(topics);
      logger.debug(message);
      if (this.applicationEventPublisher != null) {
        this.applicationEventPublisher.publishEvent(new MqttSubscribedEvent(this, message));
      }
    }
  }

  private void warnInvalidQosForSubscription(String[] topics, int[] requestedQos, int[] grantedQos) {
    for (int i = 0; i < requestedQos.length; i++) {
      if (grantedQos[i] != requestedQos[i]) {
        logger.warn(() -> "Granted QOS different to Requested QOS; topics: " + Arrays.toString(topics)
          + " requested: " + Arrays.toString(requestedQos)
          + " granted: " + Arrays.toString(grantedQos));
        break;
      }
    }
  }

  private synchronized void cancelReconnect() {
    if (this.reconnectFuture != null) {
      this.reconnectFuture.cancel(false);
      this.reconnectFuture = null;
    }
  }

  private synchronized void scheduleReconnect() {
    cancelReconnect();
    if (isActive()) {
      try {
        this.reconnectFuture = getTaskScheduler().schedule(() -> {
          try {
            logger.debug("Attempting reconnect");
            synchronized (this) {
              if (!this.connected) {
                connectAndSubscribe();
                this.reconnectFuture = null;
              }
            }
          } catch (MqttException ex) {
            logger.error(ex, "Exception while connecting and subscribing");
            scheduleReconnect();
          }
        }, new Date(System.currentTimeMillis() + this.recoveryInterval));
      } catch (Exception ex) {
        logger.error(ex, "Failed to schedule reconnect");
      }
    }
  }

  @Override
  public synchronized void connectionLost(Throwable cause) {
    if (isRunning()) {
      this.logger.error(() -> "Lost connection: " + cause.getMessage() + "; retrying...");
      this.connected = false;
      if (this.client != null) {
        try {
          this.client.setCallback(null);
          this.client.close();
        } catch (MqttException e) {
          // NOSONAR
        }
      }
      this.client = null;
      scheduleReconnect();
      if (this.applicationEventPublisher != null) {
        this.applicationEventPublisher.publishEvent(new MqttConnectionFailedEvent(this, cause));
      }
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage mqttMessage) {
    AbstractIntegrationMessageBuilder<?> builder = toMessageBuilder(topic, mqttMessage);
    if (builder != null) {
      if (this.manualAcks) {
        builder.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
          new AcknowledgmentImpl(mqttMessage.getId(), mqttMessage.getQos(), this.client));
      }
      Message<?> message = builder.build();
      try {
        sendMessage(message);
      } catch (RuntimeException ex) {
        logger.error(ex, () -> "Unhandled exception for " + message);
        throw ex;
      }
    }
  }

  private AbstractIntegrationMessageBuilder<?> toMessageBuilder(String topic, MqttMessage mqttMessage) {
    AbstractIntegrationMessageBuilder<?> builder = null;
    Exception conversionError = null;
    try {
      builder = getConverter().toMessageBuilder(topic, mqttMessage);
    } catch (Exception ex) {
      conversionError = ex;
    }

    if (builder == null && conversionError == null) {
      conversionError = new IllegalStateException("'MqttMessageConverter' returned 'null'");
    }

    if (conversionError != null) {
      GenericMessage<MqttMessage> message = new GenericMessage<>(mqttMessage);
      if (!sendErrorMessageIfNecessary(message, conversionError)) {
        MessageConversionException conversionException;
        if (conversionError instanceof MessageConversionException) {
          conversionException = (MessageConversionException) conversionError;
        } else {
          conversionException = new MessageConversionException(message, "Failed to convert from MQTT Message",
            conversionError);
        }
        throw conversionException;
      }
    }
    return builder;
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
  }

  /**
   * Used to complete message arrival when {@link #manualAcks} is true.
   *
   * @since 5.3
   */
  private static class AcknowledgmentImpl implements SimpleAcknowledgment {

    private final int id;

    private final int qos;

    private final IMqttClient ackClient;

    /**
     * Construct an instance with the provided properties.
     *
     * @param id     the message id.
     * @param qos    the message QOS.
     * @param client the client.
     */
    AcknowledgmentImpl(int id, int qos, IMqttClient client) {
      this.id = id;
      this.qos = qos;
      this.ackClient = client;
    }

    @Override
    public void acknowledge() {
      if (this.ackClient != null) {
        try {
          this.ackClient.messageArrivedComplete(this.id, this.qos);
        } catch (MqttException e) {
          throw new IllegalStateException(e);
        }
      } else {
        throw new IllegalStateException("Client has changed");
      }
    }

  }
}
