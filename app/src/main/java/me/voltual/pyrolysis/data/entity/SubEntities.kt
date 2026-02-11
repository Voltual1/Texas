package me.voltual.pyrolysis.data.entity

import android.content.Context
import android.content.pm.PermissionGroupInfo
import android.content.pm.PermissionInfo
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
/*import com.machiav3lli.fdroid.FILTER_CATEGORY_ALL
import com.machiav3lli.fdroid.R
import com.machiav3lli.fdroid.data.content.Preferences
import com.machiav3lli.fdroid.data.database.entity.Release
import com.machiav3lli.fdroid.ui.compose.icons.Icon
import com.machiav3lli.fdroid.ui.compose.icons.Phosphor
import com.machiav3lli.fdroid.ui.compose.icons.icon.IcDonateLiberapay
import com.machiav3lli.fdroid.ui.compose.icons.icon.IcDonateLitecoin
import com.machiav3lli.fdroid.ui.compose.icons.icon.IcDonateOpencollective
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ApplePodcastsLogo
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ArrowSquareOut
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Asterisk
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Barbell
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.BookBookmark
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Books
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Brain
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Browser
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Calendar
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Cardholder
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Chat
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Chats
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CheckCircle
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CheckSquare
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ChefHat
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CircleWavyWarning
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CirclesFour
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CirclesThreePlus
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Clock
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CloudArrowDown
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CloudSun
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Code
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Command
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Compass
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CurrencyBTC
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.CurrencyDollarSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Download
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Envelope
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.GameController
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Ghost
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Globe
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.GlobeSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Graph
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.HeartStraight
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.HeartStraightFill
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.House
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Image
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Images
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Key
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Keyboard
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Leaf
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.MapPin
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.MathOperations
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Metronome
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Microphone
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Newspaper
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.NotePencil
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Nut
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.PaintBrush
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Password
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.PenNib
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Phone
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Pizza
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.PlayCircle
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Robot
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.RssSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Scales
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ScribbleLoop
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShareNetwork
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShieldCheck
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShieldStar
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.ShoppingCart
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.SlidersHorizontal
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Storefront
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Swatches
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.TelevisionSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.TrainSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Translate
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.TrashSimple
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.VideoConference
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Wallet
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.WifiHigh
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.Wrench
import com.machiav3lli.fdroid.ui.compose.icons.phosphor.X*/
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Author(val name: String = "", val email: String = "", val web: String = "") {
    fun toJSON() = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Author>(json)
    }
}

@Serializable
sealed class Donate {
    @Serializable
    data class Regular(val url: String) : Donate()

    @Serializable
    data class Bitcoin(val address: String) : Donate()

    @Serializable
    data class Litecoin(val address: String) : Donate()

    @Serializable
    data class Liberapay(val id: String) : Donate()

    @Serializable
    data class OpenCollective(val id: String) : Donate()

    fun toJSON() = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Donate>(json)
    }
}