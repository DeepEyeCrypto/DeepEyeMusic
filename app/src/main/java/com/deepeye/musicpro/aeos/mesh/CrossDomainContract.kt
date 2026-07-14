package com.deepeye.musicpro.aeos.mesh

/**
 * AEOS Day-0 Vibe-Coding Genesis
 * Cross Domain Contract - Single source of truth for communication between planes.
 */
interface CrossDomainContract<T, R> {
    suspend fun execute(request: T): R
}
