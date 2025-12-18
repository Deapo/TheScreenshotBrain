package com.example.thescreenshotbrain.core.common

import android.util.Log

object VietQrParser {

    private val BANK_BIN_MAP = mapOf(
        "970407" to "Techcombank", "970422" to "MBBank", "970415" to "VietinBank",
        "970418" to "BIDV", "970405" to "Agribank", "970436" to "Vietcombank",
        "970423" to "TPBank", "970432" to "VPBank", "970403" to "Sacombank",
        "970441" to "VIB", "970443" to "SHB", "970416" to "ACB",
        "970454" to "VietCapital", "970428" to "NamABank", "970406" to "DongA Bank",
        "970412" to "PVcomBank", "970400" to "SaigonBank", "970437" to "HDBank",
        "970468" to "SeABank", "970433" to "VietBank", "970452" to "KienLongBank",
        "970455" to "PG Bank", "970449" to "LienVietPostBank", "970425" to "AnBinhBank",
        "970424" to "Shinhan Bank", "970431" to "Eximbank", "970414" to "OceanBank",
        "970408" to "GPBank", "970439" to "Public Bank", "970429" to "SCB",
        "970458" to "United Overseas", "970410" to "Standard Chartered", "970419" to "NCB"
    )

    fun extractBankInfo(qrContent: String): String? {
        try {
            // BƯỚC 1: VỆ SINH TUYỆT ĐỐI
            // Xóa tất cả khoảng trắng, xuống dòng, tab... để chuỗi liền mạch
            val cleanContent = qrContent.replace("\\s+".toRegex(), "")
            Log.d("VietQrParser", "Raw QR Content: $cleanContent")

            // BƯỚC 2: Parse toàn bộ
            val rootTags = parseTlvToMap(cleanContent)

            // Log xem tìm thấy những tag nào (Để debug)
            Log.d("VietQrParser", "Found Tags: $rootTags")

            // BƯỚC 3: Lấy dữ liệu
            // Thử lấy Tag 59 (Tên chủ thẻ)
            val ownerName = rootTags["59"]?.uppercase() ?: "CHỦ TÀI KHOẢN"

            // Lấy thông tin Bank (Tag 38 -> Tag 01 -> Tag 00/01)
            val tag38Content = rootTags["38"] ?: return null
            val beneficiaryTags = parseTlvToMap(tag38Content) // Cấp 2

            // Một số QR lồng nhau 2 cấp, một số 3 cấp. Kiểm tra kỹ:
            // Chuẩn VietQR: 38 -> 01 (Beneficiary Org) -> 00(BIN) & 01(STK)
            var finalTags = beneficiaryTags
            if (beneficiaryTags.containsKey("01")) {
                val tag01Content = beneficiaryTags["01"]!!
                // Kiểm tra xem value của tag 01 có phải là TLV không hay là STK luôn?
                // Nếu nó bắt đầu bằng "00" (BIN tag) và độ dài hợp lý thì parse tiếp
                if (tag01Content.startsWith("00") && tag01Content.length > 6) {
                    finalTags = parseTlvToMap(tag01Content) // Cấp 3
                }
            }

            val accountNumber = finalTags["01"] ?: return null
            val binCode = finalTags["00"]

            val bankName = BANK_BIN_MAP[binCode] ?: "Ngân hàng ($binCode)"

            return "$bankName ($ownerName)\n$accountNumber"

        } catch (e: Exception) {
            Log.e("VietQrParser", "Lỗi Critical: ${e.message}")
            return null
        }
    }

    private fun parseTlvToMap(input: String): Map<String, String> {
        val tags = mutableMapOf<String, String>()
        var index = 0

        while (index < input.length) {
            try {
                if (index + 4 > input.length) break

                val id = input.substring(index, index + 2)
                val lenStr = input.substring(index + 2, index + 4)
                val length = lenStr.toIntOrNull() ?: break

                if (index + 4 + length > input.length) break

                val value = input.substring(index + 4, index + 4 + length)
                tags[id] = value

                index += 4 + length
            } catch (e: Exception) {
                break
            }
        }
        return tags
    }
}