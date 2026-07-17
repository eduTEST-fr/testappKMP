package com.example.eduflow.sensor

/** Acumula intervalos sin depender de incrementar un contador cada segundo. */
class StudyTimer(private val nowMillis: () -> Long) {
    private var accumulatedMillis = 0L
    private var activeSinceMillis: Long? = null

    val isActive: Boolean get() = activeSinceMillis != null

    fun reset() {
        accumulatedMillis = 0L
        activeSinceMillis = null
    }

    fun setActive(active: Boolean) {
        val now = nowMillis()
        if (active && activeSinceMillis == null) {
            activeSinceMillis = now
        } else if (!active && activeSinceMillis != null) {
            accumulatedMillis += (now - activeSinceMillis!!).coerceAtLeast(0L)
            activeSinceMillis = null
        }
    }

    fun totalMillis(): Long {
        val current = activeSinceMillis?.let { (nowMillis() - it).coerceAtLeast(0L) } ?: 0L
        return accumulatedMillis + current
    }

    fun finish(): Long {
        setActive(false)
        return accumulatedMillis
    }
}
