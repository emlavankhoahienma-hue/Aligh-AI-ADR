package com.alignai.camera.data.models

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

// MARK: - Composition Rule Types
enum class CompositionRule(
    val id: String,
    val displayNameVietnamese: String,
    val descriptionVietnamese: String
) {
    RULE_OF_THIRDS("Rule of Thirds", "Quy tắc 1/3", "Đặt chủ thể tại 4 điểm giao thoa kinh điển"),
    GOLDEN_RATIO("Golden Ratio", "Tỷ lệ vàng (1.618)", "Bố cục tỷ lệ vàng 1:1.618 chuẩn thị giác"),
    GOLDEN_SPIRAL("Golden Spiral", "Xoắn ốc Fibonacci", "Đường xoắn ốc dẫn dắt ánh nhìn vào tâm điểm"),
    CENTER_SYMMETRY("Center Pro", "Tâm đối xứng", "Căn chủ thể chính xác tại tâm đối xứng"),
    DYNAMIC_AI("AI Auto-Select", "AI Tự động tối ưu", "Tự động đề xuất bố cục theo ngữ cảnh");
}

// MARK: - Camera Capture Mode
enum class CameraCaptureMode(val title: String) {
    PHOTO("Ảnh"),
    VIDEO("Video"),
    PRO_VIDEO("Pro");

    val isVideo: Boolean get() = this == VIDEO || this == PRO_VIDEO
}

// MARK: - Pro Video Parameter Tabs
enum class ProVideoParameterTab(val title: String) {
    ISO("ISO"),
    SHUTTER("SEC"),
    APERTURE_EV("EV"),
    WB("WB"),
    FOCUS("FOCUS")
}

// MARK: - Video Format Options & Codecs
enum class VideoFormatOption(
    val title: String,
    val width: Int,
    val height: Int,
    val fps: Int
) {
    HD_60("1080P 60FPS", 1920, 1080, 60),
    HD_30("1080P 30FPS", 1920, 1080, 30),
    UHD_60("4K 60FPS", 3840, 2160, 60),
    UHD_30("4K 30FPS", 3840, 2160, 30)
}

enum class VideoCodec(val title: String) {
    HEVC("HEVC"),
    H264("H.264")
}

// MARK: - AI Framing Session State Machine
sealed class AISessionState {
    object Idle : AISessionState()
    object Analyzing : AISessionState()
    data class TargetPlaced(val isLocked: Boolean) : AISessionState()
    object AlignmentPerfect : AISessionState()
    object Capturing : AISessionState()
    object Done : AISessionState()

    val displayMessage: String
        get() = when (this) {
            is Idle -> "Nhấn nút AI để bắt đầu phân tích"
            is Analyzing -> "AI đang phân tích cảnh vật..."
            is TargetPlaced -> if (isLocked) "Mục tiêu đã khóa — Di chuyển tâm trắng vào vòng vàng" else "Di chuyển máy để căn chỉnh bố cục"
            is AlignmentPerfect -> "✓ Khớp hoàn hảo! Chuẩn bị chụp..."
            is Capturing -> "Đang chụp ảnh..."
            is Done -> "Hoàn tất!"
        }

    val accentColor: Color
        get() = when (this) {
            is Idle -> Color.White.copy(alpha = 0.5f)
            is Analyzing -> Color(0xFFFFCC00)
            is TargetPlaced -> Color(0xFFFF9500)
            is AlignmentPerfect -> Color(0xFF34C759)
            is Capturing, is Done -> Color(0xFF32ADE6)
        }

    val isSessionActive: Boolean
        get() = this !is Idle && this !is Done
}

// MARK: - Tracking Quality State Machine
enum class TrackingQuality {
    LOCKED,        // Đang bám tốt bằng optical tracking
    PREDICTING,    // Vừa mất optical, ngoại suy bằng vận tốc + gyro
    REACQUIRING,   // Mất lâu hơn, đang cố tìm lại
    LOST           // Mất hẳn, cần chạm lại để đặt target
}

// MARK: - Tracking Sensitivity Preset
enum class TrackingSensitivityPreset(val title: String, val shortName: String) {
    LOW("Thấp (Ổn định, chống giật)", "Thấp"),
    MEDIUM("Vừa (Cân bằng tiêu chuẩn)", "Vừa"),
    HIGH("Cao (Phản hồi tức thì)", "Cao")
}

// MARK: - Active AI Indicator Type
enum class ActiveAIIndicatorType {
    NONE,
    LOCAL,
    CLOUD
}

// MARK: - Smart Autofocus Target Type
enum class SmartFocusType {
    FACE,
    SALIENT_OBJECT,
    CENTER,
    AI_TARGET
}

// MARK: - Scene Classification Types
enum class DetectedSceneType(val localizedName: String) {
    PORTRAIT("Chân dung (Portrait)"),
    PET("Thú cưng (Pet)"),
    LANDSCAPE("Phong cảnh (Landscape)"),
    SUNSET("Hoàng hôn (Golden Hour)"),
    ARCHITECTURE("Kiến trúc (Architecture)"),
    SKY("Bầu trời / Mây (Sky)"),
    WATER("Mặt nước (Water)"),
    FOLIAGE("Cây cối / Lá (Foliage)"),
    NIGHT("Ban đêm (Night Scene)"),
    FOOD("Ẩm thực (Food)"),
    MACRO("Cận cảnh (Macro)"),
    STREET("Đường phố (Street)"),
    GENERAL("Tự nhiên (Natural)");

    val recommendedFilter: FilmPreset
        get() = when (this) {
            PORTRAIT -> FilmPreset.FUJI_PRO_400H
            PET -> FilmPreset.KODAK_PORTRA_400
            LANDSCAPE -> FilmPreset.VELVIA_50
            SUNSET -> FilmPreset.SUNSET_GLOW
            ARCHITECTURE -> FilmPreset.CLASSIC_CHROME
            SKY -> FilmPreset.VELVIA_50
            WATER -> FilmPreset.CINEMA_TEAL_ORANGE
            FOLIAGE -> FilmPreset.TOKYO_AIRY
            NIGHT -> FilmPreset.CINESTILL_800T
            FOOD -> FilmPreset.EKTAR_100
            MACRO -> FilmPreset.FUJI_PRO_400H
            STREET -> FilmPreset.STREET_CLASSIC
            GENERAL -> FilmPreset.STANDARD
        }

    val aiFullColorParameters: AIColorParameters
        get() = when (this) {
            PORTRAIT -> AIColorParameters(-0.08f, 1.05f, 1.04f, 0.04f, 0.92f, 0.15f, 0.20f, AIColorGrade.SOFT_WARM)
            PET -> AIColorParameters(0.08f, 1.15f, 1.06f, 0.05f, 0.93f, 0.10f, 0.15f, AIColorGrade.VIBRANT)
            LANDSCAPE -> AIColorParameters(0.05f, 1.18f, 1.12f, 0.02f, 0.96f, 0.10f, 0.25f, AIColorGrade.COOL_NATURAL)
            SUNSET -> AIColorParameters(0.30f, 1.35f, 1.15f, 0.06f, 0.88f, 0.12f, 0.35f, AIColorGrade.GOLDEN)
            ARCHITECTURE -> AIColorParameters(-0.05f, 1.08f, 1.20f, 0.00f, 1.00f, 0.05f, 0.15f, AIColorGrade.TEAL_ORANGE)
            SKY -> AIColorParameters(-0.10f, 1.25f, 1.10f, 0.01f, 0.98f, 0.05f, 0.20f, AIColorGrade.COOL_NATURAL)
            WATER -> AIColorParameters(-0.12f, 1.20f, 1.15f, 0.02f, 0.95f, 0.08f, 0.22f, AIColorGrade.COOL_NATURAL)
            FOLIAGE -> AIColorParameters(0.02f, 1.22f, 1.08f, 0.04f, 0.94f, 0.08f, 0.18f, AIColorGrade.VIBRANT)
            NIGHT -> AIColorParameters(-0.15f, 0.80f, 1.35f, 0.08f, 0.85f, 0.35f, 0.55f, AIColorGrade.MOODY)
            FOOD -> AIColorParameters(0.12f, 1.22f, 1.08f, 0.05f, 0.94f, 0.08f, 0.18f, AIColorGrade.VIBRANT)
            MACRO -> AIColorParameters(0.04f, 1.20f, 1.10f, 0.03f, 0.95f, 0.08f, 0.22f, AIColorGrade.VIBRANT)
            STREET -> AIColorParameters(-0.03f, 0.95f, 1.18f, 0.01f, 0.97f, 0.22f, 0.30f, AIColorGrade.CLASSIC)
            GENERAL -> AIColorParameters(0.0f, 1.05f, 1.05f, 0.02f, 0.98f, 0.10f, 0.10f, AIColorGrade.SOFT_WARM)
        }
}

// MARK: - AI Color Parameters
enum class AIColorGrade(val title: String) {
    SOFT_WARM("Soft Warm"),
    COOL_NATURAL("Cool Natural"),
    GOLDEN("Golden Hour"),
    TEAL_ORANGE("Teal & Orange"),
    MOODY("Dark Moody"),
    VIBRANT("Vibrant"),
    CLASSIC("Classic BW"),
    CINEMATIC("Cinematic Film")
}

data class AIColorParameters(
    val warmthShift: Float,      // -1.0 to +1.0
    val saturationBoost: Float,  // 0.5 to 1.6
    val contrastCurve: Float,    // 0.8 to 1.5
    val shadowLift: Float,       // 0.0 to 0.25
    val highlightRoll: Float,    // 0.75 to 1.0
    val filmGrain: Float,        // 0.0 to 0.5
    val vignetteAmount: Float,   // 0.0 to 0.7
    val colorGrade: AIColorGrade,
    val exposureBias: Float = 0.0f,
    val tintShift: Float = 0.0f
)

// MARK: - Film Simulation Presets
enum class FilmPreset(
    val id: String,
    val displayName: String,
    val shortTitle: String,
    val description: String,
    val idealScenario: String
) {
    STANDARD("Standard Clean", "Tự nhiên", "TỰ NHIÊN", "Màu thực tế trung thực, dải sáng tối đa", "Mọi cảnh chụp cần độ chân thực tuyệt đối"),
    FUJI_PRO_400H("Fuji Pro 400H", "Fuji Pastel", "PASTEL", "Tone xanh pastel nhẹ, tôn da tươi sáng trong trẻo", "Chân dung ban ngày ngoài trời"),
    KODAK_PORTRA_400("Kodak Portra 400", "Portra Ấm", "PORTRA", "Sắc ấm vàng dịu, chuyển màu highlight và da mượt mà", "Chân dung nắng chiều ấm áp"),
    CLASSIC_CHROME("Classic Chrome", "Classic Chrome", "CHROME", "Màu phim phóng sự tài liệu, độ bão hòa dịu", "Ảnh tài liệu, phố cổ, kiến trúc"),
    CINEMA_TEAL_ORANGE("Teal & Orange", "Điện ảnh Teal", "TEAL", "Tương phản điện ảnh Hollywood, shadow teal đối lập da ấm", "Du lịch, biển đảo, bầu trời xanh"),
    VELVIA_50("Fuji Velvia 50", "Velvia Rực rỡ", "VELVIA", "Sắc màu rực rỡ bùng nổ, xanh lá và biển sâu thẳm", "Phong cảnh núi non hùng vĩ, biển"),
    SUNSET_GLOW("Sunset Glow", "Hoàng hôn Vàng", "HOÀNG HÔN", "Ấm áp rực rỡ, nhấn mạnh ánh sáng ven vàng ruộm", "Hoàng hôn, bình minh, ngược sáng"),
    TOKYO_AIRY("Tokyo Clean", "Tokyo Mơ màng", "TOKYO", "Phong cách Nhật Bản mơ màng, highlight trong trẻo", "Nàng thơ học đường, hoa anh đào"),
    HK_CINEMA_90S("HK Cinema 90s", "Hồng Kông 90s", "HK 90S", "Shadow ngọc lục bảo (Wong Kar-wai), đèn vàng hoài niệm", "Quán ăn đêm, phố hoa đèn màu"),
    CINESTILL_800T("CineStill 800T", "CineStill Đêm", "CINESTILL", "Phim điện ảnh đêm, tone lạnh dịu quầng sáng ấm", "Đêm thành phố, trạm xăng, neon"),
    LEICA_MONOCHROM("Leica Monochrom", "Leica Đen trắng", "LEICA BW", "Đen trắng thuần khiết Leica, dải xám bạc vô cực", "Chân dung nghệ thuật, đặc tả cảm xúc"),
    MONOCHROME_NOIR("Noir High Contrast", "Noir Tương phản", "NOIR", "Đen trắng tương phản cao nghệ thuật, bóng sâu", "Hình khối kiến trúc tương phản gắt"),
    TRI_X_400("Kodak Tri-X 400", "Tri-X Phóng sự", "TRI-X", "Đen trắng phóng sự báo chí, hạt phim rõ nét", "Phóng sự đời thường, chuyển động"),
    VINTAGE_WARM("Vintage Warm 70s", "Hoài niệm 70s", "HOÀI NIỆM", "Phong cách retro thập niên 70, fade nhẹ vùng đen", "Đồ vật cổ xưa, không gian gỗ"),
    STREET_CLASSIC("Street Classic", "Đường phố Pro", "ĐƯỜNG PHỐ", "Màu đường phố sắc nét, micro-contrast cao", "Nhiếp ảnh đường phố snap, nhịp sống"),
    NORDIC_COLD("Nordic Minimal", "Bắc Âu Lạnh", "BẮC ÂU", "Tone lạnh Bắc Âu tối giản, khử bão hòa màu nóng", "Ngày âm u, sương mù, tối giản"),
    EKTAR_100("Kodak Ektar 100", "Ektar Sắc nét", "EKTAR", "Hạt siêu mịn, sắc đỏ và xanh dương rực rỡ sắc sảo", "Thời trang, xe cộ, kiến trúc sắc sảo"),
    NEON_CYBERPUNK("Cyberpunk Night", "Cyberpunk Đêm", "CYBER", "Shadow lam tím huyền bí, highlight hồng tím neon", "Đêm mưa ướt, ánh đèn neon"),
    AI_FULL_AUTO("AI Full Auto Color", "Tự động AI", "TỰ ĐỘNG", "Tự động phân tích và áp dụng preset tối ưu", "Tự động nhận diện bối cảnh");

    companion object {
        val selectablePresets: List<FilmPreset> get() = values().filter { it != AI_FULL_AUTO }
    }
}

// MARK: - Captured Photo Item
data class CapturedPhotoItem(
    val id: UUID = UUID.randomUUID(),
    val uri: Uri? = null,
    val bitmap: Bitmap? = null,
    val sceneType: DetectedSceneType = DetectedSceneType.GENERAL,
    val appliedPreset: FilmPreset = FilmPreset.STANDARD,
    val compositionRule: CompositionRule = CompositionRule.RULE_OF_THIRDS,
    val alignmentScore: Double = 1.0,
    val timestamp: Long = System.currentTimeMillis(),
    val iso: Float = 100f,
    val shutterSpeed: Double = 0.016
)

// MARK: - Subject Detection Result
data class SubjectDetectionResult(
    var faceRectangles: List<RectF> = emptyList(),
    var salientPoints: List<PointF> = emptyList(),
    var dominantSubjectRect: RectF? = null,
    var primaryEyePosition: PointF? = null,
    var lookingDirection: PointF = PointF(0f, 0f),
    var detectedScene: DetectedSceneType = DetectedSceneType.GENERAL,
    var confidence: Float = 0.0f,
    var averageLuminance: Float = 0.5f,
    var estimatedColorTemp: Float = 5500f
)

// MARK: - Realtime Histogram Data
data class HistogramBarData(
    val id: Int,
    val height: Float, // 0.05 to 1.0
    val color: Color
)

// MARK: - Live Camera Stats
data class LiveCameraStats(
    val iso: Float = 100f,
    val shutterSpeedString: String = "1/60s",
    val exposureDurationSeconds: Double = 1.0 / 60.0,
    val lensPosition: Float = 0.5f
)

// MARK: - Framing Target Result
data class FramingTargetResult(
    val targetPoint: PointF,          // Normalized coordinate (0.0...1.0)
    val currentCenter: PointF,        // Camera optical center (0.5, 0.5)
    val offsetVector: PointF,         // Vector from optical center to target
    val distance: Float,              // Euclidean distance
    val angleDegrees: Float,          // Angle in degrees for compass indicator
    val alignmentScore: Double,       // 0.0 (far) to 1.0 (perfectly aligned)
    val isAligned: Boolean,           // True when distance <= tolerance
    val recommendedZoomFactor: Float, // Recommended zoom (1.0x, 2.0x, 3.0x...)
    val optimalRule: CompositionRule, // Active or auto-selected rule
    val guideDescription: String      // Actionable advice for the photographer
)
