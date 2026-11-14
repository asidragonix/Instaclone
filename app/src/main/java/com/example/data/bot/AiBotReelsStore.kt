package com.example.data.bot

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AiBotReelsStore(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private var bots: MutableList<AiBotAccount> = mutableListOf()
    private var botReels: MutableList<BotReel> = mutableListOf()
    private var userReels: MutableList<BotReel> = mutableListOf()
    private var logs: MutableList<BotActivityLog> = mutableListOf()
    private var isWatchdogActive: Boolean = true
    private var lastBanner: String = "100 AI Bots active • 1 video/day per bot • 24h auto-replace"

    private val _uiState = MutableStateFlow(AiBotReelsUiState())
    val uiState: StateFlow<AiBotReelsUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    @Synchronized
    fun initialize() {
        loadFromPrefs()
        val firstBoot = botReels.isEmpty()
        run24HourLifecycleCheckInternal(forceSimulateNextDay = false)
        if (firstBoot) {
            appendLog("Initialized 100 AI Bots (strictly 1 active video/day per bot, 24h auto-replace active).")
        } else {
            appendLog("Restored 100 AI Bots from device storage (${botReels.size}/100 active reels verified).")
        }
        persistAndEmit()
    }

    @Synchronized
    fun run24HourLifecycleCheck(forceSimulateNextDay: Boolean = false): Int {
        val replaced = run24HourLifecycleCheckInternal(forceSimulateNextDay)
        if (replaced > 0 || forceSimulateNextDay) {
            persistAndEmit()
        }
        return replaced
    }

    @Synchronized
    fun simulateNextDay24h(): Int {
        val expiredTimestamp = System.currentTimeMillis() - (ONE_DAY_MS + 60_000L)
        botReels = botReels.map { it.copy(createdAtMillis = expiredTimestamp) }.toMutableList()
        val replaced = run24HourLifecycleCheckInternal(forceSimulateNextDay = true)
        persistAndEmit()
        return replaced
    }

    @Synchronized
    fun replaceSingleBotReel(botId: String) {
        val bot = bots.find { it.id == botId } ?: return
        val currentReel = botReels.find { it.botId == botId }
        val fresh = buildFreshReelForBot(
            bot = bot,
            previousVideoUrl = currentReel?.videoUrl,
            customCreatedAt = System.currentTimeMillis(),
            cycleNum = (currentReel?.cycleNumber ?: 1) + 1
        )
        botReels = (listOf(fresh) + botReels.filter { it.botId != botId }).toMutableList()
        appendLog("[Single Bot 24h Replace] @${bot.username}: old reel deleted -> new reel uploaded (1/1).")
        lastBanner = "Replaced @${bot.username}'s active reel (still strictly 1/1 video)."
        persistAndEmit()
    }

    @Synchronized
    fun toggleAutoWatchdog() {
        isWatchdogActive = !isWatchdogActive
        appendLog(
            if (isWatchdogActive) "24-Hour Auto-Replace Watchdog RESUMED."
            else "24-Hour Auto-Replace Watchdog PAUSED."
        )
        persistAndEmit()
    }

    @Synchronized
    fun toggleLikeReel(reelId: String) {
        var updated = false
        botReels = botReels.map { reel ->
            if (reel.id == reelId) {
                updated = true
                val nextLiked = !reel.isLiked
                val nextCount = if (nextLiked) reel.likesCount + 1L else (reel.likesCount - 1L).coerceAtLeast(0L)
                reel.copy(isLiked = nextLiked, likesCount = nextCount)
            } else reel
        }.toMutableList()

        if (!updated) {
            userReels = userReels.map { reel ->
                if (reel.id == reelId) {
                    val nextLiked = !reel.isLiked
                    val nextCount = if (nextLiked) reel.likesCount + 1L else (reel.likesCount - 1L).coerceAtLeast(0L)
                    reel.copy(isLiked = nextLiked, likesCount = nextCount)
                } else reel
            }.toMutableList()
        }
        persistAndEmit()
    }

    @Synchronized
    fun addComment(reelId: String, author: String, text: String) {
        val trimmed = text.trim().take(400)
        if (trimmed.isEmpty()) return
        val newComment = ReelComment(
            id = "rc_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            author = author.ifBlank { "you" },
            text = trimmed,
            createdAtMillis = System.currentTimeMillis()
        )
        botReels = botReels.map { r ->
            if (r.id == reelId) r.copy(comments = r.comments + newComment) else r
        }.toMutableList()
        userReels = userReels.map { r ->
            if (r.id == reelId) r.copy(comments = r.comments + newComment) else r
        }.toMutableList()
        persistAndEmit()
    }

    @Synchronized
    fun toggleFollowBot(targetBotId: String?, targetUsername: String) {
        if (targetBotId != null) {
            var nextFollowing = false
            bots = bots.map { bot ->
                if (bot.id == targetBotId) {
                    nextFollowing = !bot.isFollowing
                    bot.copy(isFollowing = nextFollowing)
                } else bot
            }.toMutableList()
            botReels = botReels.map { r ->
                if (r.botId == targetBotId) r.copy(isFollowing = nextFollowing) else r
            }.toMutableList()
        } else {
            userReels = userReels.map { r ->
                if (r.username == targetUsername) r.copy(isFollowing = !r.isFollowing) else r
            }.toMutableList()
        }
        persistAndEmit()
    }

    @Synchronized
    fun publishEditedReel(
        username: String,
        displayName: String,
        avatarUrl: String,
        videoUrl: String,
        posterUrl: String,
        caption: String,
        audioTrack: String,
        filterPreset: String,
        playbackSpeed: Float,
        trimStartPct: Float,
        trimEndPct: Float,
        overlayText: String
    ): BotReel {
        val created = BotReel(
            id = "userreel_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}",
            botId = null,
            username = username.ifBlank { "creator" },
            displayName = displayName.ifBlank { "Creator" },
            avatarUrl = avatarUrl,
            videoUrl = videoUrl.ifBlank { MEDIA_CATALOG.first().videoUrl },
            posterUrl = posterUrl.ifBlank { MEDIA_CATALOG.first().posterUrl },
            caption = caption.trim().ifBlank { "Created in InstaClone Reels Studio #reels" },
            audioTrack = audioTrack.ifBlank { "Original Studio Audio" },
            niche = "Creator Studio",
            isAiBot = false,
            cycleNumber = 1,
            likesCount = 1L,
            isLiked = false,
            isFollowing = true,
            createdAtMillis = System.currentTimeMillis(),
            filterPreset = filterPreset,
            playbackSpeed = playbackSpeed,
            trimStartPct = trimStartPct,
            trimEndPct = trimEndPct,
            overlayText = overlayText.trim(),
            comments = emptyList()
        )
        userReels = (listOf(created) + userReels).toMutableList()
        appendLog("[Reels Studio] Published edited reel by @${created.username} ($filterPreset, ${playbackSpeed}x).")
        lastBanner = "Published your edited reel to the Reels feed!"
        persistAndEmit()
        return created
    }

    private fun run24HourLifecycleCheckInternal(forceSimulateNextDay: Boolean): Int {
        if (bots.size != 100) {
            bots = create100BotAccounts().toMutableList()
        }
        val now = System.currentTimeMillis()
        val activeMap = HashMap<String, BotReel>(bots.size)
        for (reel in botReels) {
            val bId = reel.botId ?: continue
            val prev = activeMap[bId]
            if (prev == null || reel.createdAtMillis > prev.createdAtMillis) {
                activeMap[bId] = reel
            }
        }

        var replacedCount = 0
        val nextBotReels = ArrayList<BotReel>(bots.size)

        bots.forEachIndexed { idx, bot ->
            val currentReel = activeMap[bot.id]
            val ageMs = if (currentReel != null) now - currentReel.createdAtMillis else ONE_DAY_MS + 1L
            val isExpired = forceSimulateNextDay || currentReel == null || ageMs >= ONE_DAY_MS

            if (isExpired) {
                val prevUrl = currentReel?.videoUrl
                val nextCycle = if (currentReel != null) currentReel.cycleNumber + 1 else 1
                val assignedCreatedAt = if (currentReel == null && !forceSimulateNextDay) {
                    now - ((idx * 13L * 60_000L) % (22L * 3600_000L))
                } else {
                    now
                }
                nextBotReels.add(
                    buildFreshReelForBot(
                        bot = bot,
                        previousVideoUrl = prevUrl,
                        customCreatedAt = assignedCreatedAt,
                        cycleNum = nextCycle
                    )
                )
                if (currentReel != null) replacedCount++
            } else if (currentReel != null) {
                nextBotReels.add(currentReel.copy(isFollowing = bot.isFollowing))
            }
        }

        botReels = enforceStrictOneReelPerBot(bots, nextBotReels).toMutableList()

        if (forceSimulateNextDay) {
            val msg = "[24h Cycle Simulated] Deleted $replacedCount expired reels -> Uploaded ${botReels.size} fresh daily reels (1 per bot)."
            appendLog(msg)
            lastBanner = msg
        } else if (replacedCount > 0) {
            val msg = "[Auto 24h Check] Deleted $replacedCount expired reel(s) -> Uploaded $replacedCount fresh replacement reel(s)."
            appendLog(msg)
            lastBanner = msg
        }

        return replacedCount
    }

    private fun enforceStrictOneReelPerBot(
        botAccounts: List<AiBotAccount>,
        reels: List<BotReel>
    ): List<BotReel> {
        val byBot = HashMap<String, BotReel>(botAccounts.size)
        for (reel in reels) {
            val bId = reel.botId ?: continue
            val existing = byBot[bId]
            if (existing == null || reel.createdAtMillis > existing.createdAtMillis) {
                byBot[bId] = reel
            }
        }
        return botAccounts.mapNotNull { byBot[it.id] }
    }

    private fun buildFreshReelForBot(
        bot: AiBotAccount,
        previousVideoUrl: String?,
        customCreatedAt: Long,
        cycleNum: Int
    ): BotReel {
        val nicheItem = MEDIA_CATALOG.find { it.niche == bot.niche } ?: MEDIA_CATALOG.first()
        val chosen = if (previousVideoUrl != null) {
            val alternatives = MEDIA_CATALOG.filter { it.videoUrl != previousVideoUrl }
            if (alternatives.isNotEmpty()) alternatives.random() else nicheItem
        } else {
            nicheItem
        }

        val caption = chosen.captions.random()
        val audio = chosen.audios.random()
        val cleanTag = bot.username.replace(Regex("[^a-zA-Z0-9]"), "")

        return BotReel(
            id = "botreel_${bot.id}_${customCreatedAt}_${Random.nextInt(100, 999)}",
            botId = bot.id,
            username = bot.username,
            displayName = bot.displayName,
            avatarUrl = bot.avatarUrl,
            videoUrl = chosen.videoUrl,
            posterUrl = chosen.posterUrl,
            caption = "$caption #$cleanTag",
            audioTrack = audio,
            niche = chosen.niche,
            isAiBot = true,
            cycleNumber = cycleNum,
            likesCount = Random.nextLong(140L, 2400L),
            isLiked = false,
            isFollowing = bot.isFollowing,
            createdAtMillis = customCreatedAt,
            comments = listOf(
                ReelComment(
                    id = "rc_seed_${bot.id}",
                    author = "priya_designs",
                    text = "Clean daily 24h drop!",
                    createdAtMillis = customCreatedAt
                )
            )
        )
    }

    private fun create100BotAccounts(): List<AiBotAccount> {
        val result = ArrayList<AiBotAccount>(100)
        for (i in 0 until 100) {
            val first = BOT_FIRST_NAMES[i % BOT_FIRST_NAMES.lengthOrSize()]
            val last = BOT_LAST_TAGS[(i * 7 + (i / BOT_FIRST_NAMES.size)) % BOT_LAST_TAGS.size]
            val suffix = if (i >= 50) "_${(i % 50) + 1}" else ""
            val username = "${first.lowercase(Locale.ROOT)}.$last$suffix"
            val catalogItem = MEDIA_CATALOG[i % MEDIA_CATALOG.size]
            result.add(
                AiBotAccount(
                    id = String.format(Locale.US, "bot-%03d", i + 1),
                    username = username,
                    displayName = "$first AI",
                    avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=$username",
                    bio = BOT_BIOS[i % BOT_BIOS.size],
                    niche = catalogItem.niche,
                    followersCount = 1400L + ((i * 873L) % 85_000L),
                    isFollowing = i < 5
                )
            )
        }
        return result
    }

    private fun List<String>.lengthOrSize(): Int = size

    private fun appendLog(message: String) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        logs.add(0, BotActivityLog(formatter.format(Date()), message))
        if (logs.size > 25) {
            logs = logs.take(25).toMutableList()
        }
    }

    private fun persistAndEmit() {
        saveToPrefs()
        val combined = buildCombinedReels()
        _uiState.value = AiBotReelsUiState(
            bots = bots.toList(),
            reels = combined,
            botReelsCount = botReels.size,
            logs = logs.toList(),
            isWatchdogActive = isWatchdogActive,
            lastBanner = lastBanner
        )
    }

    private fun buildCombinedReels(): List<BotReel> {
        val sortedBotReels = botReels.sortedByDescending { it.createdAtMillis }
        val merged = userReels + sortedBotReels + defaultFriendReels
        val seen = HashSet<String>(merged.size)
        return merged.filter { seen.add(it.id) }
    }

    private fun saveToPrefs() {
        val botsArr = JSONArray()
        bots.forEach { b ->
            botsArr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("username", b.username)
                    put("displayName", b.displayName)
                    put("avatarUrl", b.avatarUrl)
                    put("bio", b.bio)
                    put("niche", b.niche)
                    put("followersCount", b.followersCount)
                    put("isFollowing", b.isFollowing)
                }
            )
        }

        val botReelsArr = JSONArray()
        botReels.forEach { r -> botReelsArr.put(reelToJson(r)) }

        val userReelsArr = JSONArray()
        userReels.forEach { r -> userReelsArr.put(reelToJson(r)) }

        val logsArr = JSONArray()
        logs.take(25).forEach { log ->
            logsArr.put(
                JSONObject().apply {
                    put("timeLabel", log.timeLabel)
                    put("message", log.message)
                }
            )
        }

        prefs.edit()
            .putString(KEY_BOTS, botsArr.toString())
            .putString(KEY_BOT_REELS, botReelsArr.toString())
            .putString(KEY_USER_REELS, userReelsArr.toString())
            .putString(KEY_LOGS, logsArr.toString())
            .putBoolean(KEY_WATCHDOG, isWatchdogActive)
            .putString(KEY_BANNER, lastBanner)
            .apply()
    }

    private fun loadFromPrefs() {
        isWatchdogActive = prefs.getBoolean(KEY_WATCHDOG, true)
        lastBanner = prefs.getString(KEY_BANNER, lastBanner) ?: lastBanner

        val botsJson = prefs.getString(KEY_BOTS, null)
        bots = if (!botsJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(botsJson)
                val parsed = ArrayList<AiBotAccount>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    parsed.add(
                        AiBotAccount(
                            id = o.getString("id"),
                            username = o.getString("username"),
                            displayName = o.getString("displayName"),
                            avatarUrl = o.optString("avatarUrl", ""),
                            bio = o.optString("bio", ""),
                            niche = o.optString("niche", MEDIA_CATALOG.first().niche),
                            followersCount = o.optLong("followersCount", 2500L),
                            isFollowing = o.optBoolean("isFollowing", false)
                        )
                    )
                }
                if (parsed.size == 100) parsed else create100BotAccounts().toMutableList()
            }.getOrElse { create100BotAccounts().toMutableList() }
        } else {
            create100BotAccounts().toMutableList()
        }

        val botReelsJson = prefs.getString(KEY_BOT_REELS, null)
        botReels = if (!botReelsJson.isNullOrBlank()) {
            runCatching { parseReelsArray(JSONArray(botReelsJson)).toMutableList() }
                .getOrElse { mutableListOf() }
        } else mutableListOf()
        botReels = enforceStrictOneReelPerBot(bots, botReels).toMutableList()

        val userReelsJson = prefs.getString(KEY_USER_REELS, null)
        userReels = if (!userReelsJson.isNullOrBlank()) {
            runCatching { parseReelsArray(JSONArray(userReelsJson)).toMutableList() }
                .getOrElse { mutableListOf() }
        } else mutableListOf()

        val logsJson = prefs.getString(KEY_LOGS, null)
        logs = if (!logsJson.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(logsJson)
                val parsed = ArrayList<BotActivityLog>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    parsed.add(BotActivityLog(o.getString("timeLabel"), o.getString("message")))
                }
                parsed
            }.getOrElse { mutableListOf() }
        } else mutableListOf()
    }

    private fun reelToJson(r: BotReel): JSONObject {
        val commentsArr = JSONArray()
        r.comments.forEach { c ->
            commentsArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("author", c.author)
                    put("text", c.text)
                    put("createdAtMillis", c.createdAtMillis)
                }
            )
        }
        return JSONObject().apply {
            put("id", r.id)
            put("botId", r.botId ?: JSONObject.NULL)
            put("username", r.username)
            put("displayName", r.displayName)
            put("avatarUrl", r.avatarUrl)
            put("videoUrl", r.videoUrl)
            put("posterUrl", r.posterUrl)
            put("caption", r.caption)
            put("audioTrack", r.audioTrack)
            put("niche", r.niche)
            put("isAiBot", r.isAiBot)
            put("cycleNumber", r.cycleNumber)
            put("likesCount", r.likesCount)
            put("isLiked", r.isLiked)
            put("isFollowing", r.isFollowing)
            put("createdAtMillis", r.createdAtMillis)
            put("filterPreset", r.filterPreset)
            put("playbackSpeed", r.playbackSpeed.toDouble())
            put("trimStartPct", r.trimStartPct.toDouble())
            put("trimEndPct", r.trimEndPct.toDouble())
            put("overlayText", r.overlayText)
            put("comments", commentsArr)
        }
    }

    private fun parseReelsArray(arr: JSONArray): List<BotReel> {
        val parsed = ArrayList<BotReel>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val commentsJson = o.optJSONArray("comments") ?: JSONArray()
            val parsedComments = ArrayList<ReelComment>(commentsJson.length())
            for (j in 0 until commentsJson.length()) {
                val c = commentsJson.getJSONObject(j)
                parsedComments.add(
                    ReelComment(
                        id = c.optString("id", "rc_$j"),
                        author = c.optString("author", "user"),
                        text = c.optString("text", ""),
                        createdAtMillis = c.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            parsed.add(
                BotReel(
                    id = o.getString("id"),
                    botId = if (o.isNull("botId")) null else o.optString("botId"),
                    username = o.getString("username"),
                    displayName = o.optString("displayName", o.getString("username")),
                    avatarUrl = o.optString("avatarUrl", ""),
                    videoUrl = o.getString("videoUrl"),
                    posterUrl = o.optString("posterUrl", ""),
                    caption = o.optString("caption", ""),
                    audioTrack = o.optString("audioTrack", "Original Audio"),
                    niche = o.optString("niche", "Reels"),
                    isAiBot = o.optBoolean("isAiBot", true),
                    cycleNumber = o.optInt("cycleNumber", 1),
                    likesCount = o.optLong("likesCount", 0L),
                    isLiked = o.optBoolean("isLiked", false),
                    isFollowing = o.optBoolean("isFollowing", false),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                    filterPreset = o.optString("filterPreset", "Original"),
                    playbackSpeed = o.optDouble("playbackSpeed", 1.0).toFloat(),
                    trimStartPct = o.optDouble("trimStartPct", 0.0).toFloat(),
                    trimEndPct = o.optDouble("trimEndPct", 100.0).toFloat(),
                    overlayText = o.optString("overlayText", ""),
                    comments = parsedComments
                )
            )
        }
        return parsed
    }

    data class CatalogVideoItem(
        val niche: String,
        val videoUrl: String,
        val posterUrl: String,
        val captions: List<String>,
        val audios: List<String>
    )

    companion object {
        const val ONE_DAY_MS: Long = 24L * 60L * 60L * 1000L
        private const val PREFS_NAME = "instaclone_ai_bot_reels_v1"
        private const val KEY_BOTS = "ai_bots_json"
        private const val KEY_BOT_REELS = "ai_bot_reels_json"
        private const val KEY_USER_REELS = "user_edited_reels_json"
        private const val KEY_LOGS = "ai_bot_logs_json"
        private const val KEY_WATCHDOG = "ai_bot_watchdog_active"
        private const val KEY_BANNER = "ai_bot_last_banner"

        val MEDIA_CATALOG: List<CatalogVideoItem> = listOf(
            CatalogVideoItem(
                niche = "Surf & Coastlines",
                videoUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-576p.mp4",
                posterUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
                captions = listOf(
                    "Dawn patrol barrel ride at sunrise #SurfReels #OceanVibes #DailyReel",
                    "Chasing glassy morning swells along the coast #SurfLife #24hReel #Travel",
                    "When the tide hits just right at golden hour #AdventureReels #Coastal"
                ),
                audios = listOf(
                    "Coastal Horizon • Sunset Beats Mix",
                    "Pacific Swell • Chillwave Radio",
                    "Golden Tide • Original Bot Audio"
                )
            ),
            CatalogVideoItem(
                niche = "Deep Ocean & Marine",
                videoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
                captions = listOf(
                    "Gliding beside sea turtles over turquoise coral reefs #DeepOcean #MarineLife",
                    "Zero-gravity calm 30 feet below the surface #UnderwaterReels #Nature24h",
                    "Ocean currents & sunlit reef wonders #ScubaReels #BluePlanet"
                ),
                audios = listOf(
                    "Deep Blue Drift • Ambient Synth",
                    "Coral Echoes • Lo-Fi Underwater",
                    "Oceanic Pulse • Cinema Soundscape"
                )
            ),
            CatalogVideoItem(
                niche = "Botanical Slow-Mo",
                videoUrl = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                posterUrl = "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=800",
                captions = listOf(
                    "Spring petals unfurling in macro slow-motion #BotanicalReels #MacroNature",
                    "Morning dew & golden sunlight in full bloom #SlowMoReels #Aesthetic",
                    "Tiny garden universe waking up today #NatureDaily #CleanVisuals"
                ),
                audios = listOf(
                    "Sunlit Garden • Acoustic Lo-Fi",
                    "Ambient Bloom • Chill Studio Mix",
                    "Morning Dew • Soft Piano Loops"
                )
            )
        )

        private val BOT_FIRST_NAMES = listOf(
            "Aria", "Kael", "Nova", "Zane", "Maya", "Liam", "Chloe", "Rohan", "Elena", "Devon",
            "Sienna", "Lucas", "Kiara", "Arjun", "Mila", "Theo", "Nadia", "Caleb", "Zara", "Ethan",
            "Ananya", "Julian", "Leila", "Marco", "Ruby", "Kabir", "Iris", "Noah", "Freya", "Vikram",
            "Talia", "Owen", "Clara", "Reyansh", "Sora", "Dante", "Yuki", "Mateo", "Amara", "Felix",
            "Nina", "Ishaan", "Vera", "Kieran", "Hazel", "Aiden", "Lina", "Soren", "Kaya", "Rafael"
        )

        private val BOT_LAST_TAGS = listOf(
            "frames", "motion", "cinematics", "waves", "botanica", "visuals", "reels", "studio", "drift", "lens",
            "vibes", "horizon", "bloom", "tide", "glow", "captures", "daily", "aesthetics", "chronicle", "flux"
        )

        private val BOT_BIOS = listOf(
            "Daily 24h vertical video drops | AI Reel Creator",
            "1 curated reel every single day | Auto-refreshed 24h",
            "Ocean, adventure & macro cinema",
            "Capturing fleeting 24-hour moments in 9:16",
            "Slow-motion visuals & ambient soundtracks"
        )

        private val defaultFriendReels: List<BotReel> = listOf(
            BotReel(
                id = "friend-reel-1",
                botId = null,
                username = "sara_vlogs",
                displayName = "Sara Ali",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                videoUrl = "https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-576p.mp4",
                posterUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
                caption = "Chasing waves with friends #ocean #surf #weekendvibes",
                audioTrack = "Original Soundtrack • Ocean Chill Remix",
                niche = "Friends",
                isAiBot = false,
                likesCount = 1840L,
                isLiked = false,
                isFollowing = false,
                createdAtMillis = System.currentTimeMillis() - 2L * 3600_000L,
                comments = listOf(
                    ReelComment("fc_1", "mohit_v", "Epic wave shot!", System.currentTimeMillis() - 3600_000L)
                )
            )
        )

        fun formatExpiresInBadge(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
            val elapsed = (nowMillis - createdAtMillis).coerceAtLeast(0L)
            val remainingMs = (ONE_DAY_MS - elapsed).coerceAtLeast(0L)
            val totalMinutes = ((remainingMs + 59_999L) / 60_000L).toInt()
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return when {
                hours >= 1 -> "Expires in ${hours}h"
                mins > 0 -> "Expires in ${mins}m"
                else -> "Replacing soon"
            }
        }

        fun formatFollowerCount(num: Long): String =
            if (num >= 1000L) {
                val k = num / 1000.0
                val text = String.format(Locale.US, "%.1f", k).removeSuffix(".0")
                "${text}K"
            } else {
                num.toString()
            }
    }
}
