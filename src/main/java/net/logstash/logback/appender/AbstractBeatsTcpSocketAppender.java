/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.logstash.logback.appender;

import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import net.logstash.logback.appender.listener.TcpAppenderListener;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import net.logstash.logback.encoder.StreamingEncoder;
import net.logstash.logback.encoder.converter.ChainedPayloadConverter;
import net.logstash.logback.encoder.converter.LumberjackPayloadConverter;
import net.logstash.logback.encoder.converter.PayloadConverter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.logstash.logback.util.ByteUtil.bytesToInt;
import static net.logstash.logback.util.ByteUtil.intToBytes;

/**
 * An appender that is compatible with Lumberjack protocol (Beats input).
 *
 * See <a href="https://github.com/logstash-plugins/logstash-input-beats/blob/master/PROTOCOL.md">Logstash's documentation</a> for more info.
 */
public abstract class AbstractBeatsTcpSocketAppender<Event extends DeferredProcessingAware, Listener extends TcpAppenderListener<Event>>
        extends AbstractLogstashTcpSocketAppender<Event, Listener> {

    private static final byte PROTOCOL_VERSION = '2';
    private static final byte PAYLOAD_WINDOW_TYPE = 'W';
    private static final byte PAYLOAD_ACK_TYPE = 'A';

    /**
     * Counter for sequence number of published events.
     * Sent to remote Beats input to track how many messages were sent
     * and used to track when to wait for remote to be ready to accept new logs.
     */
    private final AtomicInteger counter = new AtomicInteger();
    /**
     * A queue of currently accepted ACKs from remote
     */
    private final BlockingQueue<AckEvent> ackEvents = new ArrayBlockingQueue<>(10);

    /**
     * Used to tell the reader the maximum number of unacknowledged data frames
     * the writer (this appender) will send before blocking for ACKs.
     */
    private int windowSize;

    /**
     * Callable that reads ACKs from remote reader.
     */
    private AckReaderCallable ackReaderCallable;

    public AbstractBeatsTcpSocketAppender() {
        this.windowSize = 10;
    }

    @Override
    public synchronized void start() {
        Encoder<Event> encoder = getEncoder();
        PayloadConverter converter = installOrCreateConverter(encoder);
        setEncoder(wrapAsBeatsEncoder(encoder, converter));
        super.start();
    }

    /**
     * Setups {@link LumberjackPayloadConverter}.
     * Installs it into an encoder if possible or else creates a new instance.
     * @param encoder an encoder to install the converter in
     * @return a converter if installing was not possible
     */
    private PayloadConverter installOrCreateConverter(Encoder<Event> encoder) {
        if (encoder instanceof CompositeJsonEncoder) {
            installConverter((CompositeJsonEncoder<? extends DeferredProcessingAware>) encoder);
            return null;
        } else {
            return createConverter();
        }
    }

    /**
     * Installs and configures {@link LumberjackPayloadConverter} into {@link CompositeJsonEncoder<E>}.
     * @param encoder a target encoder to install the converter
     * @param <E> log event type
     */
    private <E extends DeferredProcessingAware> void installConverter(CompositeJsonEncoder<E> encoder) {
        PayloadConverter payloadConverter = encoder.getPayloadConverter();
        if (payloadConverter == null) {
            encoder.setPayloadConverter(createConverter());
        } else if (payloadConverter instanceof LumberjackPayloadConverter) {
            LumberjackPayloadConverter lumberjackPayloadConverter = (LumberjackPayloadConverter) payloadConverter;
            lumberjackPayloadConverter.setWindowSize(windowSize);
            lumberjackPayloadConverter.setCounter(counter);
        } else {
            ChainedPayloadConverter chain = new ChainedPayloadConverter();
            chain.add(createConverter());
            chain.add(payloadConverter);
            encoder.setPayloadConverter(chain);
        }
    }

    private LumberjackPayloadConverter createConverter() {
        LumberjackPayloadConverter lumberjackPayloadConverter = new LumberjackPayloadConverter();
        lumberjackPayloadConverter.setWindowSize(windowSize);
        lumberjackPayloadConverter.setCounter(counter);
        return lumberjackPayloadConverter;
    }

    private <E> Encoder<E> wrapAsBeatsEncoder(Encoder<E> original, PayloadConverter converter) {
        if (original instanceof StreamingEncoder) {
            addWarn(String.format(
                    "The selected encoder %s is a StreamingEncoder, which is incompatible with %s."
                            + "It will be used anyway, but streaming will be unavailable.",
                    original.getClass().getName(), this.getClass().getName()
            ));
        }

        return new BeatsEncoderWrapper<>(original, converter);
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Beats-compatible encoder.
     * Wraps the original encoder and blocks if we should wait for ACK.
     *
     * @param <E> log event type
     */
    private class BeatsEncoderWrapper<E> extends EncoderBase<E> {

        private final Encoder<E> original;
        private final PayloadConverter converter;

        BeatsEncoderWrapper(Encoder<E> original, PayloadConverter converter) {
            this.original = original;
            this.converter = converter;
        }

        @Override
        public byte[] headerBytes() {
            return original.headerBytes();
        }

        @Override
        public byte[] encode(E event) {
            if (counter.get() == windowSize) {
                try {
                    ackEvents.take();
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] encoded = original.encode(event);
            if (converter != null && converter.isStarted()) {
                try {
                    return converter.convert(encoded);
                } catch (IOException e) {
                    addWarn("Error encountered while converting log event. Event: " + event, e);
                    return new byte[0];
                }
            } else {
                return encoded;
            }
        }

        @Override
        public byte[] footerBytes() {
            return original.footerBytes();
        }
    }

    @Override
    protected Future<?> scheduleReaderCallable(Callable<Void> readerCallable) {
        this.ackReaderCallable = new AckReaderCallable();
        return getExecutorService().submit(this.ackReaderCallable);
    }

    @Override
    protected void fireConnectionOpened(Socket socket) {
        if (this.ackReaderCallable == null) {
            addError("Connection was not initialized properly - the ACK reader is not running. This will block the appender.");
            throw new IllegalStateException("ACK reader not running - appender would hang");
        }

        try {
            ackReaderCallable.setInputStream(socket.getInputStream());
            ackReaderCallable.readyLock.notify();
            sendWindowSize(socket.getOutputStream());
        } catch (IOException e) {
            addError("Error during Beats connection initialization", e);
        }

        counter.set(0);
        super.fireConnectionOpened(socket);
    }

    /**
     * Writes window size frame to the output stream to tell the reader
     * how often we would like to receive ACKs.
     */
    private void sendWindowSize(OutputStream outputStream) throws IOException {
        byte[] message = new byte[6];
        message[0] = PROTOCOL_VERSION;
        message[1] = PAYLOAD_WINDOW_TYPE;
        byte[] windowSizeBytes = intToBytes(windowSize);
        System.arraycopy(windowSizeBytes, 0, message, 2, windowSizeBytes.length);
        outputStream.write(message);
        outputStream.flush();
    }

    /**
     * Callable that reads all ACKs from the remote log reader.
     */
    private class AckReaderCallable implements Callable<Void> {

        /**
         * An InputStream that will be read from.
         * Due to current {@link AbstractLogstashTcpSocketAppender} implementation,
         * this field is lazily initialized.
         */
        private volatile InputStream inputStream;
        /**
         * Lock that guards {@link #inputStream} state.
         * Used to prevent a race condition during lazy {@link #inputStream} initialization.
         */
        private final Lock readyLock = new ReentrantLock();

        AckReaderCallable() {
            super();
        }

        @Override
        public Void call() throws Exception {
            updateCurrentThreadName();
            try {
                readyLock.wait();
                while (true) {
                    try {
                        int responseByte = inputStream.read();
                        if (responseByte == -1) {
                            // end of stream
                            return null;
                        } else {
                            readAckResponse(responseByte);
                        }
                    } catch (SocketTimeoutException e) {
                        // try again
                    }
                }
            } finally {
                if (!Thread.currentThread().isInterrupted()) {
                    getExecutorService().submit(() -> {
                        // https://github.com/logstash/logstash-logback-encoder/issues/341
                        // see ReaderCallable from AbstractLogstashTcpSocketAppender for more clarification
                        getDisruptor().getRingBuffer().tryPublishEvent(getEventTranslator(), null);
                    });
                }
            }
        }

        private void readAckResponse(int responseByte) throws IOException {
            if (responseByte != PROTOCOL_VERSION) {
                addWarn(String.format("Protocol version is incorrect: %d, expected: %d.", responseByte, PROTOCOL_VERSION));
                return;
            }

            int payloadType = inputStream.read();
            if (payloadType != PAYLOAD_ACK_TYPE) {
                addWarn(String.format("Unknown payload type: %d, expected: %d (ACK)", payloadType, PAYLOAD_ACK_TYPE));
                return;
            }

            byte[] sequenceNumber = new byte[4];
            for (int i = 0; i < 4; i++) {
                int next = inputStream.read();
                if (next == -1) {
                    throw new IOException("End of stream while reading ACK sequence number");
                }
                sequenceNumber[i] = (byte) next;
            }
            ackEvents.offer(new AckEvent(bytesToInt(sequenceNumber)));
        }

        public void setInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }
    }

    private static class AckEvent {

        private final int sequenceNumber;

        private AckEvent(int sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        public int getSequenceNumber() {
            return sequenceNumber;
        }
    }
}
