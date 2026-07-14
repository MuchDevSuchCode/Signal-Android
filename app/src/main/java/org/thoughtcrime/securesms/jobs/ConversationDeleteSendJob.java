package org.thoughtcrime.securesms.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.crypto.SealedSenderAccessUtil;
import org.thoughtcrime.securesms.dependencies.AppDependencies;
import org.thoughtcrime.securesms.jobmanager.Job;
import org.thoughtcrime.securesms.jobmanager.JsonJobData;
import org.thoughtcrime.securesms.jobmanager.impl.NetworkConstraint;
import org.thoughtcrime.securesms.net.NotPushRegisteredException;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.recipients.RecipientUtil;
import org.thoughtcrime.securesms.transport.RetryLaterException;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.SignalServiceMessageSender.IndividualSendEvents;
import org.whispersystems.signalservice.api.crypto.ContentHint;
import org.whispersystems.signalservice.api.messages.SendMessageResult;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * NON-UPSTREAM. Asks the other party in a 1:1 chat to delete the entire conversation.
 *
 * <p>All state needed to send is captured at creation time, because the local copy of the
 * conversation is deleted immediately when the request is made -- there is no thread or message row
 * left to read by the time this runs.
 */
public final class ConversationDeleteSendJob extends BaseJob {

  public static final String KEY = "ConversationDeleteSendJob";

  private static final String TAG = Log.tag(ConversationDeleteSendJob.class);

  private static final String KEY_RECIPIENT             = "recipient";
  private static final String KEY_REQUESTED_AT          = "requested_at";

  private final RecipientId recipientId;
  private final long        requestedAtTimestamp;

  public static @NonNull ConversationDeleteSendJob create(@NonNull RecipientId recipientId, long requestedAtTimestamp) {
    return new ConversationDeleteSendJob(new Parameters.Builder()
                                                       .setQueue(recipientId.toQueueKey())
                                                       .addConstraint(NetworkConstraint.KEY)
                                                       .setLifespan(TimeUnit.DAYS.toMillis(1))
                                                       .setMaxAttempts(Parameters.UNLIMITED)
                                                       .build(),
                                         recipientId,
                                         requestedAtTimestamp);
  }

  private ConversationDeleteSendJob(@NonNull Parameters parameters,
                                    @NonNull RecipientId recipientId,
                                    long requestedAtTimestamp)
  {
    super(parameters);

    this.recipientId          = recipientId;
    this.requestedAtTimestamp = requestedAtTimestamp;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder()
                          .putString(KEY_RECIPIENT, recipientId.serialize())
                          .putLong(KEY_REQUESTED_AT, requestedAtTimestamp)
                          .serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    Recipient recipient = Recipient.resolved(recipientId);

    if (recipient.isGroup()) {
      Log.w(TAG, "Conversation delete is only supported for 1:1 chats. Skipping.");
      return;
    }

    if (recipient.isSelf()) {
      Log.w(TAG, "Refusing to send a conversation delete to ourselves.");
      return;
    }

    if (recipient.isUnregistered()) {
      Log.w(TAG, recipientId + " not registered!");
      return;
    }

    SignalServiceMessageSender messageSender = AppDependencies.getSignalServiceMessageSender();
    SignalServiceAddress       address       = RecipientUtil.toSignalServiceAddress(context, recipient);

    SignalServiceDataMessage dataMessage = SignalServiceDataMessage.newBuilder()
                                                                   .withTimestamp(System.currentTimeMillis())
                                                                   .withConversationDelete(new SignalServiceDataMessage.ConversationDelete(requestedAtTimestamp))
                                                                   .build();

    SendMessageResult result = messageSender.sendDataMessage(address,
                                                             SealedSenderAccessUtil.getSealedSenderAccessFor(recipient),
                                                             ContentHint.IMPLICIT,
                                                             dataMessage,
                                                             IndividualSendEvents.EMPTY,
                                                             false,
                                                             recipient.getNeedsPniSignature());

    if (result.getIdentityFailure() != null) {
      Log.w(TAG, "Identity failure for " + recipientId + ". The conversation delete will not be delivered.");
    } else if (result.isUnregisteredFailure()) {
      Log.w(TAG, "Unregistered failure for " + recipientId + ". The conversation delete will not be delivered.");
    } else if (result.getSuccess() == null) {
      throw new RetryLaterException();
    } else {
      Log.i(TAG, "Conversation delete sent to " + recipientId);
    }
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    if (e instanceof NotPushRegisteredException) return false;
    return e instanceof IOException ||
           e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to send the conversation delete to " + recipientId + ". Their copy of the chat will remain.");
  }

  public static final class Factory implements Job.Factory<ConversationDeleteSendJob> {
    @Override
    public @NonNull ConversationDeleteSendJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      return new ConversationDeleteSendJob(parameters,
                                           RecipientId.from(data.getString(KEY_RECIPIENT)),
                                           data.getLong(KEY_REQUESTED_AT));
    }
  }
}
