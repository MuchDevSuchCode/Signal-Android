/*
 * NON-UPSTREAM. Tracks which conversations currently have their inline media hidden by the
 * "hide media when backgrounded" privacy feature.
 *
 * State is intentionally in-memory only: it is transient privacy state that should reset when the
 * process dies. Keyed by thread id so that hiding survives leaving and re-entering a chat within the
 * same session.
 */
package org.thoughtcrime.securesms.conversation.v2

object ConversationMediaVisibility {

  private val hiddenThreads = mutableSetOf<Long>()

  @Synchronized
  fun isHidden(threadId: Long): Boolean = hiddenThreads.contains(threadId)

  @Synchronized
  fun setHidden(threadId: Long, hidden: Boolean) {
    if (hidden) {
      hiddenThreads.add(threadId)
    } else {
      hiddenThreads.remove(threadId)
    }
  }
}
