package com.example.model

enum class TournamentType {
    DAILY,
    WEEKLY
}

enum class TournamentStatus {
    ACTIVE,
    UPCOMING,
    EXPIRED
}

data class Tournament(
    val id: String,
    val title: String,
    val gameId: String,
    val gameTitle: String,
    val type: TournamentType,
    val description: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val virtualPrizePoolCoins: Int,
    val entryFeeCoins: Int = 0,
    val status: TournamentStatus = TournamentStatus.ACTIVE
)

data class TournamentEntry(
    val rank: Int = 0,
    val userId: String = "",
    val name: String = "Anonymous",
    val score: Int = 0,
    val level: Int = 1,
    val badge: String = "Contender",
    val avatarColorHex: Long = 0xFF3D5AFE,
    val timestamp: Long = 0L,
    val isCurrentPlayer: Boolean = false
)

data class TournamentSubmitResult(
    val tournamentId: String,
    val tournamentTitle: String,
    val score: Int,
    val isNewTournamentBest: Boolean,
    val currentRank: Int,
    val totalParticipants: Int,
    val coinsEarned: Int,
    val xpEarned: Int
)
