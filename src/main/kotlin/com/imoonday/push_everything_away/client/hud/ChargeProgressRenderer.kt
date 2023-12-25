package com.imoonday.push_everything_away.client.hud

import com.imoonday.push_everything_away.config.ClientConfig
import com.imoonday.push_everything_away.config.Config
import com.imoonday.push_everything_away.utils.ChargeManager
import com.imoonday.push_everything_away.utils.PushableObject
import com.imoonday.push_everything_away.utils.PushableObject.Companion.getMaxAttribute
import me.x150.renderer.util.RendererUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import java.awt.Color

@Environment(EnvType.CLIENT)
class ChargeProgressRenderer(private val client: MinecraftClient) {
    fun render(context: DrawContext) {
        val window = client.window
        val player = client.player ?: return
        val chargingTimes = ChargeManager.chargingTimes
        if (chargingTimes <= 0) {
            return
        }
        val speed = player.mainHandStack.getMaxAttribute { obj: PushableObject, level: Int -> obj.getPushSpeed(level) }
            .toFloat()
        val chargeTime = Config.instance.enchantmentSettings.chargeTime
        val progress = (chargingTimes * (if (ChargeManager.cooling) 1.0f else speed) / chargeTime).coerceIn(0.0f, 1.0f)
        val scaledWidth = window.scaledWidth
        val scaledHeight = window.scaledHeight
        val settings = ClientConfig.instance.chargeProgressBarSettings
        val offset = settings.offset
        val borderColor: Color
        val progressColor: Color
        val backgroundColor = Color.DARK_GRAY
        if (ChargeManager.cooling) {
            borderColor = Color.BLACK
            progressColor = Color.WHITE
        } else {
            borderColor = Color.WHITE
            progressColor = RendererUtils.lerp(Color.GREEN, Color.RED, progress.toDouble())
        }
        val size = settings.size
        val width = size.x
        val height = size.y
        val x = scaledWidth / 2 - width / 2 + offset.x
        val y = scaledHeight / 2 - 16 - height / 2 + offset.y
        context.drawBorder(x, y, width, height, borderColor.rgb)
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor.rgb)
        context.fill(x + 1, y + 1, x + ((width - 2) * progress).toInt() + 1, y + height - 1, progressColor.rgb)
    }
}
