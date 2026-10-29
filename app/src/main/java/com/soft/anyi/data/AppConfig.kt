package com.soft.anyi.data

import org.json.JSONObject

/** 音效远程开关（ui_config.sfx，服务端权威） */
data class SfxConfig(
    val master: Boolean = true,
    val download: Boolean = true,
    val sign: Boolean = true,
    val message: Boolean = true,
    val update: Boolean = true
) {
    companion object {
        fun fromJson(o: JSONObject?): SfxConfig {
            if (o == null) return SfxConfig()
            fun b(k: String) = o.optBoolean(k, true)
            return SfxConfig(b("master"), b("download"), b("sign"), b("message"), b("update"))
        }
    }
}

/**
 * 远程配置（字段对准 /api/softlib/config/index，与 Flutter 版 AppConfig 一致）。
 */
data class AppConfig(
    val placard: String = "",
    val feedbackGroup: String = "",
    val feedbackUser: String = "",
    val feedbackGroupOn: Boolean = true,
    val feedbackUserOn: Boolean = true,
    val splashEnable: Boolean = true,
    val splashImage: String = "",
    val splashSeconds: Int = 3,
    val splashUrl: String = "",
    val splashTitle: String = "",
    val splashDesc: String = "",
    val noticeEnable: Boolean = false,
    val noticeTitle: String = "公告",
    val noticeContent: String = "",
    val noticeUrl: String = "",
    val noticeForce: Boolean = false,
    val noticeDailyLimit: Int = 0,
    val maintainEnable: Boolean = false,
    val maintainText: String = "",
    val maintainTemplate: String = "default",
    val serviceUrl: String = "",
    val agreement: String = "",
    val privacy: String = "",
    val agreement: String = "",
    val appSource: String = "all",
    val aboutEnable: Boolean = true,
    val aboutName: String = "",
    val aboutVersion: String = "",
    val aboutLogo: String = "",
    val aboutSlogan: String = "",
    val aboutDesc: String = "",
    val aboutCopyright: String = "",
    val aboutContact: String = "",
    val aboutWebsite: String = "",
    val aboutUpdateUrl: String = "",
    val aboutExtra: String = "",
    val aboutFooter: String = "",
    val shareTitle: String = "",
    val shareDesc: String = "",
    val shareUrl: String = "",
    val shareImage: String = "",
    val emailAllowDomains: String = "",
    val emailNonQqOn: Boolean = false,
    val regCodeOn: Boolean = true,
    val themePalette: String = "",
    val sfx: SfxConfig = SfxConfig(),
    val featureApiCache: Boolean = true,
    val homeBanner: Boolean = true,
    val homeNotice: Boolean = true,
    val uiRaw: String = ""
) {
    companion object {
        private fun b(v: Any?) = v == true || v == 1 || v == "1" || v == "true"
        private fun i(v: Any?) = v?.toString()?.toIntOrNull() ?: 0
        private fun s(v: Any?) = v?.toString() ?: ""

        fun fromJson(j: JSONObject): AppConfig {
            // ui_config 是嵌套 JSON 字符串（双层 JSON 坑：外层解析后内层还要 loads 一次）
            val uiRaw = s(j.opt("ui_config"))
            val ui = try {
                if (uiRaw.startsWith("{")) JSONObject(uiRaw) else JSONObject()
            } catch (_: Exception) { JSONObject() }
            val sfx = SfxConfig.fromJson(ui.optJSONObject("sfx"))
            val features = ui.optJSONObject("features") ?: JSONObject()

            return AppConfig(
                placard = s(j.opt("placard")),
                feedbackGroup = s(j.opt("feedback_group")),
                feedbackUser = s(j.opt("feedback_user")),
                feedbackGroupOn = if (j.has("feedback_group_on")) b(j.opt("feedback_group_on")) else true,
                feedbackUserOn = if (j.has("feedback_user_on")) b(j.opt("feedback_user_on")) else true,
                splashEnable = if (j.has("splash_enable")) b(j.opt("splash_enable")) else true,
                splashImage = s(j.opt("splash_image")),
                splashSeconds = i(j.opt("splash_seconds")).let { if (it <= 0) 3 else it },
                splashUrl = s(j.opt("splash_url")),
                splashTitle = s(j.opt("splash_title")),
                splashDesc = s(j.opt("splash_desc")),
                noticeEnable = b(j.opt("notice_enable")),
                noticeTitle = s(j.opt("notice_title")).ifEmpty { "公告" },
                noticeContent = s(j.opt("notice_content")),
                noticeUrl = s(j.opt("notice_url")),
                noticeForce = b(j.opt("notice_force")),
                noticeDailyLimit = i(j.opt("notice_daily_limit")),
                maintainEnable = b(j.opt("maintain_enable")),
                maintainText = s(j.opt("maintain_text")),
                maintainTemplate = s(j.opt("maintain_template")).ifEmpty { "default" },
                serviceUrl = s(j.opt("service_url")),
                agreement = s(j.opt("agreement")),
                privacy = s(j.opt("privacy")),
                agreement = s(j.opt("agreement")),
                appSource = s(j.opt("app_source")).ifEmpty { "all" },
                aboutEnable = if (j.has("about_enable")) b(j.opt("about_enable")) else true,
                aboutName = s(j.opt("about_name")),
                aboutVersion = s(j.opt("about_version")),
                aboutLogo = s(j.opt("about_logo")),
                aboutSlogan = s(j.opt("about_slogan")),
                aboutDesc = s(j.opt("about_desc")),
                aboutCopyright = s(j.opt("about_copyright")),
                aboutContact = s(j.opt("about_contact")),
                aboutWebsite = s(j.opt("about_website")),
                aboutUpdateUrl = s(j.opt("about_update_url")),
                aboutExtra = s(j.opt("about_extra")),
                aboutFooter = s(j.opt("about_footer")),
                shareTitle = s(j.opt("share_title")),
                shareDesc = s(j.opt("share_desc")),
                shareUrl = s(j.opt("share_url")),
                shareImage = s(j.opt("share_image")),
                emailAllowDomains = s(j.opt("email_allow_domains")),
                emailNonQqOn = b(j.opt("email_nonqq_on")),
                regCodeOn = if (j.has("reg_code_on")) b(j.opt("reg_code_on")) else true,
                themePalette = s(j.opt("theme_palette")),
                sfx = sfx,
                featureApiCache = if (ui.has("feature_api_cache")) b(ui.opt("feature_api_cache"))
                    else if (features.has("api_cache")) b(features.opt("api_cache")) else true,
                homeBanner = if (ui.has("banner")) b(ui.opt("banner")) else true,
                homeNotice = if (ui.has("notice")) b(ui.opt("notice")) else true,
                uiRaw = uiRaw
            )
        }
    }
}
