package com.soft.anyi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 支付仓库（对应 PayService）：会员方案 / 充值下单 / 轮询支付状态。
 */
object PayRepo {
    data class Plan(
        val id: Int, val name: String, val price: String,
        val days: Int, val desc: String, val tag: String
    ) {
        companion object {
            fun listFrom(d: JSONObject): List<Plan> {
                val arr = d.optJSONArray("plans") ?: d.optJSONArray("list")
                    ?: (d.opt("data") as? org.json.JSONArray) ?: return emptyList()
                return (0 until arr.length()).mapNotNull { n ->
                    val o = arr.optJSONObject(n) ?: return@mapNotNull null
                    Plan(
                        o.optInt("id"), o.optString("name"), o.optString("price"),
                        o.optInt("days"), o.optString("desc", ""), o.optString("tag", "")
                    )
                }
            }
        }
    }

    data class Order(val outTradeNo: String, val payUrl: String)

    suspend fun rechargePlans(): List<Plan> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/pay/recharge_plans")
        Plan.listFrom(d)
    }

    suspend fun vipPlans(): List<Plan> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/pay/plans")
        Plan.listFrom(d)
    }

    suspend fun plans(): Pair<List<Plan>, List<Plan>> = withContext(Dispatchers.IO) {
        val vip = vipPlans()
        val rec = rechargePlans()
        Pair(vip, rec)
    }

    /** 创建支付订单，返回跳转链接 */
    suspend fun create(planId: Int, payType: String = "wxpay"): Order = withContext(Dispatchers.IO) {
        val d = Http.post(
            "/api/softlib/pay/create",
            mapOf("plan_id" to planId, "type" to payType)
        )
        Order(
            d.optString("out_trade_no").ifEmpty { d.optString("out_trade_no".substringBefore('_')) },
            d.optString("pay_url").ifEmpty { d.optString("url").ifEmpty { d.optString("qrcode") } }
        )
    }

    suspend fun recharge(amount: String, payType: String = "wxpay"): Order = withContext(Dispatchers.IO) {
        val d = Http.post(
            "/api/softlib/pay/recharge",
            mapOf("money" to amount, "type" to payType)
        )
        Order(d.optString("out_trade_no"), d.optString("pay_url").ifEmpty { d.optString("url") })
    }

    suspend fun isPaid(outTradeNo: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val d = Http.post("/api/softlib/pay/status", mapOf("out_trade_no" to outTradeNo))
            d.optInt("status", 0) == 1 || d.optBoolean("paid")
        } catch (_: Exception) { false }
    }

    /** 轮询直到支付成功或超时 */
    suspend fun waitPaid(outTradeNo: String, timeoutSec: Int = 120, onTick: (Int) -> Unit = {}): Boolean =
        withContext(Dispatchers.IO) {
            val deadline = System.currentTimeMillis() + timeoutSec * 1000L
            var n = 0
            while (System.currentTimeMillis() < deadline) {
                if (isPaid(outTradeNo)) return@withContext true
                onTick(++n)
                kotlinx.coroutines.delay(3000)
            }
            false
        }
}
