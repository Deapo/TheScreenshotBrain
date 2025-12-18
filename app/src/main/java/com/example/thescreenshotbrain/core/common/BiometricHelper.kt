package com.example.thescreenshotbrain.core.common

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    /**
     * Kiểm tra xem máy có hỗ trợ vân tay/FaceID không
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Hiển thị hộp thoại yêu cầu quét vân tay
     * @param activity: Màn hình đang đứng (phải là FragmentActivity hoặc AppCompatActivity)
     * @param onSuccess: Hàm sẽ chạy khi quét đúng
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: () -> Unit = {}
    ) {
        // 1. Cấu hình Executor (chạy trên Main Thread)
        val executor = ContextCompat.getMainExecutor(activity)

        // 2. Tạo Callback lắng nghe kết quả
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // QUÉT THÀNH CÔNG -> Chạy hàm onSuccess
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Lỗi (hoặc user bấm hủy)
                Toast.makeText(activity, "Xác thực thất bại: $errString", Toast.LENGTH_SHORT).show()
                onError()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Quét sai vân tay (cho quét lại)
                Toast.makeText(activity, "Vân tay không khớp, thử lại!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Khởi tạo BiometricPrompt
        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        // 4. Cấu hình giao diện hộp thoại
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Bảo mật Screenshot Brain")
            .setSubtitle("Xác thực để xem thông tin Ngân hàng")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) // Cho phép dùng cả Mã PIN máy nếu vân tay lỗi
            .build()

        // 5. HIỆN LÊN
        biometricPrompt.authenticate(promptInfo)
    }
}